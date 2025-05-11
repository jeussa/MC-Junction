package com.jeussa.mc.junction.core.network;

import com.jeussa.mc.junction.JunctionBridge;
import com.jeussa.mc.junction.JunctionPacket;
import com.jeussa.mc.junction.JunctionServer;
import com.jeussa.mc.junction.core.JMCapi;
import com.jeussa.mc.junction.network.JunctionNetworkPacketEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;

public class JMCnetworker{

    private record Connection(JMCnetworkserver server, JunctionBridge bridge){}
    private static class Timeout{

        private final String failMsg;
        private long endAt;
        private int attempts;

        public Timeout(String failMsg){
            this.failMsg = failMsg;
            this.reset();
        }

        public void reset(){
            this.endAt = 0L;
            this.attempts = 0;
        }

        public boolean ready(){
            return System.currentTimeMillis() > this.endAt;
        }

        public void fail(@Nullable Exception e){
            this.attempts++;
            int timeoutS;

            if(this.attempts < 5)timeoutS = 10;
            else if(this.attempts < 10)timeoutS = 30;
            else timeoutS = 60;

            if(JMCapi.getInstance().isDebug()){
                if(this.attempts < 10){
                    JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, this.failMsg, this.attempts <= 1 ? e : null);
                    JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, "Retrying in " + timeoutS + " seconds...");
                }else if(this.attempts == 10){
                    JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, this.failMsg);
                    JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, "Retrying silently every " + timeoutS + " seconds...");
                }
            }

            this.endAt = System.currentTimeMillis() + 1000L * timeoutS;
        }
        public void fail(){
            this.fail(null);
        }
    }


    private final @NotNull JMCnetwork network;

    private Connection con_parent = null;
    private boolean con_parent_pending = false;
    private final Timeout con_parent_timeout = new Timeout("Failed to connect to network parent!");

    private List<Connection> con_children = null;

    private JunctionServer poll_server = null;
    private final Timeout poll_server_timeout = new Timeout("Failed to start network server to receive downstream connections!");

    private @Nullable Consumer<@NotNull JunctionNetworkPacketEvent> callback = null;


    public JMCnetworker(@NotNull JMCnetwork network){
        this.network = network;
    }


    /**
     * Broadcast
     */
    public void broadcast(@NotNull JunctionPacket packet){
        this.handle(new JMCnetworkpacket.Broadcast(packet), null);
    }


    /**
     * Callback
     */
    public void setCallback(@Nullable Consumer<@NotNull JunctionNetworkPacketEvent> callback){
        this.callback = callback;
    }


    /**
     * Close
     */
    public void close(){
        this.poll();    // Final poll

        if(this.poll_server != null){
            this.poll_server.close();
            this.poll_server = null;
            this.poll_server_timeout.reset();
        }

        if(this.con_children != null){
            for(var con : this.con_children)if(con.bridge.isAlive())con.bridge.close();
            this.con_children = null;
        }

        if(this.con_parent != null && this.con_parent.bridge.isAlive()){
            this.con_parent.bridge.close();
            this.con_parent = null;
            this.con_parent_timeout.reset();
        }
    }


    /**
     * Handle
     */
    private void handle(@NotNull JMCnetworkpacket packet, @Nullable JMCnetworkserver from){

        // Update lastPing for all servers in path (we know they are alive now)
        for(String s : packet.path)Objects.requireNonNull(this.network.getServerByName(s)).lastPing = System.currentTimeMillis();

        // If directed, make sure we only go down stream if the target exists there
        if(!((packet instanceof JMCnetworkpacket.Directed dp) && dp.target.equalsIgnoreCase(this.network.getLocalServer().getName()))){

            // Share packet downward, except to sender
            if(this.con_children != null)for(var con : this.con_children){
                if(con.server.equals(from))continue;
                if(packet instanceof JMCnetworkpacket.Directed dp){
                    if(!con.server.hasChild(dp.target))continue;
                    con.bridge.send(dp);
                    return;     // Once the directed packet has been sent, we can stop the rest of this function
                }
                con.bridge.send(packet);
            }

            // Share packet upward, unless parent is sender
            if(this.con_parent != null && !this.con_parent.server.equals(from)) this.con_parent.bridge.send(packet);
        }

        // Handle locally
        if(from != null){
            if(this.callback != null){
                try{
                    this.callback.accept(new JMCnetworkpacketevent(packet));
                }catch(Exception e){
                    JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, "An error occurred whilst handling an incoming packet through the callback!", e);
                }
            }
        }
    }


    /**
     * Poll
     */
    public void poll(){
        var local = this.network.getLocalServer();

        // = Upward =
        var parent = local.getParent();
        if(parent != null){
            if(this.con_parent != null && !this.con_parent.bridge.isAlive()){
                JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, "Connection lost with parent! Retrying in 30 seconds...");
                this.con_parent_timeout.endAt = System.currentTimeMillis() + 30000L;
                this.con_parent = null;
            }
            else if(this.con_parent == null){
                if(!this.con_parent_pending){
                    if(this.con_parent_timeout.ready()){
                        this.con_parent_pending = true;
                        JMCapi.getInstance().connectToSocketServer(
                            local.livesWithParent() ? "127.0.0.1" : Objects.requireNonNull(parent.ap_host),
                            parent.ap_port,
                            true,
                            b -> {
                                this.con_parent_pending = false;
                                if(b == null){
                                    this.con_parent = null;
                                    this.con_parent_timeout.fail();
                                }
                                else{
                                    this.con_parent = new Connection(parent, b);
                                    parent.lastPing = System.currentTimeMillis();
                                    b.send(new JMCnetworkpacket.Handshake(local));
                                    b.setCallback(p -> {
                                        if(p instanceof JMCnetworkpacket np){
                                            this.handle(np, parent);
                                        }
                                        else if(p instanceof JMCnetworkpacket.FailedHandshake fhs){
                                            JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, "Failed handshake with parent: " + fhs.message);
                                        }
                                    });
                                    this.con_parent_timeout.reset();
                                    JMCapi.getInstance().getPlugin().getLogger().log(Level.INFO, "Successfully connected to parent!");
                                }
                            }
                        );
                    }
                }
            }
        }

        // = Downward =
        if(local.hasChildren()){

            // Poll existing connections
            if(this.con_children != null)this.con_children.removeIf(con -> {
                if(!con.bridge.isAlive()){
                    JMCapi.getInstance().getPlugin().getLogger().log(Level.INFO, "Connection lost with network server '" + con.server.getName() + "'");
                    return true;
                }
                return false;
            });

            // Poll server
            if(this.poll_server != null && !this.poll_server.isAlive()){
                JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, "Network server died! Restarting in 10 seconds...");
                this.poll_server_timeout.endAt = System.currentTimeMillis() + 10000L;
                this.poll_server = null;
            }
            else if(this.poll_server == null){
                if(this.poll_server_timeout.ready()){
                    if(this.con_children == null)this.con_children = new ArrayList<>();
                    this.poll_server = JMCapi.getInstance().createSocketServer(
    //                    Objects.requireNonNull(local.ap_host).equals("127.0.0.1") ? "127.0.0.1" : "0.0.0.0",
                        local.ap_port,
                        true,
                        b -> {
                            b.setCallback(p -> {
                                if(p instanceof JMCnetworkpacket.Handshake hs){
                                    if(!hs.key.equals(this.network.key)){
                                        JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, "Incoming connection failed to identify itself! (Invalid key)");
                                        b.send(new JMCnetworkpacket.FailedHandshake("Invalid key"));
                                        b.close();
                                        return;
                                    }

                                    var server = this.network.getServerByName(hs.name);
                                    if(server == null){
                                        JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, "Incoming connection failed to identify itself! (Invalid name)");
                                        b.send(new JMCnetworkpacket.FailedHandshake("Invalid name"));
                                        b.close();
                                        return;
                                    }

                                    if(!hs.validation.equals(this.network.util_validator())){
                                        JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, "Incoming connection identified as '" + hs.name + "' uses a different networking hierarchy. MAKE SURE THE network.yml IS IDENTICAL ACROSS ALL SERVERS!!");
                                        b.send(new JMCnetworkpacket.FailedHandshake("Invalid hierarchy"));
                                        b.close();
                                        return;
                                    }

                                    for(var con : this.con_children)if(con.bridge.isAlive() && con.server.getName().equalsIgnoreCase(hs.name)){
                                        JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, "Incoming connection identified as '" + hs.name + "'. But that server was already connected? Closing old connection... (If this message keeps repeating, please check for two servers configured with the same name.)");
                                        con.bridge.close();
                                    }

                                    JMCapi.getInstance().getPlugin().getLogger().log(Level.INFO, "New connection established with named server '" + server.getName() + "'");
                                    this.con_children.add(new Connection(server, b));
                                    server.lastPing = System.currentTimeMillis();
                                    b.setCallback(pack -> {
                                        if(pack instanceof JMCnetworkpacket np)this.handle(np, server);
                                    });
                                }
                                else{
                                    JMCapi.getInstance().getPlugin().getLogger().log(Level.WARNING, "Did not receive handshake packet from remote connection! Closing connection...");
                                    b.close();
                                }
                            });
                        },
                        this.poll_server_timeout::fail
                    );
                }
            }
        }
    }


    /**
     * Send
     */
    public void send(@NotNull JunctionPacket packet, @NotNull JMCnetworkserver target){
        this.handle(new JMCnetworkpacket.Directed(target, packet), null);
    }
}
