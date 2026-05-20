package com.mox.MoxCamii.commands;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.managers.RegionManager;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CamiAdminCommand implements CommandExecutor {

    private final MoxCamii plugin;

    public CamiAdminCommand(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", ""));

        if (!sender.hasPermission("moxcamii.admin.*") && !sender.hasPermission("moxcamii.admin.help")) {
            sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.NoPermission", "")));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendAdminHelp(sender);
            return true;
        }

        String subCmd = args[0];

        if (subCmd.equalsIgnoreCase("reload") && checkPerm(sender, "moxcamii.admin.reload")) {
            plugin.reloadPlugin();
            sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.Reloaded", "")));
            return true;
        }

        if (subCmd.equalsIgnoreCase("wand") && checkPerm(sender, "moxcamii.admin.wand")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
            ItemMeta meta = wand.getItemMeta();
            meta.setDisplayName(ColorUtils.color("&#f1c40f&lCami Seçici"));
            wand.setItemMeta(meta);
            p.getInventory().addItem(wand);
            p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.WandGiven", "")));
            return true;
        }

        if (subCmd.equalsIgnoreCase("doorwand") && checkPerm(sender, "moxcamii.admin.wand")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            ItemStack wand = new ItemStack(Material.DIAMOND_AXE);
            ItemMeta meta = wand.getItemMeta();
            meta.setDisplayName(ColorUtils.color("&#2980b9&lKapı Seçici"));
            wand.setItemMeta(meta);
            p.getInventory().addItem(wand);
            p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.DoorWandGiven", "")));
            return true;
        }

        if (subCmd.equalsIgnoreCase("create") && checkPerm(sender, "moxcamii.admin.create")) {
            if (args.length < 2) {
                sender.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Kullanım: /camiadmin create <cami|kapı>"));
                return true;
            }
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            RegionManager rm = plugin.getRegionManager();

            if (args[1].equalsIgnoreCase("cami")) {
                if (rm.hasCamiRegion()) {
                    p.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Zaten kayıtlı bir Cami bölgesi var. Önce &f/camiadmin sil &eile mevcut bölgeyi silmelisiniz."));
                    return true;
                }
                if (rm.getPos1(p.getUniqueId()) == null || rm.getPos2(p.getUniqueId()) == null) {
                    p.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Önce seçim baltası ile 2 nokta seçmelisin."));
                    return true;
                }
                rm.createRegion(rm.getPos1(p.getUniqueId()), rm.getPos2(p.getUniqueId()));
                p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.RegionCreated", "")));

            } else if (args[1].equalsIgnoreCase("kapı") || args[1].equalsIgnoreCase("kapi")) {
                if (rm.hasDoorRegion()) {
                    p.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Zaten kayıtlı bir Kapı bölgesi var. Önce &f/camiadmin sil &eile mevcut bölgeyi silmelisiniz."));
                    return true;
                }
                if (rm.getPos1(p.getUniqueId()) == null || rm.getPos2(p.getUniqueId()) == null) {
                    p.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Önce seçim baltası ile 2 nokta seçmelisin."));
                    return true;
                }
                rm.createDoorRegion(rm.getPos1(p.getUniqueId()), rm.getPos2(p.getUniqueId()));
                p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.DoorCreated", "")));
            }
            return true;
        }

        if (subCmd.equalsIgnoreCase("sil") && checkPerm(sender, "moxcamii.admin.sil")) {
            if (sender instanceof Player) plugin.getGuiManager().openConfirmDeleteGUI((Player) sender, "Bölge");
            return true;
        }

        if (subCmd.equalsIgnoreCase("setspawn") && checkPerm(sender, "moxcamii.admin.setspawn")) {
            if (args.length < 2) {
                sender.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Kullanım: /camiadmin setspawn <abdest|ban>"));
                return true;
            }
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            if (args[1].equalsIgnoreCase("abdest")) {
                plugin.getRegionManager().setSpawn(p.getLocation());
                p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.SpawnSet", "")));
            } else if (args[1].equalsIgnoreCase("ban")) {
                plugin.getRegionManager().setBanSpawn(p.getLocation());
                p.sendMessage(prefix + ColorUtils.color("&#27ae60✔ Banlıların doğacağı nokta ayarlandı."));
            }
            return true;
        }

        if (subCmd.equalsIgnoreCase("abdest") && checkPerm(sender, "moxcamii.admin.abdest")) {
            if (!(sender instanceof Player)) return true;
            Player p = (Player) sender;
            ItemStack stick = new ItemStack(Material.STICK);
            ItemMeta meta = stick.getItemMeta();
            meta.setDisplayName(ColorUtils.color("&#27ae60&lAbdest Musluğu Belirleyici"));
            stick.setItemMeta(meta);
            p.getInventory().addItem(stick);
            p.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.AbdestWandGiven", "")));
            return true;
        }

        if (subCmd.equalsIgnoreCase("ban") && checkPerm(sender, "moxcamii.admin.ban")) {
            if (args.length < 3) {
                sender.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Kullanım: /camiadmin ban <oyuncu> <sebep>"));
                return true;
            }
            String targetName = args[1];
            StringBuilder rb = new StringBuilder();
            for (int i = 2; i < args.length; i++) rb.append(args[i]).append(" ");
            plugin.getBanManager().banPlayer(targetName, rb.toString().trim(), sender.getName(), 0L);
            sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.BanSuccess", "✔").replace("{PLAYER}", targetName)));
            return true;
        }

        if (subCmd.equalsIgnoreCase("tempban") && checkPerm(sender, "moxcamii.admin.ban")) {
            if (args.length < 4) {
                sender.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Kullanım: /camiadmin tempban <oyuncu> <saat> <sebep>"));
                return true;
            }
            String targetName = args[1];
            long hours = Long.parseLong(args[2]);
            long expireTime = System.currentTimeMillis() + (hours * 3600000L);
            StringBuilder rb = new StringBuilder();
            for (int i = 3; i < args.length; i++) rb.append(args[i]).append(" ");
            plugin.getBanManager().banPlayer(targetName, rb.toString().trim(), sender.getName(), expireTime);
            sender.sendMessage(prefix + ColorUtils.color("&#27ae60✔ " + targetName + " " + hours + " saatliğine yasaklandı."));
            return true;
        }

        if (subCmd.equalsIgnoreCase("unban") && checkPerm(sender, "moxcamii.admin.unban")) {
            if (args.length < 2) {
                sender.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Kullanım: /camiadmin unban <oyuncu>"));
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
                sender.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Kullanım: /camiadmin test <vakit>"));
                return true;
            }
            String vakit = args[1];
            String displayName = plugin.getMessagesConfig().getString("NamazIsimleri." + vakit, vakit);
            plugin.getPrayerTimeManager().triggerEzanTest(vakit, displayName);
            sender.sendMessage(prefix + ColorUtils.color(plugin.getMessagesConfig().getString("Messages.TestTriggered").replace("{VAKIT}", displayName)));
            return true;
        }

        sendAdminHelp(sender);
        return true;
    }

    private boolean checkPerm(CommandSender sender, String perm) {
        if (sender.hasPermission(perm) || sender.hasPermission("moxcamii.admin.*")) return true;
        sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", "") + plugin.getMessagesConfig().getString("Messages.NoPermission")));
        return false;
    }

    private void sendAdminHelp(CommandSender sender) {
        for (String line : plugin.getMessagesConfig().getStringList("Messages.AdminHelp")) {
            sender.sendMessage(ColorUtils.color(line));
        }
    }
}