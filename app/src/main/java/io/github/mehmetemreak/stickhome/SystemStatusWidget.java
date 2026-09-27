package io.github.mehmetemreak.stickhome;

import android.app.ActivityManager;
import android.content.Context;
import android.os.PowerManager;

/** Fully local, zero-network system status readout - RAM + thermal level. */
public class SystemStatusWidget {

    public static String read(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
        if (am != null) {
            am.getMemoryInfo(mem);
        }
        long availMb = mem.availMem / (1024 * 1024);
        long totalMb = mem.totalMem / (1024 * 1024);

        String thermal = "";
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            int status = pm.getCurrentThermalStatus();
            thermal = "  ·  " + thermalLabel(status);
        }

        return "Boş RAM " + availMb + " / " + totalMb + " MB" + thermal;
    }

    private static String thermalLabel(int status) {
        switch (status) {
            case PowerManager.THERMAL_STATUS_NONE:
                return "sıcaklık normal";
            case PowerManager.THERMAL_STATUS_LIGHT:
                return "hafif ısınma";
            case PowerManager.THERMAL_STATUS_MODERATE:
                return "orta ısınma";
            case PowerManager.THERMAL_STATUS_SEVERE:
                return "yüksek ısınma";
            case PowerManager.THERMAL_STATUS_CRITICAL:
            case PowerManager.THERMAL_STATUS_EMERGENCY:
            case PowerManager.THERMAL_STATUS_SHUTDOWN:
                return "kritik ısınma";
            default:
                return "";
        }
    }
}
