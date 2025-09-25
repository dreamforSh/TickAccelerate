package com.xinian.tickaccelerate.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.xinian.tickaccelerate.TickAccelerate;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.util.List;

public class TOMLConfiguration {
    public final String fileName;
    protected final CommentedFileConfig config;

    public TOMLConfiguration(String fileName) {
        this.fileName = fileName;
        File file = new File(FMLPaths.CONFIGDIR.get().toAbsolutePath() + "/tickaccelerate/", fileName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        this.config = CommentedFileConfig.builder(file).autosave().build();
    }

    public void save() {
        config.save();
    }

    public void reload() {
        config.load();
    }

    public <T> T get(String key) {
        return config.get(key);
    }

    public <T> T getOrDefault(String key, T defaultValue) {
        T value = config.get(key);
        return value == null ? defaultValue : value;
    }

    public void set(String key, Object value) {
        config.set(key, value);
    }

    public void setComment(String key, String comment) {
        config.setComment(key, comment);
    }

    public void putIfEmpty(String key, Object value) {
        if (!config.contains(key)) {
            set(key, value);
        }
    }

    public CommentedConfig getRawConfig() {
        return config;
    }
}
