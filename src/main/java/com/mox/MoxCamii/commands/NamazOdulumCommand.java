package com.mox.MoxCamii.commands;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class NamazOdulumCommand implements CommandExecutor {
    private final MoxCamii plugin;

    public NamazOdulumCommand(MoxCamii plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bu komut sadece oyuncular icindir.");
            return true;
        }

        Player p = (Player) sender;
        FileConfiguration rewardCfg = plugin.getRewardManager().getConfig();
        String prefix = ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", ""));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String tier = plugin.getDatabaseManager().getPendingReward(p.getUniqueId());

            if (tier == null) {
                String noReward = rewardCfg.getString("monthly-rewards.no-reward-message", "&#e74c3c✖ Hesabınıza tanımlanmış bir namaz ödülü bulunmamaktadır!");
                p.sendMessage(prefix + ColorUtils.color(noReward));
                return;
            }

            List<String> cmds = rewardCfg.getStringList("monthly-rewards.tops." + tier + ".commands");

            Bukkit.getScheduler().runTask(plugin, () -> {
                for (String cmd : cmds) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{PLAYER}", p.getName()).replace("{player}", p.getName()));
                }

                plugin.getDatabaseManager().removePendingReward(p.getUniqueId());
                String claimed = rewardCfg.getString("monthly-rewards.reward-claimed-message", "&#27ae60✔ Aylık namaz sıralaması ödülünüz başarıyla tanımlandı.");
                p.sendMessage(prefix + ColorUtils.color(claimed));
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            });
        });

        return true;
    }
}