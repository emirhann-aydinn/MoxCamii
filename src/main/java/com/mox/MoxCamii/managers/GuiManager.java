package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import com.mox.MoxCamii.utils.HeadUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.*;

public class GuiManager {

    private final MoxCamii plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();

    public GuiManager(MoxCamii plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        configs.clear();
        String[] menus = {"clocks", "own-info", "top", "ban-list", "confirm"};
        for (String m : menus) {
            File file = new File(plugin.getDataFolder(), "menus/" + m + ".yml");
            if (file.exists()) {
                configs.put(m, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    public void openVakitlerGUI(Player p) {
        FileConfiguration cfg = configs.get("clocks");
        if (cfg == null) {
            p.sendMessage(ColorUtils.color("&cMenü dosyası (clocks.yml) bulunamadı!"));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, cfg.getInt("size", 27), ColorUtils.color(cfg.getString("title", "&8Namaz Vakitleri")));
        Map<String, String> times = plugin.getPrayerTimeManager().getPrayerTimes();
        String[] order = {"Imsak", "Gunes", "Ogle", "Ikindi", "Aksam", "Yatsi"};

        List<String> pattern = cfg.getStringList("pattern");
        int slot = 0;
        int orderIndex = 0;

        for (String row : pattern) {
            String[] elements = row.split(" ");
            for (String e : elements) {
                if (e.equals("#")) {
                    inv.setItem(slot, buildItem(cfg, "#"));
                } else if (e.equals("@") && orderIndex < order.length) {
                    String vakit = order[orderIndex++];
                    String displayName = plugin.getConfig().getString("NamazIsimleri." + vakit, vakit);
                    String timeStr = times.getOrDefault(vakit, "Bilinmiyor");
                    String headB64 = cfg.getString("Heads." + vakit, "");
                    ItemStack item = buildItem(cfg, "@", displayName, timeStr, headB64);
                    inv.setItem(slot, item);
                } else if (e.equals("O")) {
                    inv.setItem(slot, buildItem(cfg, "O"));
                }
                slot++;
            }
        }
        p.openInventory(inv);
    }

    public void openSorguGUI(Player p, String targetName) {
        FileConfiguration cfg = configs.get("own-info");
        if (cfg == null) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!plugin.getDatabaseManager().hasData(target.getUniqueId()) && !target.hasPlayedBefore() && !target.isOnline()) {
                p.sendMessage(ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", "") + plugin.getConfig().getString("Messages.NotFound", "&cOyuncu bulunamadı.")));
                return;
            }

            Map<String, Integer> stats = plugin.getDatabaseManager().getAllStats(target.getUniqueId());
            String lastPrayer = plugin.getDatabaseManager().getLastPrayer(target.getUniqueId());
            String banReason = plugin.getDatabaseManager().getBanReason(target.getUniqueId());

            // Abdest durumu kontrolü (Online ise net bilgi, offline ise abdestsiz kabul edilir)
            String wuduStatus = (target.isOnline() && plugin.getWuduManager().hasWudu(target.getUniqueId())) ? "&aAbdestli" : "&cAbdestsiz";

            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(null, cfg.getInt("size", 27), ColorUtils.color(cfg.getString("title", "&8Sorgu")));
                List<String> pattern = cfg.getStringList("pattern");
                int slot = 0;
                for (String row : pattern) {
                    String[] elements = row.split(" ");
                    for (String e : elements) {
                        if (e.equals("#")) {
                            inv.setItem(slot, buildItem(cfg, "#"));
                        } else if (e.equals("A")) {
                            ItemStack item = buildItem(cfg, "A");
                            ItemMeta m = item.getItemMeta();
                            List<String> lore = m.getLore();
                            if (lore != null) {
                                lore.replaceAll(s -> s.replace("{MONTHLY}", String.valueOf(stats.getOrDefault("monthly", 0)))
                                        .replace("{TOTAL}", String.valueOf(stats.getOrDefault("total", 0)))
                                        .replace("{RANK}", "?")); // Sıralama placeholder'ı eklenebilir
                                m.setLore(lore);
                            }
                            item.setItemMeta(m);
                            inv.setItem(slot, item);
                        } else if (e.equals("P")) {
                            ItemStack item = buildItem(cfg, "P");
                            SkullMeta m = (SkullMeta) item.getItemMeta();
                            m.setOwningPlayer(target);
                            m.setDisplayName(ColorUtils.color(m.getDisplayName().replace("{PLAYER}", targetName)));
                            List<String> lore = m.getLore();
                            if (lore != null) {
                                lore.replaceAll(s -> s.replace("{IMSAK}", String.valueOf(stats.getOrDefault("imsak", 0)))
                                        .replace("{GUNES}", String.valueOf(stats.getOrDefault("gunes", 0)))
                                        .replace("{OGLE}", String.valueOf(stats.getOrDefault("ogle", 0)))
                                        .replace("{IKINDI}", String.valueOf(stats.getOrDefault("ikindi", 0)))
                                        .replace("{AKSAM}", String.valueOf(stats.getOrDefault("aksam", 0)))
                                        .replace("{YATSI}", String.valueOf(stats.getOrDefault("yatsi", 0)))
                                        .replace("{TERAVIH}", String.valueOf(stats.getOrDefault("teravih", 0)))
                                        .replace("{WUDU_STATUS}", ColorUtils.color(wuduStatus))
                                        .replace("{LAST}", lastPrayer));
                                m.setLore(lore);
                            }
                            item.setItemMeta(m);
                            inv.setItem(slot, item);
                        } else if (e.equals("B")) {
                            ItemStack item = buildItem(cfg, "B");
                            ItemMeta m = item.getItemMeta();
                            List<String> lore = m.getLore();
                            if (lore != null) {
                                String bStatus = banReason != null ? ColorUtils.color("&#e74c3cYasaklı (" + banReason + ")") : ColorUtils.color("&#27ae60Temiz");
                                lore.replaceAll(s -> s.replace("{BAN_STATUS}", bStatus));
                                m.setLore(lore);
                            }
                            item.setItemMeta(m);
                            inv.setItem(slot, item);
                        }
                        slot++;
                    }
                }
                p.openInventory(inv);
            });
        });
    }

