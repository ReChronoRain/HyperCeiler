package com.sevtinge.cemiuiler.ui.home.base;

import android.os.Bundle;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;

public abstract class BaseHomeActivity extends BaseAppCompatActivity {

    public String mAppLabel;
    public final String mPackageName = "com.miui.home";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mAppLabel = this.getString(R.string.home);
        setActionBarEndViewEnable(true);
        setOnRestartListener(new OnRestartListener() {
            @Override
            public void onRestart() {
                showRestartAppsDialog(mAppLabel, mPackageName);
            }
        });
    }
}
