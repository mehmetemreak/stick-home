package io.github.mehmetemreak.stickhome;

import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CalendarContract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Reads upcoming events from the device's local Calendar Provider (populated by the
 *  standard Android account-sync system) — no network code, no OAuth, purely local reads. */
public class CalendarWidget {

    public static class UpcomingEvent {
        public final String title;
        public final long startMillis;

        UpcomingEvent(String title, long startMillis) {
            this.title = title;
            this.startMillis = startMillis;
        }

        public String friendlyTime() {
            Calendar now = Calendar.getInstance();
            Calendar start = Calendar.getInstance();
            start.setTimeInMillis(startMillis);

            boolean sameDay = now.get(Calendar.YEAR) == start.get(Calendar.YEAR)
                    && now.get(Calendar.DAY_OF_YEAR) == start.get(Calendar.DAY_OF_YEAR);

            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", new Locale("tr", "TR"));
            SimpleDateFormat dayFmt = new SimpleDateFormat("d MMM, HH:mm", new Locale("tr", "TR"));
            return sameDay ? "Bugün " + timeFmt.format(start.getTime()) : dayFmt.format(start.getTime());
        }
    }

    public static boolean hasPermission(Context context) {
        return context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static List<UpcomingEvent> loadUpcoming(Context context, int maxCount, int windowDays) {
        List<UpcomingEvent> result = new ArrayList<>();
        if (!hasPermission(context)) return result;

        long now = System.currentTimeMillis();
        long end = now + windowDays * 24L * 60 * 60 * 1000;

        android.net.Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, now);
        ContentUris.appendId(builder, end);

        String[] projection = {
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN
        };

        try (Cursor cursor = context.getContentResolver().query(
                builder.build(), projection, null, null,
                CalendarContract.Instances.BEGIN + " ASC")) {
            if (cursor != null) {
                while (cursor.moveToNext() && result.size() < maxCount) {
                    String title = cursor.getString(0);
                    long begin = cursor.getLong(1);
                    if (title == null || title.trim().isEmpty()) title = "(başlıksız)";
                    result.add(new UpcomingEvent(title, begin));
                }
            }
        } catch (SecurityException e) {
            // Permission revoked between check and query; return what we have.
        }

        return result;
    }
}
