package com.jeussa.mc.junction.core.socket;

import com.jeussa.mc.junction.core.JMCapi;
import com.jeussa.mc.junction.core.JMCplugin;
import com.jeussa.mc.junction.core.JMCserver;
import com.jeussa.mc.junction.core.util.AsyncRunnable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.logging.Level;

public class JMCsocketserver extends AsyncRunnable<Void, JMCsocketbridge> implements JMCserver{

    private final boolean doEncrypt;
    private final ServerSocket handle;


    public JMCsocketserver(int port, boolean doEncrypt)throws IOException{
        try{
            this.doEncrypt = doEncrypt;
            this.handle = new ServerSocket(port);
        }catch(Exception e){
            this.close();
            throw e;
        }
    }


    /**
     * Accept
     */
    @Override
    public @Nullable JMCsocketbridge accept(){ return super.sync_receive(); }


    /**
     * Alive
     */
    @Override
    public boolean isAlive(){ return !(super.getThreadState() == State.DIEING || super.getThreadState() == State.DEAD); }


    /**
     * Close
     */
    @Override
    public void close(){ super.stopThread(); }


    /**
     * Abstract stuff
     */
    @Override
    protected boolean async_poll(){
        if(this.handle == null)return false;
        try{
            this.async_send(new JMCsocketbridge(this.handle.accept(), this.doEncrypt));
        }catch(SocketTimeoutException _){
        }catch(Exception e){
            JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, "An error occurred whilst polling for incoming connections.", e);
            return false;
        }
        return true;
    }

    @Override
    protected void async_stop(){
        try{
            if(this.handle != null)this.handle.close();
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }
}
