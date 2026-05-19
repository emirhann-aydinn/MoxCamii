package com.mox.MoxCamii;

import com.mox.MoxCamii.commands.CamiCommand;
import com.mox.MoxCamii.commands.NamazlarCommand;
import com.mox.MoxCamii.hooks.MoxCamiiExpansion;
import com.mox.MoxCamii.listeners.*;
import com.mox.MoxCamii.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class MoxCamii extends JavaPlugin {

    private static MoxCamii instance;
    private DatabaseManager databaseManager;
    private RegionManager regionManager;
    private WuduManager wuduManager;
    private BanManager banManager;
    private RewardManager rewardManager;
    private RedisManager redisManager;
    private PrayerTimeManager prayerTimeManager;

    @Override
    public void onEnable() {
        instance = this;

        File locationsDir = new File(getDataFolder(), "locations");
        if (!locationsDir.exists()) locationsDir.mkdirs();

        saveDefaultConfig();

        // Çökmeyi engelleyen güvenli dosya çıkarma metodu
        safeSaveResource("clocks.yml");
        safeSaveResource("rewards.yml");

        this.databaseManager = new DatabaseManager(this);
        this.regionManager = new RegionManager(this);
        this.wuduManager = new WuduManager(this);
        this.banManager = new BanManager(this);
        this.rewardManager = new RewardManager(this);
        this.redisManager = new RedisManager(this);
        this.prayerTimeManager = new PrayerTimeManager(this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MoxCamiiExpansion(this).register();
        }

        getCommand("cami").setExecutor(new CamiCommand(this));
        getCommand("namazlar").setExecutor(new NamazlarCommand(this));

        Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VehicleExitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);

        this.redisManager.connect();
        this.prayerTimeManager.startTasks();
    }

    @Override
    public void onDisable() {
        if (this.redisManager != null) this.redisManager.disconnect();
        if (this.regionManager != null) this.regionManager.saveData();
        if (this.wuduManager != null) this.wuduManager.saveData();
        if (this.databaseManager != null) this.databaseManager.close();
    }

    public void reloadPlugin() {
        reloadConfig();
        this.databaseManager.reload();
        this.regionManager.reload();
        this.rewardManager.reload();
        this.wuduManager.reload();
        this.prayerTimeManager.reload();

        if (this.redisManager != null) {
            this.redisManager.disconnect();
            this.redisManager = new RedisManager(this);
            this.redisManager.connect();
        }
    }

    public void safeSaveResource(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            if (getResource(name) != null) {
                saveResource(name, false);
            } else {
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                } catch (Exception ignored) {}
            }
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
}