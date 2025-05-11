package com.jeussa.mc.junction.bungee;

import com.jeussa.mc.junction.ConfigProvider;
import net.md_5.bungee.config.Configuration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class BungeeConfigProvider implements ConfigProvider{

    private final @Nullable File file;
    private final Configuration handle;

    public BungeeConfigProvider(@Nullable File file, @NotNull Configuration handle){
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
    public int getInt(@NotNull String path, int def){ return this.handle.getInt(path, def); }

    /**
     * Keys
     */
    @Override
    public Set<String> getKeys(){ return new HashSet<>(this.handle.getKeys()); }

    /**
     * Section
     */
    @Override
    public @Nullable BungeeConfigProvider getSection(@NotNull String path){
        return this.contains(path) ? new BungeeConfigProvider(null, this.handle.getSection(path)) : null;
    }

    /**
     * String
     */
    @Override
    public @Nullable String getString(@NotNull String path, @Nullable String def){
        return this.handle.getString(path, def);
    }
}
