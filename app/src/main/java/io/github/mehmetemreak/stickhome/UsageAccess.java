package io.github.mehmetemreak.stickhome;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Process;

final class UsageAccess {

    private UsageAccess() {}

    static boolean isGranted(Context context) {
        AppOpsManager ops = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (ops == null) return false;
        int mode = ops.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
