package com.jeussa.mc.junction.core;

import com.jeussa.mc.junction.JunctionServer;
import org.jetbrains.annotations.Nullable;

public interface JMCserver extends JunctionServer{

    /**
     * Accept
     */
    @Nullable JMCbridge accept();
}
