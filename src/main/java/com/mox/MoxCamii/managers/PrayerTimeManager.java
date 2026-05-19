package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.IstanbulPrayerAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class PrayerTimeManager {

    private final MoxCamii plugin;
    private File file;
    private FileConfiguration config;
    private final Map<String, String> prayerTimes = new HashMap<>();

    private String lastEzanCache = "";
    private String lastReminderCache = "";

    public PrayerTimeManager(MoxCamii plugin) {
        this.plugin = plugin;
        loadFile();
    }

    private void loadFile() {
        file = new File(plugin.getDataFolder(), "clocks.yml");
        config = YamlConfiguration.loadConfiguration(file);
        reloadTimesFromConfig();
    }

    public void reload() {
        loadFile();
    }

    public void reloadTimesFromConfig() {
        prayerTimes.clear();
        prayerTimes.put("Imsak", config.getString("Imsak", "05:30"));
        prayerTimes.put("Gunes", config.getString("Gunes", "07:00"));
        prayerTimes.put("Ogle", config.getString("Ogle", "13:00"));
        prayerTimes.put("Ikindi", config.getString("Ikindi", "16:30"));
        prayerTimes.put("Aksam", config.getString("Aksam", "19:45"));
        prayerTimes.put("Yatsi", config.getString("Yatsi", "21:15"));
    }

    public Map<String, String> getPrayerTimes() {
        return prayerTimes;
    }

    public void startTasks() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            boolean fetched = IstanbulPrayerAPI.fetchAndSave(file, config);
            if (fetched) reloadTimesFromConfig();
        }, 0L, 20L * 60 * 60 * 24);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            LocalTime now = LocalTime.now(ZoneId.of("Europe/Istanbul"));
            int currentTotalMin = now.getHour() * 60 + now.getMinute();

            for (Map.Entry<String, String> entry : prayerTimes.entrySet()) {
                String vakitName = entry.getKey();
                String displayName = plugin.getConfig().getString("NamazIsimleri." + vakitName, vakitName);
                String timeStr = entry.getValue();
                String[] split = timeStr.split(":");
                int targetHour = Integer.parseInt(split[0]);
                int targetMin = Integer.parseInt(split[1]);
                int targetTotalMin = targetHour * 60 + targetMin;

                int fark = targetTotalMin - currentTotalMin;

                if (fark == 5 || fark == 3 || fark == 1) {
                    String cacheKey = vakitName + "-" + fark;
                    if (!lastReminderCache.equals(cacheKey)) {
                        lastReminderCache = cacheKey;
                        plugin.getRedisManager().publishReminder(displayName, fark);
                    }
                } else if (fark == 0) {
                    if (!lastEzanCache.equals(vakitName)) {
                        lastEzanCache = vakitName;
                        plugin.getRedisManager().publishEzan(displayName);

                        int delay = plugin.getConfig().getInt("Settings.RewardDelaySeconds", 5);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            plugin.getRewardManager().distributeRewards(displayName);
                        }, delay * 20L);
                    }
                }
            }
        }, 20L, 20L * 60);
    }

    public void triggerEzanTest(String vakitNameRaw, String displayName) {
        plugin.getRedisManager().publishEzan(displayName);
        int delay = plugin.getConfig().getInt("Settings.RewardDelaySeconds", 5);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getRewardManager().distributeRewards(displayName);
        }, delay * 20L);
    }
}