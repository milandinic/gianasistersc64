package com.mdinic.game.giana;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.mdinic.game.giana.service.SettingsService;

public class SettingsServiceDroid implements SettingsService {

    private static final String SOUND_ENABLED = "SOUND_ENABLED";
    private final Activity activity;

    private boolean soundEnabled;

    public SettingsServiceDroid(Activity activity) {
        super();
        this.activity = activity;

        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);

        soundEnabled = sharedPref.getBoolean(SOUND_ENABLED, true);
    }

    @Override
    public void enableSound(boolean enable) {
        soundEnabled = enable;
        SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(SOUND_ENABLED, enable);
        editor.commit();
    }

    @Override
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

}
