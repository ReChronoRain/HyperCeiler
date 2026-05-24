package com.sevtinge.hyperceiler.ui;

import android.content.Context;

import com.sevtinge.hyperceiler.utils.LanguageHelper;

import fan.appcompat.app.AppCompatActivity;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.wrapContext(newBase));
    }
}
