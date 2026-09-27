package io.github.mehmetemreak.stickhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.Intent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists the ordered dock (home shelf) as "pkg|activity" entries plus a
 * remembered label per entry, under key home/dock_v1.
 */
public class DockStore {

    private static final String PREFS = "stick_home_prefs";
    private static final String KEY_DOCK = "home/dock_v1";
    private static final String ENTRY_SEP = ";;";
    private static final String FIELD_SEP = "|";

    private final SharedPreferences prefs;

    public DockStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static class Entry {
        public final String packageName;
        public final String activityName;
        public final String label;

        public Entry(String packageName, String activityName, String label) {
            this.packageName = packageName;
            this.activityName = activityName;
            this.label = label;
        }

        String serialize() {
            return packageName + FIELD_SEP + activityName + FIELD_SEP + label;
        }

        static Entry parse(String raw) {
            String[] parts = raw.split("\\" + FIELD_SEP, 3);
            if (parts.length < 3) return null;
            return new Entry(parts[0], parts[1], parts[2]);
        }

        public String key() {
            return packageName + FIELD_SEP + activityName;
        }
    }

    public boolean hasSavedDock() {
        return prefs.contains(KEY_DOCK);
    }

    public List<Entry> load() {
        List<Entry> result = new ArrayList<>();
        String raw = prefs.getString(KEY_DOCK, "");
        if (raw.isEmpty()) return result;
        for (String piece : raw.split(ENTRY_SEP)) {
            Entry e = Entry.parse(piece);
            if (e != null) result.add(e);
        }
        return result;
    }

    public void save(List<Entry> entries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(ENTRY_SEP);
            sb.append(entries.get(i).serialize());
        }
        prefs.edit().putString(KEY_DOCK, sb.toString()).apply();
    }

    /** Seeds the dock with every currently-launchable Leanback app, only if no dock was ever saved. */
    public List<Entry> seedIfEmpty(Context context) {
        if (hasSavedDock()) {
            return load();
        }
        PackageManager pm = context.getPackageManager();
        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN);
        leanbackIntent.addCategory("android.intent.category.LEANBACK_LAUNCHER");
        List<ResolveInfo> apps = pm.queryIntentActivities(leanbackIntent, 0);

        Map<String, Entry> unique = new LinkedHashMap<>();
        for (ResolveInfo info : apps) {
            String pkg = info.activityInfo.packageName;
            if (context.getPackageName().equals(pkg)) continue;
            Entry e = new Entry(pkg, info.activityInfo.name, info.loadLabel(pm).toString());
            unique.putIfAbsent(e.key(), e);
        }

        List<Entry> sorted = new ArrayList<>(unique.values());
        sorted.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
        save(sorted);
        return sorted;
    }

    public void addToDock(Entry entry) {
        List<Entry> entries = load();
        for (Entry e : entries) {
            if (e.key().equals(entry.key())) return;
        }
        entries.add(entry);
        save(entries);
    }

    public void removeFromDock(String key) {
        List<Entry> entries = load();
        entries.removeIf(e -> e.key().equals(key));
        save(entries);
    }

    public boolean isOnDock(String key) {
        for (Entry e : load()) {
            if (e.key().equals(key)) return true;
        }
        return false;
    }
}
