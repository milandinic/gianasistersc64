package com.mdinic.game.giana;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.parse.Parse;

public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Parse.initialize(this, "AKmNv40ZmUudVezreRT6NpGOBmH6D4gSOCtdnHOe", "kBM5rL0mW3zHD6ifZl9mTU5aqHPqd3Ye33k6P3CP");

        GianaSistersC64 gianaSistersC64 = new GianaSistersC64();
        gianaSistersC64.setHighScoreService(new HighScoreServiceDroid());
        gianaSistersC64.getHighScoreService().setUsername(getUsername());

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useWakelock = true;
        initialize(gianaSistersC64, config);
    }

    public String getUsername() {

        final AccountManager manager = AccountManager.get(this);

        String account = getAccount(manager);
        if (account != null) {
            String[] split = account.split("@");
            return split[0];
        }
        return null;
    }

    private static String getAccount(AccountManager accountManager) {
        Account[] accounts = accountManager.getAccountsByType("com.google");
        // first look for gmail
        for (Account account : accounts) {
            if (account.name.endsWith("gmail.com")) {
                return account.name;
            }
        }
        // take any
        if (accounts.length > 0) {
            return accounts[0].name;
        }
        // no accounts found
        return null;

    }
}
