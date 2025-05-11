package com.jeussa.mc.junction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface JunctionBridge{

    /**
     * Alive
     */
    boolean isAlive();

    /**
     * Callback
     */
    void setCallback(@Nullable Consumer<@NotNull JunctionPacket> callback);

    /**
     * Close
     */
    void close();

    /**
     * Send
     */
    boolean send(@NotNull JunctionPacket packet);
}
