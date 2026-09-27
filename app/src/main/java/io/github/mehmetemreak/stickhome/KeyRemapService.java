package io.github.mehmetemreak.stickhome;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Uses the AccessibilityService key-filtering API (FLAG_REQUEST_FILTER_KEY_EVENTS) to intercept
 * hardware remote buttons (dedicated Netflix/Prime Video/All Apps keys) that never reach normal
 * app window dispatch on this device - a system-level "GlobalKey" handler consumes them first in
 * the input pipeline, but accessibility key filtering sits earlier and can intercept before that
 * happens. No root required.
 */
public class KeyRemapService extends AccessibilityService {

    private static final String TAG = "KeyRemapService";

    private static final int KEYCODE_ALL_APPS = 284;
    private static final int KEYCODE_NETFLIX_BUTTON = 193; // KEYCODE_BUTTON_6 on this remote
    private static final int KEYCODE_PRIME_BUTTON = 194;   // KEYCODE_BUTTON_7 on this remote

    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = 0; // key filtering only - no UI events needed
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);
        Log.d(TAG, "Service connected, key filtering active");
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        int code = event.getKeyCode();
        String target = null;
        if (code == KEYCODE_NETFLIX_BUTTON || code == KEYCODE_PRIME_BUTTON) {
            SettingsStore settings = new SettingsStore(this);
            target = code == KEYCODE_NETFLIX_BUTTON ? settings.getNetflixTarget() : settings.getPrimeTarget();
            if (target == null) {
                return false; // not remapped: the button keeps its stock behavior
            }
        } else if (code != KEYCODE_ALL_APPS) {
            return false;
        }

        if (event.getRepeatCount() > 0) {
            return true; // held button: swallow repeats instead of relaunching each time
        }

        if (code == KEYCODE_ALL_APPS) {
            launchAllApps();
            return true;
        }

        String finalTarget = target;
        launchPackage(finalTarget);
        // The stock GlobalKey launch for these buttons races us and wins focus ~300-500ms later
        // (confirmed via logcat timing) - relaunch after it settles so our target ends up on top.
        new Handler(Looper.getMainLooper()).postDelayed(() -> launchPackage(finalTarget), 550);
        return true;
    }

    private void launchPackage(String packageName) {
        Intent launch = AppLauncher.intentFor(this, packageName, null);
        if (launch == null) {
            Log.w(TAG, "No launch intent for " + packageName);
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(launch);
    }

    private void launchAllApps() {
        Intent launch = new Intent();
        launch.setComponent(new ComponentName(this, AllAppsActivity.class));
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launch);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used - we only care about onKeyEvent.
    }

    @Override
    public void onInterrupt() {
    }
}
