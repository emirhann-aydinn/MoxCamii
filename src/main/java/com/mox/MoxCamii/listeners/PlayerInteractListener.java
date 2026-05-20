// src/main/java/com/mox/MoxCamii/listeners/PlayerInteractListener.java
package com.mox.MoxCamii.listeners;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
        String prefix = ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", ""));

        // ==========================================
        // YÖNETİCİ SEÇİM ARAÇLARI KONTROLÜ
        // ==========================================

        // Cami Seçici (Altın Balta)
        if (item != null && item.getType() == Material.GOLDEN_AXE && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Cami Seçici")) {
            event.setCancelled(true);
            if (event.getClickedBlock() == null) return;
            Location loc = event.getClickedBlock().getLocation();

            // Koordinat formatı
            String coords = "&8(&f" + loc.getBlockX() + "&8, &f" + loc.getBlockY() + "&8, &f" + loc.getBlockZ() + "&8)";

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                plugin.getRegionManager().setPos1(p.getUniqueId(), loc);
                p.sendMessage(prefix + ColorUtils.color("&#27ae60✔ Pos-1 seçildi " + coords));
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                plugin.getRegionManager().setPos2(p.getUniqueId(), loc);
                p.sendMessage(prefix + ColorUtils.color("&#27ae60✔ Pos-2 seçildi " + coords));
            }
            return;
        }

        // Kapı Seçici (Elmas Balta)
        if (item != null && item.getType() == Material.DIAMOND_AXE && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Kapı Seçici")) {
            event.setCancelled(true);
            if (event.getClickedBlock() == null) return;
            Location loc = event.getClickedBlock().getLocation();

            // Koordinat formatı
            String coords = "&8(&f" + loc.getBlockX() + "&8, &f" + loc.getBlockY() + "&8, &f" + loc.getBlockZ() + "&8)";

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                plugin.getRegionManager().setDoorPos1(p.getUniqueId(), loc);
                p.sendMessage(prefix + ColorUtils.color("&#2980b9✔ Kapı Pos-1 seçildi " + coords));
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                plugin.getRegionManager().setDoorPos2(p.getUniqueId(), loc);
                p.sendMessage(prefix + ColorUtils.color("&#2980b9✔ Kapı Pos-2 seçildi " + coords));
            }
            return;
        }

        // ==========================================
        // OYUNCU ETKİLEŞİMLERİ (ABDEST VE OTURMA)
        // ==========================================

        if (event.getClickedBlock() == null) return;
        Block clickedBlock = event.getClickedBlock();
        Location blockLoc = clickedBlock.getLocation();

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            // Abdest Musluğu Belirleyici Çubuğu
            if (item != null && item.getType() == Material.STICK && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Abdest Musluğu")) {
                event.setCancelled(true);
                if (plugin.getWuduManager().isTap(blockLoc)) {
                    plugin.getWuduManager().removeTap(blockLoc);
                    p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.TapRemoved", "&#e74c3cMusluk başarıyla kaldırıldı.")));
                } else {
                    plugin.getWuduManager().addTap(blockLoc);
                    p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.TapAdded", "&#27ae60Musluk başarıyla eklendi.")));
                }
                return;
            }

            // Abdest Alma İşlemi
            if (plugin.getWuduManager().isTap(blockLoc)) {
                event.setCancelled(true);
                if (plugin.getWuduManager().hasWudu(p.getUniqueId())) {
                    p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.AlreadyHasWudu", "&#e74c3cZaten abdestiniz bulunuyor.")));
                } else {
                    plugin.getWuduManager().giveWudu(p.getUniqueId());
                    p.sendTitle(
                            ColorUtils.color(plugin.getConfig().getString("Messages.WuduTakenTitle", "&#2980b9Abdest Alındı")),
                            ColorUtils.color(plugin.getConfig().getString("Messages.WuduTakenSubtitle", "&7Namaza hazırsınız.")),
                            10, 60, 10
                    );
                    p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_BUCKET_FILL, 1.0f, 1.0f);
                }
                return;
            }

            // Halı (Carpet) Tespit Sistemi - Direkt halıya veya halının altındaki bloğa tıklama desteği
            Block aboveBlock = clickedBlock.getRelative(BlockFace.UP);
            Location targetCarpetLoc = null;

            if (clickedBlock.getType().name().contains("CARPET")) {
                targetCarpetLoc = blockLoc;
            } else if (aboveBlock.getType().name().contains("CARPET")) {
                targetCarpetLoc = aboveBlock.getLocation();
            }

            // Eğer bir halı tespit edildiyse ve Cami bölgesinin içindeyse oturma işlemini başlat
            if (targetCarpetLoc != null && plugin.getRegionManager().isInRegion(targetCarpetLoc)) {
                if (p.isInsideVehicle()) return;

                event.setCancelled(true);

                // Abdest Kontrolü
                if (!plugin.getWuduManager().hasWudu(p.getUniqueId())) {
                    p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.NoWudu", "&#e74c3cAbdestsiz saf tutamazsınız.")));
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                // GSit benzeri kusursuz yükseklik ve hitbox ayarları
                double yOffset = plugin.getConfig().getDouble("Settings.Others.Seat-YOffset", -1.7);
                Location seatLoc = targetCarpetLoc.clone().add(0.5, yOffset, 0.5);

                ArmorStand seat = (ArmorStand) seatLoc.getWorld().spawnEntity(seatLoc, EntityType.ARMOR_STAND);
                seat.setGravity(false);
                seat.setVisible(false);
                seat.setInvulnerable(true);
                seat.setSmall(true);
                seat.setMarker(true); // Bloğun içine gömülmeyi ve vurulma buglarını engeller
                seat.setCollidable(false);
                seat.getPersistentDataContainer().set(new NamespacedKey(plugin, "CamiKoltuk"), PersistentDataType.BYTE, (byte) 1);

                seat.addPassenger(p);

                // Safa katılma mesajı ve sesi
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ColorUtils.color(plugin.getConfig().getString("Messages.SafJoined", "&#27ae60Safa katıldınız."))));
                p.playSound(p.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.0f);
            }
        }
    }
}