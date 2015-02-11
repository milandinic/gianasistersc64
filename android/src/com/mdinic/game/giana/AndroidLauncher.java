package com.mdinic.game.giana;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.mdinic.game.giana.service.InternetConnectionChecker;
import com.parse.Parse;

public class AndroidLauncher extends AndroidApplication implements InternetConnectionChecker {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Parse.initialize(this, "AKmNv40ZmUudVezreRT6NpGOBmH6D4gSOCtdnHOe", "kBM5rL0mW3zHD6ifZl9mTU5aqHPqd3Ye33k6P3CP");

        GianaSistersC64 gianaSistersC64 = new GianaSistersC64();
        gianaSistersC64.setSettingsService(new SettingsServiceDroid(this));
        gianaSistersC64.setHighScoreService(new HighScoreServiceDroid(this, this));
        gianaSistersC64.setGeneralService(new GeneralServiceDrod(this));

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useWakelock = true;
        initialize(gianaSistersC64, config);
    }

    @Override
    public boolean isAvailableConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
