// src/main/java/com/mox/MoxCamii/hooks/MoxCamiiExpansion.java
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
    public String getIdentifier() {
        return "moxcamii";
    }

    @Override
    public String getAuthor() {
        return "MoxNetwork";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return "";

        if (params.equalsIgnoreCase("namaz_count")) {
            return String.valueOf(plugin.getDatabaseManager().getNamazCount(player.getUniqueId()));
        }

        return null;
    }
}