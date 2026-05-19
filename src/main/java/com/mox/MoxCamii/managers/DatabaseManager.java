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
import java.util.UUID;

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
        String type = plugin.getConfig().getString("Database.Type", "SQLite");

        if (type.equalsIgnoreCase("MySQL")) {
            String host = plugin.getConfig().getString("Database.Host", "127.0.0.1");
            int port = plugin.getConfig().getInt("Database.Port", 3306);
            String db = plugin.getConfig().getString("Database.Database", "moxnetwork");
            String user = plugin.getConfig().getString("Database.Username", "root");
            String pass = plugin.getConfig().getString("Database.Password", "");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&autoReconnect=true");
            config.setUsername(user);
            config.setPassword(pass);
        } else {
            // SQLite için dosya ve klasör kontrolü
            File dbFile = new File(plugin.getDataFolder(), "data/database.db");
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs(); // Data klasörü yoksa oluştur
            }

            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);

        dataSource = new HikariDataSource(config);
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection()) {
            String usersTable = "CREATE TABLE IF NOT EXISTS moxcamii_users (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16), " +
                    "namaz_count INT DEFAULT 0, " +
                    "last_prayer VARCHAR(32) DEFAULT 'Yok'" +
                    ");";
            try (PreparedStatement ps = conn.prepareStatement(usersTable)) {
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE moxcamii_users ADD COLUMN last_prayer VARCHAR(32) DEFAULT 'Yok';")) {
                ps.executeUpdate();
            } catch (SQLException ignored) {}

            String bansTable = "CREATE TABLE IF NOT EXISTS moxcamii_bans (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(16), " +
                    "reason TEXT, " +
                    "author VARCHAR(16), " +
                    "date VARCHAR(32)" +
                    ");";
            try (PreparedStatement ps = conn.prepareStatement(bansTable)) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasData(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM moxcamii_users WHERE uuid = ? UNION SELECT uuid FROM moxcamii_bans WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public int getNamazCount(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT namaz_count FROM moxcamii_users WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("namaz_count");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public String getLastPrayer(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT last_prayer FROM moxcamii_users WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("last_prayer");
        } catch (SQLException e) { e.printStackTrace(); }
        return "Yok";
    }

    public void recordPrayer(UUID uuid, String name, String prayerName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection()) {
                String sql = plugin.getConfig().getString("Database.Type").equalsIgnoreCase("MySQL")
                        ? "INSERT INTO moxcamii_users (uuid, name, namaz_count, last_prayer) VALUES (?, ?, 1, ?) ON DUPLICATE KEY UPDATE namaz_count = namaz_count + 1, last_prayer = ?"
                        : "INSERT INTO moxcamii_users (uuid, name, namaz_count, last_prayer) VALUES (?, ?, 1, ?) ON CONFLICT(uuid) DO UPDATE SET namaz_count = namaz_count + 1, last_prayer = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name);
                    ps.setString(3, prayerName);
                    ps.setString(4, prayerName);
                    ps.executeUpdate();
                }
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void banUser(UUID uuid, String name, String reason, String author, String date) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection()) {
                String sql = plugin.getConfig().getString("Database.Type").equalsIgnoreCase("MySQL")
                        ? "INSERT INTO moxcamii_bans (uuid, name, reason, author, date) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE reason = ?, author = ?, date = ?"
                        : "INSERT INTO moxcamii_bans (uuid, name, reason, author, date) VALUES (?, ?, ?, ?, ?) ON CONFLICT(uuid) DO UPDATE SET reason = ?, author = ?, date = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name);
                    ps.setString(3, reason);
                    ps.setString(4, author);
                    ps.setString(5, date);
                    ps.setString(6, reason);
                    ps.setString(7, author);
                    ps.setString(8, date);
                    ps.executeUpdate();
                }
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public void unbanUser(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM moxcamii_bans WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) { e.printStackTrace(); }
        });
    }

    public String getBanReason(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT reason FROM moxcamii_bans WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("reason");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
}