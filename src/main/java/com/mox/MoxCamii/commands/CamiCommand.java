package com.mox.MoxCamii.commands;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
            if (!p.hasPermission("moxcamii.sorgu")) {
                p.sendMessage(ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", "") + plugin.getConfig().getString("Messages.NoPermission")));
                return true;
            }
            // Sadece /cami sorgu yazarsa kendini, isim yazarsa o oyuncuyu sorgular
            String target = (args.length > 1) ? args[1] : p.getName();
            plugin.getGuiManager().openSorguGUI(p, target);
            return true;
        }

        if (args[0].equalsIgnoreCase("vakitler") || args[0].equalsIgnoreCase("namazlar")) {
            plugin.getGuiManager().openVakitlerGUI(p);
            return true;
        }

        if (args[0].equalsIgnoreCase("top")) {
            plugin.getGuiManager().openTopGUI(p, "monthly");
            return true;
        }

        sendPlayerHelp(p);
        return true;
    }

    private void sendPlayerHelp(Player p) {
        for (String line : plugin.getConfig().getStringList("Messages.PlayerHelp")) {
            p.sendMessage(ColorUtils.color(line));
        }
        if (p.hasPermission("moxcamii.admin.*")) {
            p.sendMessage(ColorUtils.color("  &8(Yönetici komutları için: &f/camiadmin&8)"));
        }
    }
}