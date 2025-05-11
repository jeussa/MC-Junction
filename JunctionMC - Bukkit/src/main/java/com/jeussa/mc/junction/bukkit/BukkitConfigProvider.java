package com.jeussa.mc.junction.bukkit;

import com.jeussa.mc.junction.ConfigProvider;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Set;

public class BukkitConfigProvider implements ConfigProvider{

    private final @Nullable File file;
    private final ConfigurationSection handle;

    public BukkitConfigProvider(@Nullable File file, @NotNull ConfigurationSection handle){
        this.file = file;
        this.handle = handle;
    }

    /**
     * Boolean
     */
    @Override
    public boolean getBoolean(@NotNull String path, boolean def){ return this.handle.getBoolean(path, def); }

    /**
     * Contains
     */
    @Override
    public boolean contains(@NotNull String path){ return this.handle.contains(path); }

    /**
     * Integer
     */
    @Override
    public int getInt(@NotNull String path, int def){
        return this.handle.getInt(path, def);
    }

    /**
     * Keys
     */
    @Override
    public Set<String> getKeys(){
        return this.handle.getKeys(false);
    }

    /**
     * Section
     */
    @Override
    public @Nullable BukkitConfigProvider getSection(@NotNull String path){
        var s = this.handle.getConfigurationSection(path);
        return s == null ? null : new BukkitConfigProvider(null, s);
    }

    /**
     * String
     */
    @Override
    public @Nullable String getString(@NotNull String path, @Nullable String def){
        return this.handle.getString(path, def);
    }
}
