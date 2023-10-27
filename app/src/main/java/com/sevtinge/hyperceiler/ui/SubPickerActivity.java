package com.sevtinge.hyperceiler.ui;

import android.content.Intent;
import android.os.Bundle;

import com.sevtinge.hyperceiler.callback.IAppSelectCallback;
import com.sevtinge.hyperceiler.ui.base.SettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.sub.AppPicker;

public class SubPickerActivity extends SettingsActivity {
    AppPicker mAppSelectFragment = new AppPicker();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppSelectFragment.setAppSelectCallback(new IAppSelectCallback() {
            @Override
            public void sendMsgToActivity(byte[] appIcon, String appName, String appPackageName, String appVersion, String appActivityName) {
                Intent intent = new Intent();
                intent.putExtra("appIcon", appIcon);
                intent.putExtra("appName", appName);
                intent.putExtra("appPackageName", appPackageName);
                intent.putExtra("appVersion", appVersion);
                intent.putExtra("appActivityName", appActivityName);
                setResult(1, intent);
            }
            @Override
            public String getMsgFromActivity(String s) {
                return null;
            }
        });
        setFragment(mAppSelectFragment);
    }
}
