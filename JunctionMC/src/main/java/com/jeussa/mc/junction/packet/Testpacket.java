package com.jeussa.mc.junction.packet;

import com.jeussa.mc.junction.JunctionPacket;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class Testpacket implements JunctionPacket{

    public String message;

    public Testpacket(String message){this.message = message;}
    public Testpacket(){this.message = "empty";}

    @Override
    public void read(@NotNull DataInputStream in)throws IOException{
        this.message = in.readUTF();
    }

    @Override
    public void write(@NotNull DataOutputStream out)throws IOException{
        out.writeUTF(this.message);
    }
}
