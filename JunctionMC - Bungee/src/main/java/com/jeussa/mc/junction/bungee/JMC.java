package com.jeussa.mc.junction.bungee;

import com.jeussa.mc.junction.JunctionAPI;
import com.jeussa.mc.junction.core.JMCapi;
import com.jeussa.mc.junction.core.JMCplugin;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class JMC extends Plugin implements JMCplugin{


    private static JMC instance = null;
    public static JunctionAPI api(){ return JMC.instance.api; }


    private final JMCapi api;

    public JMC(){
        if(JMC.instance != null)throw new IllegalStateException("JMC was already initialised!");
        JMC.instance = this;

        this.api = new JMCapi(this);
    }


    /**
     * onEnable
     */
    @Override
    public void onEnable(){
        this.api.onEnable();
        this.getProxy().getScheduler().schedule(this, this.api::poll, 50L, 50L, TimeUnit.MILLISECONDS);

        ProxyServer.getInstance().getPluginManager().registerCommand(this, new cmdTest(this));
    }


    /**
     * onDisable
     */
    @Override
    public void onDisable(){
        this.api.onDisable();
    }


    /**
     * Config
     */
    @Override
    public BungeeConfigProvider getConfigurationFile(String name){
        File file = new File(this.getDataFolder(), name);

        Configuration config = null;
        boolean dobackup = false;

        try{
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        }catch(IOException e){
            this.getLogger().log(Level.SEVERE, "Failed to load " + name + " !", e);
            dobackup = true;
        }

        // Backup for security / debug
        if(dobackup){
            Path copy = new File(this.getDataFolder(), name + ".backup." + System.currentTimeMillis() + ".yml").toPath();
            this.getLogger().log(Level.WARNING, "Failed to (fully) read " + name + " ! Copying existing file to " + copy + " to avoid potential data loss...");
            try{
                Files.copy(file.toPath(), copy);
            }catch(IOException e){
                this.getLogger().log(Level.SEVERE, "Failed to copy " + name + " for debug!", e);
            }
        }

        return new BungeeConfigProvider(file, config == null ? ConfigurationProvider.getProvider(YamlConfiguration.class).load("") : config);
    }
}
