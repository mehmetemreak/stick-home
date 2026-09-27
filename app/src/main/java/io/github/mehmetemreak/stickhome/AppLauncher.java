package io.github.mehmetemreak.stickhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/** Resolves the TV (leanback) entry point first: getLaunchIntentForPackage() alone can pick an
 *  app's phone UI when it ships both. */
final class AppLauncher {

    private AppLauncher() {}

    /** Returns null if the package has no launchable activity. Caller adds launch flags. */
    static Intent intentFor(Context context, String packageName, String activityName) {
        PackageManager pm = context.getPackageManager();
        if (activityName != null) {
            Intent explicit = new Intent(Intent.ACTION_MAIN)
                    .addCategory("android.intent.category.LEANBACK_LAUNCHER")
                    .setClassName(packageName, activityName);
            if (explicit.resolveActivityInfo(pm, 0) != null) return explicit;
        }
        Intent launch = pm.getLeanbackLaunchIntentForPackage(packageName);
        if (launch == null) launch = pm.getLaunchIntentForPackage(packageName);
        return launch;
    }
}
