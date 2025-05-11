package com.jeussa.mc.junction.core.network;

import com.jeussa.mc.junction.JunctionPacket;
import com.jeussa.mc.junction.core.JMCapi;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class JMCnetworkpacket implements JunctionPacket{

    public List<String> path;
    public JunctionPacket packet;

    protected JMCnetworkpacket(JunctionPacket packet){
        this.path = new ArrayList<>();
        this.packet = packet;
    }

    protected JMCnetworkpacket(){}

    // = Read =
    @Override
    public void read(@NotNull DataInputStream in)throws IOException{
        String[] path = new String[in.readInt()];
        for(int i=0; i<path.length; i++)path[i] = in.readUTF();
        this.path = List.of(path);

        var bytes = in.readNBytes(in.readInt());
        this.packet = Objects.requireNonNull(JMCapi.getInstance().getPacketFromBytes(bytes));
    }

    // = Write =
    @Override
    public void write(@NotNull DataOutputStream out)throws IOException{
        var bytes = Objects.requireNonNull(JMCapi.getInstance().getBytesFromPacket(this.packet));

        out.writeInt(this.path.size() + 1);
        for(String s : this.path)out.writeUTF(s);
        out.writeUTF(JMCapi.getInstance().getNetworkManager().getLocalServer().getName());

        out.writeInt(bytes.length);
        out.write(bytes);
    }

    // = Sender =
    public String getSender(){ return this.path.isEmpty() ? JMCapi.getInstance().getNetworkManager().getLocalServer().getName() : this.path.getFirst(); }


    /**
     * Broadcast
     */
    public static class Broadcast extends JMCnetworkpacket{

        public Broadcast(JunctionPacket packet){
            super(packet);
        }
        public Broadcast(){ super(); }
    }


    /**
     * Directed
     */
    public static class Directed extends JMCnetworkpacket{

        public String target;

        public Directed(JMCnetworkserver target, JunctionPacket packet){
            super(packet);
            this.target = target.getName();
        }
        public Directed(){ super(); }

        // = Read =
        @Override
        public void read(@NotNull DataInputStream in)throws IOException{
            this.target = in.readUTF();
            super.read(in);
        }

        // = Write =
        @Override
        public void write(@NotNull DataOutputStream out)throws IOException{
            out.writeUTF(this.target);
            super.write(out);
        }
    }


    /**
     * Handshake
     */
    public static class Handshake implements JunctionPacket{

        public String name;
        public String key;
        public String validation;

        public Handshake(JMCnetworkserver sender){
            this.name = sender.getName();
            this.key = sender.getNetwork().key;
            this.validation = sender.getNetwork().util_validator();
        }
        public Handshake(){}

        // = Read =
        @Override
        public void read(@NotNull DataInputStream in)throws IOException{
            this.name = in.readUTF();
            this.key = in.readUTF();
            this.validation = in.readUTF();
        }

        // = Write =
        @Override
        public void write(@NotNull DataOutputStream out)throws IOException{
            out.writeUTF(this.name);
            out.writeUTF(this.key);
            out.writeUTF(this.validation);
        }
    }


    /**
     * Failed Handshake
     */
    public static class FailedHandshake implements JunctionPacket{

        public String message;

        public FailedHandshake(String message){
            this.message = message;
        }
        public FailedHandshake(){}

        // = Read =
        @Override
        public void read(@NotNull DataInputStream in)throws IOException{
            this.message = in.readUTF();
        }

        // = Write =
        @Override
        public void write(@NotNull DataOutputStream out)throws IOException{
            out.writeUTF(this.message);
        }
    }
}
