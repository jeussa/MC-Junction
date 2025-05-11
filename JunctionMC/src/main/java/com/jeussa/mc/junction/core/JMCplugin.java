package com.jeussa.mc.junction.core;

import com.jeussa.mc.junction.ConfigProvider;

import java.util.logging.Logger;

public interface JMCplugin{

    /**
     * Config
     */
    ConfigProvider getConfigurationFile(String name);

    /**
     * Logger
     */
    Logger getLogger();
}
