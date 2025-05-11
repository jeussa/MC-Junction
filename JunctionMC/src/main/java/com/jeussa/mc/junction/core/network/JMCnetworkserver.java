package com.jeussa.mc.junction.core.network;

import com.jeussa.mc.junction.ConfigProvider;
import com.jeussa.mc.junction.network.JunctionNetworkServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class JMCnetworkserver implements JunctionNetworkServer{


    private final @NotNull JMCnetwork network;
    private final @Nullable JMCnetworkserver parent;

    private final @NotNull String name;
    private final @NotNull JunctionNetworkServer.Role role;
    private final boolean livesWithParent;

    public final @Nullable String ap_host;
    public final int ap_port;
    private final Set<JunctionNetworkServer> children;

    public long lastPing = -1L;


    public JMCnetworkserver(@NotNull JMCnetwork network, @Nullable JMCnetworkserver parent, @NotNull String name, @NotNull ConfigProvider config){
        this.network = network;
        this.parent = parent;

        this.name = name;

        var roleStr = config.getString("role", null);
        if(roleStr == null)throw new NullPointerException("Network server '" + name + "' has no role assigned!");
        JunctionNetworkServer.Role role = null;
        for(var r : JunctionNetworkServer.Role.values())if(r.name().equalsIgnoreCase(roleStr)){
            role = r;
            break;
        }
        if(role == null)throw new IllegalArgumentException("Unknown role '" + roleStr + "' specified for network server '" + name + "' !");
        this.role = role;

        if(parent == null)this.livesWithParent = false;
        else{
            if(!config.contains("lives-with-parent"))throw new NullPointerException("Network server '" + name + "' is missing value for key 'lives-with-parent' !");
            this.livesWithParent = config.getBoolean("lives-with-parent");
        }

        var children = config.getSection("children");
        if(children != null){
            var host = config.getString("host", null);
            if(host == null)throw new NullPointerException("Network server '" + name + "' has children but no hostname was specified!");
            this.ap_host = host;

            var port = config.getInt("port", -1);
            if(port <= 0)throw new NullPointerException("Network server '" + name + "' has no valid port specified!");
            this.ap_port = port;

            var list = new ArrayList<JMCnetworkserver>();
            for(String key : children.getKeys()){
                list.add(new JMCnetworkserver(network, this, key, Objects.requireNonNull(children.getSection(key))));
            }
            this.children = new HashSet<>(list);
        }
        else{
            this.ap_host = null;
            this.ap_port = -1;
            this.children = Collections.emptySet();
        }
    }


    /**
     * Children
     */
    @Override
    public @NotNull Set<JunctionNetworkServer> getChildren(){
        return this.children;
    }
    @Override
    public boolean hasChildren(){
        return !this.children.isEmpty();
    }
    @Override
    public boolean hasChild(@Nullable String name){
        if(name == null)return false;
        if(this.name.equalsIgnoreCase(name))return true;
        for(var child : this.children)if(child.hasChild(name))return true;
        return false;
    }


    /**
     * Lives With Parent
     */
    @Override
    public boolean livesWithParent(){
        return this.livesWithParent;
    }


    /**
     * Local
     */
    @Override
    public boolean isLocalServer(){ return this.network.getLocalServer().equals(this); }


    /**
     * Main
     */
    @Override
    public boolean isMainServer(){ return this.network.getMainServer().equals(this); }


    /**
     * Name
     */
    @Override
    public @NotNull String getName(){
        return this.name;
    }


    /**
     * Network
     */
    @Override
    public @NotNull JMCnetwork getNetwork(){
        return this.network;
    }


    /**
     * Parent
     */
    @Override
    public @Nullable JMCnetworkserver getParent(){
        return this.parent;
    }
    @Override
    public boolean hasParent(){
        return this.parent != null;
    }


    /**
     * Ping
     */
    @Override
    public long getLastPing(){
        return this.isLocalServer() ? -2L : this.lastPing;
    }


    /**
     * Role
     */
    @Override
    public @NotNull JunctionNetworkServer.Role getRole(){
        return this.role;
    }
}
