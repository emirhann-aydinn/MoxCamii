// src/main/java/com/mox/MoxCamii/listeners/VehicleExitListener.java
package com.mox.MoxCamii.listeners;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.persistence.PersistentDataType;

public class VehicleExitListener implements Listener {

    private final MoxCamii plugin;

    public VehicleExitListener(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getExited() instanceof Player) {
            Player p = (Player) event.getExited();
            if (event.getVehicle() instanceof ArmorStand) {
                ArmorStand seat = (ArmorStand) event.getVehicle();
                if (seat.getPersistentDataContainer().has(new NamespacedKey(plugin, "CamiKoltuk"), PersistentDataType.BYTE)) {
                    seat.remove();
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ColorUtils.color(plugin.getConfig().getString("Messages.SafLeft"))));
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_HORSE_SADDLE, 1.0f, 1.0f);
                }
            }
        }
    }
}