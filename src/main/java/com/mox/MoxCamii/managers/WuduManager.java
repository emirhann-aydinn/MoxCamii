package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
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
    private final Set<Location> taps = new HashSet<>();
    private final Map<UUID, Long> wuduTimes = new HashMap<>();

    private File file;
    private FileConfiguration config;

    public WuduManager(MoxCamii plugin) {
        this.plugin = plugin;
        loadFile();
    }

    private void loadFile() {
        file = new File(plugin.getDataFolder(), "locations/abdest_muslugu.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadTaps();
    }

    public void reload() {
        saveData();
        loadFile();
    }

    private void loadTaps() {
        taps.clear();
        if (config.contains("Taps")) {
            for (String key : config.getConfigurationSection("Taps").getKeys(false)) {
                Location loc = config.getLocation("Taps." + key);
                if (loc != null) taps.add(loc);
            }
        }
    }

    public void saveData() {
        if (config == null || file == null) return;
        config.set("Taps", null);
        int i = 0;
        for (Location loc : taps) {
            config.set("Taps.tap" + i, loc);
            i++;
        }
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public boolean isTap(Location loc) {
        return taps.contains(loc);
    }

    public void addTap(Location loc) {
        taps.add(loc);
        saveData();
    }

    public void removeTap(Location loc) {
        taps.remove(loc);
        saveData();
    }

    public boolean hasWudu(UUID uuid) {
        if (!wuduTimes.containsKey(uuid)) return false;

        long takeTime = wuduTimes.get(uuid);
        long expireTime = plugin.getConfig().getLong("Settings.Others.Wudu-Expire-Minutes", 60) * 60 * 1000;

        if (System.currentTimeMillis() - takeTime > expireTime) {
            wuduTimes.remove(uuid); // Süresi dolunca abdest silinir
            return false;
        }
        return true;
    }

    // Abdest ver ve Veritabanında sayacı 1 artır
    public void giveWudu(UUID uuid) {
        wuduTimes.put(uuid, System.currentTimeMillis());
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            plugin.getDatabaseManager().incrementWuduCount(uuid, p.getName());
        }
    }

    // Komut veya admin tarafından zorla abdesti silme
    public void removeWudu(UUID uuid) {
        wuduTimes.remove(uuid);
    }
}