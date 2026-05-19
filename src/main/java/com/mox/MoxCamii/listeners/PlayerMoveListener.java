// src/main/java/com/mox/MoxCamii/listeners/PlayerMoveListener.java
package com.mox.MoxCamii.listeners;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class PlayerMoveListener implements Listener {

    private final MoxCamii plugin;

    public PlayerMoveListener(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;

        Player p = event.getPlayer();

        boolean wasInDoor = plugin.getRegionManager().isInDoorRegion(from);
        boolean isInDoor = plugin.getRegionManager().isInDoorRegion(to);

        boolean wasInRegion = plugin.getRegionManager().isInRegion(from);
        boolean isInRegion = plugin.getRegionManager().isInRegion(to);

        String prefix = ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", "&8[&6MoxCamii&8] &7"));

        String banReason = plugin.getDatabaseManager().getBanReason(p.getUniqueId());
        if (banReason != null && (!wasInRegion && isInRegion || !wasInDoor && isInDoor)) {
            String banMsg = plugin.getConfig().getString("Messages.Banned");
            if (banMsg != null) {
                p.sendMessage(prefix + ColorUtils.color(banMsg.replace("{REASON}", banReason)));
            }

            Location spawn = plugin.getRegionManager().getSpawn();
            if (spawn != null) {
                p.teleport(spawn);
            } else {
                event.setCancelled(true);
            }
            return;
        }

        if (isInDoor && !plugin.getWuduManager().hasWudu(p.getUniqueId())) {
            if (wasInDoor) {
                Location spawn = plugin.getRegionManager().getSpawn();
                if (spawn != null) p.teleport(spawn);
                return;
            }

            event.setTo(from);

            p.sendTitle(
                    ColorUtils.color(plugin.getConfig().getString("Messages.DoorDeniedTitle")),
                    ColorUtils.color(plugin.getConfig().getString("Messages.DoorDeniedSubtitle")),
                    5, 20, 5
            );
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);

            double strength = plugin.getConfig().getDouble("Settings.DoorPushbackStrength", 1.5);
            Vector push = p.getLocation().getDirection().multiply(-1).normalize().multiply(strength).setY(0.4);
            if (!Double.isNaN(push.getX()) && !Double.isNaN(push.getZ())) {
                p.setVelocity(push);
            }
        }
    }
}