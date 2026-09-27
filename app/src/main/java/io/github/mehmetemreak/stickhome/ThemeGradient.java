package io.github.mehmetemreak.stickhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.DisplayMetrics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Builds the Home background at runtime from a chosen preset, so it can be switched from
 *  "Arayüz Ayarları" without needing separate XML per theme.
 *
 *  Three kinds of preset:
 *  - DARK:  3-stop radial base anchored bottom-center + two soft corner glows.
 *  - LIGHT: same, plus a soft dark shade over the top 50% so the (always white) clock /
 *           weather / agenda text stays readable on the lighter colors.
 *  - PHOTO: a user photo, center-cropped, with top + bottom shades for the same reason.
 *           Looked up in this order:
 *             1. <external files dir>/background.jpg (or .png) — push it with:
 *                adb push foto.jpg /sdcard/Android/data/io.github.mehmetemreak.stickhome/files/background.jpg
 *             2. the preset's bundled drawable (res/drawable-nodpi/bg_photo_*.jpg)
 *             3. the preset's own gradient colors, if neither exists yet. */
public class ThemeGradient {

    public static final int KIND_DARK = 0;
    public static final int KIND_LIGHT = 1;
    public static final int KIND_PHOTO = 2;

    public static class Preset {
        public final String name;
        public final int kind;
        public final int base, baseCenter, baseEnd;
        public final int topLeft, topRight;
        public final String photoResName;

        Preset(String name, int kind, String base, String baseCenter, String baseEnd,
               String topLeft, String topRight) {
            this(name, kind, base, baseCenter, baseEnd, topLeft, topRight, null);
        }

        Preset(String name, int kind, String base, String baseCenter, String baseEnd,
               String topLeft, String topRight, String photoResName) {
            this.name = name;
            this.kind = kind;
            this.base = Color.parseColor(base);
            this.baseCenter = Color.parseColor(baseCenter);
            this.baseEnd = Color.parseColor(baseEnd);
            this.topLeft = parseOrTransparent(topLeft);
            this.topRight = parseOrTransparent(topRight);
            this.photoResName = photoResName;
        }
    }

    public static final Preset[] PRESETS = {
            // --- Fotoğraf (varsayılan) — bundled, dosya yoksa bu renklerle koyu gradyana düşer ---
            new Preset("Gün Batımı (Sarı)", KIND_PHOTO, "#0A0705", "#150E09", "#21160D", "TRANSPARENT", "TRANSPARENT", "bg_photo_yellow"),
            new Preset("Gün Batımı (Mavi)", KIND_PHOTO, "#05070D", "#0A1020", "#121B30", "TRANSPARENT", "TRANSPARENT", "bg_photo_blue"),
            // --- Koyu, altı siyah: raf bölgesi en karanlık yer ---
            new Preset("Gece Mavisi", KIND_DARK, "#05070D", "#0A1020", "#121B30", "#4D4F46E5", "#4D0EA5E9"),
            new Preset("Kehribar",    KIND_DARK, "#0A0705", "#150E09", "#21160D", "#4DD97706", "#40C2410C"),
            new Preset("Grafit",      KIND_DARK, "#080809", "#0F0F11", "#1A1A1D", "#38A1A1AA", "#2ED4D4D8"),
            // --- Koyu mesh: altı renkli, iki kenar arası karanlık ---
            new Preset("Aurora",      KIND_DARK, "#0B3B3A", "#0A2226", "#0A1119", "#4D7C3AED", "#4410B981"),
            new Preset("Gün Batımı",  KIND_DARK, "#4A1D3A", "#2A1128", "#140A18", "#4DF59E0B", "#4DDB2777"),
            // --- Açık (orta ton pastel) — beyaz yazı için üstte gölge perdesi ---
            new Preset("Lavanta Sis", KIND_LIGHT, "#4B4868", "#6A6690", "#8A84AE", "#5EF0ABFC", "#5093C5FD"),
            new Preset("Şeftali",     KIND_LIGHT, "#5A4040", "#7D5A55", "#9C766A", "#5EFDBA74", "#4DF9A8D4"),
            // --- Düz siyah, parıltı yok. Sona eklendi: kayıtlı tema indeksleri kaymasın ---
            new Preset("Karanlık",    KIND_DARK, "#000000", "#000000", "#000000", "TRANSPARENT", "TRANSPARENT"),
    };

    /** Top shade used by LIGHT and PHOTO: ~40% black at the very top, gone by mid-screen. */
    private static final int TOP_SHADE = 0x66000000;
    /** Bottom shade used by PHOTO so the app shelf labels stay readable over any image. */
    private static final int BOTTOM_SHADE = 0xB3000000;
    /** Flat darken over the whole photo (softens upscale/compression texture, boosts contrast for UI). */
    private static final int PHOTO_DARKEN = 0x3A000000;
    /** Vignette edge color - transparent at center, this at the far edge. */
    private static final int VIGNETTE_EDGE = 0x80000000;

    // Decoded photo is kept across activities (Home / All Apps / Settings all call build()).
    private static Bitmap cachedPhoto;
    private static String cachedPhotoKey;

