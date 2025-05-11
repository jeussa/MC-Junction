package com.jeussa.mc.junction.core.socket;

import com.jeussa.mc.junction.core.JMCapi;
import com.jeussa.mc.junction.core.JMCbridge;
import com.jeussa.mc.junction.JunctionPacket;
import com.jeussa.mc.junction.core.util.AsyncRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.function.Consumer;
import java.util.logging.Level;

public class JMCsocketbridge extends AsyncRunnable<byte[], byte[]> implements JMCbridge{

    private static final int MAX_PACKET_SIZE = 65536;   // 64 kB
    private static final int RSA_KEY_BITS = 4096;
    private static final int MAX_CHUNK_SIZE = (JMCsocketbridge.RSA_KEY_BITS >> 3) - 11;

    private static final int CMD_CLOSE = 1;

    /**
     * NOTE: avoid thread racing conditions, don't mess too much with the local arguments!
     */

    private final Socket handle;
    private final InputStream input;
    private final OutputStream output;

    private final KeyPair localKeys;
    private PublicKey remoteKey;

    private Consumer<@NotNull JunctionPacket> callback = null;


    public JMCsocketbridge(Socket handle, boolean doEncrypt)throws IOException{
        this.handle = handle;
        this.input = this.handle.getInputStream();
        this.output = this.handle.getOutputStream();

        // Generate RSA keypair (if needed)
        this.remoteKey = null;
        if(doEncrypt)this.localKeys = null;
        else{
            try{
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                gen.initialize(new RSAKeyGenParameterSpec(4096, RSAKeyGenParameterSpec.F4));
                this.localKeys = gen.generateKeyPair();
            }catch(NoSuchAlgorithmException | InvalidAlgorithmParameterException e){
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * Callback
     */
    @Override
    public void setCallback(@Nullable Consumer<@NotNull JunctionPacket> callback){
        this.callback = callback;
    }


    /**
     * Poll
     */
    @Override
    public void poll(){
        byte[] bytes;
        while((bytes = super.sync_receive()) != null){
            if(this.callback != null){
                var packet = JMCapi.getInstance().getPacketFromBytes(bytes);
                if(packet != null)try{
                    this.callback.accept(packet);
                }catch(Exception e){
                    JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, "An error occurred whilst running the callback!", e);
                }
            }
        }
    }


    /**
     * Alive
     */
    @Override
    public boolean isAlive(){ return !(super.getThreadState() == State.DIEING || super.getThreadState() == State.DEAD); }


    /**
     * Send
     */
    public void send(byte @NotNull [] packet){
        if(packet.length > JMCsocketbridge.MAX_PACKET_SIZE)throw new RuntimeException(new IOException("Packet size exceeds limit: " + packet.length + " > " + JMCsocketbridge.MAX_PACKET_SIZE + "  bytes"));
        super.sync_send(packet);
    }
    @Override
    public boolean send(@NotNull JunctionPacket packet){
        var bytes = JMCapi.getInstance().getBytesFromPacket(packet);
        if(bytes == null)return false;
        this.send(bytes);
        return true;
    }


    /**
     * Close
     */
    @Override
    public void close(){ super.stopThread(); }


    /**
     * Abstract stuff
     */
    @Override
    protected void async_start(){
        try{
            // Share local public key
            if(this.localKeys == null)this.util_writeInt(0);
            else{
                byte[] bytes = this.localKeys.getPublic().getEncoded();
                this.util_writeInt(bytes.length);
                this.output.write(bytes);
            }

            // Read remote public key
            int size = this.util_readInt();
            this.remoteKey = size == 0 ? null : KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(this.input.readNBytes(size)));
        }catch(IOException | NoSuchAlgorithmException | InvalidKeySpecException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean async_poll(){
        if(this.handle.isClosed())return false;

        try{
            // Submit queue
            byte[] packet;
            while((packet = super.async_receive()) != null){
                this.util_writeInt(packet.length);
                if(this.remoteKey == null)this.output.write(packet);
                else{
                    Cipher cipher = Cipher.getInstance("RSA");
                    cipher.init(Cipher.ENCRYPT_MODE, this.remoteKey);
                    for(int i=0; i<packet.length; i+=JMCsocketbridge.MAX_CHUNK_SIZE){
                        this.output.write(cipher.doFinal(packet, i, Math.min(packet.length, i + JMCsocketbridge.MAX_CHUNK_SIZE) - i));
                    }
                }
            }

            // Receive data
            while(this.input.available() != 0){
                int size = this.util_readInt();
                if(size > JMCsocketbridge.MAX_PACKET_SIZE)throw new IOException("Packet size exceeds limit: " + size + " > " + JMCsocketbridge.MAX_PACKET_SIZE);
                else if(size == 0){
                    int cmd;
                    while((cmd = this.util_readInt()) == 0)continue;
                    switch(cmd){
                        case JMCsocketbridge.CMD_CLOSE -> {
                            this.handle.close();
                            return false;
                        }
                        default -> {}
                    }
                }

                if(this.localKeys == null)packet = this.input.readNBytes(size);
                else{
                    packet = new byte[size];
                    Cipher cipher = Cipher.getInstance("RSA");
                    cipher.init(Cipher.DECRYPT_MODE, this.localKeys.getPrivate());
                    for(int i=0; i<size; i+=JMCsocketbridge.MAX_CHUNK_SIZE){
                        System.arraycopy(cipher.doFinal(this.input.readNBytes(JMCsocketbridge.RSA_KEY_BITS >> 3)), 0, packet, i, Math.min(packet.length, i + JMCsocketbridge.MAX_CHUNK_SIZE) - i);
                    }
                }
                super.async_send(packet);
            }

            // Flush
            this.output.flush();

            return true;
        }catch(IOException | GeneralSecurityException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void async_stop(){
        if(!this.handle.isClosed())try{
            this.util_writeInt(0);
            this.util_writeInt(JMCsocketbridge.CMD_CLOSE);
            this.output.flush();
            this.handle.close();
        }catch(IOException e){
            e.printStackTrace(System.err);
        }
    }


    /**
     * Utilities
     */
    private void util_writeInt(int value)throws IOException{
        for(int i=3; i>=0; i--)this.output.write((value >>> (i * 8)) & 0xFF);
    }
    private int util_readInt()throws IOException{
        int value = 0, b;
        for(int i=3; i>=0; i--){
            b = this.input.read();
            if(b == -1)throw new EOFException("Unexpected end of stream");
            value |= (b & 0xFF) << (i * 8);
        }
        return value;
    }
}
