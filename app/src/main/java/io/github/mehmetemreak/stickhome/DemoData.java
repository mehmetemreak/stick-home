package io.github.mehmetemreak.stickhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/** Sample calendar/match data for README screenshots. Debug builds only:
 *  adb shell am start -n io.github.mehmetemreak.stickhome/.HomeActivity --ez demo true */
final class DemoData {

    private DemoData() {}

    static boolean isRequested(Context context, Intent intent) {
        boolean debuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        return debuggable && intent != null && intent.getBooleanExtra("demo", false);
    }

    static List<CalendarWidget.UpcomingEvent> events() {
        return Arrays.asList(
                new CalendarWidget.UpcomingEvent("Proje toplantısı", at(0, 2)),
                new CalendarWidget.UpcomingEvent("Ayşe'nin doğum günü", atClock(1, 19, 30)),
                new CalendarWidget.UpcomingEvent("Diş hekimi", atClock(3, 10, 0)));
    }

    static List<FootballWidget.Match> matches() {
        return Arrays.asList(
                new FootballWidget.Match("Galatasaray - Fenerbahçe", at(0, 0) + 25 * 60_000L),
                new FootballWidget.Match("Real Madrid - Barcelona", at(0, 0) + 50 * 60_000L));
    }

    private static long at(int days, int hours) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, days);
        c.add(Calendar.HOUR_OF_DAY, hours);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.MINUTE, hours == 0 ? c.get(Calendar.MINUTE) : 0);
        return c.getTimeInMillis();
    }

    private static long atClock(int days, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, days);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
