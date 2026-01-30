/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemsettings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.ArrayMap;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.Locale;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

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
    public void init() {
        if (mChoose != 0) {
            findAndHookMethod("com.android.settings.connecteddevice.usb.UsbModeChooserActivity",
                "onCreate", Bundle.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                        activity = (Activity) param.getThisObject();
                        // XposedLog.e(TAG, "before: " + activity);
                    }
                }
            );

            findAndHookMethod("com.android.settings.connecteddevice.usb.UsbModeChooserActivity",
                "initModesList", long[].class, new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        // XposedLog.e(TAG, "long: " + param.args[0]);
                        long[] jArr = {0, 8, 4, 16, 128};
                        if (addAll) return;
                        setLanguage(activity);
                        for (long l : jArr) {
                            int getTitle = (int) callStaticMethod(
                                findClassIfExists("com.android.settings.connecteddevice.usb.UsbModeChooserActivity"),
                                "getTitleMiui12", l);
                            if (getTitle != 0) {
                                String get = (String) callMethod(
                                    param.getThisObject(), "getString", getTitle
                                );
                                mode.put(get,
                                    (int) l);
                                // XposedLog.e(TAG, "get: " + get);
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
                "initDialog", new IMethodHook() {
                    @SuppressLint("WrongConstant")
                    @Override
                    public void before(BeforeHookParam param) {
                        String action = activity.getIntent().getAction();
                        // XposedLog.e(TAG, "ac: " + action);
                        if (getMode.isEmpty())
                            setAllMode();
                        if (action == null) {
                            int choose = mode.get(getMode.get(mChoose));
                            // XposedLog.e(TAG, "choose: " + choose);
                            if (choose != -1) {
                                Object mBackend = getObjectField(param.getThisObject(), "mBackend");
                                // XposedLog.e(TAG, "cc: " + getMode.get(mChoose));
                                if (getMode.get(mChoose).equals("反向充电")) {
                                    if ((boolean) callMethod(param.getThisObject(), "isSupportReverseCharging")) {
                                        callMethod(mBackend, "setCurrentFunctions", (long) choose);
                                    } else {
                                        XposedLog.e(TAG, "Your phone can't reverse charging.");
                                    }
                                } else {
                                    callMethod(mBackend, "setCurrentFunctions", (long) choose);
                                    // XposedLog.e(TAG, "set: " + choose + " name: " + getMode.get(mChoose));
                                }
                            } else if (choose == -1) {
                                Object tethering = activity.getSystemService("tethering");
                                int end = (int) callMethod(tethering, "setUsbTethering", true);
                                XposedLog.i(TAG, "tethering: " + end);
                            }
                            if (modes) {
                                param.setResult(null);
                                activity.finish();
                            }
                            // XposedLog.e(TAG, "finish");
                        }
                    }
                }
            );
        } else if (modes) {
            findAndHookMethod("com.android.settings.connecteddevice.usb.UsbModeChooserActivity",
                "initDialog", new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
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
