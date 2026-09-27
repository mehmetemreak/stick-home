package io.github.mehmetemreak.stickhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends Activity {

    private SettingsStore store;
    private LinearLayout root;
    private GridLayout themeRow;
    private GridLayout leagueRow;
    private LinearLayout matchWindowRow;
    private LinearLayout weatherRow;
    private LinearLayout calendarWindowRow;
    private LinearLayout appearanceRow;
    private LinearLayout otherSportsRow;
    private TextView netflixTargetLabel;
    private TextView primeTargetLabel;
    private TextView accessibilityWarning;
    private TextView usageWarning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new SettingsStore(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(ThemeGradient.build(this, store.getThemeId()));
        int margin = dp(48);

        TextView title = new TextView(this);
        title.setText("Arayüz Ayarları");
        title.setTextColor(Color.parseColor("#F2FBFF"));
        title.setTextSize(22);
        title.setPadding(margin, margin, margin, dp(24));
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(margin, 0, margin, margin);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        content.addView(sectionHeader("Tema"));
        themeRow = new GridLayout(this);
        themeRow.setColumnCount(4);
        themeRow.setClipChildren(false);
        themeRow.setPadding(0, 0, 0, dp(16));
        content.addView(themeRow);
        renderThemes();

        content.addView(sectionHeader("Görünüm"));
        appearanceRow = new LinearLayout(this);
        appearanceRow.setOrientation(LinearLayout.HORIZONTAL);
        appearanceRow.setClipChildren(false);
        appearanceRow.setPadding(0, 0, 0, dp(24));
        content.addView(appearanceRow);
        renderAppearance();

        content.addView(sectionHeader("Takvim"));
        calendarWindowRow = new LinearLayout(this);
        calendarWindowRow.setOrientation(LinearLayout.HORIZONTAL);
        calendarWindowRow.setClipChildren(false);
        calendarWindowRow.setPadding(0, 0, 0, dp(24));
        content.addView(calendarWindowRow);
        renderCalendarWindow();

        content.addView(sectionHeader("Hava durumu"));
        weatherRow = new LinearLayout(this);
        weatherRow.setOrientation(LinearLayout.HORIZONTAL);
        weatherRow.setClipChildren(false);
        weatherRow.setPadding(0, 0, 0, dp(24));
        content.addView(weatherRow);
        renderWeather();

        content.addView(sectionHeader("Yaklaşan Maçlar — Futbol"));

        matchWindowRow = new LinearLayout(this);
        matchWindowRow.setOrientation(LinearLayout.HORIZONTAL);
        matchWindowRow.setClipChildren(false);
        matchWindowRow.setPadding(0, 0, 0, dp(12));
        content.addView(matchWindowRow);
        renderMatchWindow();

        leagueRow = new GridLayout(this);
        leagueRow.setColumnCount(4);
        leagueRow.setClipChildren(false);
        leagueRow.setPadding(0, 0, 0, dp(16));
        content.addView(leagueRow);

        content.addView(sectionHeader("Diğer sporlar"));
        otherSportsRow = new LinearLayout(this);
        otherSportsRow.setOrientation(LinearLayout.HORIZONTAL);
        otherSportsRow.setClipChildren(false);
        otherSportsRow.setPadding(0, 0, 0, dp(24));
        content.addView(otherSportsRow);

        renderLeagues();

        content.addView(sectionHeader("Kumanda tuşları"));

        accessibilityWarning = new TextView(this);
        accessibilityWarning.setTextColor(Color.parseColor("#FFB84D"));
        accessibilityWarning.setTextSize(12);
        accessibilityWarning.setPadding(0, 0, 0, dp(10));
        accessibilityWarning.setFocusable(true);
        accessibilityWarning.setFocusableInTouchMode(true);
        accessibilityWarning.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        content.addView(accessibilityWarning);

        TextView note = new TextView(this);
        note.setText("Not: bu tuşların çalışması Erişilebilirlik servisinin açık olmasına bağlı. Uygulama güncellendiğinde Android bazen bu izni otomatik kapatabiliyor — kapanırsa buradan (veya Ayarlar → Erişilebilirlik) tekrar açman gerekir.");
        note.setTextColor(Color.parseColor("#888888"));
        note.setTextSize(11);
        note.setPadding(0, 0, 0, dp(14));
        content.addView(note);

        LinearLayout netflixRow = new LinearLayout(this);
        netflixRow.setOrientation(LinearLayout.HORIZONTAL);
        netflixRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView netflixLabel = new TextView(this);
        netflixLabel.setText("Netflix tuşu →  ");
        netflixLabel.setTextColor(Color.parseColor("#CCCCCC"));
        netflixLabel.setTextSize(14);
        netflixRow.addView(netflixLabel);
        netflixTargetLabel = buildPickerChip(v -> pickApp(true));
        netflixRow.addView(netflixTargetLabel);
        content.addView(netflixRow);

        LinearLayout primeRow = new LinearLayout(this);
        primeRow.setOrientation(LinearLayout.HORIZONTAL);
        primeRow.setGravity(Gravity.CENTER_VERTICAL);
        primeRow.setPadding(0, dp(10), 0, 0);
        TextView primeLabel = new TextView(this);
        primeLabel.setText("Prime Video tuşu →  ");
        primeLabel.setTextColor(Color.parseColor("#CCCCCC"));
        primeLabel.setTextSize(14);
        primeRow.addView(primeLabel);
        primeTargetLabel = buildPickerChip(v -> pickApp(false));
        primeRow.addView(primeTargetLabel);
        content.addView(primeRow);

        TextView cleanupHeader = sectionHeader("Arka plan temizliği");
        cleanupHeader.setPadding(0, dp(24), 0, dp(10));
        content.addView(cleanupHeader);
        usageWarning = new TextView(this);
        usageWarning.setTextSize(12);
        usageWarning.setFocusable(true);
        usageWarning.setFocusableInTouchMode(true);
        usageWarning.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } catch (Exception e) {
                android.widget.Toast.makeText(this,
                        "Bu cihazda ekranı yok: adb shell appops set " + getPackageName() + " GET_USAGE_STATS allow",
                        android.widget.Toast.LENGTH_LONG).show();
            }
        });
        content.addView(usageWarning);

        setContentView(root);
        refreshTargetLabels();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAccessibilityWarning();
        if (UsageAccess.isGranted(this)) {
            usageWarning.setText("✓ Açık — Ana ekrana dönünce son kullanılan uygulamalar bellekten atılır (müzik çalan uygulamalar hariç).");
            usageWarning.setTextColor(Color.parseColor("#8FEBFF"));
        } else {
            usageWarning.setText("⚠ Kapalı — \"Kullanım erişimi\" izni gerekiyor. Açmak için buraya bas.");
            usageWarning.setTextColor(Color.parseColor("#FFB84D"));
        }
    }

    private void refreshAccessibilityWarning() {
        boolean isOn = isKeyRemapServiceEnabled();
        if (isOn) {
            accessibilityWarning.setText("✓ Erişilebilirlik servisi açık, kumanda tuşları çalışıyor olmalı.");
            accessibilityWarning.setTextColor(Color.parseColor("#8FEBFF"));
        } else {
            accessibilityWarning.setText("⚠ Erişilebilirlik servisi KAPALI — kumanda tuşları çalışmaz. Açmak için buraya bas.");
            accessibilityWarning.setTextColor(Color.parseColor("#FFB84D"));
        }
    }

    /** The setting may store the component in short ("pkg/.Cls") or full ("pkg/pkg.Cls") form. */
    private boolean isKeyRemapServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        ComponentName ours = new ComponentName(this, KeyRemapService.class);
        for (String entry : enabled.split(":")) {
            if (ours.equals(ComponentName.unflattenFromString(entry))) return true;
        }
        return false;
    }

    private TextView sectionHeader(String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextColor(Color.parseColor("#8FEBFF"));
        header.setTextSize(13);
        header.setPadding(0, dp(4), 0, dp(10));
        return header;
    }

    private void renderThemes() {
        themeRow.removeAllViews();
        int selected = store.getThemeId();
        for (int i = 0; i < ThemeGradient.PRESETS.length; i++) {
            ThemeGradient.Preset preset = ThemeGradient.PRESETS[i];
            int id = i;
            TextView chip = new TextView(this);
            chip.setText(preset.name);
            chip.setTextColor(id == selected ? Color.parseColor("#8FEBFF") : Color.parseColor("#CCCCCC"));
            chip.setTextSize(13);
            int padH = dp(16), padV = dp(10);
            chip.setPadding(padH, padV, padH, padV);
            chip.setFocusable(true);
            chip.setFocusableInTouchMode(true);
            chip.setBackgroundResource(R.drawable.tile_focus_background);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.setMargins(0, 0, dp(8), dp(8));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                store.setThemeId(id);
                root.setBackground(ThemeGradient.build(this, id));
                renderThemes();
            });
            themeRow.addView(chip);
        }
    }

    private void renderAppearance() {
        appearanceRow.removeAllViews();

        String[] spacingLabels = {"İkon Boşluğu: Dar", "İkon Boşluğu: Orta", "İkon Boşluğu: Geniş"};
        int spacing = store.getIconSpacing();
        TextView spacingChip = simpleChip(spacingLabels[spacing]);
        spacingChip.setOnClickListener(v -> {
            store.setIconSpacing((store.getIconSpacing() + 1) % 3);
            renderAppearance();
        });
        appearanceRow.addView(spacingChip);

        boolean dockOn = store.getDockPanelEnabled();
        TextView dockChip = simpleChip("Dock Paneli: " + (dockOn ? "Açık" : "Kapalı"));
        dockChip.setOnClickListener(v -> {
            store.setDockPanelEnabled(!store.getDockPanelEnabled());
            renderAppearance();
        });
        appearanceRow.addView(dockChip);
    }

    private void renderMatchWindow() {
        matchWindowRow.removeAllViews();
        String[] labels = {"Maç Aralığı: 1 saat", "Maç Aralığı: 1 gün", "Maç Aralığı: 3 gün"};
        int window = store.getMatchWindow();
        TextView chip = simpleChip(labels[window]);
        chip.setOnClickListener(v -> {
            store.setMatchWindow((store.getMatchWindow() + 1) % 3);
            renderMatchWindow();
        });
        matchWindowRow.addView(chip);
    }

    private TextView simpleChip(String text) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextColor(Color.parseColor("#8FEBFF"));
        chip.setTextSize(13);
        int padH = dp(14), padV = dp(10);
        chip.setPadding(padH, padV, padH, padV);
        chip.setFocusable(true);
        chip.setFocusableInTouchMode(true);
        chip.setBackgroundResource(R.drawable.tile_focus_background);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(10), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private void renderLeagues() {
        leagueRow.removeAllViews();
        otherSportsRow.removeAllViews();
        Set<String> enabled = store.getEnabledLeagues();
        for (FootballLeagues.League league : FootballLeagues.ALL) {
            boolean isOn = enabled.contains(league.id);
            TextView chip = new TextView(this);
            chip.setText((isOn ? "✓ " : "") + league.name);
            chip.setTextColor(isOn ? Color.parseColor("#8FEBFF") : Color.parseColor("#999999"));
            chip.setTextSize(13);
            int padH = dp(14), padV = dp(10);
            chip.setPadding(padH, padV, padH, padV);
            chip.setFocusable(true);
            chip.setFocusableInTouchMode(true);
            chip.setBackgroundResource(R.drawable.tile_focus_background);
            chip.setOnClickListener(v -> {
                Set<String> current = new HashSet<>(store.getEnabledLeagues());
                if (current.contains(league.id)) {
                    current.remove(league.id);
                } else {
                    current.add(league.id);
                }
                store.setEnabledLeagues(current);
                renderLeagues();
            });

            if ("Futbol".equals(league.sport)) {
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.setMargins(0, 0, dp(8), dp(8));
                chip.setLayoutParams(lp);
                leagueRow.addView(chip);
            } else {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, dp(8), 0);
                chip.setLayoutParams(lp);
                otherSportsRow.addView(chip);
            }
        }
    }

    private TextView buildPickerChip(View.OnClickListener onClick) {
        TextView chip = new TextView(this);
        chip.setTextColor(Color.parseColor("#8FEBFF"));
        chip.setTextSize(14);
        int padH = dp(16), padV = dp(8);
        chip.setPadding(padH, padV, padH, padV);
        chip.setFocusable(true);
        chip.setFocusableInTouchMode(true);
        chip.setBackgroundResource(R.drawable.tile_focus_background);
        chip.setOnClickListener(onClick);
        return chip;
    }

    private void refreshTargetLabels() {
        netflixTargetLabel.setText(appLabel(store.getNetflixTarget()));
        primeTargetLabel.setText(appLabel(store.getPrimeTarget()));
    }

    private String appLabel(String packageName) {
        if (packageName == null) return "Değiştirme (orijinal davranış)";
        try {
            return getPackageManager().getApplicationInfo(packageName, 0)
                    .loadLabel(getPackageManager()).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private void pickApp(boolean forNetflix) {
        List<ResolveInfo> apps = launchableApps();
        String[] labels = new String[apps.size() + 1];
        labels[0] = "Değiştirme (orijinal davranış)";
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i).loadLabel(getPackageManager()).toString();
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(forNetflix ? "Netflix tuşu neyi açsın?" : "Prime Video tuşu neyi açsın?");
        builder.setItems(labels, (dialog, which) -> {
            String pkg = which == 0 ? null : apps.get(which - 1).activityInfo.packageName;
            if (forNetflix) {
                store.setNetflixTarget(pkg);
            } else {
                store.setPrimeTarget(pkg);
            }
            refreshTargetLabels();
        });
        builder.show();
    }

    private List<ResolveInfo> launchableApps() {
        PackageManager pm = getPackageManager();
        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN);
        leanbackIntent.addCategory("android.intent.category.LEANBACK_LAUNCHER");
        List<ResolveInfo> apps = pm.queryIntentActivities(leanbackIntent, 0);
        List<ResolveInfo> filtered = new ArrayList<>();
        for (ResolveInfo info : apps) {
            if (!getPackageName().equals(info.activityInfo.packageName)) {
                filtered.add(info);
            }
        }
        filtered.sort((a, b) -> a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        return filtered;
    }

    private void renderCalendarWindow() {
        calendarWindowRow.removeAllViews();
        String[] labels = {"Takvim Aralığı: 3 gün", "Takvim Aralığı: 1 hafta", "Takvim Aralığı: 1 ay"};
        TextView chip = simpleChip(labels[store.getCalendarWindow()]);
        chip.setOnClickListener(v -> {
            store.setCalendarWindow((store.getCalendarWindow() + 1) % 3);
            renderCalendarWindow();
        });
        calendarWindowRow.addView(chip);
    }

    private void renderWeather() {
        weatherRow.removeAllViews();
        String city = store.getWeatherName();
        TextView chip = simpleChip(city == null ? "Şehir seç (hava durumu kapalı)" : "Şehir: " + city);
        chip.setOnClickListener(v -> askCity());
        weatherRow.addView(chip);
        if (city != null) {
            TextView off = simpleChip("Kapat");
            off.setTextColor(Color.parseColor("#999999"));
            off.setOnClickListener(v -> {
                store.clearWeatherLocation();
                renderWeather();
            });
            weatherRow.addView(off);
        }
    }

    private void askCity() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Şehir adı, ör. Ankara");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        new android.app.AlertDialog.Builder(this)
                .setTitle("Hava durumu için şehir")
                .setView(input)
                .setPositiveButton("Ara", (d, w) -> {
                    String q = input.getText().toString().trim();
                    if (!q.isEmpty()) searchCity(q);
                })
                .setNegativeButton("Vazgeç", null)
                .show();
    }

    private void searchCity(String query) {
        Toast.makeText(this, "Aranıyor…", Toast.LENGTH_SHORT).show();
        WeatherWidget.search(query, new WeatherWidget.SearchCallback() {
            @Override
            public void onResult(List<WeatherWidget.Place> places) {
                if (isFinishing()) return;
                if (places.isEmpty()) {
                    Toast.makeText(SettingsActivity.this, "\"" + query + "\" bulunamadı", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] labels = new String[places.size()];
                for (int i = 0; i < places.size(); i++) {
                    labels[i] = places.get(i).name + " — " + places.get(i).detail;
                }
                new android.app.AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Hangisi?")
                        .setItems(labels, (d, which) -> {
                            WeatherWidget.Place p = places.get(which);
                            store.setWeatherLocation(p.name, p.lat, p.lon);
                            renderWeather();
                        })
                        .show();
            }

            @Override
            public void onError() {
                if (isFinishing()) return;
                Toast.makeText(SettingsActivity.this, "Arama başarısız, internet bağlantısını kontrol et", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
