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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.utils.shell.ShellUtils.safeExecCommandWithRoot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArrayMap;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.TileUtils;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.shell.ShellUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;

public class SunlightModeHigh extends TileUtils {
    public static String path = null;
    public static boolean mMode = false;
    public static boolean useSystem = false;
    public static boolean shell = false;
    public static int lastSunlight = 0;
    // public static int lastBrightnessMode = 0;
    // public static int maxSunlight = 0;
    public static int pathSunlight = 0;
    public static boolean intentListening = false;
    // public static boolean imOpenCustomMode = false;
    public static final String screenBrightness = "screen_brightness";
    public static final String screenBrightnessEnable = "screen_brightness_enable";
    public static final String screenBrightnessMode = "screen_brightness_mode";
    public static final String screenBrightnessCustomMode = "screen_brightness_custom_mode";
    public static final String sunlightMode = "sunlight_mode";
    public static int setScreenBrightness = mPrefsMap.getInt("system_control_center_sunshine_mode_brightness", 2048);
    public static int currentScreenBrightness = 1;
    public static boolean isCustomSunshineMode = false;

    @Override
    public void init() {
        modeSwitch();
        setPath();
        super.init();
    }

    /*public void sLog(String log) {
        Log.i("SunlightMode", "sLog: " + log);
    }*/

    public void modeSwitch() {
        int mode = mPrefsMap.getStringAsInt("system_control_center_sunshine_new_mode_high", 0);
        switch (mode) {
            case 1 -> mMode = false;
            case 2 -> mMode = true;
            case 3 -> {
                shell = true;
                mMode = true;
            }
        }
    }

    public void setPath() {
        String fileOne = "/sys/class/mi_display/disp-DSI-0/brightness_clone";
        String fileTwo = "/sys/class/backlight/panel0-backlight/brightness";
        File file = new File(fileOne);
        if (file.exists()) {
            path = fileOne;
        } else {
            File file1 = new File(fileTwo);
            if (file1.exists()) {
                path = fileTwo;
            }
        }
        /*ShellUtils.CommandResult commandResult = ShellUtils.execCommand("[ -f " + fileOne + " ]", true, false);
        if (commandResult.result == 0) {
            path = fileOne;
        } else {
            ShellUtils.CommandResult shell = ShellUtils.execCommand("[ -f " + fileOne + " ]", true, false);
            if (shell.result == 0) {
                path = fileTwo;
            }
        }
        sLog("tileCheck: shell result is: " + commandResult.result);
        intentListening = true;*/
        if (path == null) {
            useSystem = true;
            logE(TAG, this.lpparam.packageName, "Missing directory, unable to set this mode: true");
        } else {
            ShellUtils.execCommand("chmod 777 " + path, true, false);
            // logI("setPath: im get file: " + path);
        }
    }

    @Override
    public Class<?> customClass() {
        return findClassIfExists("com.android.systemui.qs.tiles.PowerSaverTile");
    }

    @Override
    public String setTileProvider() {
        return "powerSaverTileProvider";
    }

    @Override
    public String customName() {
        return "custom_SUN";
    }

    @Override
    public int customRes() {
        return R.string.tiles_sunshine_mode;
    }

    public void refreshState(Object o) {
        XposedHelpers.callMethod(o, "refreshState");
    }

    @Override
    public void tileCheck(MethodHookParam param, String tileName) {
        param.setResult(true);
    }

    @Override
    public void tileLongClickIntent(MethodHookParam param, String tileName) {
        param.setResult(null);
    }

