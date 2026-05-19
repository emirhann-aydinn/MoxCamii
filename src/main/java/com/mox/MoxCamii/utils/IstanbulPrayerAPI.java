// src/main/java/com/mox/MoxCamii/utils/IstanbulPrayerAPI.java
package com.mox.MoxCamii.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IstanbulPrayerAPI {

    public static boolean fetchAndSave(java.io.File file, FileConfiguration config) {
        try {
            URL url = new URL("http://api.aladhan.com/v1/timingsByCity?city=Istanbul&country=Turkey&method=13");
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            request.connect();

            JsonParser jp = new JsonParser();
            JsonObject root = jp.parse(new InputStreamReader((java.io.InputStream) request.getContent())).getAsJsonObject();
            JsonObject timings = root.getAsJsonObject("data").getAsJsonObject("timings");

            config.set("Imsak", timings.get("Fajr").getAsString());
            config.set("Gunes", timings.get("Sunrise").getAsString());
            config.set("Ogle", timings.get("Dhuhr").getAsString());
            config.set("Ikindi", timings.get("Asr").getAsString());
            config.set("Aksam", timings.get("Maghrib").getAsString());
            config.set("Yatsi", timings.get("Isha").getAsString());

            config.save(file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}