package com.sevtinge.cemiuiler.ui.customhook;

import android.os.Bundle;

import com.sevtinge.cemiuiler.R;

import moralnorm.appcompat.app.PickerDragActivity;

public class CustomHookConfigActivity extends PickerDragActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setDragContentView(R.layout.activity_custom_hook_config);
    }
}
