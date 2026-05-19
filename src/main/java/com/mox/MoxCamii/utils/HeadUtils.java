// src/main/java/com/mox/MoxCamii/utils/HeadUtils.java
package com.mox.MoxCamii.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Base64;
import java.util.UUID;

public class HeadUtils {

    public static ItemStack getCustomHead(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (base64 == null || base64.isEmpty()) return head;

        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            JsonObject json = JsonParser.parseString(decoded).getAsJsonObject();
            String urlString = json.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            URL url = new URL(urlString);

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(url);
            profile.setTextures(textures);

            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return head;
    }
}