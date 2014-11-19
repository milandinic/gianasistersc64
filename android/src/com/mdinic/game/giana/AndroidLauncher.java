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

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useWakelock = true;
        initialize(gianaSistersC64, config);
    }

    public String getUsername() {

        final AccountManager manager = AccountManager.get(this);

        Account account = getAccount(manager);
        if (account == null) {
            return "";
        } else {
            String email = account.name;
            String[] parts = email.split("@");
            if (parts.length > 0 && parts[0] != null)
                return parts[0];
            else
                return "";
        }
    }

    private static Account getAccount(AccountManager accountManager) {
        Account[] accounts = accountManager.getAccountsByType("com.google");
        Account account;
        if (accounts.length > 0) {
            account = accounts[0];
        } else {
            account = null;
        }
        return account;
    }
}
