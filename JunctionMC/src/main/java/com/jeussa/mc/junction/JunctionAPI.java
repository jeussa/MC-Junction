package com.jeussa.mc.junction;

import com.jeussa.mc.junction.core.JMCapi;
import com.jeussa.mc.junction.core.JMCserver;
import com.jeussa.mc.junction.network.JunctionNetwork;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.logging.Level;

public interface JunctionAPI{

    /**
     * Network
     */
    @NotNull JunctionNetwork getNetworkManager();

    /**
     * Socket
     */
    // = Server =
    @Nullable JMCserver createSocketServer(int port, boolean doEncrypt, @Nullable Consumer<@NotNull JunctionBridge> callback, @NotNull Consumer<@NotNull Exception> onFail);
    default @Nullable JMCserver createSocketServer(int port, boolean doEncrypt, @Nullable Consumer<@NotNull JunctionBridge> callback){ return this.createSocketServer(port, doEncrypt, callback, e -> {
        ((JMCapi)this).getPlugin().getLogger().log(Level.WARNING, "An error occurred whilst starting a new SocketServer", e);
    }); }
    default @Nullable JMCserver createSocketServer(int port, boolean doEncrypt){ return this.createSocketServer(port, doEncrypt, null); }
    default @Nullable JMCserver createSocketServer(int port, @Nullable Consumer<@NotNull JunctionBridge> callback){ return this.createSocketServer(port, true, callback); }
    default @Nullable JMCserver createSocketServer(int port){ return this.createSocketServer(port, true, null); }
    // = Client =
    void connectToSocketServer(@NotNull String host, int port, boolean doEncrypt, @Nullable Consumer<@Nullable JunctionBridge> callback);
    default void connectToSocketServer(@NotNull String host, int port, boolean doEncrypt){ this.connectToSocketServer(host, port, doEncrypt, null); }
    default void connectToSocketServer(@NotNull String host, int port, @Nullable Consumer<@Nullable JunctionBridge> callback){ this.connectToSocketServer(host, port, true, callback); }
    default void connectToSocketServer(@NotNull String host, int port){ this.connectToSocketServer(host, port, true, null); }

    /**
     * Status
     */
    int getNumPendingClients();
    int getNumPendingServers();
    int getNumBridges();
}
