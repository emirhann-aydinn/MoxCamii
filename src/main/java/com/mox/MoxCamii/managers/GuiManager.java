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
    private final Map<String, String> resolvedTitles = new HashMap<>();
    private final Map<UUID, String> playerSorts = new HashMap<>();
    private final Map<UUID, String[]> confirmActions = new HashMap<>();

    public GuiManager(MoxCamii plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        configs.clear();
        resolvedTitles.clear();
        // EKSİK OLAN "special_clocks" BURAYA EKLENDİ
        String[] menus = {"clocks", "special_clocks", "own-info", "top", "ban-list", "confirm"};
        for (String m : menus) {
            File file = new File(plugin.getDataFolder(), "menus/" + m + ".yml");
            if (file.exists()) {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                configs.put(m, cfg);
                if (cfg.contains("title")) {
                    resolvedTitles.put(m, ColorUtils.color(cfg.getString("title")));
                }
            }
        }
    }

    public boolean isPluginMenu(String title) {
        if (title == null) return false;
        String t = org.bukkit.ChatColor.stripColor(ColorUtils.color(title)).toLowerCase();
        return t.contains("sıralama") ||
                t.contains("yasaklılar") ||
                t.contains("özel") ||
                t.contains("namaz vakitleri") ||
                t.contains("sorgu") ||
                t.contains("emin misiniz");
    }

    public String getMenuTitle(String key) {
        return resolvedTitles.get(key);
    }

    public void cycleSort(Player p) {
        String current = playerSorts.getOrDefault(p.getUniqueId(), "monthly_count");
        switch (current) {
            case "monthly_count": playerSorts.put(p.getUniqueId(), "total_count"); break;
            case "total_count": playerSorts.put(p.getUniqueId(), "abdest_count"); break;
            case "abdest_count": playerSorts.put(p.getUniqueId(), "monthly_count"); break;
            default: playerSorts.put(p.getUniqueId(), "monthly_count"); break;
        }
        if (plugin.getSoundManager() != null) plugin.getSoundManager().playSound(p, "Click");
        openTopGUI(p);
    }

    public void openVakitlerGUI(Player p) {
        FileConfiguration cfg = configs.get("clocks");
        if (cfg == null) return;

        Inventory inv = Bukkit.createInventory(null, cfg.getInt("size", 27), ColorUtils.color(cfg.getString("title", "&8Namaz Vakitleri")));
        Map<String, String> times = plugin.getPrayerTimeManager().getPrayerTimes();
        String[] order = {"Imsak", "Gunes", "Ogle", "Ikindi", "Aksam", "Yatsi"};

        List<String> pattern = cfg.getStringList("pattern");
        int slot = 0;
        int orderIndex = 0;

        for (String row : pattern) {
            String[] elements = row.split(" ");
            for (String e : elements) {
                if (slot >= inv.getSize()) break;
                if (e.equals("#")) {
                    inv.setItem(slot, buildItem(cfg, "#"));
                } else if (e.equals("@") && orderIndex < order.length) {
                    String vakit = order[orderIndex++];
                    String displayName = plugin.getMessagesConfig().getString("Namaz-Names." + vakit, vakit);
                    String timeStr = times.getOrDefault(vakit, "Bilinmiyor");
                    String headB64 = cfg.getString("Heads." + vakit, "");
                    inv.setItem(slot, buildItem(cfg, "@", displayName, timeStr, headB64));
                } else if (e.equals("O")) {
                    inv.setItem(slot, buildItem(cfg, "O"));
                }
                slot++;
            }
        }
        p.openInventory(inv);
    }

    // EKSİK OLAN ÖZEL NAMAZLAR MENÜSÜ METODU EKLENDİ
    public void openSpecialVakitlerGUI(Player p) {
        FileConfiguration cfg = configs.get("special_clocks");
        if (cfg == null) {
            p.sendMessage(ColorUtils.color("&cMenü dosyası (special_clocks.yml) bulunamadı!"));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, cfg.getInt("size", 27), ColorUtils.color(cfg.getString("title", "&8Özel Namaz Vakitleri")));
        FileConfiguration scConfig = plugin.getSpecialClocksConfig();

        String[] specialPrayers = {"ramazan_bayrami_namazi", "teravih", "kurban_bayrami_namazi"};
        String[] configPaths = {
                "special_clocks.clocks.ramazan.types.ramazan_bayrami_namazi",
                "special_clocks.clocks.ramazan.types.teravih",
                "special_clocks.clocks.kurban_bayrami.types.kurban_bayrami_namazi"
        };
        String[] langKeys = {"Ramazan-Bayram", "Teravih", "kurban-Bayram"};

        List<String> pattern = cfg.getStringList("pattern");
        int slot = 0;
        int specialIndex = 0;

        for (String row : pattern) {
            String[] elements = row.split(" ");
            for (String e : elements) {
                if (slot >= inv.getSize()) break;
                if (e.equals("#")) {
                    inv.setItem(slot, buildItem(cfg, "#"));
                } else if (e.equals("@") && specialIndex < specialPrayers.length) {
                    String path = configPaths[specialIndex];
                    String nameKey = langKeys[specialIndex];
                    String displayName = plugin.getMessagesConfig().getString("Namaz-Names." + nameKey, specialPrayers[specialIndex]);

                    boolean active = scConfig != null && scConfig.getBoolean(path + ".status", false);
                    String statusStr = active ? "&aAktif" : "&cAktif Değil";
                    String timeStr = scConfig != null ? scConfig.getString(path + ".time", "00:00") : "00:00";
                    String headB64 = cfg.getString("Heads." + nameKey, "");

                    ItemStack item = buildItem(cfg, "@", displayName, timeStr, headB64);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.getLore() != null) {
                        List<String> lore = meta.getLore();
                        lore.replaceAll(s -> ColorUtils.color(s.replace("{STATUS}", statusStr).replace("{OZEL_durum}", statusStr).replace("{OZEL_SAAT}", timeStr)));
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                    inv.setItem(slot, item);
                    specialIndex++;
                } else if (e.equals("*")) {
                    inv.setItem(slot, buildItem(cfg, "*"));
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
            Map<String, Integer> stats = plugin.getDatabaseManager().getAllStats(target.getUniqueId());
            String lastPrayer = plugin.getDatabaseManager().getLastPrayer(target.getUniqueId());
            int rank = plugin.getDatabaseManager().getRank(target.getUniqueId(), false);
            String banReason = plugin.getDatabaseManager().getBanReason(target.getUniqueId());
            String wuduStatus = (target.isOnline() && plugin.getWuduManager().hasWudu(target.getUniqueId())) ? "&aAbdestli" : "&cAbdestsiz";

            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(null, cfg.getInt("size", 27), ColorUtils.color(cfg.getString("title", "&8Sorgu")));
                List<String> pattern = cfg.getStringList("pattern");
                int slot = 0;

                for (String row : pattern) {
                    String[] elements = row.split(" ");
                    for (String e : elements) {
                        if (slot >= inv.getSize()) break;
                        if (e.equals("#")) {
                            inv.setItem(slot, buildItem(cfg, "#"));
                        } else {
                            ItemStack item = buildItem(cfg, e);
                            if (item != null && item.getType() != Material.AIR) {
                                ItemMeta m = item.getItemMeta();

                                if (e.equals("P") && m instanceof SkullMeta) {
                                    ((SkullMeta) m).setOwningPlayer(target);
                                }

                                if (m != null) {
                                    if (m.getDisplayName() != null) {
                                        m.setDisplayName(ColorUtils.color(m.getDisplayName().replace("{PLAYER}", targetName)));
                                    }

                                    if (m.getLore() != null) {
                                        List<String> newLore = new ArrayList<>();
                                        for (String line : m.getLore()) {
                                            if (line.contains("{BAN_LINES}")) {
                                                if (banReason != null) {
                                                    newLore.add(ColorUtils.color("&8- &cCami'ye Giriş: &4Yasaklı"));
                                                    newLore.add(ColorUtils.color("&8- &cSebep: &f" + banReason));
                                                }
                                            } else {
                                                String bStatus = banReason != null ? ColorUtils.color("&#e74c3cYasaklı (" + banReason + ")") : ColorUtils.color("&#27ae60Temiz");
                                                line = line.replace("{RANK}", String.valueOf(rank))
                                                        .replace("{TOTAL}", String.valueOf(stats.getOrDefault("total", 0)))
                                                        .replace("{MONTHLY}", String.valueOf(stats.getOrDefault("monthly", 0)))
                                                        .replace("{IMSAK}", String.valueOf(stats.getOrDefault("imsak", 0)))
                                                        .replace("{GUNES}", String.valueOf(stats.getOrDefault("gunes", 0)))
                                                        .replace("{OGLE}", String.valueOf(stats.getOrDefault("ogle", 0)))
                                                        .replace("{IKINDI}", String.valueOf(stats.getOrDefault("ikindi", 0)))
                                                        .replace("{AKSAM}", String.valueOf(stats.getOrDefault("aksam", 0)))
                                                        .replace("{YATSI}", String.valueOf(stats.getOrDefault("yatsi", 0)))
                                                        .replace("{TERAVIH}", String.valueOf(stats.getOrDefault("teravih", 0)))
                                                        .replace("{BAYRAM}", String.valueOf(stats.getOrDefault("bayram", 0)))
                                                        .replace("{ABDEST_COUNT}", String.valueOf(stats.getOrDefault("abdest", 0)))
                                                        .replace("{LAST}", lastPrayer)
                                                        .replace("{WUDU_STATUS}", ColorUtils.color(wuduStatus))
                                                        .replace("{BAN_STATUS}", bStatus);
                                                newLore.add(ColorUtils.color(line));
                                            }
                                        }
                                        m.setLore(newLore);
                                    }
                                    item.setItemMeta(m);
                                }
                                inv.setItem(slot, item);
                            }
                        }
                        slot++;
                    }
                }
                p.openInventory(inv);
            });
        });
    }

    public void openTopGUI(Player p) {
        FileConfiguration cfg = configs.get("top");
        if (cfg == null) return;

        String sortType = playerSorts.getOrDefault(p.getUniqueId(), "monthly_count");
        String sortName = sortType.equals("monthly_count") ? "Aylık Namaz" :
                sortType.equals("total_count") ? "Toplam Namaz" : "Alınan Abdest";

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map.Entry<String, Integer>> topList = plugin.getDatabaseManager().getTop10(sortType);

            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(null, cfg.getInt("size", 45), ColorUtils.color(cfg.getString("title", "").replace("{SORT}", sortName)));
                List<String> pattern = cfg.getStringList("pattern");
                int slot = 0, topIdx = 0;

                for (String row : pattern) {
                    String[] elements = row.split(" ");
                    for (String e : elements) {
                        if (slot >= inv.getSize()) break;
                        if (e.equals("#")) {
                            inv.setItem(slot, buildItem(cfg, "#"));
                        } else if (e.equals("M")) {
                            inv.setItem(slot, buildItem(cfg, "M"));
                        } else if (e.equals("S")) {
                            ItemStack hopper = buildItem(cfg, "S");
                            ItemMeta hm = hopper.getItemMeta();
                            if (hm != null && hm.getLore() != null) {
                                List<String> lore = hm.getLore();
                                lore.replaceAll(s -> ColorUtils.color(s.replace("{SORT_NAME}", sortName)));
                                hm.setLore(lore);
                                hopper.setItemMeta(hm);
                            }
                            inv.setItem(slot, hopper);
                        } else if (!e.equals("#") && !e.equals("M") && !e.equals("S")) {
                            if (topIdx < topList.size()) {
                                Map.Entry<String, Integer> entry = topList.get(topIdx);

                                ItemStack head = buildItem(cfg, "X");
                                if(head == null || head.getType() == Material.AIR) head = new ItemStack(Material.PLAYER_HEAD);

                                ItemMeta sm = head.getItemMeta();
                                if (sm instanceof SkullMeta) {
                                    ((SkullMeta) sm).setOwningPlayer(Bukkit.getOfflinePlayer(entry.getKey()));
                                }

                                String pName = entry.getKey();
                                String score = String.valueOf(entry.getValue());
                                String rank = String.valueOf(topIdx + 1);

                                if (sm != null) {
                                    if (sm.getDisplayName() == null || sm.getDisplayName().isEmpty()) {
                                        sm.setDisplayName(ColorUtils.color("&#f1c40f#" + rank + " &#27ae60" + pName));
                                    } else {
                                        sm.setDisplayName(ColorUtils.color(sm.getDisplayName().replace("{RANK}", rank).replace("{PLAYER}", pName).replace("{SCORE}", score)));
                                    }

                                    if (sm.getLore() != null) {
                                        List<String> l = sm.getLore();
                                        l.replaceAll(s -> ColorUtils.color(s.replace("{RANK}", rank).replace("{PLAYER}", pName).replace("{SCORE}", score)));
                                        sm.setLore(l);
                                    } else {
                                        sm.setLore(Collections.singletonList(ColorUtils.color("&7Skor: &f" + score)));
                                    }
                                    head.setItemMeta(sm);
                                }
                                inv.setItem(slot, head);
                            } else {
                                ItemStack empty = new ItemStack(Material.BARRIER);
                                ItemMeta em = empty.getItemMeta();
                                if (em != null) {
                                    em.setDisplayName(ColorUtils.color("&cBoş"));
                                    empty.setItemMeta(em);
                                }
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

    public void openConfirmGUI(Player p, String target, String actionType) {
        FileConfiguration cfg = configs.get("confirm");
        if (cfg == null) return;

        confirmActions.put(p.getUniqueId(), new String[]{actionType, target});

        Inventory inv = Bukkit.createInventory(null, cfg.getInt("size", 27), ColorUtils.color(cfg.getString("title", "&8Emin misiniz?")));
        List<String> pattern = cfg.getStringList("pattern");
        int slot = 0;
        for (String row : pattern) {
            String[] elements = row.split(" ");
            for (String e : elements) {
                if (slot >= inv.getSize()) break;
                if (e.equals("#")) inv.setItem(slot, buildItem(cfg, "#"));
                else if (e.equals("Y")) {
                    ItemStack item = buildItem(cfg, "Y");
                    ItemMeta m = item.getItemMeta();
                    if (m != null && m.getLore() != null) {
                        List<String> l = m.getLore();
                        String actionText = actionType.equals("UNBAN") ? (target + " Yasak Kaldırma") : target;
                        l.replaceAll(s -> ColorUtils.color(s.replace("{TARGET}", actionText)));
                        m.setLore(l);
                        item.setItemMeta(m);
                    }
                    inv.setItem(slot, item);
                } else if (e.equals("N")) inv.setItem(slot, buildItem(cfg, "N"));
                slot++;
            }
        }
        p.openInventory(inv);
    }

    public void executeConfirmAction(Player p) {
        String[] actionInfo = confirmActions.get(p.getUniqueId());
        if (actionInfo == null) return;

        if (actionInfo[0].equals("UNBAN")) {
            plugin.getBanManager().unbanPlayer(actionInfo[1], p);
            p.closeInventory();
        } else if (actionInfo[0].equals("DELETE_REGION")) {
            plugin.getRegionManager().deleteRegion();
            p.sendMessage(ColorUtils.color(plugin.getConfig().getString("Settings.Prefix", "") + plugin.getMessagesConfig().getString("Messages.RegionDeleted", "&aBölge başarıyla silindi.")));
            p.closeInventory();
        }
        confirmActions.remove(p.getUniqueId());
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
                        if (slot >= inv.getSize()) break;
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
            if (meta.getDisplayName() != null) {
                meta.setDisplayName(ColorUtils.color(meta.getDisplayName().replace("{VAKIT}", vName).replace("{OZEL_VAKIT}", vName)));
            }
            if (meta.getLore() != null) {
                List<String> l = meta.getLore();
                l.replaceAll(s -> ColorUtils.color(s.replace("{SAAT}", time).replace("{VAKIT}", vName).replace("{OZEL_VAKIT}", vName)));
                meta.setLore(l);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}