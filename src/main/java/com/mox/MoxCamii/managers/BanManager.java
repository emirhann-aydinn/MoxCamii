// src/main/java/com/mox/MoxCamii/managers/BanManager.java
package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class BanManager {

    private final MoxCamii plugin;

    public BanManager(MoxCamii plugin) {
        this.plugin = plugin;
    }

    public void banPlayer(String playerName, String reason, String author) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = op.getUniqueId();
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date());

        plugin.getDatabaseManager().banUser(uuid, op.getName() != null ? op.getName() : playerName, reason, author, date);
    }

    public void unbanPlayer(String playerName, CommandSender sender) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = op.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String reason = plugin.getDatabaseManager().getBanReason(uuid);
            String prefix = ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", ""));

            if (reason != null) {
                plugin.getDatabaseManager().unbanUser(uuid);
                sender.sendMessage(prefix + ColorUtils.color("&#27ae60✔ " + playerName + " yasağı kaldırıldı."));
            } else {
                sender.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Bu oyuncu zaten yasaklı değil."));
            }
        });
    }
}