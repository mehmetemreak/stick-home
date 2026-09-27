package io.github.mehmetemreak.stickhome;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** Backs the in-app "Arayüz Ayarları" screen: theme, tracked leagues, remote-button app targets. */
public class SettingsStore {

    private static final String PREFS = "stick_home_prefs";
    private static final String KEY_THEME = "settings/theme";
    private static final String KEY_LEAGUES = "settings/leagues";
    private static final String KEY_NETFLIX_TARGET = "settings/netflix_target";
    private static final String KEY_PRIME_TARGET = "settings/prime_target";
    private static final String KEY_ICON_SPACING = "settings/icon_spacing";
    private static final String KEY_DOCK_PANEL = "settings/dock_panel";
    private static final String KEY_MATCH_WINDOW = "settings/match_window";

    public static final int SPACING_DAR = 0;
    public static final int SPACING_ORTA = 1;
    public static final int SPACING_GENIS = 2;

    public static final int MATCH_WINDOW_1H = 0;
    public static final int MATCH_WINDOW_1D = 1;
    public static final int MATCH_WINDOW_3D = 2;

    private static final String KEY_CALENDAR_WINDOW = "settings/calendar_window";
    public static final int CALENDAR_WINDOW_3D = 0;
    public static final int CALENDAR_WINDOW_1W = 1;
    public static final int CALENDAR_WINDOW_1M = 2;

    private static final String KEY_WEATHER_NAME = "settings/weather_name";
    private static final String KEY_WEATHER_LAT = "settings/weather_lat";
    private static final String KEY_WEATHER_LON = "settings/weather_lon";

    private final SharedPreferences prefs;

    public SettingsStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public int getThemeId() {
        return prefs.getInt(KEY_THEME, 0);
    }

    public void setThemeId(int id) {
        prefs.edit().putInt(KEY_THEME, id).apply();
    }

    public Set<String> getEnabledLeagues() {
        Set<String> defaults = new HashSet<>();
        for (FootballLeagues.League l : FootballLeagues.ALL) {
            if (l.enabledByDefault) defaults.add(l.id);
        }
        return prefs.getStringSet(KEY_LEAGUES, defaults);
    }

    public void setEnabledLeagues(Set<String> ids) {
        prefs.edit().putStringSet(KEY_LEAGUES, ids).apply();
    }

    /** Null = not remapped, the button keeps its stock behavior. */
    public String getNetflixTarget() {
        return prefs.getString(KEY_NETFLIX_TARGET, null);
    }

    public void setNetflixTarget(String packageName) {
        prefs.edit().putString(KEY_NETFLIX_TARGET, packageName).apply();
    }

    /** Null = not remapped, the button keeps its stock behavior. */
    public String getPrimeTarget() {
        return prefs.getString(KEY_PRIME_TARGET, null);
    }

    public void setPrimeTarget(String packageName) {
        prefs.edit().putString(KEY_PRIME_TARGET, packageName).apply();
    }

    /** Null = no city chosen, weather is hidden. */
    public String getWeatherName() {
        return prefs.getString(KEY_WEATHER_NAME, null);
    }

    public double getWeatherLat() {
        return Double.longBitsToDouble(prefs.getLong(KEY_WEATHER_LAT, 0));
    }

    public double getWeatherLon() {
        return Double.longBitsToDouble(prefs.getLong(KEY_WEATHER_LON, 0));
    }

    public void setWeatherLocation(String name, double lat, double lon) {
        prefs.edit()
                .putString(KEY_WEATHER_NAME, name)
                .putLong(KEY_WEATHER_LAT, Double.doubleToRawLongBits(lat))
                .putLong(KEY_WEATHER_LON, Double.doubleToRawLongBits(lon))
                .apply();
    }

    public void clearWeatherLocation() {
        prefs.edit().remove(KEY_WEATHER_NAME).remove(KEY_WEATHER_LAT).remove(KEY_WEATHER_LON).apply();
    }

    public int getIconSpacing() {
        return prefs.getInt(KEY_ICON_SPACING, SPACING_ORTA);
    }

    public void setIconSpacing(int spacing) {
        prefs.edit().putInt(KEY_ICON_SPACING, spacing).apply();
    }

    public boolean getDockPanelEnabled() {
        return prefs.getBoolean(KEY_DOCK_PANEL, false);
    }

    public void setDockPanelEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DOCK_PANEL, enabled).apply();
    }

    public int getMatchWindow() {
        return prefs.getInt(KEY_MATCH_WINDOW, MATCH_WINDOW_1H);
    }

    public void setMatchWindow(int window) {
        prefs.edit().putInt(KEY_MATCH_WINDOW, window).apply();
    }

    public int getCalendarWindow() {
        return prefs.getInt(KEY_CALENDAR_WINDOW, CALENDAR_WINDOW_1W);
    }

    public void setCalendarWindow(int window) {
        prefs.edit().putInt(KEY_CALENDAR_WINDOW, window).apply();
    }
}
