package com.jeussa.mc.junction;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface ConfigProvider{

    /**
     * Boolean
     */
    boolean getBoolean(@NotNull String path, boolean def);
    default boolean getBoolean(@NotNull String path){ return this.getBoolean(path, false); }

    /**
     * Contains
     */
    boolean contains(@NotNull String path);

    /**
     * Integer
     */
    int getInt(@NotNull String path, int def);
    default int getInt(@NotNull String path){ return this.getInt(path, 0); }

    /**
     * Keys
     */
    Set<String> getKeys();

    /**
     * Section
     */
    @Nullable ConfigProvider getSection(String path);

    /**
     * String
     */
    @Nullable String getString(@NotNull String path, @Nullable String def);
    default @Nullable String getString(@NotNull String path){ return this.getString(path, null); }
}
