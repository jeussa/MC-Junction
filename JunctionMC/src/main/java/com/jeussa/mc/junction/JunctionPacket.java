package com.jeussa.mc.junction;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface JunctionPacket{

    /**
     * Read
     */
    void read(@NotNull DataInputStream in)throws IOException;

    /**
     * Write
     */
    void write(@NotNull DataOutputStream out)throws IOException;
}
