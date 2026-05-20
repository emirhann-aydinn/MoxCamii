package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class SoundManager {
    private final MoxCamii plugin;
    private FileConfiguration config;

    public SoundManager(MoxCamii plugin) {
        this.plugin = plugin;
        loadFile();
    }

    public void loadFile() {
        File file = new File(plugin.getDataFolder(), "settings/sounds.yml");
        if (!file.exists()) plugin.safeSaveResource("settings/sounds.yml");
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void playSound(Player p, String key) {
        String soundData = config.getString("Sounds." + key);
        if (soundData == null || soundData.equalsIgnoreCase("none")) return;

        try {
            String[] parts = soundData.split(";");
            Sound sound = Sound.valueOf(parts[0].toUpperCase());
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
            p.playSound(p.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Ses efekti oynatılamadı! Hatalı ses türü: " + soundData);
        }
    }
}