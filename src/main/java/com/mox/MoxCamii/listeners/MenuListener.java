// src/main/java/com/mox/MoxCamii/listeners/MenuListener.java
package com.mox.MoxCamii.listeners;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    private final MoxCamii plugin;

    public MenuListener(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = ColorUtils.color(plugin.getConfig().getString("GUI.Title", "&8&lNamaz Vakitleri"));
        if (e.getView().getTitle().equals(title)) {
            e.setCancelled(true);
        }
    }
}