package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.IstanbulPrayerAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
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
    private String nextPrayerName = "Bilinmiyor";
    private String countdownFormatted = "00:00:00";

    private int currentMinDiff = Integer.MAX_VALUE;

    public PrayerTimeManager(MoxCamii plugin) {
        this.plugin = plugin;
        loadFile();
    }

    private void loadFile() {
        file = new File(plugin.getDataFolder(), "settings/clocks.yml");
        if (!file.exists()) plugin.safeSaveResource("settings/clocks.yml");
        config = YamlConfiguration.loadConfiguration(file);
        reloadTimesFromConfig();
    }

    public void reload() { loadFile(); }
    public FileConfiguration getConfig() { return config; }
    public void saveConfig() {
        try { if (config != null && file != null) config.save(file); } catch (IOException e) { e.printStackTrace(); }
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

    public Map<String, String> getPrayerTimes() { return prayerTimes; }
    public String getNextPrayerName() { return nextPrayerName; }
    public String getCountdownFormatted() { return countdownFormatted; }
    public int getCurrentMinDiff() { return currentMinDiff; }

    public void startTasks() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            boolean fetched = IstanbulPrayerAPI.fetchAndSave(plugin);
            if (fetched) reloadTimesFromConfig();
        }, 0L, 20L * 60 * 60 * 24);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            LocalTime now = LocalTime.now(ZoneId.of("Europe/Istanbul"));
            int currentTotalMin = now.getHour() * 60 + now.getMinute();

            int minDiff = Integer.MAX_VALUE;
            String upcoming = "İmsak";
            String rawUpcoming = "Imsak";

            for (Map.Entry<String, String> entry : prayerTimes.entrySet()) {
                String vakitName = entry.getKey();
                String displayName = plugin.getMessagesConfig().getString("Namaz-Names." + vakitName, vakitName);
                String timeStr = entry.getValue();
                String[] split = timeStr.split(":");
                int targetTotalMin = Integer.parseInt(split[0]) * 60 + Integer.parseInt(split[1]);

                int fark = targetTotalMin - currentTotalMin;

                if (fark > 0 && fark < minDiff) {
                    minDiff = fark;
                    upcoming = displayName;
                    rawUpcoming = vakitName;
                }

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
                        plugin.getSoundManager().playSound(null, "Ezan");

                        int delay = plugin.getConfig().getInt("Settings.Others.Reward-Delay-Seconds", 5);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            plugin.getRewardManager().distributeRewards(vakitName, displayName);
                        }, delay * 20L);
                    }
                }
            }

            if (minDiff != Integer.MAX_VALUE) {
                this.currentMinDiff = minDiff;
                this.nextPrayerName = upcoming;
                int hoursLeft = minDiff / 60;
                int minsLeft = (minDiff % 60) - 1;
                int secsLeft = 60 - now.getSecond();
                if (secsLeft == 60) { secsLeft = 0; minsLeft += 1; }
                if (minsLeft < 0) { minsLeft = 0; hoursLeft = 0; }
                this.countdownFormatted = String.format("%02d:%02d:%02d", hoursLeft, minsLeft, secsLeft);
            } else {
                String[] split = prayerTimes.getOrDefault("Imsak", "05:30").split(":");
                int imsakTotalMin = Integer.parseInt(split[0]) * 60 + Integer.parseInt(split[1]);
                int fark = (24 * 60 - currentTotalMin) + imsakTotalMin;
                this.currentMinDiff = fark;
                this.nextPrayerName = plugin.getMessagesConfig().getString("Namaz-Names.Imsak", "İmsak");
                int hoursLeft = fark / 60;
                int minsLeft = (fark % 60) - 1;
                int secsLeft = 60 - now.getSecond();
                if (secsLeft == 60) { secsLeft = 0; minsLeft += 1; }
                this.countdownFormatted = String.format("%02d:%02d:%02d", hoursLeft, minsLeft, secsLeft);
            }

        }, 20L, 20L);
    }

    public void triggerEzanTest(String vakitNameRaw, String displayName) {
        plugin.getRedisManager().publishEzan(displayName);
        plugin.getSoundManager().playSound(null, "Ezan");
        int delay = plugin.getConfig().getInt("Settings.Others.Reward-Delay-Seconds", 5);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getRewardManager().distributeRewards(vakitNameRaw, displayName);
        }, delay * 20L);
    }
}