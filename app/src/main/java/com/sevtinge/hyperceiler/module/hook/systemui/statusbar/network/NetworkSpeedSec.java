package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class NetworkSpeedSec extends BaseHook {
    @Override
    public void init() {
        try {
            findClass("com.android.systemui.statusbar.views.NetworkSpeedView").getDeclaredMethod("setNetworkSpeed", String.class);
            findAndHookMethod("",
                "setNetworkSpeed", String.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        if (param.args[0] != null) {
                            String mText = (String) param.args[0];
                            param.args[0] = mText.replace("/", "")
                                .replace("s", "")
                                .replace("'", "")
                                .replace("วิ", "");
                        }
                    }
                }
            );
        } catch (NoSuchMethodException e) {
            try {
                findClass("com.android.systemui.statusbar.views.NetworkSpeedView").getDeclaredMethod("setNetworkSpeed", String.class, String.class);
                findAndHookMethod("com.android.systemui.statusbar.views.NetworkSpeedView",
                    "setNetworkSpeed", String.class, String.class,
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            // logE(TAG, "1: " + param.args[0] + " 2: " + param.args[1]);
                            String mText = (String) param.args[1];
                            param.args[1] = mText.replace("/", "")
                                .replace("B", "")
                                .replace("s", "")
                                .replace("'", "")
                                .replace("วิ", "");
                        }
                    }
                );
            } catch (NoSuchMethodException f) {
                logE(TAG, "No such: " + e + " And: " + f);
            }
        }

    }
}
