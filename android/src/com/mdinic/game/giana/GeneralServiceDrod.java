package com.mdinic.game.giana;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;

import com.badlogic.gdx.Gdx;
import com.mdinic.game.giana.service.GeneralService;

public class GeneralServiceDrod implements GeneralService {

    Builder alert;
    AndroidLauncher androidLauncher;

    public GeneralServiceDrod(AndroidLauncher androidLauncher) {
        this.androidLauncher = androidLauncher;

        alert = new AlertDialog.Builder(androidLauncher).setTitle("Confirm")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Gdx.app.exit();
                        dialog.cancel();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alert.create();

    }

    @Override
    public void showConfirmExitDialog() {
        androidLauncher.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                alert.show();

            }
        });
    }
}
