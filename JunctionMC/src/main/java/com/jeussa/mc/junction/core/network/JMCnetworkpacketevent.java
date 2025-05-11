package com.jeussa.mc.junction.core.network;

import com.jeussa.mc.junction.JunctionPacket;
import com.jeussa.mc.junction.core.JMCapi;
import com.jeussa.mc.junction.network.JunctionNetworkPacketEvent;
import com.jeussa.mc.junction.network.JunctionNetworkServer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class JMCnetworkpacketevent implements JunctionNetworkPacketEvent{

    private final @NotNull JMCnetworkpacket packet;

    public JMCnetworkpacketevent(@NotNull JMCnetworkpacket packet){
        this.packet = packet;
    }

    /**
     * Directed
     */
    @Override
    public boolean isDirected(){
        return this.packet instanceof JMCnetworkpacket.Directed;
    }

    /**
     * Packet
     */
    @Override
    public @NotNull JunctionPacket getPacket(){
        return this.packet.packet;
    }

    /**
     * Path
     */
    @Override
    public @NotNull List<@NotNull JunctionNetworkServer> getPath(){
        return this.packet.path.stream().map(s -> Objects.requireNonNull(JMCapi.getInstance().getNetworkManager().getServerByName(s))).toList();
    }
    @Override
    public @NotNull List<@NotNull String> getStringPath(){
        return Collections.unmodifiableList(this.packet.path);
    }

    /**
     * Sender
     */
    @Override
    public @NotNull JunctionNetworkServer getSender(){
        return Objects.requireNonNull(JMCapi.getInstance().getNetworkManager().getServerByName(this.packet.getSender()));
    }
    @Override
    public @NotNull String getSenderName(){
        return this.packet.getSender();
    }
}
