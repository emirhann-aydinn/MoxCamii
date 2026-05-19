// src/main/java/com/mox/MoxCamii/listeners/PlayerInteractListener.java
package com.mox.MoxCamii.listeners;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class PlayerInteractListener implements Listener {

    private final MoxCamii plugin;

    public PlayerInteractListener(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        String prefix = ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", "&8[&6MoxCamii&8] &7"));

        if (item != null && item.getType() == Material.GOLDEN_AXE && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Cami Seçici")) {
            event.setCancelled(true);
            if (event.getClickedBlock() == null) return;
            Location loc = event.getClickedBlock().getLocation();
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                plugin.getRegionManager().setPos1(p.getUniqueId(), loc);
                p.sendMessage(prefix + ColorUtils.color("&aPos1 ayarlandı."));
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                plugin.getRegionManager().setPos2(p.getUniqueId(), loc);
                p.sendMessage(prefix + ColorUtils.color("&aPos2 ayarlandı."));
            }
            return;
        }

        if (item != null && item.getType() == Material.DIAMOND_AXE && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Kapı Seçici")) {
            event.setCancelled(true);
            if (event.getClickedBlock() == null) return;
            Location loc = event.getClickedBlock().getLocation();
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                plugin.getRegionManager().setDoorPos1(p.getUniqueId(), loc);
                p.sendMessage(prefix + ColorUtils.color("&bKapı Pos1 ayarlandı."));
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                plugin.getRegionManager().setDoorPos2(p.getUniqueId(), loc);
                p.sendMessage(prefix + ColorUtils.color("&bKapı Pos2 ayarlandı."));
            }
            return;
        }

        if (event.getClickedBlock() == null) return;
        Location blockLoc = event.getClickedBlock().getLocation();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item != null && item.getType() == Material.STICK && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Abdest Musluğu")) {
                event.setCancelled(true);
                if (plugin.getWuduManager().isTap(blockLoc)) {
                    plugin.getWuduManager().removeTap(blockLoc);
                    p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.TapRemoved")));
                } else {
                    plugin.getWuduManager().addTap(blockLoc);
                    p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.TapAdded")));
                }
                return;
            }

            if (plugin.getWuduManager().isTap(blockLoc)) {
                event.setCancelled(true);
                if (plugin.getWuduManager().hasWudu(p.getUniqueId())) {
                    p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.AlreadyHasWudu")));
                } else {
                    plugin.getWuduManager().giveWudu(p.getUniqueId());
                    p.sendTitle(
                            ColorUtils.color(plugin.getConfig().getString("Messages.WuduTakenTitle")),
                            ColorUtils.color(plugin.getConfig().getString("Messages.WuduTakenSubtitle")),
                            10, 60, 10
                    );
                    p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_BUCKET_FILL, 1.0f, 1.0f);
                }
                return;
            }

            Material type = event.getClickedBlock().getType();
            if (type.name().contains("CARPET") && plugin.getRegionManager().isInRegion(blockLoc)) {
                if (p.isInsideVehicle()) return;

                event.setCancelled(true);

                if (!plugin.getWuduManager().hasWudu(p.getUniqueId())) {
                    p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.NoWudu")));
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                // GSit stili milimetrik ayarlanabilir yükseklik (Configden ayarlanabilir)
                double yOffset = plugin.getConfig().getDouble("Settings.SeatYOffset", -1.2);
                Location seatLoc = blockLoc.clone().add(0.5, yOffset, 0.5);
                ArmorStand seat = (ArmorStand) seatLoc.getWorld().spawnEntity(seatLoc, EntityType.ARMOR_STAND);
                seat.setGravity(false);
                seat.setVisible(false);
                seat.setInvulnerable(true);
                seat.setSmall(true); // Küçük yaparak collider bozulmasını önlüyoruz
                // seat.setMarker(true); yapmıyoruz çünkü oturma mekaniği bozulabilir (sabit yOffset kullanıyoruz)
                seat.getPersistentDataContainer().set(new NamespacedKey(plugin, "CamiKoltuk"), PersistentDataType.BYTE, (byte) 1);
                seat.addPassenger(p);

                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ColorUtils.color(plugin.getConfig().getString("Messages.SafJoined"))));
                p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.0f);
            }
        }
    }
}