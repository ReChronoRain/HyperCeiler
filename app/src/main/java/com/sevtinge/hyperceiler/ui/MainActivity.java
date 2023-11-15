package com.sevtinge.hyperceiler.ui;

import android.content.Intent;
import android.os.Bundle;


import com.sevtinge.hyperceiler.ui.base.NavigationActivity;
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.SearchHelper;

public class MainActivity extends NavigationActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Thread(new Runnable() {
            public void run() {
                SearchHelper.getAllMods(MainActivity.this, savedInstanceState != null);
            }
        }).start();
        Helpers.checkXposedActivateState(this);
    }

    private void requestCta() {
        /*if (!CtaUtils.isCtaEnabled(this)) {
            CtaUtils.showCtaDialog(this, REQUEST_CODE);
        }*/
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        requestCta();
    }

}
