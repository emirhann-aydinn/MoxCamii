// src/main/java/com/mox/MoxCamii/commands/NamazlarCommand.java
package com.mox.MoxCamii.commands;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import com.mox.MoxCamii.utils.HeadUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NamazlarCommand implements CommandExecutor {

    private final MoxCamii plugin;

    public NamazlarCommand(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bu komut sadece oyuncular icindir.");
            return true;
        }

        Player p = (Player) sender;
        String title = ColorUtils.color(plugin.getConfig().getString("GUI.Title", "&8&lNamaz Vakitleri"));
        int size = plugin.getConfig().getInt("GUI.Size", 27);

        Inventory inv = Bukkit.createInventory(null, size, title);
        Map<String, String> times = plugin.getPrayerTimeManager().getPrayerTimes();

        String[] order = {"Imsak", "Gunes", "Ogle", "Ikindi", "Aksam", "Yatsi"};
        int[] slots = {10, 11, 12, 14, 15, 16};

        for (int i = 0; i < order.length; i++) {
            String vakitRaw = order[i];
            String displayName = plugin.getConfig().getString("NamazIsimleri." + vakitRaw, vakitRaw);
            String timeStr = times.getOrDefault(vakitRaw, "Bilinmiyor");
            String base64 = plugin.getConfig().getString("GUI.Heads." + vakitRaw, "");

            ItemStack head = HeadUtils.getCustomHead(base64);
            ItemMeta meta = head.getItemMeta();

            String itemName = plugin.getConfig().getString("GUI.ItemName", "{VAKIT} &fVakti");
            meta.setDisplayName(ColorUtils.color(itemName.replace("{VAKIT}", displayName)));

            List<String> loreList = plugin.getConfig().getStringList("GUI.ItemLore");
            List<String> coloredLore = new ArrayList<>();
            for (String l : loreList) {
                coloredLore.add(ColorUtils.color(l.replace("{SAAT}", timeStr).replace("{VAKIT}", displayName)));
            }
            meta.setLore(coloredLore);
            head.setItemMeta(meta);

            inv.setItem(slots[i], head);
        }

        p.openInventory(inv);
        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);

        return true;
    }
}