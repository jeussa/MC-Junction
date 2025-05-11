package com.jeussa.mc.junction.network;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface JunctionNetworkServer{

    enum Role{
        BACKEND,
        PROXY
    }

    interface Status{}

    /**
     * Children
     */
    @NotNull Set<JunctionNetworkServer> getChildren();
    boolean hasChildren();
    boolean hasChild(@Nullable String name);

    /**
     * Lives With Parent
     */
    boolean livesWithParent();

    /**
     * Local
     */
    boolean isLocalServer();

    /**
     * Main
     */
    boolean isMainServer();

    /**
     * Name
     */
    @NotNull String getName();

    /**
     * Network
     */
    @NotNull JunctionNetwork getNetwork();

    /**
     * Parent
     */
    @Nullable JunctionNetworkServer getParent();
    boolean hasParent();

    /**
     * Ping
     */
    long getLastPing();

    /**
     * Role
     */
    @NotNull Role getRole();
}
