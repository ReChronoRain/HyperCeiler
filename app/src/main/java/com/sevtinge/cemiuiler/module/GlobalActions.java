package com.sevtinge.cemiuiler.module;

import static java.lang.System.currentTimeMillis;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.KeyEvent;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.LogUtils;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import de.robv.android.xposed.XposedHelpers;

public class GlobalActions extends BaseHook {

    @Override
    public void init() {
        setupGlobalActions();
        setupRestartActions();
    }

    //GlobalActions
    public void setupGlobalActions() {
        hookAllConstructors("com.android.server.accessibility.AccessibilityManagerService", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context mGlobalContext = (Context)param.args[0];
                IntentFilter mFilter = new IntentFilter();
                // Actions
                mFilter.addAction(ACTION_PREFIX + "LockScreen");
                mFilter.addAction(ACTION_PREFIX + "GoToSleep");
                mFilter.addAction(ACTION_PREFIX + "ScreenCapture");
                mFilter.addAction(ACTION_PREFIX + "OpenPowerMenu");
                mFilter.addAction(ACTION_PREFIX + "LaunchIntent");
                mGlobalContext.registerReceiver(mGlobalReceiver, mFilter);
            }
        });
    }

    public static void proxySystemProperties(String method, String prop, String val, ClassLoader classLoader) {
        XposedHelpers.callStaticMethod(XposedHelpers.findClassIfExists("android.os.SystemProperties", classLoader),
                method, prop, val);
    }


    private final BroadcastReceiver mGlobalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Class<?> clsWMG = findClass("android.view.WindowManagerGlobal", null);
                Object wms = XposedHelpers.callStaticMethod(clsWMG, "getWindowManagerService");

                String action = intent.getAction();

                switch (action) {
                    case ACTION_PREFIX + "LockScreen":
                        XposedHelpers.callMethod(context.getSystemService(Context.POWER_SERVICE), "goToSleep", SystemClock.uptimeMillis());
                        XposedHelpers.callMethod(wms, "lockNow", (Object)null);
                        break;

                    case ACTION_PREFIX + "GoToSleep":
                        XposedHelpers.callMethod(context.getSystemService(Context.POWER_SERVICE), "goToSleep", SystemClock.uptimeMillis());
                        break;

                    case ACTION_PREFIX + "ScreenCapture":
                        context.sendBroadcast(new Intent("android.intent.action.CAPTURE_SCREENSHOT"));
                        break;

                    case ACTION_PREFIX + "OpenPowerMenu":
                        clsWMG = findClass("android.view.WindowManagerGlobal");
                        wms = XposedHelpers.callStaticMethod(clsWMG, "getWindowManagerService");
                        XposedHelpers.callMethod(wms, "showGlobalActions");
                        break;

                    case ACTION_PREFIX + "LaunchIntent":
                        Intent launchIntent = intent.getParcelableExtra("intent");
                        if (launchIntent != null) {
                            int user = 0;
                            if (launchIntent.hasExtra("user")) {
                                user = launchIntent.getIntExtra("user", 0);
                                launchIntent.removeExtra("user");
                            }
                            if (user != 0)
                                XposedHelpers.callMethod(context, "startActivityAsUser", launchIntent, XposedHelpers.newInstance(UserHandle.class, user));
                            else
                                context.startActivity(launchIntent);
                        }
                        break;
                }
            } catch (Throwable t) {
                LogUtils.log(t);
            }
        }
    };

    //RestartActions
    public void setupRestartActions() {
        hookAllMethods("com.android.server.policy.PhoneWindowManager", "init", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context mContext = (Context)XposedHelpers.getObjectField(param.thisObject, "mContext");
                IntentFilter intentfilter = new IntentFilter();
                intentfilter.addAction(ACTION_PREFIX + "RestartApps");
                intentfilter.addAction(ACTION_PREFIX + "RestartHome");
                mContext.registerReceiver(mRestartReceiver, intentfilter);
            }
        });
    }

    private static void forceStopPackage(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        XposedHelpers.callMethod(am, "forceStopPackage", packageName);
    }

    private static final BroadcastReceiver mRestartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (action == null) return;

                switch (action) {
                    case ACTION_PREFIX + "RestartApps":
                        forceStopPackage(context,intent.getStringExtra("packageName"));
                        break;

                    case ACTION_PREFIX + "RestartHome":
                        forceStopPackage(context, "com.miui.home");
                        break;
                }
            } catch (Exception e) {
                LogUtils.log(e);
            }
        }
    };




    public static boolean handleAction(Context context, String key) {
        return handleAction(context, key, false);
    }

    public static boolean handleAction(Context context, String key, boolean skipLock) {
        if (key == null || key.isEmpty()) return false;
        int action = PrefsUtils.getSharedIntPrefs(context, key + "_action", 0);
        if (action <= 0) return false;
        if (action >= 85 && action <= 88) {
            if (GlobalActions.isMediaActionsAllowed(context))
                GlobalActions.sendDownUpKeyEvent(context, action, false);
            return true;
        }
        switch (action) {
            case 1: return setAction(context,"OpenNotificationCenter");
            case 4: return setAction(context, "LockScreen");
            case 5 : return setAction(context,"GoToSleep");
            case 6: return setAction(context,"ScreenCapture");
            case 12: return setAction(context, "OpenPowerMenu");
            case 13: return launchAppIntent(context, key, skipLock);
            /*
            case 3: return expandEQS(context);
            case 4: return lockDevice(context);
            case 5: return goToSleep(context);
            case 6: return takeScreenshot(context);
            case 7: return openRecents(context);
            case 8: return launchAppIntent(context, key, skipLock);
            case 9: return launchShortcutIntent(context, key, skipLock);
            case 20: return launchActivityIntent(context, key, skipLock);
            case 10: return toggleThis(context, Helpers.getSharedIntPref(context, key + "_toggle", 0));
            case 11: return switchToPrevApp(context);
            case 12: return openPowerMenu(context);
            case 13: return clearMemory(context);
            case 14: return toggleColorInversion(context);
            case 15: return goBack(context);
            case 16: return simulateMenu(context);
            case 17: return openVolumeDialog(context);
            case 18: return volumeUp(context);
            case 19: return volumeDown(context);
            case 21: return switchKeyboard(context);
            case 22: return switchOneHandedLeft(context);
            case 23: return switchOneHandedRight(context);
            case 24: return forceClose(context);*/
            default: return false;
        }
    }


    // Actions
    public static boolean setAction(Context context, String action) {
        try {
            context.sendBroadcast(new Intent(ACTION_PREFIX + action));
            return true;
        } catch (Throwable t) {
            LogUtils.log(t);
            return false;
        }
    }

    /*public static boolean openNotificationCenter(Context context) {
        try {
            context.sendBroadcast(new Intent(ACTION_PREFIX + "ExpandNotifications"));
            return true;
        } catch (Throwable t) {
            LogUtils.log(t);
            return false;
        }
    }

    public static boolean goToSleep(Context context) {
        try {
            context.sendBroadcast(new Intent(ACTION_PREFIX + "GoToSleep"));
            return true;
        } catch (Throwable t) {
            LogUtils.log(t);
            return false;
        }
    }*/


    public static boolean isMediaActionsAllowed(Context mContext) {
        AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        boolean isMusicActive = am.isMusicActive();
        boolean isMusicActiveRemotely  = (Boolean)XposedHelpers.callMethod(am, "isMusicActiveRemotely");
        boolean isAllowed = isMusicActive || isMusicActiveRemotely;
        if (!isAllowed) {
            long mCurrentTime = currentTimeMillis();
            long mLastPauseTime = Settings.System.getLong(mContext.getContentResolver(), "last_music_paused_time", mCurrentTime);
            if (mCurrentTime - mLastPauseTime < 10 * 60 * 1000) isAllowed = true;
        }
        return isAllowed;
    }

    public static void sendDownUpKeyEvent(Context mContext, int keyCode, boolean vibrate) {
        AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));

        if (vibrate && PrefsUtils.getSharedBoolPrefs(mContext, "prefs_key_controls_volumemedia_vibrate", true));
            /*Helpers.performStrongVibration(mContext, PrefsUtils.getSharedBoolPrefs(mContext, "prefa_key_controls_volumemedia_vibrate_ignore", false));*/
    }

    public static boolean launchAppIntent(Context context, String key, boolean skipLock) {
        return launchIntent(context, getIntent(context, key, IntentType.APP, skipLock));
    }

    public static boolean launchIntent(Context context, Intent intent) {
        if (intent == null) return false;
        Intent bIntent = new Intent(ACTION_PREFIX + "LaunchIntent");
        bIntent.putExtra("intent", intent);
        context.sendBroadcast(bIntent);
        return true;
    }

    enum IntentType {
        APP, ACTIVITY, SHORTCUT
    }

    public static Intent getIntent(Context context, String prefs, IntentType intentType, boolean skipLock) {
        try {
            if (intentType == IntentType.APP) prefs += "_app";
            else if (intentType == IntentType.ACTIVITY) prefs += "_activity";
            else if (intentType == IntentType.SHORTCUT) prefs += "_shortcut_intent";

            String prefValue = PrefsUtils.getSharedStringPrefs(context, prefs, null);
            if (prefValue == null) return null;

            Intent intent = new Intent();
            if (intentType == IntentType.SHORTCUT) {
                intent = Intent.parseUri(prefValue, 0);
            } else {
                String[] pkgAppArray = prefValue.split("\\|");
                if (pkgAppArray.length < 2) return null;
                ComponentName name = new ComponentName(pkgAppArray[0], pkgAppArray[1]);
                intent.setComponent(name);
                int user = PrefsUtils.getSharedIntPrefs(context, prefs + "_user", 0);
                if (user != 0) intent.putExtra("user", user);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            if (intentType == IntentType.APP) {
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
            }

            if (skipLock) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("ShowCameraWhenLocked", true);
                intent.putExtra("StartActivityWhenLocked", true);
            }

            return intent;
        } catch (Throwable t) {
            LogUtils.log(t);
            return null;
        }
    }
}
