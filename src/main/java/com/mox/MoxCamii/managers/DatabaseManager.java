package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DatabaseManager {
    private final MoxCamii plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(MoxCamii plugin) {
        this.plugin = plugin;
        connect();
        createTables();
    }

    public void reload() {
        close();
        connect();
        createTables();
    }

    private void connect() {
        HikariConfig config = new HikariConfig();
        String type = plugin.getConfig().getString("Settings.Database.Type", "SQLite");

        if (type.equalsIgnoreCase("MySQL")) {
            String host = plugin.getConfig().getString("Settings.Database.Host");
            int port = plugin.getConfig().getInt("Settings.Database.Port");
            String db = plugin.getConfig().getString("Settings.Database.Database");
            String user = plugin.getConfig().getString("Settings.Database.Username");
            String pass = plugin.getConfig().getString("Settings.Database.Password");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&autoReconnect=true");
            config.setUsername(user);
            config.setPassword(pass);
        } else {
            File dbFile = new File(plugin.getDataFolder(), "data/database.db");
            if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection()) {
            String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");

            String usersTable = "CREATE TABLE IF NOT EXISTS " + prefix + "users (" +
                    "uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16), " +
                    "imsak INT DEFAULT 0, gunes INT DEFAULT 0, ogle INT DEFAULT 0, ikindi INT DEFAULT 0, " +
                    "aksam INT DEFAULT 0, yatsi INT DEFAULT 0, teravih INT DEFAULT 0, bayram INT DEFAULT 0, " +
                    "monthly_count INT DEFAULT 0, total_count INT DEFAULT 0, abdest_count INT DEFAULT 0, last_prayer VARCHAR(32) DEFAULT 'Yok');";
            try (PreparedStatement ps = conn.prepareStatement(usersTable)) { ps.executeUpdate(); }

            String bansTable = "CREATE TABLE IF NOT EXISTS " + prefix + "bans (" +
                    "uuid VARCHAR(36) PRIMARY KEY, name VARCHAR(16), reason TEXT, " +
                    "author VARCHAR(16), date VARCHAR(32), expire_time BIGINT DEFAULT 0);";
            try (PreparedStatement ps = conn.prepareStatement(bansTable)) { ps.executeUpdate(); }

            String rewardsTable = "CREATE TABLE IF NOT EXISTS " + prefix + "pending_rewards (" +
                    "uuid VARCHAR(36) PRIMARY KEY, tier VARCHAR(10));";
            try (PreparedStatement ps = conn.prepareStatement(rewardsTable)) { ps.executeUpdate(); }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public boolean hasData(UUID uuid) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM " + prefix + "users WHERE uuid = ? UNION SELECT uuid FROM " + prefix + "bans WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public UUID getUUIDFromName(String name) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM " + prefix + "users WHERE name = ? LIMIT 1")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return UUID.fromString(rs.getString("uuid"));
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public Map<String, Integer> getAllStats(UUID uuid) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        Map<String, Integer> stats = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + prefix + "users WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                stats.put("imsak", rs.getInt("imsak"));
                stats.put("gunes", rs.getInt("gunes"));
                stats.put("ogle", rs.getInt("ogle"));
                stats.put("ikindi", rs.getInt("ikindi"));
                stats.put("aksam", rs.getInt("aksam"));
                stats.put("yatsi", rs.getInt("yatsi"));
                stats.put("teravih", rs.getInt("teravih"));
                stats.put("bayram", rs.getInt("bayram"));
                stats.put("monthly", rs.getInt("monthly_count"));
                stats.put("total", rs.getInt("total_count"));
                stats.put("abdest", rs.getInt("abdest_count"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return stats;
    }

    public String getLastPrayer(UUID uuid) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT last_prayer FROM " + prefix + "users WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("last_prayer");
        } catch (SQLException e) { e.printStackTrace(); }
        return "Yok";
    }

    public int getRank(UUID uuid, boolean isMonthly) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        String col = isMonthly ? "monthly_count" : "total_count";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) + 1 AS rank FROM " + prefix + "users WHERE " + col + " > (SELECT " + col + " FROM " + prefix + "users WHERE uuid = ?)"
             )) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("rank");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public void incrementWuduCount(UUID uuid, String name) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection()) {
                String sql = plugin.getConfig().getString("Settings.Database.Type").equalsIgnoreCase("MySQL")
                        ? "INSERT INTO " + prefix + "users (uuid, name, abdest_count) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE abdest_count = abdest_count + 1"
                        : "INSERT INTO " + prefix + "users (uuid, name, abdest_count) VALUES (?, ?, 1) ON CONFLICT(uuid) DO UPDATE SET abdest_count = abdest_count + 1";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name);
                    ps.executeUpdate();
                }
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void recordPrayer(UUID uuid, String name, String prayerNameRaw) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection()) {
                String cleanName = org.bukkit.ChatColor.stripColor(com.mox.MoxCamii.utils.ColorUtils.color(prayerNameRaw)).toLowerCase(new java.util.Locale("tr", "TR"));
                String typeCol = "total_count";
                if (cleanName.contains("imsak")) typeCol = "imsak";
                else if (cleanName.contains("güneş") || cleanName.contains("gunes")) typeCol = "gunes";
                else if (cleanName.contains("öğle") || cleanName.contains("ogle")) typeCol = "ogle";
                else if (cleanName.contains("ikindi")) typeCol = "ikindi";
                else if (cleanName.contains("akşam") || cleanName.contains("aksam")) typeCol = "aksam";
                else if (cleanName.contains("yatsı") || cleanName.contains("yatsi")) typeCol = "yatsi";
                else if (cleanName.contains("teravih")) typeCol = "teravih";
                else if (cleanName.contains("bayram")) typeCol = "bayram";

                String sql = plugin.getConfig().getString("Settings.Database.Type").equalsIgnoreCase("MySQL")
                        ? "INSERT INTO " + prefix + "users (uuid, name, " + typeCol + ", monthly_count, total_count, last_prayer) VALUES (?, ?, 1, 1, 1, ?) ON DUPLICATE KEY UPDATE " + typeCol + " = " + typeCol + " + 1, monthly_count = monthly_count + 1, total_count = total_count + 1, last_prayer = ?"
                        : "INSERT INTO " + prefix + "users (uuid, name, " + typeCol + ", monthly_count, total_count, last_prayer) VALUES (?, ?, 1, 1, 1, ?) ON CONFLICT(uuid) DO UPDATE SET " + typeCol + " = " + typeCol + " + 1, monthly_count = monthly_count + 1, total_count = total_count + 1, last_prayer = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name);
                    String displayForLastPrayer = org.bukkit.ChatColor.stripColor(com.mox.MoxCamii.utils.ColorUtils.color(prayerNameRaw));
                    ps.setString(3, displayForLastPrayer);
                    ps.setString(4, displayForLastPrayer);
                    ps.executeUpdate();
                }
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public List<Map.Entry<String, Integer>> getTop10(String sortType) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        List<Map.Entry<String, Integer>> list = new ArrayList<>();

        String col = "total_count";
        if (sortType.equalsIgnoreCase("monthly_count")) col = "monthly_count";
        else if (sortType.equalsIgnoreCase("abdest_count")) col = "abdest_count";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name, " + col + " FROM " + prefix + "users ORDER BY " + col + " DESC LIMIT 10")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new AbstractMap.SimpleEntry<>(rs.getString("name"), rs.getInt(col)));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void resetMonthlyStats() {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE " + prefix + "users SET monthly_count = 0")) {
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void banUser(UUID uuid, String name, String reason, String author, String date, long expireTime) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection()) {
                String sql = plugin.getConfig().getString("Settings.Database.Type").equalsIgnoreCase("MySQL")
                        ? "INSERT INTO " + prefix + "bans (uuid, name, reason, author, date, expire_time) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE reason = ?, author = ?, date = ?, expire_time = ?"
                        : "INSERT INTO " + prefix + "bans (uuid, name, reason, author, date, expire_time) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET reason = ?, author = ?, date = ?, expire_time = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString()); ps.setString(2, name);
                    ps.setString(3, reason); ps.setString(4, author);
                    ps.setString(5, date); ps.setLong(6, expireTime);
                    ps.setString(7, reason); ps.setString(8, author);
                    ps.setString(9, date); ps.setLong(10, expireTime);
                    ps.executeUpdate();
                }
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void unbanUser(UUID uuid) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + prefix + "bans WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public String getBanReason(UUID uuid) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT reason, expire_time FROM " + prefix + "bans WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long expire = rs.getLong("expire_time");
                if (expire > 0 && System.currentTimeMillis() > expire) {
                    unbanUser(uuid);
                    return null;
                }
                return rs.getString("reason");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public List<String[]> getBans(int offset, int limit) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        List<String[]> bans = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name, reason, author, date FROM " + prefix + "bans LIMIT ? OFFSET ?")) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                bans.add(new String[]{rs.getString("name"), rs.getString("reason"), rs.getString("author"), rs.getString("date")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return bans;
    }

    public String getPendingReward(UUID uuid) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT tier FROM " + prefix + "pending_rewards WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("tier");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void setPendingReward(UUID uuid, String tier) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO " + prefix + "pending_rewards (uuid, tier) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET tier = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, tier);
            ps.setString(3, tier);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void removePendingReward(UUID uuid) {
        String prefix = plugin.getConfig().getString("Settings.Database.Table-Prefix", "moxcamii_");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + prefix + "pending_rewards WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}