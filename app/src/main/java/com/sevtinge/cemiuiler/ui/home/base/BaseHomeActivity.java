package com.sevtinge.cemiuiler.ui.home.base;

import android.os.Bundle;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;

public abstract class BaseHomeActivity extends BaseAppCompatActivity {

    public final String mAppLabel = getString(R.string.home);
    public final String mPackageName = "com.miui.home";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarEndViewEnable(true);
        setOnRestartListener(new OnRestartListener() {
            @Override
            public void onRestart() {
                showRestartAppsDialog(mAppLabel, mPackageName);
            }
        });
    }
}
