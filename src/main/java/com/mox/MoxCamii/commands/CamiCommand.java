package com.mox.MoxCamii.commands;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CamiCommand implements CommandExecutor {

    private final MoxCamii plugin;

    public CamiCommand(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bu komut sadece oyun icinden kullanilabilir.");
            return true;
        }
        Player p = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("yardım") || args[0].equalsIgnoreCase("help")) {
            sendPlayerHelp(p);
            return true;
        }

        if (args[0].equalsIgnoreCase("sorgu")) {
            String perm = plugin.getPermissionManager() != null ? plugin.getPermissionManager().get("Sorgu", "moxcamii.sorgu") : "moxcamii.sorgu";
            if (!p.hasPermission(perm)) {
                p.sendMessage(ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", "") + plugin.getMessagesConfig().getString("Messages.NoPermission", "&cYetkiniz yok.")));
                return true;
            }
            String target = (args.length > 1) ? args[1] : p.getName();
            plugin.getGuiManager().openSorguGUI(p, target);
            return true;
        }

        if (args[0].equalsIgnoreCase("vakitler") || args[0].equalsIgnoreCase("namazlar")) {
            plugin.getGuiManager().openVakitlerGUI(p);
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            plugin.getGuiManager().openTopGUI(p);
            return true;
        }

        sendPlayerHelp(p);
        return true;
    }

    private void sendPlayerHelp(Player p) {
        List<String> helpLines = plugin.getMessagesConfig().getStringList("Commands.PlayerHelp");
        if (helpLines == null || helpLines.isEmpty()) {
            p.sendMessage(ColorUtils.color("&c(Hata) Commands.PlayerHelp listesi messages.yml dosyanızda bulunamadı!"));
            return;
        }
        for (String line : helpLines) {
            p.sendMessage(ColorUtils.color(line));
        }

        String adminPerm = plugin.getPermissionManager() != null ? plugin.getPermissionManager().get("Admin-Tumu", "moxcamii.admin.*") : "moxcamii.admin.*";
        if (p.hasPermission(adminPerm)) {
            p.sendMessage(ColorUtils.color("  &8(Yönetici komutları için: &f/camiadmin&8)"));
        }
    }
}