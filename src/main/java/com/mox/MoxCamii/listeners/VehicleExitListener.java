package com.mox.MoxCamii.listeners;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.spigotmc.event.entity.EntityDismountEvent;

public class VehicleExitListener implements Listener {

    private final MoxCamii plugin;

    public VehicleExitListener(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            if (event.getDismounted() instanceof ArmorStand) {
                ArmorStand seat = (ArmorStand) event.getDismounted();
                if (seat.getPersistentDataContainer().has(new NamespacedKey(plugin, "CamiKoltuk"), PersistentDataType.BYTE)) {

                    // Güvenli lokasyon hesaplaması (Bloğun üstüne oturtur)
                    double yOffset = plugin.getConfig().getDouble("Settings.Others.Seat-YOffset", -1.7);
                    Location safeLoc = seat.getLocation().clone().add(0, Math.abs(yOffset) + 0.2, 0);

                    seat.remove();

                    // 1 Tick gecikmeli ışınlama (Minecraft'ın inme buglarını sıfırlar)
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        p.teleport(safeLoc);
                    });

                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ColorUtils.color(plugin.getMessagesConfig().getString("Messages.SafLeft", "&#e74c3c✖ Saftan ayrıldınız."))));
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_HORSE_SADDLE, 1.0f, 1.0f);
                }
            }
        }
    }
}