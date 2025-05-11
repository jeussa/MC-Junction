package com.jeussa.mc.junction.core.socket;

import com.jeussa.mc.junction.core.JMCclient;
import com.jeussa.mc.junction.core.JMCplugin;
import com.jeussa.mc.junction.core.util.AsyncRunnable;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

public class JMCsocketclient extends AsyncRunnable<Void, JMCsocketbridge> implements JMCclient{

    private final String host;
    private final int port;
    private final boolean doEncrypt;


    public JMCsocketclient(String host, int port, boolean doEncrypt){
        this.host = host;
        this.port = port;
        this.doEncrypt = doEncrypt;
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
     * Abstract stuff
     */
    @Override
    protected boolean async_poll(){
        try{
            this.async_send(new JMCsocketbridge(new Socket(this.host, this.port), this.doEncrypt));
            return false;
        }catch(ConnectException _){
            return false;
        }catch(IOException e){
            e.printStackTrace(System.err);
            return false;
        }
    }
}
