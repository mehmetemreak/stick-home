package io.github.mehmetemreak.stickhome;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.TimeZone;

/**
 * Shows matches kicking off within a user-configurable window (1 hour / 1 day / 3 days), from
 * a fixed set of leagues, using TheSportsDB's free public API (test key "123", no signup). One
 * fetch per Home resume, no polling/background service.
 *
 * League selection and the time window are user-configurable via "Arayüz Ayarları"
 * (SettingsStore), from the curated list in FootballLeagues.
 *
 * TODO(open-source release): should also let users pick other sports (basketball, etc.) -
 * TheSportsDB supports non-soccer leagues too via the same eventsnextleague.php endpoint with
 * different league IDs.
 */
public class FootballWidget {

    public static class Match {
        public final String title;
        public final long startMillis;

        Match(String title, long startMillis) {
            this.title = title;
            this.startMillis = startMillis;
        }
    }

    public interface Callback {
        void onResult(List<Match> matches);
        void onError();
    }

    private static final long CACHE_MS = 30L * 60 * 1000;
    // All upcoming fixtures for the league set, unfiltered by window; main thread only.
    private static List<Match> cachedMatches;
    private static java.util.Set<String> cachedLeagues;
    private static long cachedAt;

    public static void fetch(Context context, Callback callback) {
        SettingsStore settings = new SettingsStore(context);
        java.util.Set<String> leagueIds = new java.util.HashSet<>(settings.getEnabledLeagues());
        long windowMillis = windowMillis(settings.getMatchWindow());
        Handler main = new Handler(Looper.getMainLooper());

        if (cachedMatches != null && leagueIds.equals(cachedLeagues)
                && System.currentTimeMillis() - cachedAt < CACHE_MS) {
            List<Match> hit = inWindow(cachedMatches, windowMillis);
            main.post(() -> callback.onResult(hit));
            return;
        }

        new Thread(() -> {
            List<Match> all = new ArrayList<>();
            boolean anySucceeded = false;

            long now = System.currentTimeMillis();

            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));

            for (String leagueId : leagueIds) {
                try {
                    URL url = new URL("https://www.thesportsdb.com/api/v1/json/123/eventsnextleague.php?id=" + leagueId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    InputStream in = conn.getInputStream();
                    String body = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A").next();
                    in.close();
                    anySucceeded = true;

                    JSONObject json = new JSONObject(body);
                    JSONArray events = json.optJSONArray("events");
                    if (events == null) continue;

                    for (int i = 0; i < events.length(); i++) {
                        JSONObject event = events.getJSONObject(i);
                        String timestamp = event.optString("strTimestamp", null);
                        if (timestamp == null) continue;

                        Date matchDate = parser.parse(timestamp);
                        long matchMillis = matchDate.getTime();
                        if (matchMillis >= now) {
                            String home = event.optString("strHomeTeam", "?");
                            String away = event.optString("strAwayTeam", "?");
                            all.add(new Match(home + " - " + away, matchMillis));
                        }
                    }
                } catch (Exception e) {
                    // Skip this league on error, keep trying the others.
                }
            }

            all.sort((a, b) -> Long.compare(a.startMillis, b.startMillis));

            boolean succeeded = anySucceeded;
            main.post(() -> {
                if (succeeded) {
                    cachedMatches = all;
                    cachedLeagues = leagueIds;
                    cachedAt = System.currentTimeMillis();
                    callback.onResult(inWindow(all, windowMillis));
                } else {
                    callback.onError();
                }
            });
        }).start();
    }

    private static List<Match> inWindow(List<Match> all, long windowMillis) {
        long now = System.currentTimeMillis();
        List<Match> result = new ArrayList<>();
        for (Match m : all) {
            if (m.startMillis >= now && m.startMillis <= now + windowMillis) result.add(m);
        }
        return result;
    }

    private static long windowMillis(int matchWindow) {
        switch (matchWindow) {
            case SettingsStore.MATCH_WINDOW_1D: return 24L * 60 * 60 * 1000;
            case SettingsStore.MATCH_WINDOW_3D: return 3L * 24 * 60 * 60 * 1000;
            default: return 60L * 60 * 1000;
        }
    }
}
