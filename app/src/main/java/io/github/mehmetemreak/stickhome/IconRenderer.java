package io.github.mehmetemreak.stickhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.LruCache;
import android.view.Gravity;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** Normalizes any app icon (adaptive or legacy) into a consistent rounded-square glass card.
 *  Many launcher icons ship with a lot of built-in transparent padding (adaptive safe zones,
 *  or just a designer's choice) which makes them look tiny and off-center inside a uniform
 *  card - so we render the source icon to a bitmap, auto-trim its transparent margins, and
 *  scale the trimmed content to consistently fill the card, the way real launchers do. */
public class IconRenderer {

    private static final int RENDER_PX = 144;

    // Keyed by component + lastUpdateTime so an app update refreshes its icon.
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    public static FrameLayout build(Context context, String packageName, String activityName, int cardSizeDp) {
        PackageManager pm = context.getPackageManager();
        String key;
        try {
            key = packageName + "/" + activityName + "@" + pm.getPackageInfo(packageName, 0).lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            return missingCard(context, cardSizeDp);
        }

        Bitmap icon = CACHE.get(key);
        if (icon == null) {
            Drawable raw;
            try {
                raw = pm.getActivityIcon(new ComponentName(packageName, activityName));
            } catch (PackageManager.NameNotFoundException e) {
                return missingCard(context, cardSizeDp);
            }
            icon = trimTransparent(foregroundOf(raw));
            if (icon == null) {
                return card(context, cardSizeDp, null, foregroundOf(raw));
            }
            CACHE.put(key, icon);
        }
        return card(context, cardSizeDp, icon, null);
    }

    private static FrameLayout missingCard(Context context, int cardSizeDp) {
        FrameLayout card = card(context, cardSizeDp, null, null);
        card.setAlpha(0.3f);
        return card;
    }

    private static FrameLayout card(Context context, int cardSizeDp, Bitmap bitmap, Drawable drawable) {
        int cardSize = dp(context, cardSizeDp);
        int radius = dp(context, 14);

        FrameLayout card = new FrameLayout(context);
        card.setLayoutParams(new FrameLayout.LayoutParams(cardSize, cardSize));

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#2BFFFFFF"), Color.parseColor("#0AFFFFFF")});
        bg.setCornerRadius(radius);
        bg.setStroke(dp(context, 1), Color.parseColor("#33FFFFFF"));
        card.setBackground(bg);
        card.setClipToOutline(true);
        card.setOutlineProvider(ViewOutlineProvider.BACKGROUND);

        if (bitmap == null && drawable == null) {
            return card;
        }

        ImageView iconView = new ImageView(context);
        int inset = dp(context, Math.round(cardSizeDp * 0.16f));
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                cardSize - inset * 2, cardSize - inset * 2);
        iconParams.gravity = Gravity.CENTER;
        iconView.setLayoutParams(iconParams);
        if (bitmap != null) {
            iconView.setImageBitmap(bitmap);
        } else {
            iconView.setImageDrawable(drawable);
        }
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(iconView);

        return card;
    }

    private static Drawable foregroundOf(Drawable rawIcon) {
        if (Build.VERSION.SDK_INT >= 26 && rawIcon instanceof AdaptiveIconDrawable) {
            Drawable fg = ((AdaptiveIconDrawable) rawIcon).getForeground();
            if (fg != null) return fg;
        }
        return rawIcon;
    }

    /** Renders a drawable to a bitmap and crops away fully/near-transparent edges. */
    private static Bitmap trimTransparent(Drawable drawable) {
        try {
            Bitmap bitmap = Bitmap.createBitmap(RENDER_PX, RENDER_PX, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, RENDER_PX, RENDER_PX);
            drawable.draw(canvas);

            int left = RENDER_PX, right = 0, top = RENDER_PX, bottom = 0;
            int[] row = new int[RENDER_PX];
            boolean found = false;

            for (int y = 0; y < RENDER_PX; y++) {
                bitmap.getPixels(row, 0, RENDER_PX, 0, y, RENDER_PX, 1);
                for (int x = 0; x < RENDER_PX; x++) {
                    if ((row[x] >>> 24) > 12) { // alpha threshold
                        found = true;
                        if (x < left) left = x;
                        if (x > right) right = x;
                        if (y < top) top = y;
                        if (y > bottom) bottom = y;
                    }
                }
            }

            if (!found || right <= left || bottom <= top) {
                return bitmap;
            }

            int w = right - left + 1;
            int h = bottom - top + 1;
            return Bitmap.createBitmap(bitmap, left, top, w, h);
        } catch (Exception e) {
            return null;
        }
    }

    private static int dp(Context context, int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
