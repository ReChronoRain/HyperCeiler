package com.sevtinge.hyperceiler.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.provision.activity.DefaultActivity;

import fan.appcompat.app.AppCompatActivity;
import fan.provision.OobeUtils;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 获取配置信息
        SharedPreferences prefs = getSharedPreferences("pref_state", MODE_PRIVATE);
        boolean isProvisioned = prefs.getBoolean("is_provisioned", true);
        Intent intent;
        // 2. 根据逻辑分发
        if (OobeUtils.isProvisioned(this)) {
            // 跳转到主页
            intent = new Intent(this, HyperCeilerTabActivity.class);
            startActivity(intent);
        } else {
            // 跳转到引导页
            intent = new Intent(this, DefaultActivity.class);
            startActivity(intent);
        }
        startActivity(intent);
        // 3. 必须 finish，否则用户按返回键会回到空白的 Splash 页面
        finish();
    }
}
