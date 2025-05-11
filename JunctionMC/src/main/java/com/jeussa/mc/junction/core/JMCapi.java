package com.jeussa.mc.junction.core;

import com.jeussa.mc.junction.ConfigProvider;
import com.jeussa.mc.junction.JunctionAPI;
import com.jeussa.mc.junction.JunctionBridge;
import com.jeussa.mc.junction.JunctionPacket;
import com.jeussa.mc.junction.core.network.JMCnetwork;
import com.jeussa.mc.junction.core.socket.JMCsocketclient;
import com.jeussa.mc.junction.core.socket.JMCsocketserver;
import com.jeussa.mc.junction.network.JunctionNetwork;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

public class JMCapi implements JunctionAPI{

    private static JMCapi instance;
    public static JMCapi getInstance(){ return JMCapi.instance; }


    private record PendingServer(@NotNull JMCserver server, @Nullable Consumer<@NotNull JunctionBridge> callback){}
    private record PendingClient(@NotNull JMCclient client, @Nullable Consumer<@Nullable JunctionBridge> callback){}


    private final JMCplugin plugin;

    private final ConfigProvider config;

    private final List<PendingServer> servers;
    private final List<PendingClient> clients;
    private final List<JMCbridge> bridges;

    private JMCnetwork networkManager;


    public JMCapi(JMCplugin plugin){
        if(JMCapi.instance != null)throw new IllegalStateException("JMCapi was already initialised!");
        JMCapi.instance = this;

        this.plugin = plugin;

        this.config = plugin.getConfigurationFile("config.yml");

        this.servers = new ArrayList<>();
        this.clients = new ArrayList<>();
        this.bridges = new ArrayList<>();

        this.networkManager = null;
    }


    /**
     * Plugin
     */
    public JMCplugin getPlugin(){
        return this.plugin;
    }


    /**
     * Debug
     */
    public boolean isDebug(){
        return this.config.getBoolean("debug", false);
    }


    /**
     * Network
     */
    @Override
    public @NotNull JunctionNetwork getNetworkManager(){
        if(this.networkManager == null){
            var network_yml = this.plugin.getConfigurationFile("network.yml");
            if(network_yml == null)throw new RuntimeException(new FileNotFoundException("Failed to load network.yml !"));

            var local_yml = this.plugin.getConfigurationFile("local.yml");
            if(local_yml == null)throw new RuntimeException(new FileNotFoundException("Failed to load local.yml !"));

            this.networkManager = new JMCnetwork(network_yml, local_yml);
//            this.networkManager.start();
        }
        return this.networkManager;
    }


    /**
     * Poll
     */
    public void poll(){

        // Poll servers
        this.servers.removeIf(server -> {
            JMCbridge result;
            while((result = server.server.accept()) != null){
                this.bridges.add(result);
                if(server.callback != null)try{
                    server.callback.accept(result);
                }catch(Exception e){
                    this.plugin.getLogger().log(Level.WARNING, "An error occurred whilst running the callback!", e);
                }
            }
            return !server.server.isAlive();    // Check alive at end not start to flush any remaining packets
        });

        // Poll clients
        this.clients.removeIf(client -> {
            JMCbridge result = client.client.accept();
            if(result != null)this.bridges.add(result);
            if(client.callback != null && (result != null || !client.client.isAlive())){
                try{
                    client.callback.accept(result);
                }catch(Exception e){
                    this.plugin.getLogger().log(Level.WARNING, "An error occurred whilst running the callback!", e);
                }
                return true;
            }
            return false;
        });

        // Poll bridges
        this.bridges.removeIf(bridge -> {
            if(!bridge.isAlive())return true;
            bridge.poll();
            return false;
        });

        // Poll network
        if(this.networkManager != null)this.networkManager.poll();
    }


    /**
     * onEnable
     */
    public void onEnable(){
        if(this.config.getBoolean("network-enable", false))this.getNetworkManager().start();
    }


    /**
     * onDisable
     */
    public void onDisable(){
        if(this.networkManager != null)this.networkManager.stop();

        this.poll();        // One last poll

        for(var server : this.servers)server.server.close();
        for(var bridge : this.bridges)bridge.close();

        this.servers.clear();
        this.bridges.clear();
        this.clients.clear();
    }


    /**
     * Socket
     */
    // = Server =
    @Override
    public JMCsocketserver createSocketServer(int port, boolean doEncrypt, @Nullable Consumer<@NotNull JunctionBridge> callback, @NotNull Consumer<Exception> onFail){
        try{
            var server = new JMCsocketserver(port, doEncrypt);
            this.servers.add(new PendingServer(server, callback));
            return server;
        }catch(Exception e){
            onFail.accept(e);
        }
        return null;
    }
    // = Client =
    @Override
    public void connectToSocketServer(@NotNull String host, int port, boolean doEncrypt, @Nullable Consumer<@Nullable JunctionBridge> callback){
        this.clients.add(new PendingClient(new JMCsocketclient(host, port, doEncrypt), callback));
    }


    /**
     * Packet
     */
    // = Read =
    @SuppressWarnings("unchecked")
    public @Nullable JunctionPacket getPacketFromBytes(byte @NotNull [] bytes){
        ByteArrayInputStream bin = null;
        DataInputStream din = null;

        try{
            din = new DataInputStream(bin = new ByteArrayInputStream(bytes));
            var clazz = (Class<? extends JunctionPacket>)Class.forName(din.readUTF());
            var object = clazz.getDeclaredConstructor().newInstance();
            object.read(din);
            return object;
        }catch(Exception e){
            this.plugin.getLogger().log(Level.WARNING, "Failed to read packet from bytes!", e);
            return null;
        }finally{
            if(din != null)try{din.close();}catch(Exception _){}
            if(bin != null)try{bin.close();}catch(Exception _){}
        }
    }
    // = Write =
    public byte @Nullable [] getBytesFromPacket(JunctionPacket packet){
        ByteArrayOutputStream bout = null;
        DataOutputStream dout = null;

        try{
            dout = new DataOutputStream(bout = new ByteArrayOutputStream());
            dout.writeUTF(packet.getClass().getName());
            packet.write(dout);
            return bout.toByteArray();
        }catch(Exception e){
            this.plugin.getLogger().log(Level.WARNING, "Failed to write packet to bytes!", e);
            return null;
        }finally{
            if(dout != null)try{dout.close();}catch(Exception _){}
            if(bout != null)try{bout.close();}catch(Exception _){}
        }
    }


    /**
     * Status
     */
    @Override public int getNumPendingClients(){ return this.clients.size(); }
    @Override public int getNumPendingServers(){ return this.servers.size(); }
    @Override public int getNumBridges(){ return this.bridges.size(); }
}
