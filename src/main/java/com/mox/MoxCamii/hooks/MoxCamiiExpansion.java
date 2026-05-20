package com.mox.MoxCamii.hooks;

import com.mox.MoxCamii.MoxCamii;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class MoxCamiiExpansion extends PlaceholderExpansion {

    private final MoxCamii plugin;

    public MoxCamiiExpansion(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() { return "moxcamii"; }
    @Override
    public String getAuthor() { return "MoxNetwork"; }
    @Override
    public String getVersion() { return "2.5"; }
    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";

        if (params.equalsIgnoreCase("namaz_count")) {
            int total = plugin.getDatabaseManager().getAllStats(player.getUniqueId()).getOrDefault("total", 0);
            return String.valueOf(total);
        }

        if (params.equalsIgnoreCase("namaz_monthly")) {
            int monthly = plugin.getDatabaseManager().getAllStats(player.getUniqueId()).getOrDefault("monthly", 0);
            return String.valueOf(monthly);
        }

        if (params.equalsIgnoreCase("abdest_durumu")) {
            return plugin.getWuduManager().hasWudu(player.getUniqueId()) ? "§aAbdestli" : "§cAbdestsiz";
        }

        if (params.equalsIgnoreCase("son_kilan_namaz")) {
            return plugin.getDatabaseManager().getLastPrayer(player.getUniqueId());
        }

        return null;
    }
}