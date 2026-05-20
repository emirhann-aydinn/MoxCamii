package com.mox.MoxCamii.listeners;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.managers.GuiManager;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MenuListener implements Listener {

    private final MoxCamii plugin;
    private final Map<UUID, Long> clickCooldowns = new HashMap<>();

    public MenuListener(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();

        GuiManager gm = plugin.getGuiManager();
        if (gm.isPluginMenu(title)) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;

            long now = System.currentTimeMillis();
            if (now - clickCooldowns.getOrDefault(p.getUniqueId(), 0L) < 3000) {
                p.sendMessage(ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", "") + "&cÇok hızlı işlem yapıyorsunuz. Lütfen bekleyin."));
                plugin.getSoundManager().playSound(p, "Error");
                return;
            }

            // Ana Namaz Vakitleri -> Özel Namazlar Geçişi
            if (title.equals(gm.getMenuTitle("clocks")) && e.getCurrentItem().getType() == Material.BOOK) {
                clickCooldowns.put(p.getUniqueId(), now);
                gm.openSpecialVakitlerGUI(p);
            }
            // Özel Namazlar -> Ana Namaz Vakitleri (Geri Dönüş)
            else if (title.equals(gm.getMenuTitle("special_clocks")) && e.getCurrentItem().getType() == Material.ARROW) {
                clickCooldowns.put(p.getUniqueId(), now);
                gm.openVakitlerGUI(p);
            }
            // Sıralama Filtre Değiştirme
            else if (title.contains("Sıralama") && e.getCurrentItem().getType() == Material.HOPPER) {
                clickCooldowns.put(p.getUniqueId(), now);
                gm.cycleSort(p);
            }
            // Yasaklılar Menüsü Ban Kaldırma İşlemi
            else if (title.contains("Yasaklılar")) {
                if (e.getClick().isShiftClick() && e.getClick().isRightClick() && e.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                    clickCooldowns.put(p.getUniqueId(), now);
                    String target = org.bukkit.ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
                    gm.openConfirmGUI(p, target, "UNBAN");
                }
            }
            // Onay Menüsü
            else if (title.contains("Emin misiniz")) {
                clickCooldowns.put(p.getUniqueId(), now);
                if (e.getCurrentItem().getType().name().contains("GREEN")) {
                    gm.executeConfirmAction(p);
                } else if (e.getCurrentItem().getType().name().contains("RED")) {
                    p.closeInventory();
                }
            }
        }
    }
}