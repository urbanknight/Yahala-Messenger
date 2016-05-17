/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.ui;


import android.app.Activity;

import android.content.Intent;
import android.graphics.PixelFormat;

import android.os.Bundle;
import android.view.Window;
import android.widget.RelativeLayout;

import com.yahala.messenger.R;

import java.util.ArrayList;

public class UnlockActivity extends Activity implements PasswordActivity.PasswordDelegate {


    @Override
    protected void onResume() {
        super.onResume();
        //  ApplicationLoader.resetLastPauseTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ApplicationLoader.lastPauseTime = System.currentTimeMillis();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        // getActionBar().hide();

        //setContentView(R.layout.splash);

        PasswordActivity passwordActivity = new PasswordActivity(this, null, false);
        passwordActivity.setDelegate(this);
        passwordActivity.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT));
        passwordActivity.setBackgroundColor(getResources().getColor(R.color.Red2));
        setContentView(passwordActivity);
        ApplicationLoader.applicationContext = this.getApplicationContext();


        getWindow().setBackgroundDrawableResource(R.drawable.transparent);
        getWindow().setFormat(PixelFormat.RGB_565);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onPasswordComplete(ArrayList<String> photos) {

    }

    @Override
    public void needFinish() { //Utilities.unlockOrientation(getParentActivity());
        Intent intent2 = new Intent(this, LaunchActivity.class);
        startActivity(intent2);
        finish();
    }

    @Override
    public void onContinueClicked() {

    }
}
