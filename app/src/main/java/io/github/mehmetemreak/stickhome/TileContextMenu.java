package io.github.mehmetemreak.stickhome;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Dark-glass long-press context menu. BACK dismisses. D-pad focus trapped inside by default Dialog behavior. */
public class TileContextMenu {

    public interface Listener {
        void onMove();
        void onRemoveFromHome();
        void onAppInfo();
        void onUninstall();
    }

    public static void show(Context context, boolean uninstallAllowed, Listener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 8);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#E6161A22"));
        bg.setCornerRadius(dp(context, 14));
        card.setBackground(bg);

        addOption(context, card, "Taşı", v -> {
            dialog.dismiss();
            listener.onMove();
        });
        addOption(context, card, "Ana ekrandan kaldır", v -> {
            dialog.dismiss();
            listener.onRemoveFromHome();
        });
        addOption(context, card, "Uygulama bilgisi", v -> {
            dialog.dismiss();
            listener.onAppInfo();
        });
        if (uninstallAllowed) {
            addOption(context, card, "Uygulamayı kaldır", v -> {
                dialog.dismiss();
                listener.onUninstall();
            });
        }

        dialog.setContentView(card);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(Gravity.CENTER);
        }
        dialog.show();

        card.post(() -> {
            if (card.getChildCount() > 0) card.getChildAt(0).requestFocus();
        });
    }

    private static void addOption(Context context, LinearLayout parent, String label, View.OnClickListener onClick) {
        TextView option = new TextView(context);
        option.setText(label);
        option.setTextColor(Color.parseColor("#F2F2F2"));
        option.setTextSize(16);
        int padV = dp(context, 14);
        int padH = dp(context, 24);
        option.setPadding(padH, padV, padH, padV);
        option.setFocusable(true);
        option.setFocusableInTouchMode(true);
        option.setOnClickListener(onClick);
        option.setBackgroundResource(R.drawable.tile_focus_background);
        parent.addView(option);
    }

    private static int dp(Context context, int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
