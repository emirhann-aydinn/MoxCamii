package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isSitting(p)) {
                plugin.getDatabaseManager().recordPrayer(p.getUniqueId(), p.getName(), rawName);
                if (rewardMsg != null) p.sendMessage(prefix + ColorUtils.color(rewardMsg.replace("{VAKIT}", displayName)));
                if (plugin.getSoundManager() != null) plugin.getSoundManager().playSound(p, "Reward");

                if (config.getBoolean("Rewards.Commands.Enabled", false)) {
                    List<String> cmds = config.getStringList("Rewards.Commands.List");
                    for (String cmd : cmds) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{PLAYER}", p.getName()).replace("{VAKIT}", displayName));
                    }
                }

                if (config.getBoolean("Rewards.Items.Enabled", false)) {
                    List<String> items = config.getStringList("Rewards.Items.List");
                    for (String itemStr : items) {
                        try {
                            String[] parts = itemStr.split(":");
                            Material mat = Material.matchMaterial(parts[0].toUpperCase());
                            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

                            if (mat != null && mat != Material.AIR) {
                                p.getInventory().addItem(new ItemStack(mat, amount));
                            } else {
                                plugin.getLogger().warning("Gecersiz esya turu: " + parts[0]);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Esya verilirken hata olustu: " + itemStr);
                        }
                    }
                }
            } else {
                Location checkLoc = p.getLocation().clone().add(0, 1.5, 0);
                if (plugin.getRegionManager().isInRegion(checkLoc)) {
                    if (notSittingMsg != null) p.sendMessage(prefix + ColorUtils.color(notSittingMsg));
                }
            }
        }
    }

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

    public void forceEndSeason() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::processMonthlyRewards);
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