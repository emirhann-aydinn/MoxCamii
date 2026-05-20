package com.mox.MoxCamii.commands;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.managers.RegionManager;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CamiAdminCommand implements CommandExecutor {

    private final MoxCamii plugin;

    public CamiAdminCommand(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", ""));

        if (!sender.hasPermission("moxcamii.admin.*") && !sender.hasPermission("moxcamii.admin.help")) {
            sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.NoPermission", "&cYetkiniz yok.")));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendAdminHelp(sender);
            return true;
        }

        String subCmd = args[0];

        if (subCmd.equalsIgnoreCase("reload") && checkPerm(sender, "moxcamii.admin.reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.Reloaded", "&#27ae60✔ Eklenti dosyaları başarıyla yenilendi!")));
            return true;
        }

        if (subCmd.equalsIgnoreCase("wand") && checkPerm(sender, "moxcamii.admin.wand")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
            ItemMeta meta = wand.getItemMeta();
            meta.setDisplayName(ColorUtils.color(plugin.getMessagesConfig().getString("WandName", "&#f1c40f&lMoxCamii Cami Seçici")));

            List<String> loreList = plugin.getMessagesConfig().getStringList("WandLore");
            if (!loreList.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String l : loreList) lore.add(ColorUtils.color(l));
                meta.setLore(lore);
            }
            wand.setItemMeta(meta);
            p.getInventory().addItem(wand);
            p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.WandGiven", "&#27ae60✔ Cami seçim baltası verildi.")));
            return true;
        }

        if (subCmd.equalsIgnoreCase("doorwand") && checkPerm(sender, "moxcamii.admin.wand")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
            ItemMeta meta = wand.getItemMeta();
            meta.setDisplayName(ColorUtils.color(plugin.getMessagesConfig().getString("DoorWandName", "&#3498db&lMoxCamii Kapı Seçici")));

            List<String> loreList = plugin.getMessagesConfig().getStringList("DoorWandLore");
            if (!loreList.isEmpty()) {
                List<String> lore = new ArrayList<>();
                for (String l : loreList) lore.add(ColorUtils.color(l));
                meta.setLore(lore);
            }
            wand.setItemMeta(meta);
            p.getInventory().addItem(wand);
            p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.DoorWandGiven", "&#2980b9✔ Kapı seçim baltası verildi.")));
            return true;
        }

        if (subCmd.equalsIgnoreCase("create") && checkPerm(sender, "moxcamii.admin.create")) {
            if (args.length < 2) {
                sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.CreateUsage", "&#e74c3c✖ Kullanım: /camiadmin create <cami|kapi>")));
                return true;
            }
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            RegionManager rm = plugin.getRegionManager();

            if (args[1].equalsIgnoreCase("cami")) {
                if (rm.hasCamiRegion()) {
                    p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.RegionExists", "&#e74c3c✖ Zaten kayıtlı bir Cami var.")));
                    return true;
                }
                Location p1 = rm.getPos1(p.getUniqueId());
                Location p2 = rm.getPos2(p.getUniqueId());

                if (p1 == null || p2 == null) {
                    p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.RegionNotSelected", "&#e74c3c✖ Lütfen önce balta ile 2 nokta seçin!")));
                    if (plugin.getSoundManager() != null) plugin.getSoundManager().playSound(p, "Error");
                    return true;
                }

                rm.createRegion(p1, p2);
                p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.RegionCreated", "&#27ae60✔ Bölge oluşturuldu.")));

            } else if (args[1].equalsIgnoreCase("kapı") || args[1].equalsIgnoreCase("kapi")) {
                if (rm.hasDoorRegion()) {
                    p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.DoorExists", "&#e74c3c✖ Zaten kayıtlı bir Kapı var.")));
                    return true;
                }
                Location p1 = rm.getDoorPos1(p.getUniqueId());
                Location p2 = rm.getDoorPos2(p.getUniqueId());

                if (p1 == null || p2 == null) {
                    p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.DoorNotSelected", "&#e74c3c✖ Lütfen önce kapı baltası ile 2 nokta seçin!")));
                    if (plugin.getSoundManager() != null) plugin.getSoundManager().playSound(p, "Error");
                    return true;
                }

                rm.createDoorRegion(p1, p2);
                p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.DoorCreated", "&#2980b9✔ Kapı bölgesi oluşturuldu.")));
            }
            return true;
        }

        if (subCmd.equalsIgnoreCase("sil") && checkPerm(sender, "moxcamii.admin.sil")) {
            if (sender instanceof Player) plugin.getGuiManager().openConfirmGUI((Player) sender, "Bölge", "DELETE_REGION");
            return true;
        }

        if (subCmd.equalsIgnoreCase("setspawn") && checkPerm(sender, "moxcamii.admin.setspawn")) {
            if (args.length < 2) return true;
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            if (args[1].equalsIgnoreCase("abdest")) {
                plugin.getRegionManager().setSpawn(p.getLocation());
                p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.SpawnSet", "&#27ae60✔ Doğma noktası ayarlandı.")));
            } else if (args[1].equalsIgnoreCase("ban")) {
                plugin.getRegionManager().setBanSpawn(p.getLocation());
                p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.BanSpawnSet", "&#27ae60✔ Banlıların doğacağı nokta ayarlandı.")));
            }
            return true;
        }

        if (subCmd.equalsIgnoreCase("abdest") && checkPerm(sender, "moxcamii.admin.abdest")) {

            if (args.length == 1) {
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                ItemStack stick = new ItemStack(Material.STICK);
                ItemMeta meta = stick.getItemMeta();
                meta.setDisplayName(ColorUtils.color(plugin.getMessagesConfig().getString("TapWandName", "&#27ae60&lAbdest Musluğu Belirleyici")));

                List<String> loreList = plugin.getMessagesConfig().getStringList("TapWandLore");
                if (!loreList.isEmpty()) {
                    List<String> lore = new ArrayList<>();
                    for (String l : loreList) lore.add(ColorUtils.color(l));
                    meta.setLore(lore);
                }
                stick.setItemMeta(meta);

                p.getInventory().addItem(stick);
                p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.AbdestWandGiven", "&#27ae60✔ Abdest çubuğu verildi.")));
                return true;
            }

            if (args.length >= 3) {
                String action = args[1];
                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[2]);

                if (target == null) {
                    sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.PlayerNotFound", "&#e74c3c✖ Oyuncu bulunamadı veya çevrimdışı.")));
                    return true;
                }

                if (action.equalsIgnoreCase("aldır") || action.equalsIgnoreCase("aldir")) {
                    plugin.getWuduManager().giveWudu(target.getUniqueId());
                    sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.AdminWuduGiven", "&#27ae60✔ {PLAYER} adlı oyuncuya abdest aldırıldı.").replace("{PLAYER}", target.getName())));
                    target.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.PlayerWuduGivenByAdmin", "&#27ae60✔ Bir yetkili size abdest aldırdı.")));
                    if (plugin.getSoundManager() != null) plugin.getSoundManager().playSound(target, "Wudu-Take");

                } else if (action.equalsIgnoreCase("kaldır") || action.equalsIgnoreCase("kaldir")) {
                    plugin.getWuduManager().removeWudu(target.getUniqueId());
                    sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.AdminWuduRemoved", "&#e74c3c✖ {PLAYER} adlı oyuncunun abdesti kaldırıldı.").replace("{PLAYER}", target.getName())));
                    target.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.PlayerWuduRemovedByAdmin", "&#e74c3c✖ Bir yetkili abdestinizi bozdu.")));
                    if (plugin.getSoundManager() != null) plugin.getSoundManager().playSound(target, "Wudu-Expire");
                }
            } else {
                sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.AbdestAdminUsage", "&#e74c3c✖ Kullanım: /camiadmin abdest VEYA /camiadmin abdest <aldır/kaldır> <oyuncu>")));
            }
            return true;
        }

        if (subCmd.equalsIgnoreCase("ban") && checkPerm(sender, "moxcamii.admin.ban")) {
            if (args.length < 3) {
                sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.BanUsage", "&#e74c3c✖ Kullanım: /camiadmin ban <oyuncu> <sebep>")));
                return true;
            }
            String targetName = args[1];
            StringBuilder rb = new StringBuilder();
            for (int i = 2; i < args.length; i++) rb.append(args[i]).append(" ");
            plugin.getBanManager().banPlayer(targetName, rb.toString().trim(), sender.getName(), 0L);
            sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.BanSuccess", "&#27ae60✔ {PLAYER} yasaklandı.").replace("{PLAYER}", targetName)));
            return true;
        }

        if (subCmd.equalsIgnoreCase("unban") && checkPerm(sender, "moxcamii.admin.unban")) {
            if (args.length < 2) {
                sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.UnbanUsage", "&#e74c3c✖ Kullanım: /camiadmin unban <oyuncu>")));
                return true;
            }
            plugin.getBanManager().unbanPlayer(args[1], sender);
            return true;
        }

        if (subCmd.equalsIgnoreCase("banlist") && checkPerm(sender, "moxcamii.admin.banlist")) {
            if (sender instanceof Player) plugin.getGuiManager().openBanListGUI((Player) sender, 1);
            return true;
        }

        if (subCmd.equalsIgnoreCase("test") && checkPerm(sender, "moxcamii.admin.test")) {
            if (args.length < 2) {
                sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.TestUsage", "&#e74c3c✖ Kullanım: /camiadmin test <vakit>")));
                return true;
            }
            String vakit = args[1];
            String displayName = plugin.getMessagesConfig().getString("Namaz-Names." + vakit, vakit);
            plugin.getPrayerTimeManager().triggerEzanTest(vakit, displayName);
            sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.TestTriggered", "&#2980b9✔ Test ezanı tetiklendi.").replace("{VAKIT}", displayName)));
            return true;
        }

        // --- YENİ EKLENEN SEZON BİTİRME KOMUTU ---
        if (subCmd.equalsIgnoreCase("sezon") && checkPerm(sender, "moxcamii.admin.sezon")) {
            if (args.length < 2 || !args[1].equalsIgnoreCase("bitir")) {
                sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.SezonUsage", "&#e74c3c✖ Kullanım: /camiadmin sezon bitir")));
                return true;
            }
            plugin.getRewardManager().forceEndSeason();
            sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.SezonEnded", "&#27ae60✔ Aylık sezon erken bitirildi ve ödüller sıralamadaki oyunculara tanımlandı!")));
            return true;
        }

        sendAdminHelp(sender);
        return true;
    }

    private boolean checkPerm(CommandSender sender, String perm) {
        if (sender.hasPermission(perm) || sender.hasPermission("moxcamii.admin.*")) return true;
        sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", "") + plugin.getMessagesConfig().getString("Messages.NoPermission", "&cYetkiniz yok.")));
        return false;
    }

    private void sendAdminHelp(CommandSender sender) {
        List<String> helpLines = plugin.getMessagesConfig().getStringList("Commands.AdminHelp");
        if (helpLines == null || helpLines.isEmpty()) {
            sender.sendMessage(ColorUtils.color("&c(Hata) Commands.AdminHelp listesi messages.yml dosyanızda bulunamadı!"));
            return;
        }
        for (String line : helpLines) {
            sender.sendMessage(ColorUtils.color(line));
        }
    }
}