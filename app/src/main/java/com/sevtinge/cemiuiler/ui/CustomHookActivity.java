package com.sevtinge.cemiuiler.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.customhook.CustomHookConfigActivity;

import moralnorm.appcompat.app.AppCompatActivity;

public class CustomHookActivity extends AppCompatActivity {

    Button mAddConfig;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_custom_hook);
        initView();
    }

    private void initView() {
        mAddConfig = findViewById(R.id.add_config);

        mAddConfig.setOnClickListener(v -> startActivity(new Intent(this, CustomHookConfigActivity.class)));
    }
}
