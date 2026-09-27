package io.github.mehmetemreak.stickhome;

import android.content.Context;
import android.content.SharedPreferences;

public class NameStore {

    private static final String PREFS = "stick_home_prefs";
    private static final String KEY_NAME = "home/user_name";

    private final SharedPreferences prefs;

    public NameStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public boolean hasName() {
        return prefs.contains(KEY_NAME);
    }

    public String getName() {
        return prefs.getString(KEY_NAME, null);
    }

    public void setName(String name) {
        prefs.edit().putString(KEY_NAME, name).apply();
    }
}
