package io.github.mehmetemreak.stickhome;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeActivity extends Activity {

    private static final int REQ_CALENDAR = 42;

    private DockStore dockStore;
    private NameStore nameStore;
    private SettingsStore settingsStore;
    private View rootView;
    private final List<DockStore.Entry> entries = new ArrayList<>();
    private LinearLayout shelf;
    private LinearLayout chipsRow;
    private LinearLayout upcomingPanel;
    private TextView clock;
    private TextView greeting;
    private TextView weather;
    private TextView weatherTomorrow;
    private View revealHint;

    private int moveModeIndex = -1;
    private int lastFocusedIndex = 0;
    private int upcomingGeneration = 0;
    private List<DockStore.Entry> preMovOrder = null;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, 30_000);
        }
    };

    private static final long LONG_PRESS_MS = 550;
    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingLongPress;
    private boolean longPressFired = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dockStore = new DockStore(this);
        nameStore = new NameStore(this);
        settingsStore = new SettingsStore(this);
        setContentView(R.layout.activity_home);

        rootView = findViewById(R.id.root);
        shelf = findViewById(R.id.app_shelf);
        chipsRow = findViewById(R.id.chips_row);
        upcomingPanel = findViewById(R.id.upcoming_panel);
        clock = findViewById(R.id.clock);
        greeting = findViewById(R.id.greeting);
        weather = findViewById(R.id.weather);
        weatherTomorrow = findViewById(R.id.weather_tomorrow);
        revealHint = findViewById(R.id.reveal_hint);

        entries.addAll(dockStore.seedIfEmpty(this));
        renderShelf(-1);
        renderChips();

        if (!CalendarWidget.hasPermission(this)) {
            requestPermissions(new String[]{android.Manifest.permission.READ_CALENDAR}, REQ_CALENDAR);
        }
        if (!nameStore.hasName()) {
            NamePrompt.show(this, name -> {
                nameStore.setName(name);
                updateClock();
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CALENDAR) {
            renderUpcoming();
            renderFootball();
        }
    }

    private void renderUpcoming() {
        upcomingGeneration++;
        upcomingPanel.removeAllViews();

        if (!CalendarWidget.hasPermission(this)) {
            TextView header = new TextView(this);
            header.setText("Takvim izni verilmedi");
            header.setTextColor(0x99FFFFFF);
            header.setTextSize(13);
            upcomingPanel.addView(header);
            return;
        }

        List<CalendarWidget.UpcomingEvent> events = CalendarWidget.loadUpcoming(this, 4);

        TextView header = new TextView(this);
        header.setText("Yaklaşan");
        header.setTextColor(0x99FFFFFF);
        header.setTextSize(13);
        header.setPadding(0, 0, 0, dp(6));
        upcomingPanel.addView(header);

        if (events.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("30 gün içinde etkinlik yok");
            empty.setTextColor(0x88ECECEC);
            empty.setTextSize(13);
            upcomingPanel.addView(empty);
            return;
        }

        for (CalendarWidget.UpcomingEvent e : events) {
            TextView row = new TextView(this);
            row.setText(e.friendlyTime() + "  ·  " + e.title);
            row.setTextColor(0xDDECECEC);
            row.setTextSize(14);
            row.setPadding(0, 0, 0, dp(4));
            row.setMaxLines(1);
            upcomingPanel.addView(row);
        }
    }

    private void renderFootball() {
        int generation = upcomingGeneration;
        FootballWidget.fetch(this, new FootballWidget.Callback() {
            @Override
            public void onResult(List<FootballWidget.Match> matches) {
                // A newer renderUpcoming() already rebuilt the panel; its own fetch will fill it.
                if (generation != upcomingGeneration || matches.isEmpty()) return;

                TextView header = new TextView(HomeActivity.this);
                header.setText("Yaklaşan Maçlar");
                header.setTextColor(0x99FFFFFF);
                header.setTextSize(13);
                header.setPadding(0, dp(10), 0, dp(6));
                upcomingPanel.addView(header);

                boolean multiDay = settingsStore.getMatchWindow() != SettingsStore.MATCH_WINDOW_1H;
                SimpleDateFormat timeFmt = new SimpleDateFormat(
                        multiDay ? "d MMM HH:mm" : "HH:mm", new Locale("tr", "TR"));
                for (FootballWidget.Match m : matches) {
                    TextView row = new TextView(HomeActivity.this);
                    row.setText(timeFmt.format(new Date(m.startMillis)) + "  ·  " + m.title);
                    row.setTextColor(0xDDECECEC);
                    row.setTextSize(14);
                    row.setPadding(0, 0, 0, dp(4));
                    row.setMaxLines(1);
                    upcomingPanel.addView(row);
                }
            }

            @Override
            public void onError() {
                // Silently skip - no network or API unavailable.
            }
        });
    }

    private void renderChips() {
        chipsRow.removeAllViews();
        chipsRow.addView(buildChip("Ayarlar", v -> {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show();
            }
        }));
        chipsRow.addView(buildChip("Tüm Uygulamalar", v -> startActivity(new Intent(this, AllAppsActivity.class))));
        chipsRow.addView(buildChip("Google Play", v -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage("com.android.vending");
            if (launch != null) {
                startActivity(launch);
            } else {
                Toast.makeText(this, "Play Store açılamadı", Toast.LENGTH_SHORT).show();
            }
        }));
        chipsRow.addView(buildChip("Arayüz Ayarları", v -> startActivity(new Intent(this, SettingsActivity.class))));

        for (int i = 0; i < chipsRow.getChildCount(); i++) {
            View chip = chipsRow.getChildAt(i);
            chip.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.getAction() == KeyEvent.ACTION_DOWN) {
                    hideChips();
                    return true;
                }
                return false;
            });
        }
    }

    private void revealChips() {
        chipsRow.setVisibility(View.VISIBLE);
        revealHint.setVisibility(View.INVISIBLE);
        if (chipsRow.getChildCount() > 0) {
            chipsRow.getChildAt(0).requestFocus();
        }
    }

    private void hideChips() {
        chipsRow.setVisibility(View.GONE);
        revealHint.setVisibility(View.VISIBLE);
        int count = shelf.getChildCount();
        if (count > 0) {
            shelf.getChildAt(Math.min(lastFocusedIndex, count - 1)).requestFocus();
        }
    }

    private View buildChip(String text, View.OnClickListener onClick) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextColor(Color.parseColor("#CCCCCC"));
        chip.setTextSize(10);
        int padH = dp(13);
        int padV = dp(7);
        chip.setPadding(padH, padV, padH, padV);
        chip.setFocusable(true);
        chip.setFocusableInTouchMode(true);
        chip.setBackgroundResource(R.drawable.tile_focus_background);
        chip.setOnClickListener(onClick);
        chip.setOnFocusChangeListener(this::animateFocus);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(6), 0, dp(6), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    @Override
    protected void onResume() {
        super.onResume();
        entries.clear();
        entries.addAll(dockStore.load());
        renderShelf(lastFocusedIndex);
        handler.post(clockTick);
        rootView.setBackground(ThemeGradient.build(this, settingsStore.getThemeId()));
        shelf.setBackgroundResource(settingsStore.getDockPanelEnabled() ? R.drawable.shelf_glass_background : 0);
        renderUpcoming();
        renderFootball();
        hideChips();
        cleanupBackgroundApps();
        weather.setText("");
        weatherTomorrow.setText("");
        if (settingsStore.getWeatherName() == null) return;
        WeatherWidget.fetch(settingsStore.getWeatherLat(), settingsStore.getWeatherLon(), new WeatherWidget.Callback() {
            @Override
            public void onResult(String today, String tomorrow) {
                weather.setText(today == null ? "" : today);
                weatherTomorrow.setText(tomorrow == null ? "" : tomorrow);
            }

            @Override
            public void onError() {
                weather.setText("");
                weatherTomorrow.setText("");
            }
        });
    }

    /** Every time we return to Home, proactively free memory by killing background processes
     *  for recently used apps - but never system/Google/vendor services this device needs
     *  (DRM, Cast, Assistant, Bluetooth, Play Services, etc). No root required.
     *  getRunningAppProcesses() only returns our own process since Android 5.1, so the candidate
     *  list comes from UsageStats (needs Usage Access). killBackgroundProcesses() itself only
     *  touches cached processes, so an app playing music via a foreground service survives. */
    private void cleanupBackgroundApps() {
        if (!UsageAccess.isGranted(this)) return;
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        if (am == null || usm == null) return;

        long now = System.currentTimeMillis();
        long since = now - 6L * 60 * 60 * 1000;
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, since, now);
        if (stats == null) return;

        String myPackage = getPackageName();
        Set<String> killed = new HashSet<>();
        for (UsageStats s : stats) {
            String pkg = s.getPackageName();
            if (s.getLastTimeUsed() < since || killed.contains(pkg)) continue;
            if (isProtectedPackage(pkg, myPackage)) continue;
            am.killBackgroundProcesses(pkg);
            killed.add(pkg);
        }
    }

    private boolean isProtectedPackage(String pkg, String myPackage) {
        if (pkg.equals(myPackage)) return true;
        if (pkg.startsWith("com.android.")) return true;
        if (pkg.startsWith("com.google.android.gms")) return true;
        if (pkg.startsWith("com.google.android.gsf")) return true;
        if (pkg.startsWith("com.google.process")) return true;
        if (pkg.equals("com.google.android.apps.mediashell")) return true;
        if (pkg.equals("com.google.android.katniss")) return true;
        if (pkg.equals("com.google.android.tv.remote.service")) return true;
        if (pkg.equals("com.google.android.webview")) return true;
        if (pkg.equals("com.google.android.tts")) return true;
        if (pkg.equals("com.google.android.inputmethod.latin")) return true;
        if (pkg.contains("droidlogic")) return true;
        if (pkg.contains("xiaomi")) return true;
        if (pkg.startsWith("mitv.")) return true;
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(clockTick);
    }

    private void updateClock() {
        SimpleDateFormat fmt = new SimpleDateFormat("d MMMM EEEE, HH:mm", new Locale("tr", "TR"));
        clock.setText(fmt.format(new Date()));
        String name = nameStore.getName();
        String text = greetingForHour();
        if (name != null && !name.isEmpty()) {
            text = text + ", " + name;
        }
        greeting.setText(text);
    }

    private String greetingForHour() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return "Günaydın";
        if (hour >= 12 && hour < 17) return "İyi günler";
        if (hour >= 17 && hour < 22) return "İyi akşamlar";
        return "İyi geceler";
    }

    private void renderShelf(int focusIndex) {
        shelf.removeAllViews();

        for (int i = 0; i < entries.size(); i++) {
            View tile = buildTile(entries.get(i), i);
            shelf.addView(tile);
        }

        View toFocus;
        if (focusIndex >= 0 && focusIndex < shelf.getChildCount()) {
            toFocus = shelf.getChildAt(focusIndex);
        } else if (shelf.getChildCount() > 0) {
            toFocus = shelf.getChildAt(0);
        } else {
            toFocus = null;
        }
        if (toFocus != null) {
            toFocus.post(toFocus::requestFocus);
        }
    }

    private View buildTile(DockStore.Entry entry, int index) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        int padH = dp(4);
        int padV = dp(6);
        tile.setPadding(padH, padV, padH, padV);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setBackgroundResource(R.drawable.tile_focus_background);

        if (moveModeIndex == index) {
            tile.setAlpha(0.85f);
        }

        tile.addView(IconRenderer.build(this, entry.packageName, entry.activityName, 48));

        TextView label = new TextView(this);
        label.setText(entry.label);
        label.setTextColor(0xFFECECEC);
        label.setTextSize(11);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(1);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setPadding(0, dp(6), 0, 0);
        boolean alwaysShowLabels = settingsStore.getDockPanelEnabled();
        label.setVisibility(alwaysShowLabels ? View.VISIBLE : View.INVISIBLE);
        label.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tile.addView(label);

        int spacingMargin = spacingMarginDp();
        LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(dp(66), LinearLayout.LayoutParams.WRAP_CONTENT);
        tileParams.setMargins(dp(spacingMargin), 0, dp(spacingMargin), 0);
        tile.setLayoutParams(tileParams);

        tile.setOnFocusChangeListener((v, hasFocus) -> {
            animateFocus(v, hasFocus);
            if (hasFocus) {
                lastFocusedIndex = index;
            }
            if (!alwaysShowLabels) {
                label.setVisibility(hasFocus ? View.VISIBLE : View.INVISIBLE);
            }
        });

        tile.setOnKeyListener((v, keyCode, event) -> handleTileKey(entry, index, keyCode, event));
        tile.setOnClickListener(v -> launch(entry));

        return tile;
    }

    private int spacingMarginDp() {
        switch (settingsStore.getIconSpacing()) {
            case SettingsStore.SPACING_DAR: return 0;
            case SettingsStore.SPACING_GENIS: return 8;
            default: return 2;
        }
    }

    private void animateFocus(View v, boolean hasFocus) {
        float scale = hasFocus ? 1.14f : 1.0f;
        v.animate().scaleX(scale).scaleY(scale).setDuration(160).start();
        v.setSelected(hasFocus);

        if (android.os.Build.VERSION.SDK_INT >= 28) {
            if (hasFocus) {
                v.setElevation(dp(10));
                v.setOutlineAmbientShadowColor(0xFF8FEBFF);
                v.setOutlineSpotShadowColor(0xFF8FEBFF);
            } else {
                v.setElevation(0);
            }
        }
    }

    private boolean handleTileKey(DockStore.Entry entry, int index, int keyCode, KeyEvent event) {
        if (moveModeIndex == -1 && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                revealChips();
            }
            return true;
        }

        if (moveModeIndex == index && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                int target = keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? index - 1 : index + 1;
                if (target >= 0 && target < entries.size()) {
                    DockStore.Entry tmp = entries.get(index);
                    entries.set(index, entries.get(target));
                    entries.set(target, tmp);
                    moveModeIndex = target;
                    renderShelf(target);
                }
            }
            return true;
        }

        if (moveModeIndex == index && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                confirmMove();
            }
            return true;
        }

        if (moveModeIndex == index && keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                cancelMove();
            }
            return true;
        }

        if (moveModeIndex != -1) {
            // A move is active elsewhere/blocked: swallow other keys on this tile.
            return keyCode != KeyEvent.KEYCODE_DPAD_UP && keyCode != KeyEvent.KEYCODE_DPAD_DOWN ? true : false;
        }

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0) {
                    longPressFired = false;
                    pendingLongPress = () -> {
                        longPressFired = true;
                        openContextMenu(entry, index);
                    };
                    longPressHandler.postDelayed(pendingLongPress, LONG_PRESS_MS);
                }
                if (event.isLongPress()) {
                    if (pendingLongPress != null) longPressHandler.removeCallbacks(pendingLongPress);
                    if (!longPressFired) {
                        longPressFired = true;
                        openContextMenu(entry, index);
                    }
                }
                return true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                if (pendingLongPress != null) longPressHandler.removeCallbacks(pendingLongPress);
                if (!longPressFired) {
                    launch(entry);
                }
                longPressFired = false;
                return true;
            }
        }

        return false;
    }

    private void openContextMenu(DockStore.Entry entry, int index) {
        boolean uninstallAllowed = isUserUninstallable(entry.packageName);
        TileContextMenu.show(this, uninstallAllowed, new TileContextMenu.Listener() {
            @Override
            public void onMove() {
                moveModeIndex = index;
                preMovOrder = new ArrayList<>(entries);
                renderShelf(index);
                Toast.makeText(HomeActivity.this, "◀ ▶ taşı · OK onayla · GERİ iptal", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRemoveFromHome() {
                dockStore.removeFromDock(entry.key());
                entries.remove(index);
                renderShelf(Math.min(index, entries.size() - 1));
            }

            @Override
            public void onAppInfo() {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.setData(Uri.parse("package:" + entry.packageName));
                startActivity(i);
            }

            @Override
            public void onUninstall() {
                Intent i = new Intent(Intent.ACTION_DELETE);
                i.setData(Uri.parse("package:" + entry.packageName));
                startActivity(i);
            }
        });
    }

    private void confirmMove() {
        moveModeIndex = -1;
        preMovOrder = null;
        dockStore.save(entries);
        renderShelf(-1);
    }

    private void cancelMove() {
        if (preMovOrder != null) {
            entries.clear();
            entries.addAll(preMovOrder);
        }
        moveModeIndex = -1;
        preMovOrder = null;
        renderShelf(-1);
    }

    private boolean isUserUninstallable(String packageName) {
        try {
            android.content.pm.ApplicationInfo ai = getPackageManager().getApplicationInfo(packageName, 0);
            return (ai.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void launch(DockStore.Entry entry) {
        Intent launch = AppLauncher.intentFor(this, entry.packageName, entry.activityName);
        try {
            if (launch == null) throw new IllegalStateException();
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(launch);
        } catch (Exception e) {
            Toast.makeText(this, "Açılamadı: " + entry.label, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (moveModeIndex != -1) {
            cancelMove();
            return;
        }
        // HOME activity: swallow BACK at the root so it doesn't exit to a blank state.
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
