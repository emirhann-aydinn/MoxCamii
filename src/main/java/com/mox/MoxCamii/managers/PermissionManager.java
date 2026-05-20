package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class PermissionManager {
    private final MoxCamii plugin;
    private FileConfiguration config;

    public PermissionManager(MoxCamii plugin) {
        this.plugin = plugin;
        loadFile();
    }

    public void loadFile() {
        File file = new File(plugin.getDataFolder(), "settings/permissions.yml");
        if (!file.exists()) {
            plugin.safeSaveResource("settings/permissions.yml");
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String path, String def) {
        return config.getString("Permissions." + path, def);
    }
}