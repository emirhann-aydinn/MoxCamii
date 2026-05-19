// src/main/java/com/mox/MoxCamii/utils/Cuboid.java
package com.mox.MoxCamii.utils;

import org.bukkit.Location;
import org.bukkit.World;

public class Cuboid {
    private final World world;
    private final double minX, minY, minZ;
    private final double maxX, maxY, maxZ;

    public Cuboid(Location loc1, Location loc2) {
        this.world = loc1.getWorld();
        this.minX = Math.min(loc1.getX(), loc2.getX());
        this.minY = Math.min(loc1.getY(), loc2.getY());
        this.minZ = Math.min(loc1.getZ(), loc2.getZ());
        this.maxX = Math.max(loc1.getX(), loc2.getX());
        this.maxY = Math.max(loc1.getY(), loc2.getY());
        this.maxZ = Math.max(loc1.getZ(), loc2.getZ());
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().equals(world)) return false;
        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    public World getWorld() { return world; }
    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMinZ() { return minZ; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    public double getMaxZ() { return maxZ; }
}