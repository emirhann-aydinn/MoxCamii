package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RewardManager {
    private final MoxCamii plugin;
    private FileConfiguration config;
    private int currentMonth;

    public RewardManager(MoxCamii plugin) {
        this.plugin = plugin;
        this.currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        loadFile();
        startMonthlyTask();
    }

    private void loadFile() {
        File file = new File(plugin.getDataFolder(), "settings/rewards.yml");
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() { loadFile(); }
    public FileConfiguration getConfig() { return config; }

    public boolean isSitting(Player p) {
        if (p.isInsideVehicle() && p.getVehicle() instanceof ArmorStand) {
            ArmorStand seat = (ArmorStand) p.getVehicle();
            return seat.getPersistentDataContainer().has(new NamespacedKey(plugin, "CamiKoltuk"), PersistentDataType.BYTE);
        }
        return false;
    }

    public void distributeRewards(String rawName, String displayName) {
        String prefix = ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", ""));
        String rewardMsg = plugin.getMessagesConfig().getString("Messages.RewardGiven");
        String notSittingMsg = plugin.getMessagesConfig().getString("Messages.NotSitting");
        List<String> rewards = config.getStringList("Rewards");

        for (Player p : Bukkit.getOnlinePlayers()) {
            Location checkLoc = p.getLocation().clone().add(0, 1.5, 0);
            boolean inRegion = plugin.getRegionManager().isInRegion(checkLoc);
            if (!inRegion) continue;

            if (isSitting(p)) {
                plugin.getDatabaseManager().recordPrayer(p.getUniqueId(), p.getName(), rawName);
                if (rewardMsg != null) p.sendMessage(prefix + ColorUtils.color(rewardMsg.replace("{VAKIT}", displayName)));
                if (plugin.getSoundManager() != null) plugin.getSoundManager().playSound(p, "Reward");

                for (String cmd : rewards) {
                    cmd = cmd.replace("{PLAYER}", p.getName()).replace("{VAKIT}", displayName);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            } else {
                if (notSittingMsg != null) p.sendMessage(prefix + ColorUtils.color(notSittingMsg));
            }
        }
    }

    // Aylık ödülleri hesaplayan ve dağıtılmak üzere askıya alan ana metot
    private void processMonthlyRewards() {
        List<Map.Entry<String, Integer>> top = plugin.getDatabaseManager().getTop10("monthly_count");

        for (int i = 0; i < Math.min(3, top.size()); i++) {
            Map.Entry<String, Integer> entry = top.get(i);
            String tier = "top" + (i + 1);
            int minNamaz = config.getInt("monthly-rewards.tops." + tier + ".minimum-namaz", 0);

            if (entry.getValue() >= minNamaz) {
                UUID uuid = plugin.getDatabaseManager().getUUIDFromName(entry.getKey());
                if (uuid != null) plugin.getDatabaseManager().setPendingReward(uuid, tier);
            }
        }
        plugin.getDatabaseManager().resetMonthlyStats();
    }

    // /camiadmin sezon bitir komutuyla tetiklenir
    public void forceEndSeason() {
        processMonthlyRewards();
    }

    private void startMonthlyTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            Calendar cal = Calendar.getInstance();
            int newMonth = cal.get(Calendar.MONTH);

            if (newMonth != currentMonth) {
                processMonthlyRewards();
                currentMonth = newMonth;
            }
        }, 1200L, 1200L);
    }
}