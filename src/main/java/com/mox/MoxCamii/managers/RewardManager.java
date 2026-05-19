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
import java.util.List;

public class RewardManager {

    private final MoxCamii plugin;
    private File file;
    private FileConfiguration config;

    public RewardManager(MoxCamii plugin) {
        this.plugin = plugin;
        loadFile();
    }

    private void loadFile() {
        file = new File(plugin.getDataFolder(), "rewards.yml");
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        loadFile();
    }

    public boolean isSitting(Player p) {
        if (p.isInsideVehicle() && p.getVehicle() instanceof ArmorStand) {
            ArmorStand seat = (ArmorStand) p.getVehicle();
            return seat.getPersistentDataContainer().has(new NamespacedKey(plugin, "CamiKoltuk"), PersistentDataType.BYTE);
        }
        return false;
    }

    public void distributeRewards(String displayName) {
        String prefix = ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", "&8[&6MoxCamii&8] &7"));
        String rewardMsg = plugin.getConfig().getString("Messages.RewardGiven");
        String notSittingMsg = plugin.getConfig().getString("Messages.NotSitting");

        List<String> rewards = config.getStringList("Rewards");

        for (Player p : Bukkit.getOnlinePlayers()) {
            Location checkLoc = p.getLocation().clone().add(0, 1.5, 0);
            boolean inRegion = plugin.getRegionManager().isInRegion(checkLoc);
            if (!inRegion) continue;

            if (isSitting(p)) {
                plugin.getDatabaseManager().recordPrayer(p.getUniqueId(), p.getName(), displayName);

                if (rewardMsg != null) p.sendMessage(prefix + ColorUtils.color(rewardMsg.replace("{VAKIT}", displayName)));
                for (String cmd : rewards) {
                    cmd = cmd.replace("{PLAYER}", p.getName()).replace("{VAKIT}", displayName);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            } else {
                if (notSittingMsg != null) p.sendMessage(prefix + ColorUtils.color(notSittingMsg));
            }
        }
    }
}