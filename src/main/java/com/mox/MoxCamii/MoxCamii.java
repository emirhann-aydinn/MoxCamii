package com.mox.MoxCamii;

import com.mox.MoxCamii.commands.*;
import com.mox.MoxCamii.hooks.MoxCamiiExpansion;
import com.mox.MoxCamii.listeners.*;
import com.mox.MoxCamii.managers.*;
import com.mox.MoxCamii.utils.ColorUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class MoxCamii extends JavaPlugin {
    private static MoxCamii instance;
    private DatabaseManager databaseManager;
    private RegionManager regionManager;
    private WuduManager wuduManager;
    private BanManager banManager;
    private RewardManager rewardManager;
    private RedisManager redisManager;
    private PrayerTimeManager prayerTimeManager;
    private GuiManager guiManager;

    private FileConfiguration messagesConfig;
    private File messagesFile;
    private FileConfiguration specialClocksConfig;
    private File specialClocksFile;

    @Override
    public void onEnable() {
        instance = this;
        File locationsDir = new File(getDataFolder(), "locations");
        if (!locationsDir.exists()) locationsDir.mkdirs();
        File dataDir = new File(getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();

        saveDefaultConfig();
        safeSaveResource("messages.yml");
        safeSaveResource("settings/clocks.yml");
        safeSaveResource("settings/special_clocks.yml");
        safeSaveResource("settings/rewards.yml");
        safeSaveResource("menus/clocks.yml");
        safeSaveResource("menus/special_clocks.yml");
        safeSaveResource("menus/own-info.yml");
        safeSaveResource("menus/top.yml");
        safeSaveResource("menus/ban-list.yml");
        safeSaveResource("menus/confirm.yml");

        this.databaseManager = new DatabaseManager(this);
        this.regionManager = new RegionManager(this);
        this.wuduManager = new WuduManager(this);
        this.banManager = new BanManager(this);
        this.rewardManager = new RewardManager(this);
        this.redisManager = new RedisManager(this);
        this.prayerTimeManager = new PrayerTimeManager(this);
        this.guiManager = new GuiManager(this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MoxCamiiExpansion(this).register();
        }

        getCommand("cami").setExecutor(new CamiCommand(this));
        getCommand("camiadmin").setExecutor(new CamiAdminCommand(this));
        getCommand("namazödülüm").setExecutor(new NamazOdulumCommand(this));

        Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VehicleExitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);

        this.redisManager.connect();
        this.prayerTimeManager.startTasks();
        startActionbarTask();
    }

    @Override
    public void onDisable() {
        if (this.redisManager != null) this.redisManager.disconnect();
        if (this.regionManager != null) this.regionManager.saveData();
        if (this.wuduManager != null) this.wuduManager.saveData();
        if (this.databaseManager != null) this.databaseManager.close();
    }

    private void startActionbarTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            String msg = ColorUtils.color(getMessagesConfig().getString("Messages.ActionbarSitting", "&#27ae60☪ Namaz kılıyorsun..."));
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (getRewardManager().isSitting(p)) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                }
            }
        }, 0L, 20L);
    }

    public void reloadPlugin() {
        reloadConfig();
        this.messagesConfig = null;
        this.specialClocksConfig = null;
        this.databaseManager.reload();
        this.regionManager.reload();
        this.rewardManager.reload();
        this.wuduManager.reload();
        this.prayerTimeManager.reload();
        this.guiManager.reload();
        if (this.redisManager != null) {
            this.redisManager.disconnect();
            this.redisManager = new RedisManager(this);
            this.redisManager.connect();
        }
    }

    public void safeSaveResource(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            if (getResource(name) != null) saveResource(name, false);
            else {
                try { file.getParentFile().mkdirs(); file.createNewFile(); } catch (Exception ignored) {}
            }
        }
    }

    public FileConfiguration getMessagesConfig() {
        if (messagesConfig == null) {
            messagesFile = new File(getDataFolder(), "messages.yml");
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        }
        return messagesConfig;
    }

    public FileConfiguration getSpecialClocksConfig() {
        if (specialClocksConfig == null) {
            specialClocksFile = new File(getDataFolder(), "settings/special_clocks.yml");
            specialClocksConfig = YamlConfiguration.loadConfiguration(specialClocksFile);
        }
        return specialClocksConfig;
    }

    public void saveSpecialClocksConfig() {
        try {
            if (specialClocksConfig != null && specialClocksFile != null) {
                specialClocksConfig.save(specialClocksFile);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static MoxCamii getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public RegionManager getRegionManager() { return regionManager; }
    public WuduManager getWuduManager() { return wuduManager; }
    public BanManager getBanManager() { return banManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public RedisManager getRedisManager() { return redisManager; }
    public PrayerTimeManager getPrayerTimeManager() { return prayerTimeManager; }
    public GuiManager getGuiManager() { return guiManager; }
}