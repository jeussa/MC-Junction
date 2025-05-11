package com.jeussa.mc.junction.network;

import com.jeussa.mc.junction.JunctionPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public interface JunctionNetwork{

    /**
     * Active
     */
    boolean isActive();

    /**
     * Broadcast
     */
    void broadcast(@NotNull JunctionPacket packet);

    /**
     * Callback
     */
    void setCallback(@Nullable Consumer<@NotNull JunctionNetworkPacketEvent> callback);

    /**
     * Local
     */
    @NotNull JunctionNetworkServer getLocalServer();

    /**
     * Main
     */
    @NotNull JunctionNetworkServer getMainServer();

    /**
     * Name
     */
    @NotNull String getName();

    /**
     * Send
     */
    void send(@NotNull JunctionPacket packet, @NotNull JunctionNetworkServer target);

    /**
     * Server
     */
    @Nullable JunctionNetworkServer getServerByName(@Nullable String name);
    @NotNull List<JunctionNetworkServer> getServers();

    /**
     * Start
     */
    void start();

    /**
     * Stop
     */
    void stop();
}
