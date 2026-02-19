package com.sevtinge.hyperceiler.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.provision.activity.DefaultActivity;

import fan.appcompat.app.AppCompatActivity;
import fan.provision.OobeUtils;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent;
        // 根据逻辑分发
        if (OobeUtils.isProvisioned(this) || BuildConfig.DEBUG) {
            // 跳转到主页
            intent = new Intent(this, HomePageActivity.class);
        } else {
            // 跳转到引导页
            intent = new Intent(this, DefaultActivity.class);
        }
        startActivity(intent);
        // 3. 必须 finish，否则用户按返回键会回到空白的 Splash 页面
        finish();
    }
}
