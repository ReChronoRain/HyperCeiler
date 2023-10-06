package com.sevtinge.cemiuiler.module.hook.systemui.controlcenter;

import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArrayMap;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.utils.ShellUtils;
import com.sevtinge.cemiuiler.utils.TileUtils;
import com.sevtinge.cemiuiler.utils.log.AndroidLogUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;

public class SunlightMode extends TileUtils {
    public static String path = null;
    public static boolean mMode = false;
    public static boolean useSystem = false;
    public static int lastSunlight = 0;
    // public static int maxSunlight = 0;
    public static int pathSunlight = 0;
    public static boolean intentListening = false;
    // public static boolean imGetSun = false;
    public static final String screenBrightness = "screen_brightness";
    public static final String sunlightMode = "sunlight_mode";
    String mQSFactoryClsName = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "com.android.systemui.qs.tileimpl.MiuiQSFactory" :
        "com.android.systemui.qs.tileimpl.QSFactoryImpl";

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
        int mode = mPrefsMap.getStringAsInt("system_control_center_sunshine_new_mode", 0);
        switch (mode) {
            case 1 -> mMode = false;
            case 2 -> mMode = true;
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
            logE("setPath: Missing directory, unable to set this mode: " + path);
        } else {
            ShellUtils.execCommand("chmod 777 " + path, true, false);
            // logI("setPath: im get file: " + path);
        }
    }

    @Override
    public Class<?> customQSFactory() {
        return findClassIfExists(mQSFactoryClsName);
    }

    @Override
    public Class<?> customClass() {
        return findClassIfExists("com.android.systemui.qs.tiles.AutoBrightnessTile");
    }

    @Override
    public String[] customTileProvider() {
        String[] TileProvider = new String[2];
        TileProvider[0] = isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU) ? "autoBrightnessTileProvider" : "mAutoBrightnessTileProvider";
        TileProvider[1] = "createTileInternal";
        return TileProvider;
    }

    @Override
    public String customName() {
        return "custom_SUN";
    }

    @Override
    public int customValue() {
        return R.string.system_control_center_sunshine_mode;
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
            if (!mMode) {
                int systemMode = Settings.System.getInt(mContext.getContentResolver(), sunlightMode);
                if (systemMode == 1) {
                    Settings.System.putInt(mContext.getContentResolver(), sunlightMode, 0);
                    refreshState(param.thisObject);
                } else if (systemMode == 0) {
                    Settings.System.putInt(mContext.getContentResolver(), sunlightMode, 1);
                    refreshState(param.thisObject);
                } else {
                    logE("tileClick: ERROR Int For sunlight_mode");
                }
            } else {
                if (!useSystem) {
                    if (lastSunlight == 0 || Integer.parseInt(readAndWrit(null, false)) != pathSunlight) {
                        setBroadcastReceiver(mContext, param);
                        // lastSunlight = Settings.System.getInt(mContext.getContentResolver(), screenBrightness);
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
                        // sLog("tileClick: comeback lastSunlight: " + lastSunlight + " pathSunlight: " + pathSunlight);
                        // readAndWrit(null, false);
                        BroadcastReceiver broadcastReceiver = (BroadcastReceiver) XposedHelpers.getAdditionalInstanceField(param.thisObject, "broadcastReceiver");
                        if (broadcastReceiver != null)
                            mContext.unregisterReceiver(broadcastReceiver);
                        // Settings.System.putInt(mContext.getContentResolver(), screenBrightness, lastSunlight);
                        readAndWrit("" + lastSunlight, false);
                        intentListening = false;
                    }
                } else {
                    if (lastSunlight == 0 || Settings.System.getInt(mContext.getContentResolver(), screenBrightness) != pathSunlight) {
                        setBroadcastReceiver(mContext, param);
                        lastSunlight = Settings.System.getInt(mContext.getContentResolver(), screenBrightness);
                        Settings.System.putInt(mContext.getContentResolver(), screenBrightness, Integer.MAX_VALUE);
                        pathSunlight = Settings.System.getInt(mContext.getContentResolver(), screenBrightness);
                    } else {
                        BroadcastReceiver broadcastReceiver = (BroadcastReceiver) XposedHelpers.getAdditionalInstanceField(param.thisObject, "broadcastReceiver");
                        if (broadcastReceiver != null)
                            mContext.unregisterReceiver(broadcastReceiver);
                        Settings.System.putInt(mContext.getContentResolver(), screenBrightness, lastSunlight);
                        intentListening = false;
                    }
                }
                refreshState(param.thisObject);
            }
        } catch (Settings.SettingNotFoundException e) {
            refreshState(param.thisObject);
        }
    }

    public void setBroadcastReceiver(Context mContext, MethodHookParam param) {
        BroadcastReceiver broadcastReceiver = new Screen();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(broadcastReceiver, filter);
        XposedHelpers.setAdditionalInstanceField(param.thisObject, "broadcastReceiver", broadcastReceiver);
        intentListening = true;
    }

    @Override
    public void tileListening(MethodHookParam param, String tileName) {
        Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
        boolean mListening = (boolean) param.args[0];
        // sLog("tileListening: mListening: " + mListening);
        if (mListening) {
            ContentObserver contentObserver = new ContentObserver(new Handler(mContext.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    // Settings.System.putInt(mContext.getContentResolver(), screenBrightness, lastSunlight);
                    // sLog("tileListening: screenBrightness is change: " + selfChange);
                    refreshState(param.thisObject);
                    super.onChange(selfChange);
                }
            };
            mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(screenBrightness), false, contentObserver);
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
            logE("tileCheck: Missing system API: " + sunlightMode);
        }
        try {
            if (!mMode) {
                nowInt = Settings.System.getInt(mContext.getContentResolver(), sunlightMode);
            } else {
                if (!useSystem) {
                    // nowInt = Settings.System.getInt(mContext.getContentResolver(), screenBrightness);
                    nowSunlight = Integer.parseInt(readAndWrit(null, false));
                    // if (nowInt == maxSunlight) {
                    //     nowInt = 1;
                    // } else
                    // sLog("tileUpdateState: nowInt is: " + nowInt + " pathSunlight: " + pathSunlight + " nowSunlight: " + nowSunlight);
                } else {
                    nowSunlight = Settings.System.getInt(mContext.getContentResolver(), screenBrightness);
                }
                if (nowSunlight == pathSunlight) nowInt = 1;
            }
            if (nowInt == 1) isEnable = true;
            if (intentListening && !isEnable) {
                BroadcastReceiver receiver = (BroadcastReceiver) XposedHelpers.getAdditionalInstanceField(param.thisObject, "broadcastReceiver");
                if (receiver != null) mContext.unregisterReceiver(receiver);
                intentListening = false;
            }
            // sLog("tileUpdateState: isEnable is: " + isEnable);
        } catch (Settings.SettingNotFoundException e) {
            logE("tileUpdateState: Not Find sunlight_mode");
        }
        ArrayMap<String, Integer> tileResMap = new ArrayMap<>();
        tileResMap.put("custom_SUN_Enable", isEnable ? 1 : 0);
        tileResMap.put("custom_SUN_ON", mResHook.addResource("ic_control_center_sunlight_mode_on", R.drawable.baseline_wb_sunny_24));
        tileResMap.put("custom_SUN_OFF", mResHook.addResource("ic_control_center_sunlight_mode_off", R.drawable.baseline_wb_sunny_24));
        return tileResMap;
    }

    public static String readAndWrit(String writ, boolean need) {
        String line;
        BufferedReader reader = null;
        BufferedWriter writer = null;
        StringBuilder builder = null;
        /*try {
            // 800毫秒获得丝滑转场效果，太好笑了，记录一下
            Thread.sleep(need ? 800 : 400);
        } catch (InterruptedException e) {
            logE("sleep error: " + e);
        }*/
        if (writ != null) {
            try {
                writer = new BufferedWriter(new FileWriter(path, false));
                writer.write(writ);
            } catch (IOException e) {
                AndroidLogUtils.LogE("SunlightMode", "error to writer: " + path + " ", e);
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException e) {
                    AndroidLogUtils.LogE("SunlightMode", "close writer error: ", e);
                }
            }
        }
        try {
            reader = new BufferedReader(new FileReader(path));
            builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            AndroidLogUtils.LogE("SunlightMode", "error to read: " + path + " ", e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                AndroidLogUtils.LogE("SunlightMode", "close reader error: ", e);
            }
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
                if (lastSunlight != 0) {
                    if (!useSystem) {
                        // Log.i("SunlightMode", "onReceive: run");
                        // Settings.System.putInt(context.getContentResolver(), screenBrightness, lastSunlight);
                        readAndWrit("" + lastSunlight, false);
                    } else
                        Settings.System.putInt(context.getContentResolver(), screenBrightness, lastSunlight);
                }
            }
        }
    }
}
