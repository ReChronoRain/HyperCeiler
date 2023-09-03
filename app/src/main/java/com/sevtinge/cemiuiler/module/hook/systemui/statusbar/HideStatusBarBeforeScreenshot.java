package com.sevtinge.cemiuiler.module.hook.systemui.statusbar;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;

public class HideStatusBarBeforeScreenshot extends BaseHook {

    @Override
    public void init() {
        Helpers.findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", lpparam.classLoader, "initMiuiViewsOnViewCreated", View.class, new MethodHook() {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                View view = (View) param.args[0];
                BroadcastReceiver br = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if ("miui.intent.TAKE_SCREENSHOT".equals(intent.getAction())) {
                            boolean finished = intent.getBooleanExtra("IsFinished", true);
                            view.setVisibility(finished ? View.VISIBLE : View.INVISIBLE);
                        }
                    }
                };
                view.getContext().registerReceiver(br, new IntentFilter("miui.intent.TAKE_SCREENSHOT"));
            }
        });
    }
}