    @Override
    public void tileClick(MethodHookParam param, String tileName) {
        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        try {
            if (shell) {
                /*if (!isCustomSunshineMode) {
                    currentScreenBrightness = Integer.parseInt(safeExecCommandWithRoot("cat /sys/class/backlight/panel0-backlight/brightness"));
                    safeExecCommandWithRoot("echo " + setScreenBrightness + " > /sys/class/backlight/panel0-backlight/brightness");
                    isCustomSunshineMode = true;
                } else {
                    safeExecCommandWithRoot("echo " + currentScreenBrightness + " > /sys/class/backlight/panel0-backlight/brightness");
                    isCustomSunshineMode = false;
                }*/
                if (lastSunlight == 0 || Integer.parseInt(readAndWrit(null, false)) != pathSunlight) {
                    // imOpenCustomMode = true;
                    if (getBrightnessMode(mContext) == 1) {
                        setCustomBrightnessMode(mContext, 1);
                    }
                    setBroadcastReceiver(mContext, param);
                    lastSunlight = Integer.parseInt(readAndWrit(null, false));
                    readAndWrit("" + setScreenBrightness, true);
                        /*Settings.System.putInt(mContext.getContentResolver(), screenBrightness, Integer.MAX_VALUE);
                        if (maxSunlight == 0)
                            maxSunlight = Settings.System.getInt(mContext.getContentResolver(), screenBrightness);
                        sLog("tileClick: lastSunlight: " + lastSunlight + " pathSunlight: " + pathSunlight + " filter: " + filter);
                        ShellUtils.CommandResult commandResult = ShellUtils.execCommand("sleep 0.8 && echo " + Integer.MAX_VALUE + " > " + path + " && cat " + path, true, true);
                        try {
                            pathSunlight = Integer.parseInt(commandResult.successMsg);
                        } catch (NumberFormatException e) {
                            logE("cant to int: " + pathSunlight);
                        }*/
                } else {
                    // imOpenCustomMode = false;
                    if (getCustomBrightnessMode(mContext) == 1) {
                        setCustomBrightnessMode(mContext, 0);
                    }
                    // sLog("tileClick: comeback lastSunlight: " + lastSunlight + " pathSunlight: " + pathSunlight);
                    unBroadcastReceiver(mContext, param);
                    readAndWrit("" + lastSunlight, false);
                    lastSunlight = 0; // 重置
                }
                refreshState(param.thisObject);
            } else if (!mMode) {
                    /*系统阳光模式*/
                    int systemMode = Settings.System.getInt(mContext.getContentResolver(), sunlightMode);
                    if (systemMode == 1) {
                        Settings.System.putInt(mContext.getContentResolver(), sunlightMode, 0);
                        refreshState(param.thisObject);
                    } else if (systemMode == 0) {
                        Settings.System.putInt(mContext.getContentResolver(), sunlightMode, 1);
                        refreshState(param.thisObject);
                    } else {
                        logE(TAG, this.lpparam.packageName, "ERROR Int For sunlight_mode");
                    }
            } else {
                if (!useSystem) {
                    /*强制最高亮度*/
                    if (lastSunlight == 0 || Integer.parseInt(readAndWrit(null, false)) != pathSunlight) {
                        // imOpenCustomMode = true;
                        if (getBrightnessMode(mContext) == 1) {
                            setCustomBrightnessMode(mContext, 1);
                        }
                        setBroadcastReceiver(mContext, param);
                        lastSunlight = Integer.parseInt(readAndWrit(null, false));
                        readAndWrit("" + Integer.MAX_VALUE, true);
                        /*Settings.System.putInt(mContext.getContentResolver(), screenBrightness, Integer.MAX_VALUE);
                        if (maxSunlight == 0)
                            maxSunlight = Settings.System.getInt(mContext.getContentResolver(), screenBrightness);
                        sLog("tileClick: lastSunlight: " + lastSunlight + " pathSunlight: " + pathSunlight + " filter: " + filter);
                        ShellUtils.CommandResult commandResult = ShellUtils.execCommand("sleep 0.8 && echo " + Integer.MAX_VALUE + " > " + path + " && cat " + path, true, true);
                        try {
                            pathSunlight = Integer.parseInt(commandResult.successMsg);
                        } catch (NumberFormatException e) {
                            logE("cant to int: " + pathSunlight);
                        }*/
                    } else {
                        // imOpenCustomMode = false;
                        if (getCustomBrightnessMode(mContext) == 1) {
                            setCustomBrightnessMode(mContext, 0);
                        }
                        // sLog("tileClick: comeback lastSunlight: " + lastSunlight + " pathSunlight: " + pathSunlight);
                        unBroadcastReceiver(mContext, param);
                        readAndWrit("" + lastSunlight, false);
                        lastSunlight = 0; // 重置
                    }
                } else {
                    /*系统Api最高亮度*/
                    if (lastSunlight == 0) {
                        // imOpenCustomMode = true;
                        if (getBrightnessMode(mContext) == 1) {
                            setCustomBrightnessMode(mContext, 1);
                        }
                        setBroadcastReceiver(mContext, param);
                        lastSunlight = Settings.System.getInt(mContext.getContentResolver(), screenBrightness);
                        Settings.System.putInt(mContext.getContentResolver(), screenBrightness, Integer.MAX_VALUE);
                        Settings.System.putInt(mContext.getContentResolver(), screenBrightnessEnable, 1);
                        // pathSunlight = Settings.System.getInt(mContext.getContentResolver(), screenBrightness); // 不稳定
                    } else {
                        // imOpenCustomMode = false;
                        if (getCustomBrightnessMode(mContext) == 1) {
                            setCustomBrightnessMode(mContext, 0);
                        }
                        unBroadcastReceiver(mContext, param);
                        Settings.System.putInt(mContext.getContentResolver(), screenBrightness, lastSunlight);
                        Settings.System.putInt(mContext.getContentResolver(), screenBrightnessEnable, 0);
                        lastSunlight = 0; // 重置
                    }
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            refreshState(param.thisObject);
        }
    }

    public static int getBrightnessMode(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), screenBrightnessMode);
        } catch (Settings.SettingNotFoundException e) {
            AndroidLogUtils.logE("No Found Settings: ", e);
            return -1;
        }
    }

    public static void setBrightnessMode(Context context, int value) {
        Settings.System.putInt(context.getContentResolver(), screenBrightnessMode, value);
    }

    public static int getCustomBrightnessMode(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), screenBrightnessCustomMode);
        } catch (Settings.SettingNotFoundException e) {
            setCustomBrightnessMode(context, 0);
            AndroidLogUtils.logE("No Found Settings: ", e);
            return -1;
        }
    }

    public static void setCustomBrightnessMode(Context context, int value) {
        Settings.System.putInt(context.getContentResolver(), screenBrightnessCustomMode, value);
    }

    public void setBroadcastReceiver(Context mContext, MethodHookParam param) {
        BroadcastReceiver broadcastReceiver = new Screen();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(broadcastReceiver, filter);
        // logE("setBroadcastReceiver: registerReceiver: " + broadcastReceiver + " filter: " + filter);
        XposedHelpers.setAdditionalInstanceField(param.thisObject, "broadcastReceiver", broadcastReceiver);
        intentListening = true;
    }

    public void unBroadcastReceiver(Context mContext, MethodHookParam param) {
        BroadcastReceiver broadcastReceiver = (BroadcastReceiver) XposedHelpers.getAdditionalInstanceField(param.thisObject, "broadcastReceiver");
        // logE("unBroadcastReceiver: broadcastReceiver: " + broadcastReceiver);
        if (broadcastReceiver != null) {
            // logE("unBroadcastReceiver: unregisterReceiver: " + broadcastReceiver);
            mContext.unregisterReceiver(broadcastReceiver);
            intentListening = false;
        }
    }

    @Override
    public void tileListening(MethodHookParam param, String tileName) {
        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        boolean mListening = (boolean) param.args[0];
        // sLog("tileListening: mListening: " + mListening);
        if (mListening) {
            ContentObserver contentObserver = new ContentObserver(new Handler(mContext.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    super.onChange(selfChange, uri);
                    if (uri != null) {
                        String uriString = uri.toString();
                        String mUriString = Settings.System.getUriFor(screenBrightnessCustomMode).toString();
                        if (uriString.equals(mUriString)) {
                            if (getBrightnessMode(mContext) == 0 && getCustomBrightnessMode(mContext) == 0) {
                                setBrightnessMode(mContext, 1);
                            } else if (getBrightnessMode(mContext) == 1 && getCustomBrightnessMode(mContext) == 1) {
                                setBrightnessMode(mContext, 0);
                            }
                        }
                    }
                    refreshState(param.thisObject);
                }
            };
            getCustomBrightnessMode(mContext);
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(screenBrightness), false, contentObserver);
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(screenBrightnessCustomMode), false, contentObserver);
            XposedHelpers.setAdditionalInstanceField(param.thisObject, "tileListener", contentObserver);
        } else {
            // if (contentObserver != null) {
            //     sLog("tileListening: im unregisterContentObserver: " + contentObserver);
            //     mContext.getContentResolver().unregisterContentObserver(contentObserver);
            // }
            ContentObserver contentObserver = (ContentObserver) XposedHelpers.getAdditionalInstanceField(param.thisObject, "tileListener");
            // sLog("tileListening: im unregisterContentObserver: " + contentObserver);
            mContext.getContentResolver().unregisterContentObserver(contentObserver);
        }
    }

    @Override
    public ArrayMap<String, Integer> tileUpdateState(MethodHookParam param, Class<?> mResourceIcon, String tileName) {
        int nowInt = 0;
        int nowSunlight;
        boolean isEnable = false;
        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        try {
            Settings.System.getInt(mContext.getContentResolver(), sunlightMode);
            if (mMode) {
                try {
                    Settings.System.getInt(mContext.getContentResolver(), screenBrightness);
                } catch (Settings.SettingNotFoundException e) {
                    mMode = false;
                    // sLog("tileCheck: Missing system API: " + screenBrightness);
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            logE(TAG, lpparam.packageName, "tileUpdateState: Missing system API: " + sunlightMode);
        }
        try {
            if (!mMode) {
                nowInt = Settings.System.getInt(mContext.getContentResolver(), sunlightMode);
            } else {
                if (!useSystem) {
                    // nowInt = Settings.System.getInt(mContext.getContentResolver(), screenBrightness);
                    nowSunlight = Integer.parseInt(readAndWrit(null, false));
                    if (nowSunlight == pathSunlight) nowInt = 1;
                    // if (nowInt == maxSunlight) {
                    //     nowInt = 1;
                    // } else
                    // sLog("tileUpdateState: nowInt is: " + nowInt + " pathSunlight: " + pathSunlight + " nowSunlight: " + nowSunlight);
                } else {
                    nowSunlight = Settings.System.getInt(mContext.getContentResolver(), screenBrightnessEnable);
                    if (nowSunlight == 1) nowInt = 1;
                }
            }
            if (nowInt == 1) isEnable = true;
            if (intentListening && !isEnable) {
                unBroadcastReceiver(mContext, param);
            }
            // sLog("tileUpdateState: isEnable is: " + isEnable);
        } catch (Settings.SettingNotFoundException e) {
            logE(TAG, this.lpparam.packageName, "tileUpdateState: Not Find sunlight_mode");
        }
        ArrayMap<String, Integer> tileResMap = new ArrayMap<>();
        tileResMap.put("custom_SUN_Enable", isEnable ? 1 : 0);
        tileResMap.put("custom_SUN_ON", R.drawable.baseline_wb_sunny_24);
        tileResMap.put("custom_SUN_OFF", R.drawable.baseline_wb_sunny_24);
        return tileResMap;
    }

    public static String readAndWrit(String writ, boolean need) {
        StringBuilder builder = new StringBuilder();
        /*try {
            // 800毫秒获得丝滑转场效果，太好笑了，记录一下
            Thread.sleep(need ? 800 : 400);
        } catch (InterruptedException e) {
            logE("sleep error: " + e);
        }*/
        if (writ != null) {
            try {
                if (shell) {
                    safeExecCommandWithRoot("echo " + writ + " > /sys/class/backlight/panel0-backlight/brightness");
                } else {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, false))) {
                        writer.write(writ);
                    } // try-with-resources 会自动关闭 writer
                }
            } catch (IOException e) {
                AndroidLogUtils.logE("SunlightMode", "error to writer: " + path + " ", e);
            }
        }
        try {
            if (shell) {
                builder.append(safeExecCommandWithRoot("cat /sys/class/backlight/panel0-backlight/brightness"));
            } else {
                try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                } // try-with-resources 会自动关闭 reader
            }
        } catch (IOException e) {
            AndroidLogUtils.logE("SunlightMode", "error to read: " + path + " ", e);
        }
        if (builder != null) {
            // logE("get string: " + builder);
            if (need) pathSunlight = Integer.parseInt(builder.toString());
            return builder.toString();
        }
        return null;
    }

    public static class Screen extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 息屏还原，按照之前的逻辑写的，如果需要再改
            if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF)) {
                // Log.i("SunlightMode", "onReceive: run 1");
                // AndroidLogUtils.LogE("SunlightMode", "onReceive 1: " + lastSunlight + " use: " + useSystem, null);
                if (lastSunlight != 0) {
                    // AndroidLogUtils.LogE("SunlightMode", "onReceive 2: " + lastSunlight + " use: " + useSystem, null);
                    if (!useSystem) {
                        // Log.i("SunlightMode", "onReceive: run");
                        // Settings.System.putInt(context.getContentResolver(), screenBrightness, lastSunlight);
                        readAndWrit("" + lastSunlight, false);
                    } else {
                        Settings.System.putInt(context.getContentResolver(), screenBrightness, lastSunlight);
                        Settings.System.putInt(context.getContentResolver(), screenBrightnessEnable, 0);
                    }
                    lastSunlight = 0; // 重置
                    if (getCustomBrightnessMode(context) == 1) {
                        setBrightnessMode(context, 1);
                        setCustomBrightnessMode(context, 0);
                    }
                }
            }

        }
    }
}
