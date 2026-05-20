package com.mox.MoxCamii.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mox.MoxCamii.MoxCamii;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IstanbulPrayerAPI {

    public static boolean fetchAndSave(MoxCamii plugin) {
        try {
            String apiUrl = plugin.getConfig().getString("Settings.Namaz-Vakitleri.API-URL", "http://api.aladhan.com/v1/timingsByCity?city=Istanbul&country=Turkey&method=13");
            URL url = new URL(apiUrl);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            request.connect();

            JsonObject root = JsonParser.parseReader(new InputStreamReader(request.getInputStream())).getAsJsonObject();
            JsonObject timings = root.getAsJsonObject("data").getAsJsonObject("timings");
            JsonObject hijri = root.getAsJsonObject("data").getAsJsonObject("date").getAsJsonObject("hijri");

            FileConfiguration config = plugin.getPrayerTimeManager().getConfig();
            if (config != null) {
                config.set("Imsak", timings.get("Fajr").getAsString());
                config.set("Gunes", timings.get("Sunrise").getAsString());
                config.set("Ogle", timings.get("Dhuhr").getAsString());
                config.set("Ikindi", timings.get("Asr").getAsString());
                config.set("Aksam", timings.get("Maghrib").getAsString());
                config.set("Yatsi", timings.get("Isha").getAsString());
                plugin.getPrayerTimeManager().saveConfig();
            }

            // ÖZEL NAMAZLARIN OTOMATİK HİCRİ TAKVİM KONTROLÜ
            try {
                int hijriMonth = hijri.getAsJsonObject("month").get("number").getAsInt();
                int hijriDay = hijri.get("day").getAsInt();
                FileConfiguration scConfig = plugin.getSpecialClocksConfig();

                if (scConfig != null) {
                    // Ramazan & Teravih Kontrolü (Hicri 9. Ay)
                    if (scConfig.getBoolean("special_clocks.clocks.ramazan.auto-enable")) {
                        boolean isRamazan = (hijriMonth == 9);
                        boolean isRamazanBayrami = (hijriMonth == 10 && hijriDay == 1);

                        scConfig.set("special_clocks.clocks.ramazan.status", isRamazan || isRamazanBayrami);
                        scConfig.set("special_clocks.clocks.ramazan.types.teravih.status", isRamazan);
                        scConfig.set("special_clocks.clocks.ramazan.types.ramazan_bayrami_namazi.status", isRamazanBayrami);
                    }

                    // Kurban Bayramı Kontrolü (Hicri 12. Ay, 10. Gün)
                    if (scConfig.getBoolean("special_clocks.clocks.kurban_bayrami.auto-enable")) {
                        boolean isKurbanBayrami = (hijriMonth == 12 && hijriDay >= 10 && hijriDay <= 13);
                        scConfig.set("special_clocks.clocks.kurban_bayrami.status", isKurbanBayrami);
                        scConfig.set("special_clocks.clocks.kurban_bayrami.types.kurban_bayrami_namazi.status", (hijriMonth == 12 && hijriDay == 10));
                    }
                    plugin.saveSpecialClocksConfig();
                }
            } catch (Exception ignored) {
                plugin.getLogger().warning("Hicri takvim verisi çekilirken hata oluştu, özel namazlar güncellenemedi.");
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}