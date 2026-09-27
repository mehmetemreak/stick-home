package io.github.mehmetemreak.stickhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AllAppsActivity extends Activity {

    private DockStore dockStore;

    private static final long LONG_PRESS_MS = 550;
    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingLongPress;
    private boolean longPressFired = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dockStore = new DockStore(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackground(ThemeGradient.build(this, new SettingsStore(this).getThemeId()));
        int margin = dp(48);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        LinearLayout titleBlock = new LinearLayout(this);
        titleBlock.setOrientation(LinearLayout.VERTICAL);
        titleBlock.setPadding(margin, margin, margin, dp(8));

        TextView title = new TextView(this);
        title.setText("Tüm Uygulamalar");
        title.setTextColor(Color.parseColor("#F2FBFF"));
        title.setTextSize(22);
        titleBlock.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Seç aç · basılı tut ana ekrana ekle/çıkar");
        subtitle.setTextColor(Color.parseColor("#8FEBFF"));
        subtitle.setTextSize(13);
        subtitle.setPadding(0, dp(4), 0, 0);
        titleBlock.addView(subtitle);

        content.addView(titleBlock);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        grid.setPadding(margin, dp(8), margin, margin);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        scroll.addView(grid);
        content.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        root.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView status = new TextView(this);
        status.setText(SystemStatusWidget.read(this));
        status.setTextColor(Color.parseColor("#55FFFFFF"));
        status.setTextSize(9);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        statusParams.gravity = Gravity.BOTTOM | Gravity.END;
        statusParams.setMargins(0, 0, dp(24), dp(16));
        status.setLayoutParams(statusParams);
        root.addView(status);

        setContentView(root);
        populate(grid);
    }

    private void populate(GridLayout grid) {
        PackageManager pm = getPackageManager();
        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN);
        leanbackIntent.addCategory("android.intent.category.LEANBACK_LAUNCHER");
        List<ResolveInfo> apps = pm.queryIntentActivities(leanbackIntent, 0);

        List<DockStore.Entry> filtered = new ArrayList<>();
        for (ResolveInfo info : apps) {
            if (getPackageName().equals(info.activityInfo.packageName)) continue;
            filtered.add(new DockStore.Entry(
                    info.activityInfo.packageName, info.activityInfo.name, info.loadLabel(pm).toString()));
        }
        filtered.sort((a, b) -> a.label.compareToIgnoreCase(b.label));

        Set<String> onDock = new HashSet<>();
        for (DockStore.Entry e : dockStore.load()) onDock.add(e.key());

        View firstFocus = null;
        for (DockStore.Entry entry : filtered) {
            boolean already = onDock.contains(entry.key());

            LinearLayout tile = new LinearLayout(this);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER_HORIZONTAL);
            int padH = dp(8);
            int padV = dp(6);
            tile.setPadding(padH, padV, padH, padV);
            tile.setFocusable(true);
            tile.setFocusableInTouchMode(true);
            tile.setBackgroundResource(R.drawable.tile_focus_background);
            tile.setClipChildren(false);
            tile.setClipToPadding(false);

            String plainLabel = entry.label;

            FrameLayout iconWrap = new FrameLayout(this);
            iconWrap.setClipChildren(false);
            iconWrap.setClipToPadding(false);
            iconWrap.addView(IconRenderer.build(this, entry.packageName, entry.activityName, 56));

            ImageView badge = new ImageView(this);
            int badgeSize = dp(20);
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(badgeSize, badgeSize);
            badgeParams.gravity = Gravity.TOP | Gravity.END;
            badgeParams.topMargin = -dp(5);
            badgeParams.rightMargin = -dp(5);
            badge.setLayoutParams(badgeParams);
            badge.setImageResource(R.drawable.check_badge);
            badge.setVisibility(already ? View.VISIBLE : View.GONE);
            iconWrap.addView(badge);

            iconWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(56), dp(56)));
            tile.addView(iconWrap);

            TextView label = new TextView(this);
            label.setText(plainLabel);
            label.setTextColor(Color.parseColor("#DDDDDD"));
            label.setGravity(Gravity.CENTER);
            label.setTextSize(11);
            label.setMaxLines(1);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            label.setPadding(0, dp(6), 0, 0);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            tile.addView(label);

            tile.setOnClickListener(v -> launch(entry));

            tile.setOnKeyListener((v, keyCode, event) ->
                    handleTileKey(entry, badge, keyCode, event));

            tile.setOnFocusChangeListener((v, hasFocus) -> {
                float scale = hasFocus ? 1.14f : 1.0f;
                v.animate().scaleX(scale).scaleY(scale).setDuration(160).start();
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    if (hasFocus) {
                        v.setElevation(dp(10));
                        v.setOutlineAmbientShadowColor(0xFF8FEBFF);
                        v.setOutlineSpotShadowColor(0xFF8FEBFF);
                    } else {
                        v.setElevation(0);
                    }
                }
            });

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = dp(90);
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            grid.addView(tile, lp);

            if (firstFocus == null) firstFocus = tile;
        }

        if (firstFocus != null) firstFocus.requestFocus();
    }

    private boolean handleTileKey(DockStore.Entry entry, ImageView badge, int keyCode, KeyEvent event) {
        if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER && keyCode != KeyEvent.KEYCODE_ENTER) {
            return false;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getRepeatCount() == 0) {
                longPressFired = false;
                pendingLongPress = () -> {
                    longPressFired = true;
                    toggleDock(entry, badge);
                };
                longPressHandler.postDelayed(pendingLongPress, LONG_PRESS_MS);
            }
            if (event.isLongPress()) {
                if (pendingLongPress != null) longPressHandler.removeCallbacks(pendingLongPress);
                if (!longPressFired) {
                    longPressFired = true;
                    toggleDock(entry, badge);
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
        return false;
    }

    private void toggleDock(DockStore.Entry entry, ImageView badge) {
        boolean nowOn = dockStore.isOnDock(entry.key());
        if (nowOn) {
            dockStore.removeFromDock(entry.key());
            badge.setVisibility(View.GONE);
            Toast.makeText(this, "Ana ekrandan kaldırıldı", Toast.LENGTH_SHORT).show();
        } else {
            dockStore.addToDock(entry);
            badge.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Ana ekrana eklendi", Toast.LENGTH_SHORT).show();
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

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