    public void openTopGUI(Player p, String type) {
        FileConfiguration cfg = configs.get("top");
        if (cfg == null) return;
        boolean isMonthly = type.equalsIgnoreCase("monthly");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map.Entry<String, Integer>> topList = plugin.getDatabaseManager().getTop10(isMonthly);

            Bukkit.getScheduler().runTask(plugin, () -> {
                String titleType = isMonthly ? "Aylık" : "Kalıcı";
                Inventory inv = Bukkit.createInventory(null, cfg.getInt("size", 45), ColorUtils.color(cfg.getString("title", "").replace("{TYPE}", titleType)));
                List<String> pattern = cfg.getStringList("pattern");
                int slot = 0, topIdx = 0;

                for (String row : pattern) {
                    String[] elements = row.split(" ");
                    for (String e : elements) {
                        if (e.equals("#")) {
                            inv.setItem(slot, buildItem(cfg, "#"));
                        } else if (e.equals("M")) {
                            inv.setItem(slot, buildItem(cfg, "M"));
                        } else if (e.matches("\\d")) {
                            if (topIdx < topList.size()) {
                                Map.Entry<String, Integer> entry = topList.get(topIdx);
                                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                                SkullMeta sm = (SkullMeta) head.getItemMeta();
                                sm.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getKey()));
                                sm.setDisplayName(ColorUtils.color("&#f1c40f#" + (topIdx+1) + " &#27ae60" + entry.getKey()));
                                List<String> l = new ArrayList<>();
                                l.add(ColorUtils.color("&7Namaz Sayısı: &f" + entry.getValue()));
                                sm.setLore(l);
                                head.setItemMeta(sm);
                                inv.setItem(slot, head);
                            } else {
                                ItemStack empty = new ItemStack(Material.BARRIER);
                                ItemMeta em = empty.getItemMeta();
                                em.setDisplayName(ColorUtils.color("&cBoş"));
                                empty.setItemMeta(em);
                                inv.setItem(slot, empty);
                            }
                            topIdx++;
                        }
                        slot++;
                    }
                }
                p.openInventory(inv);
            });
        });
    }

    public void openConfirmDeleteGUI(Player p, String type) {
        FileConfiguration cfg = configs.get("confirm");
        if (cfg == null) return;
        Inventory inv = Bukkit.createInventory(null, cfg.getInt("size", 27), ColorUtils.color(cfg.getString("title", "&8Emin misiniz?")));
        List<String> pattern = cfg.getStringList("pattern");
        int slot = 0;
        for (String row : pattern) {
            String[] elements = row.split(" ");
            for (String e : elements) {
                if (e.equals("#")) inv.setItem(slot, buildItem(cfg, "#"));
                else if (e.equals("Y")) {
                    ItemStack item = buildItem(cfg, "Y");
                    ItemMeta m = item.getItemMeta();
                    if (m.getLore() != null) {
                        List<String> l = m.getLore();
                        l.replaceAll(s -> s.replace("{TARGET}", type));
                        m.setLore(l);
                    }
                    item.setItemMeta(m);
                    inv.setItem(slot, item);
                }
                else if (e.equals("N")) inv.setItem(slot, buildItem(cfg, "N"));
                slot++;
            }
        }
        p.openInventory(inv);
    }

    public void openBanListGUI(Player p, int page) {
        FileConfiguration cfg = configs.get("ban-list");
        if (cfg == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int offset = (page - 1) * 45;
            List<String[]> bans = plugin.getDatabaseManager().getBans(offset, 45);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(null, cfg.getInt("size", 54), ColorUtils.color(cfg.getString("title", "&8Yasaklılar").replace("{PAGE}", String.valueOf(page))));
                List<String> pattern = cfg.getStringList("pattern");
                int slot = 0, banIdx = 0;
                for (String row : pattern) {
                    String[] elements = row.split(" ");
                    for (String e : elements) {
                        if (e.equals("#")) inv.setItem(slot, buildItem(cfg, "#"));
                        else if (e.equals("B")) inv.setItem(slot, buildItem(cfg, "B"));
                        else if (e.equals("N")) inv.setItem(slot, buildItem(cfg, "N"));
                        else if (e.equals("X")) {
                            if (banIdx < bans.size()) {
                                String[] bInfo = bans.get(banIdx);
                                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                                SkullMeta sm = (SkullMeta) head.getItemMeta();
                                sm.setOwningPlayer(Bukkit.getOfflinePlayer(bInfo[0]));
                                sm.setDisplayName(ColorUtils.color("&#e74c3c" + bInfo[0]));
                                List<String> l = new ArrayList<>();
                                l.add(ColorUtils.color("&7Sebep: &f" + bInfo[1]));
                                l.add(ColorUtils.color("&7Yetkili: &f" + bInfo[2]));
                                l.add(ColorUtils.color("&7Tarih: &f" + bInfo[3]));
                                l.add(ColorUtils.color(""));
                                l.add(ColorUtils.color("&eKaldırmak için SHIFT+SAĞ TIKLA"));
                                sm.setLore(l);
                                head.setItemMeta(sm);
                                inv.setItem(slot, head);
                            }
                            banIdx++;
                        }
                        slot++;
                    }
                }
                p.openInventory(inv);
            });
        });
    }

    private ItemStack buildItem(FileConfiguration cfg, String key) {
        String path = "items." + key;
        if (!cfg.contains(path)) return new ItemStack(Material.AIR);
        Material mat = Material.DIRT;
        try {
            mat = Material.valueOf(cfg.getString(path + ".material", "DIRT").toUpperCase());
        } catch (Exception ignored) {}

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (cfg.contains(path + ".name")) meta.setDisplayName(ColorUtils.color(cfg.getString(path + ".name")));
            if (cfg.contains(path + ".lore")) {
                List<String> lore = new ArrayList<>();
                for (String l : cfg.getStringList(path + ".lore")) lore.add(ColorUtils.color(l));
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildItem(FileConfiguration cfg, String key, String vName, String time, String headB64) {
        ItemStack item = buildItem(cfg, key);
        if (headB64 != null && !headB64.isEmpty()) item = HeadUtils.getCustomHead(headB64);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.getDisplayName() != null) meta.setDisplayName(meta.getDisplayName().replace("{VAKIT}", vName));
            if (meta.getLore() != null) {
                List<String> l = meta.getLore();
                l.replaceAll(s -> s.replace("{SAAT}", time));
                meta.setLore(l);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}