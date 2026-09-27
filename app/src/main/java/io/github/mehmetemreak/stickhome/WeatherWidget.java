package io.github.mehmetemreak.stickhome;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Open-Meteo forecast + geocoding (no API key). One fetch per Home resume at most, cached for
 * 30 minutes - no background polling, no foreground service, no wakelock. The city is picked
 * once in Arayüz Ayarları; no location permission is used.
 */
public class WeatherWidget {

    public interface Callback {
        void onResult(String today, String tomorrow);
        void onError();
    }

    public interface SearchCallback {
        void onResult(List<Place> places);
        void onError();
    }

    public static class Place {
        public final String name;
        public final String detail;
        public final double lat;
        public final double lon;

        Place(String name, String detail, double lat, double lon) {
            this.name = name;
            this.detail = detail;
            this.lat = lat;
            this.lon = lon;
        }
    }

    private static final long CACHE_MS = 30L * 60 * 1000;
    // Main thread only.
    private static String cachedToday;
    private static String cachedTomorrow;
    private static String cachedKey;
    private static long cachedAt;

    public static void fetch(double lat, double lon, Callback callback) {
        Handler main = new Handler(Looper.getMainLooper());
        String key = lat + "," + lon;
        if (cachedToday != null && key.equals(cachedKey) && System.currentTimeMillis() - cachedAt < CACHE_MS) {
            String today = cachedToday, tomorrow = cachedTomorrow;
            main.post(() -> callback.onResult(today, tomorrow));
            return;
        }
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject(get(String.format(Locale.US,
                        "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                                + "&current_weather=true"
                                + "&daily=weathercode,temperature_2m_max,temperature_2m_min"
                                + "&timezone=auto", lat, lon)));
                JSONObject current = json.getJSONObject("current_weather");
                String today = Math.round(current.getDouble("temperature")) + "°C  "
                        + describeCode(current.getInt("weathercode"));

                String tomorrow = null;
                JSONObject daily = json.optJSONObject("daily");
                if (daily != null) {
                    JSONArray maxArr = daily.getJSONArray("temperature_2m_max");
                    JSONArray minArr = daily.getJSONArray("temperature_2m_min");
                    JSONArray codeArr = daily.getJSONArray("weathercode");
                    if (maxArr.length() > 1) {
                        tomorrow = "Yarın " + Math.round(minArr.getDouble(1)) + "° / "
                                + Math.round(maxArr.getDouble(1)) + "°  " + describeCode(codeArr.getInt(1));
                    }
                }

                String finalTomorrow = tomorrow;
                main.post(() -> {
                    cachedToday = today;
                    cachedTomorrow = finalTomorrow;
                    cachedKey = key;
                    cachedAt = System.currentTimeMillis();
                    callback.onResult(today, finalTomorrow);
                });
            } catch (Exception e) {
                main.post(callback::onError);
            }
        }).start();
    }

    public static void search(String query, SearchCallback callback) {
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject(get(
                        "https://geocoding-api.open-meteo.com/v1/search?count=8&language=tr&format=json&name="
                                + URLEncoder.encode(query, "UTF-8")));
                List<Place> places = new ArrayList<>();
                JSONArray results = json.optJSONArray("results");
                if (results != null) {
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject r = results.getJSONObject(i);
                        String admin = r.optString("admin1", "");
                        String country = r.optString("country", "");
                        String detail = admin.isEmpty() ? country : admin + ", " + country;
                        places.add(new Place(r.getString("name"), detail,
                                r.getDouble("latitude"), r.getDouble("longitude")));
                    }
                }
                main.post(() -> callback.onResult(places));
            } catch (Exception e) {
                main.post(callback::onError);
            }
        }).start();
    }

    private static String get(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try (InputStream in = conn.getInputStream()) {
            return new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
        } finally {
            conn.disconnect();
        }
    }

    private static String describeCode(int code) {
        if (code == 0) return "Açık";
        if (code <= 2) return "Parçalı bulutlu";
        if (code == 3) return "Bulutlu";
        if (code == 45 || code == 48) return "Sisli";
        if (code >= 51 && code <= 57) return "Çiseleme";
        if (code >= 61 && code <= 67) return "Yağmurlu";
        if (code >= 71 && code <= 77) return "Karlı";
        if (code >= 80 && code <= 82) return "Sağanak";
        if (code >= 85 && code <= 86) return "Kar sağanağı";
        if (code >= 95) return "Gök gürültülü";
        return "";
    }
}
