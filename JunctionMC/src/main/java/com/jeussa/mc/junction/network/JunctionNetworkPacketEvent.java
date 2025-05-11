package com.jeussa.mc.junction.network;

import com.jeussa.mc.junction.JunctionPacket;
import com.jeussa.mc.junction.core.network.JMCnetworkpacket;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface JunctionNetworkPacketEvent{

    /**
     * Directed
     */
    boolean isDirected();

    /**
     * Packet
     */
    @NotNull JunctionPacket getPacket();

    /**
     * Path
     */
    @NotNull List<@NotNull JunctionNetworkServer> getPath();
    @NotNull List<@NotNull String> getStringPath();

    /**
     * Sender
     */
    @NotNull JunctionNetworkServer getSender();
    @NotNull String getSenderName();
}
