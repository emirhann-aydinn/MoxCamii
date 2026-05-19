// src/main/java/com/mox/MoxCamii/managers/RedisManager.java
package com.mox.MoxCamii.managers;

import com.mox.MoxCamii.MoxCamii;
import com.mox.MoxCamii.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

public class RedisManager {

    private final MoxCamii plugin;
    private JedisPool pool;
    private final String channel = "MoxCamiiChannel";
    private final String clusterId;
    private final boolean enabled;

    private String lastProcessedEzan = "";
    private String lastProcessedReminder = "";

    public RedisManager(MoxCamii plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("Redis.Enabled", true);
        this.clusterId = plugin.getConfig().getString("Redis.Cluster-ID", "network-1");
    }

    public void connect() {
        if (!enabled) return;
        String host = plugin.getConfig().getString("Redis.Host", "127.0.0.1");
        int port = plugin.getConfig().getInt("Redis.Port", 6379);
        String password = plugin.getConfig().getString("Redis.Password", "");

        if (password == null || password.isEmpty()) {
            pool = new JedisPool(host, port);
        } else {
            pool = new JedisPool(new JedisPoolConfig(), host, port, 2000, password);
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String ch, String message) {
                        if (ch.equals(channel)) {
                            handleMessage(message);
                        }
                    }
                }, channel);
            } catch (Exception e) {
                plugin.getLogger().warning("Redis bağlantısı kurulamadı, anonslar sadece yerel çalışacak.");
            }
        });
    }

    public void disconnect() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }

    public void publishReminder(String displayName, int dk) {
        if (!enabled || pool == null) {
            simulateHandle(clusterId + ";REMINDER;" + displayName + ";" + dk);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.publish(channel, clusterId + ";REMINDER;" + displayName + ";" + dk);
            } catch (Exception e) { simulateHandle(clusterId + ";REMINDER;" + displayName + ";" + dk); }
        });
    }

    public void publishEzan(String displayName) {
        if (!enabled || pool == null) {
            simulateHandle(clusterId + ";EZAN;" + displayName);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.publish(channel, clusterId + ";EZAN;" + displayName);
            } catch (Exception e) { simulateHandle(clusterId + ";EZAN;" + displayName); }
        });
    }

    private void simulateHandle(String message) {
        handleMessage(message);
    }

    private void handleMessage(String message) {
        String[] parts = message.split(";");
        if (parts.length < 3) return;
        if (!parts[0].equals(clusterId)) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (parts[1].equals("REMINDER")) {
                String vakit = parts[2];
                String dk = parts[3];

                String cacheKey = vakit + "-" + dk;
                if (lastProcessedReminder.equals(cacheKey)) return;
                lastProcessedReminder = cacheKey;

                String msg = plugin.getConfig().getString("Messages.DuyuruMesaji");
                if (msg != null) {
                    msg = msg.replace("{VAKIT}", vakit).replace("{DAKIKA}", dk);
                    Bukkit.broadcastMessage(ColorUtils.color(msg));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                    }
                }
            } else if (parts[1].equals("EZAN")) {
                String vakit = parts[2];

                if (lastProcessedEzan.equals(vakit)) return;
                lastProcessedEzan = vakit;

                String msg = plugin.getConfig().getString("Messages.EzanMesaji");
                if (msg != null) {
                    msg = msg.replace("{VAKIT}", vakit);
                    Bukkit.broadcastMessage(ColorUtils.color(msg));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }
                }
            }
        });
    }
}