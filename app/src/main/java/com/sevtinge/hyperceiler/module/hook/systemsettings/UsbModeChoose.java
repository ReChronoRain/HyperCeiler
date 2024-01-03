package com.sevtinge.hyperceiler.module.hook.systemsettings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.ArrayMap;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.Locale;

import de.robv.android.xposed.XposedHelpers;

public class UsbModeChoose extends BaseHook {
    ArrayMap<String, Integer> mode = new ArrayMap<>();

    ArrayMap<Integer, String> getMode = new ArrayMap<>();
    int mChoose = mPrefsMap.getStringAsInt("system_settings_usb_mode_choose", 0);

    boolean modes = mPrefsMap.getBoolean("system_settings_usb_mode");
    boolean addAll = false;
    Resources resources;
    Activity activity;
    Locale locale;
    Configuration configuration;
    String[] allMode = {
        "",
        "仅限充电",
        "传输文件",
        "传输照片",
        "MIDI模式",
        "反向充电",
        "USB 网络共享"
    };

    @Override
    public void init() throws NoSuchMethodException {
        if (mChoose != 0) {
            findAndHookMethod("com.android.settings.connecteddevice.usb.UsbModeChooserActivity",
                "onCreate", Bundle.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        activity = (Activity) param.thisObject;
                        // logE(TAG, "before: " + activity);
                    }
                }
            );

            findAndHookMethod("com.android.settings.connecteddevice.usb.UsbModeChooserActivity",
                "initModesList", long[].class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        // logE(TAG, "long: " + param.args[0]);
                        long[] jArr = {0, 8, 4, 16, 128};
                        if (addAll) return;
                        setLanguage(activity);
                        for (int i2 = 0; i2 < jArr.length; i2++) {
                            int getTitle = (int) XposedHelpers.callStaticMethod(
                                findClassIfExists("com.android.settings.connecteddevice.usb.UsbModeChooserActivity"),
                                "getTitleMiui12", jArr[i2]);
                            if (getTitle != 0) {
                                String get = (String) XposedHelpers.callMethod(
                                    param.thisObject, "getString", getTitle
                                );
                                mode.put(get,
                                    (int) jArr[i2]);
                                // logE(TAG, "get: " + get);
                            }
                        }
                        if (mode.size() == jArr.length) {
                            mode.put("USB 网络共享", -1);
                            addAll = true;
                        }
                        revertLanguage();
                    }
                }
            );

            findAndHookMethod("com.android.settings.connecteddevice.usb.UsbModeChooserActivity",
                "initDialog", new MethodHook() {
                    @SuppressLint("WrongConstant")
                    @Override
                    protected void before(MethodHookParam param) {
                        String action = activity.getIntent().getAction();
                        // logE(TAG, "ac: " + action);
                        if (getMode.isEmpty())
                            setAllMode();
                        if (action == null) {
                            int choose = mode.get(getMode.get(mChoose));
                            // logE(TAG, "choose: " + choose);
                            if (choose != -1) {
                                Object mBackend = XposedHelpers.getObjectField(param.thisObject, "mBackend");
                                // logE(TAG, "cc: " + getMode.get(mChoose));
                                if (getMode.get(mChoose).equals("反向充电")) {
                                    if ((boolean) XposedHelpers.callMethod(param.thisObject, "isSupportReverseCharging")) {
                                        XposedHelpers.callMethod(mBackend, "setCurrentFunctions", (long) choose);
                                    } else {
                                        logE(TAG, "Your phone can't reverse charging.");
                                    }
                                } else {
                                    XposedHelpers.callMethod(mBackend, "setCurrentFunctions", (long) choose);
                                    // logE(TAG, "set: " + choose + " name: " + getMode.get(mChoose));
                                }
                            } else if (choose == -1) {
                                Object tethering = activity.getSystemService("tethering");
                                int end = (int) XposedHelpers.callMethod(tethering, "setUsbTethering", true);
                                logI(TAG, "tethering: " + end);
                            }
                            if (modes) {
                                param.setResult(null);
                                activity.finish();
                            }
                            // logE(TAG, "finish");
                        }
                    }
                }
            );
        } else if (modes) {
            findAndHookMethod("com.android.settings.connecteddevice.usb.UsbModeChooserActivity",
                "initDialog", new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        String action = activity.getIntent().getAction();
                        if (action == null) {
                            param.setResult(null);
                            activity.finish();
                        }
                    }
                }
            );
        }
    }

    public void setAllMode() {
        for (int i = 0; i < allMode.length; i++) {
            String name = allMode[i];
            getMode.put(i, name);
        }
    }

    public void setLanguage(Activity activity) {
        resources = activity.getResources();
        configuration = resources.getConfiguration();
        locale = configuration.locale;
        configuration.setLocale(Locale.SIMPLIFIED_CHINESE);
        resources.updateConfiguration(configuration, null);
    }

    public void revertLanguage() {
        if (configuration != null && resources != null) {
            configuration.setLocale(locale);
            resources.updateConfiguration(configuration, null);
        }
    }
}
