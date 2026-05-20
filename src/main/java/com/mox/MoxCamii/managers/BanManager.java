package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class BanManager {

    private final MoxCamii plugin;

    public BanManager(MoxCamii plugin) {
        this.plugin = plugin;
    }

    public void banPlayer(String playerName, String reason, String author, long expireTime) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = op.getUniqueId();
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date());

        plugin.getDatabaseManager().banUser(uuid, op.getName() != null ? op.getName() : playerName, reason, author, date, expireTime);

        Player targetPlayer = Bukkit.getPlayer(uuid);
        if (targetPlayer != null && plugin.getRegionManager().isInRegion(targetPlayer.getLocation())) {
            Location banSpawn = plugin.getRegionManager().getBanSpawn();
            if (banSpawn != null) targetPlayer.teleport(banSpawn);
            String msg = plugin.getMessagesConfig().getString("Messages.Banned", "&cCami'ye girişiniz yasaklanmıştır! Sebep: {REASON}");
            targetPlayer.sendMessage(ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", "") + msg.replace("{REASON}", reason)));
        }
    }

    public void unbanPlayer(String playerName, CommandSender sender) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = op.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String reason = plugin.getDatabaseManager().getBanReason(uuid);
            String prefix = ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", ""));

            if (reason != null) {
                plugin.getDatabaseManager().unbanUser(uuid);
                sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.UnbanSuccess", "&#27ae60{PLAYER} yasağı kaldırıldı.").replace("{PLAYER}", playerName)));
            } else {
                sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.NotBanned", "&#e74c3cBu oyuncu yasaklı değil.")));
            }
        });
    }
}