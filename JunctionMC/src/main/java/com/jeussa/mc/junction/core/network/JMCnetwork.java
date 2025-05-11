package com.jeussa.mc.junction.core.network;

import com.jeussa.mc.junction.ConfigProvider;
import com.jeussa.mc.junction.JunctionPacket;
import com.jeussa.mc.junction.network.JunctionNetwork;
import com.jeussa.mc.junction.network.JunctionNetworkPacketEvent;
import com.jeussa.mc.junction.network.JunctionNetworkServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class JMCnetwork implements JunctionNetwork{

    private final @NotNull String name;
    public final @NotNull String key;
    private final @NotNull JMCnetworkserver main;
    private final @NotNull JMCnetworkserver local;
    private final @NotNull List<JMCnetworkserver> servers;

    private final @NotNull JMCnetworker networker;
    private boolean networker_active = false;


    public JMCnetwork(@NotNull ConfigProvider config, @NotNull ConfigProvider local){
        var name = config.getString("network-name", null);
        if(name == null)throw new NullPointerException("No network-name was specified in the network configuration!");
        this.name = name;

        var key = config.getString("key", null);
        if(key == null)throw new NullPointerException("No key was specified in the network configuration!");
        this.key = key;

        var localName = local.getString("server-id", null);
        if(localName == null)throw new NullPointerException("No server-id was specified in the local configuration!");

        var serverlist = config.getSection("network");
        if(serverlist == null)throw new NullPointerException("No server list was specified in the network configuration!");
        var keys = serverlist.getKeys();
        if(keys.size() != 1)throw new NullPointerException("Invalid number of servers specified at top-level in network configuration! (The top-level should contain precisely 1 (main) server.)");
        var topServer = (String)keys.toArray()[0];

        this.main = new JMCnetworkserver(this, null, topServer, Objects.requireNonNull(serverlist.getSection(topServer)));

        this.servers = new ArrayList<>();
        new Consumer<JunctionNetworkServer>(){
            @Override public void accept(JunctionNetworkServer s){
                if(JMCnetwork.this.getServerByName(s.getName()) != null)throw new IllegalArgumentException("Duplicate network server name '" + s.getName() + "' !");
                JMCnetwork.this.servers.add((JMCnetworkserver)s);
                s.getChildren().forEach(this);
            }
        }.accept(this.main);

        var localServer = this.getServerByName(localName);
        if(localServer == null)throw new NullPointerException("Unknown local server '" + localName + "' specified in local configuration!");
        this.local = localServer;

        this.networker = new JMCnetworker(this);
    }


    /**
     * Active
     */
    @Override
    public boolean isActive(){
        return this.networker_active;
    }


    /**
     * Broadcast
     */
    @Override
    public void broadcast(@NotNull JunctionPacket packet){
        if(!this.networker_active)throw new IllegalStateException("Networker is not active - cannot broadcast packet!");
        this.networker.broadcast(packet);
    }


    /**
     * Callback
     */
    @Override
    public void setCallback(@Nullable Consumer<@NotNull JunctionNetworkPacketEvent> callback){
        this.networker.setCallback(callback);
    }


    /**
     * Local
     */
    @Override
    public @NotNull JMCnetworkserver getLocalServer(){
        return this.local;
    }


    /**
     * Main
     */
    @Override
    public @NotNull JMCnetworkserver getMainServer(){
        return this.main;
    }


    /**
     * Name
     */
    @Override
    public @NotNull String getName(){
        return this.name;
    }


    /**
     * Poll
     */
    public void poll(){
        if(this.networker_active)this.networker.poll();
    }


    /**
     * Send
     */
    @Override
    public void send(@NotNull JunctionPacket packet, @NotNull JunctionNetworkServer target){
        if(!this.networker_active)throw new IllegalStateException("Networker is not active - cannot broadcast packet!");
        this.networker.send(packet, (JMCnetworkserver)target);
    }


    /**
     * Server
     */
    @Override
    public @Nullable JMCnetworkserver getServerByName(@Nullable String name){
        if(name == null)return null;
        for(var s : this.servers)if(s.getName().equalsIgnoreCase(name))return s;
        return null;
    }
    @Override
    public @NotNull List<JunctionNetworkServer> getServers(){
        return Collections.unmodifiableList(this.servers);
    }


    /**
     * Start
     */
    @Override
    public void start(){
        this.networker_active = true;
    }


    /**
     * Stop
     */
    @Override
    public void stop(){
        if(!this.networker_active)return;
        this.networker.close();
        this.networker_active = false;
    }


    /**
     * Utilities
     */
    // = Hierarchy String = used to make a string that is unique to the server hierarchy, which helps to ensure the network.yml is identical across the network
    private String __util_validator__(JMCnetworkserver current){ return current.hasChildren() ?  current.getName() + "(" + String.join(",", current.getChildren().stream().map(c -> this.__util_validator__((JMCnetworkserver)c)).toList()) + ")" : current.getName(); }
    public String util_validator(){ return this.__util_validator__(this.getMainServer()).toLowerCase(); }
}
