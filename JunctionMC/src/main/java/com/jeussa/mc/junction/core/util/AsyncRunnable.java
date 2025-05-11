package com.jeussa.mc.junction.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.util.concurrent.*;

public abstract class AsyncRunnable<toWorker, toMain>{

    /**
     * Generic async task runner with optional communication.
     * Communication directions:
     *  - toWorker: message from main thread to worker (set to Void.class to disable)
     *  - toMain: message from worker to main thread (set to Void.class to disable)
     *  Timeout behaviour:
     *   - Call either ::sync_send, ::sync_receive, or ::poll at least every [timeout] ms to avoid timeout
     *   - ::sync_send and ::sync_receive require toWorker and toMain to not be Void.class respectively
     */

    public enum State{ NULL, STARTING, RUNNING, DIEING, DEAD }


    private final BlockingQueue<toWorker> q_toWorker;       // Queue main -> worker
    private final BlockingQueue<toMain> q_toMain;           // Queue worker -> main
    private final ScheduledExecutorService worker;
    private ScheduledFuture<?> future = null;

    private volatile State state = State.NULL;

    private final long tickMillis = 1000 / 60;              // How many MS per cycle
    private volatile long timeout = 10000L;
    private volatile long lastUpdate;


    public AsyncRunnable(){
        // Resolve class types
        var types = ((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments();

        // Initialise queues
        this.q_toWorker = Void.class.equals(types[0]) ? null : new LinkedBlockingQueue<>();
        this.q_toMain = Void.class.equals(types[1]) ? null : new LinkedBlockingQueue<>();

        // Start thread
        this.worker = Executors.newSingleThreadScheduledExecutor();
        this.future = this.worker.scheduleAtFixedRate(()->{
            try{
                switch(this.state){
                    case State.NULL -> {
                        this.state = State.STARTING;
                        this.async_start();
                        this.lastUpdate = System.currentTimeMillis();
                        this.state = State.RUNNING;
                    }
                    case State.RUNNING -> {
                        if(!this.async_poll() || System.currentTimeMillis() - this.lastUpdate > this.timeout)this.state = State.DIEING;
                    }
                    case State.DIEING -> {
                        this.async_poll();      // One last poll
                        this.async_stop();
                        this.state = State.DEAD;
                        this.future.cancel(false);
                        this.worker.shutdown();
                    }
                }
            }catch(Exception e){
                e.printStackTrace(System.err);
                this.state = State.DIEING;
            }
        }, 0, this.tickMillis, TimeUnit.MILLISECONDS);
    }


    public void stopThread(){
        if(this.state == State.STARTING || this.state == State.RUNNING)this.state = State.DIEING;
    }

    public void setThreadTimeout(long timeout){
        this.timeout = timeout;
    }

    public void pollThread(){
        this.lastUpdate = System.currentTimeMillis();
    }

    public State getThreadState(){
        return this.state;
    }


    /**
     * Sync
     */
    protected void sync_send(@NotNull toWorker toWorker){
        if(this.q_toWorker == null)throw new UnsupportedOperationException("Cannot sync_send when toWorker type is Void");
        if(this.state == State.DIEING || this.state == State.DEAD)throw new IllegalStateException("Cannot send to a dieing or dead thread!");
        this.lastUpdate = System.currentTimeMillis();
        try{
            this.q_toWorker.put(toWorker);
        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }
    }
    @Nullable
    protected toMain sync_receive(){
        if(this.q_toMain == null)throw new UnsupportedOperationException("Cannot sync_receive when toMain type is Void");
        this.lastUpdate = System.currentTimeMillis();
        try{
            return this.q_toMain.isEmpty() ? null : this.q_toMain.take();
        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }
    }


    /**
     * Async
     */
    protected void async_start(){}
    protected abstract boolean async_poll();
    protected void async_stop(){}

    protected void async_send(@NotNull toMain toMain){
        if(this.q_toMain == null)throw new UnsupportedOperationException("Cannot async_send when toMain type is Void");
        try{
            this.q_toMain.put(toMain);
        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }
    }
    @Nullable
    protected toWorker async_receive(){
        if(this.q_toWorker == null)throw new UnsupportedOperationException("Cannot async_receive when toWorker type is Void");
        try{
            return this.q_toWorker.isEmpty() ? null : this.q_toWorker.take();
        }catch(InterruptedException e){
            throw new RuntimeException(e);
        }
    }
}
