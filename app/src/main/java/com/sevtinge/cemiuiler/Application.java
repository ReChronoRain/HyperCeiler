package com.sevtinge.cemiuiler;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.sevtinge.cemiuiler.utils.PrefsUtils;

public class Application extends android.app.Application {

    @Override
    protected void attachBaseContext(Context base) {
        PrefsUtils.mSharedPreferences = PrefsUtils.getSharedPrefs(base, Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
