package com.jeussa.mc.junction.core;

import com.jeussa.mc.junction.JunctionClient;
import org.jetbrains.annotations.Nullable;

public interface JMCclient extends JunctionClient{

    /**
     * Accept
     */
    @Nullable JMCbridge accept();
}
