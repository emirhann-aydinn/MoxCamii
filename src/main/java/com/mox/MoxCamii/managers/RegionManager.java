package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegionManager {

    private final MoxCamii plugin;
    private Cuboid camiRegion;
    private Cuboid doorRegion;
    private Location spawnLocation;
    private Location banSpawnLocation;
    private File file;
    private FileConfiguration config;

    private final Map<UUID, Location> pos1Map = new HashMap<>();
    private final Map<UUID, Location> pos2Map = new HashMap<>();

    private final Map<UUID, Location> doorPos1Map = new HashMap<>();
    private final Map<UUID, Location> doorPos2Map = new HashMap<>();

    public RegionManager(MoxCamii plugin) {
        this.plugin = plugin;
        loadFile();
    }

    private void loadFile() {
        file = new File(plugin.getDataFolder(), "locations/cami_location.yml");
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (config.contains("Region.World")) {
            Location p1 = new Location(Bukkit.getWorld(config.getString("Region.World")), config.getDouble("Region.MinX"), config.getDouble("Region.MinY"), config.getDouble("Region.MinZ"));
            Location p2 = new Location(Bukkit.getWorld(config.getString("Region.World")), config.getDouble("Region.MaxX"), config.getDouble("Region.MaxY"), config.getDouble("Region.MaxZ"));
            camiRegion = new Cuboid(p1, p2);
        }

        if (config.contains("DoorRegion.World")) {
            Location p1 = new Location(Bukkit.getWorld(config.getString("DoorRegion.World")), config.getDouble("DoorRegion.MinX"), config.getDouble("DoorRegion.MinY"), config.getDouble("DoorRegion.MinZ"));
            Location p2 = new Location(Bukkit.getWorld(config.getString("DoorRegion.World")), config.getDouble("DoorRegion.MaxX"), config.getDouble("DoorRegion.MaxY"), config.getDouble("DoorRegion.MaxZ"));
            doorRegion = new Cuboid(p1, p2);
        }

        if (config.contains("Spawn.World")) {
            spawnLocation = new Location(Bukkit.getWorld(config.getString("Spawn.World")), config.getDouble("Spawn.X"), config.getDouble("Spawn.Y"), config.getDouble("Spawn.Z"), (float) config.getDouble("Spawn.Yaw"), (float) config.getDouble("Spawn.Pitch"));
        }

        if (config.contains("BanSpawn.World")) {
            banSpawnLocation = new Location(Bukkit.getWorld(config.getString("BanSpawn.World")), config.getDouble("BanSpawn.X"), config.getDouble("BanSpawn.Y"), config.getDouble("BanSpawn.Z"), (float) config.getDouble("BanSpawn.Yaw"), (float) config.getDouble("BanSpawn.Pitch"));
        }
    }

    public void reload() {
        loadFile();
    }

    public void saveData() {
        if (camiRegion != null) {
            config.set("Region.World", camiRegion.getWorld().getName());
            config.set("Region.MinX", camiRegion.getMinX());
            config.set("Region.MinY", camiRegion.getMinY());
            config.set("Region.MinZ", camiRegion.getMinZ());
            config.set("Region.MaxX", camiRegion.getMaxX());
            config.set("Region.MaxY", camiRegion.getMaxY());
            config.set("Region.MaxZ", camiRegion.getMaxZ());
        } else {
            config.set("Region", null);
        }

        if (doorRegion != null) {
            config.set("DoorRegion.World", doorRegion.getWorld().getName());
            config.set("DoorRegion.MinX", doorRegion.getMinX());
            config.set("DoorRegion.MinY", doorRegion.getMinY());
            config.set("DoorRegion.MinZ", doorRegion.getMinZ());
            config.set("DoorRegion.MaxX", doorRegion.getMaxX());
            config.set("DoorRegion.MaxY", doorRegion.getMaxY());
            config.set("DoorRegion.MaxZ", doorRegion.getMaxZ());
        } else {
            config.set("DoorRegion", null);
        }

        if (spawnLocation != null) {
            config.set("Spawn.World", spawnLocation.getWorld().getName());
            config.set("Spawn.X", spawnLocation.getX());
            config.set("Spawn.Y", spawnLocation.getY());
            config.set("Spawn.Z", spawnLocation.getZ());
            config.set("Spawn.Yaw", spawnLocation.getYaw());
            config.set("Spawn.Pitch", spawnLocation.getPitch());
        }

        if (banSpawnLocation != null) {
            config.set("BanSpawn.World", banSpawnLocation.getWorld().getName());
            config.set("BanSpawn.X", banSpawnLocation.getX());
            config.set("BanSpawn.Y", banSpawnLocation.getY());
            config.set("BanSpawn.Z", banSpawnLocation.getZ());
            config.set("BanSpawn.Yaw", banSpawnLocation.getYaw());
            config.set("BanSpawn.Pitch", banSpawnLocation.getPitch());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createRegion(Location p1, Location p2) {
        this.camiRegion = new Cuboid(p1, p2);
        saveData();
    }

    public void createDoorRegion(Location p1, Location p2) {
        this.doorRegion = new Cuboid(p1, p2);
        saveData();
    }

    public void deleteRegion() {
        this.camiRegion = null;
        saveData();
    }

    public void deleteDoorRegion() {
        this.doorRegion = null;
        saveData();
    }

    public void setSpawn(Location loc) {
        this.spawnLocation = loc;
        saveData();
    }

    public void setBanSpawn(Location loc) {
        this.banSpawnLocation = loc;
        saveData();
    }

    public boolean hasCamiRegion() { return camiRegion != null; }
    public boolean hasDoorRegion() { return doorRegion != null; }

    public boolean isInRegion(Location loc) {
        if (camiRegion == null) return false;
        return camiRegion.contains(loc);
    }

    public boolean isInDoorRegion(Location loc) {
        if (doorRegion == null) return false;
        return doorRegion.contains(loc);
    }

    public Location getSpawn() { return spawnLocation; }
    public Location getBanSpawn() { return banSpawnLocation != null ? banSpawnLocation : spawnLocation; }

    public void setPos1(UUID uuid, Location loc) { pos1Map.put(uuid, loc); }
    public void setPos2(UUID uuid, Location loc) { pos2Map.put(uuid, loc); }
    public Location getPos1(UUID uuid) { return pos1Map.get(uuid); }
    public Location getPos2(UUID uuid) { return pos2Map.get(uuid); }

    public void setDoorPos1(UUID uuid, Location loc) { doorPos1Map.put(uuid, loc); }
    public void setDoorPos2(UUID uuid, Location loc) { doorPos2Map.put(uuid, loc); }
    public Location getDoorPos1(UUID uuid) { return doorPos1Map.get(uuid); }
    public Location getDoorPos2(UUID uuid) { return doorPos2Map.get(uuid); }
}