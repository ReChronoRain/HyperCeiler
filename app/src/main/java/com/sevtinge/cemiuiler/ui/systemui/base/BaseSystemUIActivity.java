package com.sevtinge.cemiuiler.ui.systemui.base;

import android.os.Bundle;

import com.sevtinge.cemiuiler.ui.base.BaseAppCompatActivity;

public abstract class BaseSystemUIActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarEndViewEnable(true);
        setOnRestartListener(new OnRestartListener() {
            @Override
            public void onRestart() {
                showRestartSystemUIDialog();
            }
        });
    }
}
