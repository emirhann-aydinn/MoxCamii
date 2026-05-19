// src/main/java/com/mox/MoxCamii/listeners/PlayerQuitListener.java
package com.mox.MoxCamii.listeners;

import com.mox.MoxCamii.MoxCamii;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

public class PlayerQuitListener implements Listener {

    private final MoxCamii plugin;

    public PlayerQuitListener(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        if (p.isInsideVehicle() && p.getVehicle() instanceof ArmorStand) {
            ArmorStand seat = (ArmorStand) p.getVehicle();
            if (seat.getPersistentDataContainer().has(new NamespacedKey(plugin, "CamiKoltuk"), PersistentDataType.BYTE)) {
                seat.remove();
            }
        }
    }
}