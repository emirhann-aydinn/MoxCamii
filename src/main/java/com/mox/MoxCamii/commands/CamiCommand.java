// src/main/java/com/mox/MoxCamii/commands/CamiCommand.java
package com.mox.MoxCamii.commands;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.managers.RegionManager;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CamiCommand implements CommandExecutor {

    private final MoxCamii plugin;

    public CamiCommand(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", "&8[&6MoxCamii&8] &7"));

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendPlayerHelp(sender);
            return true;
        }

        // =====================================
        // OYUNCU KOMUTLARI
        // =====================================
        if (args[0].equalsIgnoreCase("vakitler") || args[0].equalsIgnoreCase("namazlar")) {
            if (sender instanceof Player) {
                ((Player) sender).performCommand("vakitler");
            } else {
                sender.sendMessage(prefix + ColorUtils.color("&cBu komut sadece oyun içinden kullanılabilir."));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("sorgu")) {
            if (!sender.hasPermission("moxcamii.sorgu")) {
                sender.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.NoPermission")));
                return true;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                OfflinePlayer target;
                String targetName;
                if (args.length == 1) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(prefix + ColorUtils.color("&cKonsoldan isim belirtmelisiniz. /cami sorgu <isim>"));
                        return;
                    }
                    target = (Player) sender;
                    targetName = target.getName();
                } else {
                    targetName = args[1];
                    target = Bukkit.getOfflinePlayer(targetName);
                }

                if (!plugin.getDatabaseManager().hasData(target.getUniqueId()) && !target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Veritabanında veya sunucuda böyle bir oyuncu bulunamadı."));
                    return;
                }

                int count = plugin.getDatabaseManager().getNamazCount(target.getUniqueId());
                String lastPrayer = plugin.getDatabaseManager().getLastPrayer(target.getUniqueId());
                String banReason = plugin.getDatabaseManager().getBanReason(target.getUniqueId());

                String banStatusStr = (banReason != null)
                        ? plugin.getConfig().getString("Messages.SorguBanned").replace("{REASON}", banReason)
                        : plugin.getConfig().getString("Messages.SorguNotBanned");

                sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Messages.SorguHeader").replace("{PLAYER}", target.getName() != null ? target.getName() : targetName)));
                sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Messages.SorguNamazCount").replace("{COUNT}", String.valueOf(count))));
                sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Messages.SorguLastPrayer").replace("{LAST}", lastPrayer)));
                sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Messages.SorguBanStatus").replace("{BAN_STATUS}", banStatusStr)));
                sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Messages.SorguFooter")));
            });
            return true;
        }

        // =====================================
        // ADMİN KOMUTLARI AĞACI (/cami admin)
        // =====================================
        if (args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("moxcamii.admin")) {
                sender.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.NoPermission")));
                return true;
            }

            if (args.length == 1 || args[1].equalsIgnoreCase("help")) {
                sendAdminHelp(sender);
                return true;
            }

            String subCmd = args[1];

            if (subCmd.equalsIgnoreCase("reload")) {
                plugin.reloadPlugin();
                sender.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.Reloaded")));
                return true;
            }

            if (subCmd.equalsIgnoreCase("wand")) {
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
                ItemMeta meta = wand.getItemMeta();
                meta.setDisplayName(ColorUtils.color("&#f1c40f&lCami Seçici"));
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtils.color("&7Sol tık: &aPos1"));
                lore.add(ColorUtils.color("&7Sağ tık: &aPos2"));
                meta.setLore(lore);
                wand.setItemMeta(meta);
                p.getInventory().addItem(wand);
                p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.WandGiven")));
                return true;
            }

            if (subCmd.equalsIgnoreCase("doorwand")) {
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                ItemStack wand = new ItemStack(Material.DIAMOND_AXE);
                ItemMeta meta = wand.getItemMeta();
                meta.setDisplayName(ColorUtils.color("&#2980b9&lKapı Seçici"));
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtils.color("&7Sol tık: &aKapı Pos1"));
                lore.add(ColorUtils.color("&7Sağ tık: &aKapı Pos2"));
                meta.setLore(lore);
                wand.setItemMeta(meta);
                p.getInventory().addItem(wand);
                p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.DoorWandGiven")));
                return true;
            }

            if (subCmd.equalsIgnoreCase("create")) {
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                RegionManager rm = plugin.getRegionManager();
                if (rm.getPos1(p.getUniqueId()) == null || rm.getPos2(p.getUniqueId()) == null) {
                    p.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Önce /cami admin wand ile 2 nokta seçmelisin."));
                    return true;
                }
                rm.createRegion(rm.getPos1(p.getUniqueId()), rm.getPos2(p.getUniqueId()));
                p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.RegionCreated")));
                return true;
            }

            if (subCmd.equalsIgnoreCase("createdoor")) {
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                RegionManager rm = plugin.getRegionManager();
                if (rm.getDoorPos1(p.getUniqueId()) == null || rm.getDoorPos2(p.getUniqueId()) == null) {
                    p.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Önce /cami admin doorwand ile kapı bölgesi seçmelisin."));
                    return true;
                }
                rm.createDoorRegion(rm.getDoorPos1(p.getUniqueId()), rm.getDoorPos2(p.getUniqueId()));
                p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.DoorCreated")));
                return true;
            }

            // YENİ EKLENEN SİLME KOMUTLARI
            if (subCmd.equalsIgnoreCase("camisil")) {
                plugin.getRegionManager().deleteRegion();
                sender.sendMessage(prefix + ColorUtils.color("&#e74c3c✔ Cami bölgesi silindi!"));
                return true;
            }

            if (subCmd.equalsIgnoreCase("kapisil")) {
                plugin.getRegionManager().deleteDoorRegion();
                sender.sendMessage(prefix + ColorUtils.color("&#e74c3c✔ Kapı bölgesi silindi!"));
                return true;
            }

            if (subCmd.equalsIgnoreCase("setspawn")) {
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                plugin.getRegionManager().setSpawn(p.getLocation());
                p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.SpawnSet")));
                return true;
            }

            if (subCmd.equalsIgnoreCase("abdestmod")) {
                if (!(sender instanceof Player)) return true;
                Player p = (Player) sender;
                ItemStack stick = new ItemStack(Material.STICK);
                ItemMeta meta = stick.getItemMeta();
                meta.setDisplayName(ColorUtils.color("&#27ae60&lAbdest Musluğu Belirleyici"));
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtils.color("&7Bunu musluk olacak bloğa sağ tıkla."));
                meta.setLore(lore);
                stick.setItemMeta(meta);
                p.getInventory().addItem(stick);
                p.sendMessage(prefix + ColorUtils.color(plugin.getConfig().getString("Messages.AbdestWandGiven")));
                return true;
            }

            if (subCmd.equalsIgnoreCase("ban")) {
                if (args.length < 4) {
                    sender.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Kullanım: /cami admin ban <oyuncu> <sebep>"));
                    return true;
                }
                String targetName = args[2];
                StringBuilder reasonBuilder = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    reasonBuilder.append(args[i]).append(" ");
                }
                String reason = reasonBuilder.toString().trim();
                plugin.getBanManager().banPlayer(targetName, reason, sender.getName());
                sender.sendMessage(prefix + ColorUtils.color("&#27ae60✔ " + targetName + " banlandı."));
                return true;
            }

            if (subCmd.equalsIgnoreCase("unban")) {
                if (args.length < 3) {
                    sender.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Kullanım: /cami admin unban <oyuncu>"));
                    return true;
                }
                plugin.getBanManager().unbanPlayer(args[2], sender);
                return true;
            }

            if (subCmd.equalsIgnoreCase("odulver")) {
                plugin.getRewardManager().distributeRewards("&#f1c40fManuel/Test");
                sender.sendMessage(prefix + ColorUtils.color("&#27ae60✔ Manuel ödül dağıtımı tetiklendi."));
                return true;
            }

            if (subCmd.equalsIgnoreCase("test")) {
                if (args.length < 3) {
                    sender.sendMessage(prefix + ColorUtils.color("&#e74c3c✖ Kullanım: /cami admin test <vakit> (örn: Ogle)"));
                    return true;
                }
                String vakit = args[2];
                String displayName = plugin.getConfig().getString("NamazIsimleri." + vakit, vakit);
                plugin.getPrayerTimeManager().triggerEzanTest(vakit, displayName);
                String msg = plugin.getConfig().getString("Messages.TestTriggered", "&a{VAKIT} ezanı okundu!");
                sender.sendMessage(prefix + ColorUtils.color(msg.replace("{VAKIT}", displayName)));
                if (sender instanceof Player) {
                    ((Player) sender).playSound(((Player) sender).getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                }
                return true;
            }

            sendAdminHelp(sender);
            return true;
        }

        sendPlayerHelp(sender);
        return true;
    }

    private void sendPlayerHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Messages.PlayerHelpHeader", "&8&m----------------------------------------\n&#27ae60&lMoxCamii Sistemi\n")));
        sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Messages.PlayerHelpSorgu", "&#f1c40f/cami sorgu [oyuncu] &7- Cami kayıtlarını gösterir.")));
        sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Messages.PlayerHelpVakitler", "&#f1c40f/cami vakitler &7- Namaz vakitlerini gösterir.")));

        if (sender.hasPermission("moxcamii.admin")) {
            sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Messages.PlayerHelpAdminTip", "\n&#198a44✦ &7Yönetici komutları için: &f/cami admin")));
        }

        sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Messages.PlayerHelpFooter", "&8&m----------------------------------------")));
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Messages.AdminHelpHeader", "&8&m----------------------------------------\n&#e74c3c&lMoxCamii Admin Paneli\n")));
        sender.sendMessage(ColorUtils.color("&#f1c40f/cami admin reload &7- Dosyaları yeniler."));
        sender.sendMessage(ColorUtils.color("&#f1c40f/cami admin wand &7- Cami alanını seçer."));
        sender.sendMessage(ColorUtils.color("&#f1c40f/cami admin create &7- Cami alanını kaydeder."));
        sender.sendMessage(ColorUtils.color("&#f1c40f/cami admin camisil &7- Kayıtlı cami alanını siler."));
        sender.sendMessage(ColorUtils.color("&#f1c40f/cami admin doorwand &7- Kapı alanını seçer."));
        sender.sendMessage(ColorUtils.color("&#f1c40f/cami admin createdoor &7- Kapı alanını kaydeder."));
        sender.sendMessage(ColorUtils.color("&#f1c40f/cami admin kapisil &7- Kayıtlı kapı alanını siler."));
        sender.sendMessage(ColorUtils.color("&#f1c40f/cami admin setspawn &7- Spawn belirler."));
        sender.sendMessage(ColorUtils.color("&#f1c40f/cami admin abdestmod &7- Musluk belirler."));
        sender.sendMessage(ColorUtils.color("&#f1c40f/cami admin ban <oyuncu> <sebep>"));
        sender.sendMessage(ColorUtils.color("&#f1c40f/cami admin unban <oyuncu>"));
        sender.sendMessage(ColorUtils.color("&#f1c40f/cami admin odulver &7- Zorla ödül verir."));
        sender.sendMessage(ColorUtils.color("&#f1c40f/cami admin test <vakit> &7- Namaz test eder."));
        sender.sendMessage(ColorUtils.color(plugin.getConfig().getString("Messages.AdminHelpFooter", "&8&m----------------------------------------")));
    }
}