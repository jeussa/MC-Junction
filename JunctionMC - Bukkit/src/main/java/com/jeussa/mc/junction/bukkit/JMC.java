package com.jeussa.mc.junction.bukkit;

import com.jeussa.mc.junction.JunctionAPI;
import com.jeussa.mc.junction.core.JMCapi;
import com.jeussa.mc.junction.core.JMCplugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class JMC extends JavaPlugin implements JMCplugin{


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
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this.api::poll, 1L, 1L);
        new cmdTest(this);
        new cmdJunction(this);
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
    public BukkitConfigProvider getConfigurationFile(String name){
        File file = new File(this.getDataFolder(), name);

        YamlConfiguration config = new YamlConfiguration();
        boolean dobackup = false;

        try{
            config.load(file);
        }catch(IOException | InvalidConfigurationException e){
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

        return new BukkitConfigProvider(file, config);
    }
}