    public static LayerDrawable build(Context context, int themeId) {
        if (themeId < 0 || themeId >= PRESETS.length) themeId = 0;
        Preset p = PRESETS[themeId];

        List<Drawable> layers = new ArrayList<>();

        GradientDrawable fallback = new GradientDrawable();
        fallback.setColor(p.base);
        layers.add(fallback);

        Bitmap photo = p.kind == KIND_PHOTO ? loadPhoto(context, p.photoResName) : null;
        if (photo != null) {
            BitmapDrawable bd = new BitmapDrawable(context.getResources(), photo);
            bd.setFilterBitmap(true);
            layers.add(bd);
            layers.add(vignette(context));
            GradientDrawable darken = new GradientDrawable();
            darken.setColor(PHOTO_DARKEN);
            layers.add(darken);
            layers.add(linear(GradientDrawable.Orientation.TOP_BOTTOM, TOP_SHADE));
            layers.add(linear(GradientDrawable.Orientation.BOTTOM_TOP, BOTTOM_SHADE));
            return new LayerDrawable(layers.toArray(new Drawable[0]));
        }

        GradientDrawable base = new GradientDrawable();
        base.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        base.setGradientRadius(dp(context, 650));
        base.setGradientCenter(0.5f, 1.0f);
        base.setColors(new int[]{p.base, p.baseCenter, p.baseEnd});
        dither(base);
        layers.add(base);

        if (p.topRight != Color.TRANSPARENT) layers.add(glow(context, p.topRight, 0.85f));
        if (p.topLeft != Color.TRANSPARENT) layers.add(glow(context, p.topLeft, 0.15f));

        if (p.kind == KIND_LIGHT) {
            layers.add(linear(GradientDrawable.Orientation.TOP_BOTTOM, TOP_SHADE));
        }

        return new LayerDrawable(layers.toArray(new Drawable[0]));
    }

    /** Transparent-center radial gradient darkening toward the edges/corners of the screen. */
    private static GradientDrawable vignette(Context context) {
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientRadius(dp(context, 500));
        g.setGradientCenter(0.5f, 0.45f);
        g.setColors(new int[]{Color.TRANSPARENT, Color.TRANSPARENT, VIGNETTE_EDGE});
        dither(g);
        return g;
    }

    private static GradientDrawable glow(Context context, int color, float centerX) {
        GradientDrawable g = new GradientDrawable();
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientRadius(dp(context, 480));
        g.setGradientCenter(centerX, 0.10f);
        g.setColors(new int[]{color, color & 0x00FFFFFF});
        dither(g);
        return g;
    }

    /** Shade from `color` at the start edge fading to transparent by the middle of the screen. */
    private static GradientDrawable linear(GradientDrawable.Orientation o, int color) {
        GradientDrawable g = new GradientDrawable(o, new int[]{color, color & 0x00FFFFFF, color & 0x00FFFFFF});
        dither(g);
        return g;
    }

    /** Dark, low-contrast gradients band visibly on a big TV panel without dithering. */
    @SuppressWarnings("deprecation")
    private static void dither(GradientDrawable g) {
        g.setDither(true);
    }

    // ---------------------------------------------------------------- photo

    private static Bitmap loadPhoto(Context context, String photoResName) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int screenW = Math.max(dm.widthPixels, dm.heightPixels);
        int screenH = Math.min(dm.widthPixels, dm.heightPixels);

        File file = findPhotoFile(context);
        int resId = context.getResources().getIdentifier(photoResName, "drawable", context.getPackageName());

        String key;
        if (file != null) key = file.getAbsolutePath() + "@" + file.lastModified();
        else if (resId != 0) key = "res:" + resId;
        else return null;
        key += "#" + screenW + "x" + screenH;

        if (key.equals(cachedPhotoKey) && cachedPhoto != null && !cachedPhoto.isRecycled()) {
            return cachedPhoto;
        }

        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            decode(context, file, resId, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, screenW, screenH);
            opts.inPreferredConfig = Bitmap.Config.RGB_565; // half the RAM of ARGB_8888; photos need no alpha
            opts.inScaled = false;
            Bitmap raw = decode(context, file, resId, opts);
            if (raw == null) return null;

            Bitmap cropped = centerCrop(raw, screenW, screenH);
            if (cropped != raw) raw.recycle();

            cachedPhoto = cropped;
            cachedPhotoKey = key;
            return cropped;
        } catch (OutOfMemoryError | RuntimeException e) {
            return null; // fall back to the gradient rather than crash the launcher
        }
    }

    private static File findPhotoFile(Context context) {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) return null;
        for (String name : new String[]{"background.jpg", "background.jpeg", "background.png", "background.webp"}) {
            File f = new File(dir, name);
            if (f.isFile() && f.length() > 0) return f;
        }
        return null;
    }

    private static Bitmap decode(Context context, File file, int resId, BitmapFactory.Options opts) {
        if (file != null) return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        return BitmapFactory.decodeResource(context.getResources(), resId, opts);
    }

    /** Largest power of two that still leaves the image at least screen-sized. */
    private static int sampleSize(int w, int h, int reqW, int reqH) {
        int s = 1;
        while (w / (s * 2) >= reqW && h / (s * 2) >= reqH) s *= 2;
        return s;
    }

    /** Crop to the screen's aspect ratio, then scale down to screen size (never up). */
    private static Bitmap centerCrop(Bitmap src, int screenW, int screenH) {
        float target = (float) screenW / screenH;
        int w = src.getWidth(), h = src.getHeight();
        int cw = w, ch = h;
        if ((float) w / h > target) cw = Math.round(h * target);
        else ch = Math.round(w / target);
        Bitmap out = src;
        if (cw != w || ch != h) out = Bitmap.createBitmap(src, (w - cw) / 2, (h - ch) / 2, cw, ch);
        if (out.getWidth() > screenW) {
            Bitmap scaled = Bitmap.createScaledBitmap(out, screenW, screenH, true);
            if (out != src) out.recycle();
            out = scaled;
        }
        return out;
    }

    // ---------------------------------------------------------------- util

    private static int parseOrTransparent(String s) {
        return "TRANSPARENT".equals(s) ? Color.TRANSPARENT : Color.parseColor(s);
    }

    private static int dp(Context context, int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
