// src/main/java/com/mox/MoxCamii/managers/WuduManager.java
package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WuduManager {

    private final MoxCamii plugin;
    private File file;
    private FileConfiguration config;
    private final Set<Location> wuduTaps = new HashSet<>();
    private final Map<UUID, Long> wuduTimes = new HashMap<>();

    public WuduManager(MoxCamii plugin) {
        this.plugin = plugin;
        loadFile();
        startExpiryTask();
    }

    private void loadFile() {
        file = new File(plugin.getDataFolder(), "locations/abdest_muslugu.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);

        wuduTaps.clear();
        if (config.contains("Taps")) {
            for (String key : config.getConfigurationSection("Taps").getKeys(false)) {
                wuduTaps.add(new Location(
                        Bukkit.getWorld(config.getString("Taps." + key + ".World")),
                        config.getDouble("Taps." + key + ".X"),
                        config.getDouble("Taps." + key + ".Y"),
                        config.getDouble("Taps." + key + ".Z")
                ));
            }
        }
    }

    public void reload() {
        loadFile();
    }

    public void saveData() {
        config.set("Taps", null);
        int i = 0;
        for (Location loc : wuduTaps) {
            config.set("Taps." + i + ".World", loc.getWorld().getName());
            config.set("Taps." + i + ".X", loc.getX());
            config.set("Taps." + i + ".Y", loc.getY());
            config.set("Taps." + i + ".Z", loc.getZ());
            i++;
        }
        try { config.save(file); } catch (IOException ignored) {}
    }

    public void addTap(Location loc) {
        wuduTaps.add(loc);
        saveData();
    }

    public void removeTap(Location loc) {
        wuduTaps.remove(loc);
        saveData();
    }

    public boolean isTap(Location loc) {
        return wuduTaps.contains(loc);
    }

    public void giveWudu(UUID uuid) {
        wuduTimes.put(uuid, System.currentTimeMillis());
    }

    public boolean hasWudu(UUID uuid) {
        return wuduTimes.containsKey(uuid);
    }

    private void startExpiryTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            int durationMinutes = plugin.getConfig().getInt("Settings.AbdestSuresiDakika", 30);
            long expiryTime = durationMinutes * 60 * 1000L;
            long now = System.currentTimeMillis();

            wuduTimes.entrySet().removeIf(entry -> {
                if (now - entry.getValue() > expiryTime) {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p != null) {
                        String prefix = plugin.getConfig().getString("Settings.Prefix", "");
                        String msg = plugin.getConfig().getString("Messages.WuduExpired");
                        p.sendMessage(ColorUtils.color(prefix + msg));
                    }
                    return true;
                }
                return false;
            });
        }, 20L * 60, 20L * 60);
    }
}