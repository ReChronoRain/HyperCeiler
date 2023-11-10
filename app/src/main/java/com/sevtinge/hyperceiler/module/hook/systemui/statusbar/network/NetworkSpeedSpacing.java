package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.network;

import android.os.Message;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class NetworkSpeedSpacing extends BaseHook {
    public boolean handler;

    @Override
    public void init() {
        try {
            findClass("com.android.systemui.statusbar.policy.NetworkSpeedController").getDeclaredMethod("postUpdateNetworkSpeedDelay", long.class);
            findAndHookMethod("com.android.systemui.statusbar.policy.NetworkSpeedController",
                "postUpdateNetworkSpeedDelay", long.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        long originInterval = (long) param.args[0];
                        if (originInterval == 4000L) {
                            originInterval = mPrefsMap.getInt(
                                "system_ui_statusbar_network_speed_update_spacing",
                                4
                            ) * 1000L;
                            param.args[0] = originInterval;
                        }
                    }
                }
            );
        } catch (NoSuchMethodException e) {
            findAndHookMethod("com.android.systemui.statusbar.policy.NetworkSpeedController$4",
                "handleMessage", Message.class, new MethodHook() {
                       /* @Override
                        protected void before(MethodHookParam param) {
                            Message message = (Message) param.args[0];
                            // Object handleMessage = XposedHelpers.getObjectField(param.thisObject, "this$0");
                            switch (message.what) {
                                case 100001, 100002 -> {
                                    handler = true;
                                    logE(TAG, "handleMessage before: " + handler);
                                }
                            }
                        }*/

                    @Override
                    protected void after(MethodHookParam param) {
                        Message message = (Message) param.args[0];
                        Object handleMessage = XposedHelpers.getObjectField(param.thisObject, "this$0");
                        Object mBgHandler = XposedHelpers.getObjectField(handleMessage, "mBgHandler");
                        if (message.what == 200001) {
                            handleMessage(mBgHandler, true);
                            // logE(TAG, "200001: " + mBgHandler);
                        }
                    }
                }
            );
            /*findAndHookMethod("android.os.Handler",
                 "sendEmptyMessageDelayed", int.class, long.class,
                 new MethodHook() {
                     @Override
                     protected void before(MethodHookParam param) {
                         if (handler) {
                             param.setResult(true);
                             logE(TAG, "sendEmptyMessageDelayed im run");
                             handler = false;
                         }
                     }
                 }
             );*/
        }
    }

    public void handleMessage(Object mBgHandler, boolean z) {
        XposedHelpers.callMethod(mBgHandler, "removeMessages", 200001);
        if (z) {
            XposedHelpers.callMethod(mBgHandler, "sendEmptyMessageDelayed", 200001,
                mPrefsMap.getInt("system_ui_statusbar_network_speed_update_spacing",
                    4
                ) * 1000L
            );
        }
    }
}
