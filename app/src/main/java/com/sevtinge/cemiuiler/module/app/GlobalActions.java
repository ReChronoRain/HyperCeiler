package com.sevtinge.cemiuiler.module.app;

import static com.sevtinge.cemiuiler.utils.log.AndroidLogUtils.LogD;
import static com.sevtinge.cemiuiler.utils.log.AndroidLogUtils.LogE;
import static java.lang.System.currentTimeMillis;

import android.annotation.SuppressLint;
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
import com.sevtinge.cemiuiler.utils.Helpers;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import de.robv.android.xposed.XposedHelpers;

@SuppressLint("UnspecifiedRegisterReceiverFlag")
public class GlobalActions extends BaseHook {


    @Override
    public void init() {
        setupGlobalActions();
        setupRestartActions();
    }

    // GlobalActions
    public void setupGlobalActions() {
        hookAllConstructors("com.android.server.accessibility.AccessibilityManagerService", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                Context mGlobalContext = (Context) param.args[0];
                IntentFilter mFilter = new IntentFilter();
                // Actions
                mFilter.addAction(ACTION_PREFIX + "ToggleColorInversion");
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

    @SuppressLint("UnsafeIntentLaunch")
    private final BroadcastReceiver mGlobalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Class<?> clsWMG = findClass("android.view.WindowManagerGlobal", null);
                Object wms = XposedHelpers.callStaticMethod(clsWMG, "getWindowManagerService");

                String action = intent.getAction();

                switch (action) {
                    case ACTION_PREFIX + "ToggleColorInversion" -> {
                        int opt = Settings.Secure.getInt(context.getContentResolver(), "accessibility_display_inversion_enabled");
                        int conflictProp = (int) Helpers.proxySystemProperties("getInt", "ro.df.effect.conflict", 0, null);
                        int conflictProp2 = (int) Helpers.proxySystemProperties("getInt", "ro.vendor.df.effect.conflict", 0, null);
                        boolean hasConflict = conflictProp == 1 || conflictProp2 == 1;
                        Object dfMgr = XposedHelpers.callStaticMethod(XposedHelpers.findClass("miui.hardware.display.DisplayFeatureManager", null), "getInstance");
                        if (hasConflict && opt == 0) {
                            XposedHelpers.callMethod(dfMgr, "setScreenEffect", 15, 1);
                        }
                        Settings.Secure.putInt(context.getContentResolver(), "accessibility_display_inversion_enabled", opt == 0 ? 1 : 0);
                        if (hasConflict && opt != 0) {
                            XposedHelpers.callMethod(dfMgr, "setScreenEffect", 15, 0);
                        }
                    }

                    case ACTION_PREFIX + "LockScreen" -> {
                        XposedHelpers.callMethod(context.getSystemService(Context.POWER_SERVICE), "goToSleep", SystemClock.uptimeMillis());
                        XposedHelpers.callMethod(wms, "lockNow", (Object) null);
                    }
                    case ACTION_PREFIX + "GoToSleep" ->
                        XposedHelpers.callMethod(context.getSystemService(Context.POWER_SERVICE), "goToSleep", SystemClock.uptimeMillis());

                    case ACTION_PREFIX + "ScreenCapture" ->
                        context.sendBroadcast(new Intent("android.intent.action.CAPTURE_SCREENSHOT"));

                    case ACTION_PREFIX + "OpenPowerMenu" -> {
                        clsWMG = findClass("android.view.WindowManagerGlobal");
                        wms = XposedHelpers.callStaticMethod(clsWMG, "getWindowManagerService");
                        XposedHelpers.callMethod(wms, "showGlobalActions");
                    }

                    case ACTION_PREFIX + "LaunchIntent" -> {
                        Intent launchIntent = intent.getParcelableExtra("intent");
                        if (launchIntent != null) {
                            int user = 0;
                            if (launchIntent.hasExtra("user")) {
                                user = launchIntent.getIntExtra("user", 0);
                                launchIntent.removeExtra("user");
                            }
                            if (user != 0) {
                                XposedHelpers.callMethod(context, "startActivityAsUser", launchIntent, XposedHelpers.newInstance(UserHandle.class, user));
                            } else {
                                context.startActivity(launchIntent);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                LogD(TAG, "onReceive", t);
            }
        }
    };

    /**
     * RestartActions
     */
    public void setupRestartActions() {
        hookAllMethods("com.android.server.policy.PhoneWindowManager", "init", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                IntentFilter intentfilter = new IntentFilter();
                intentfilter.addAction(ACTION_PREFIX + "RestartApps");
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
                if (action == null) {
                    return;
                }

                if ((ACTION_PREFIX + "RestartApps").equals(action)) {
                    forceStopPackage(context, intent.getStringExtra("packageName"));
                }
            } catch (Exception e) {
                LogE("GlobalActions", null, e);
            }
        }
    };


    public static boolean handleAction(Context context, String key) {
        return handleAction(context, key, false);
    }

    public static boolean handleAction(Context context, String key, boolean skipLock) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        int action = PrefsUtils.getSharedIntPrefs(context, key + "_action", 0);
        if (action <= 0) {
            return false;
        }
        if (action >= 85 && action <= 88) {
            if (GlobalActions.isMediaActionsAllowed(context)) {
                GlobalActions.sendDownUpKeyEvent(context, action, false);
            }
            return true;
        }
        return switch (action) {
            case 1 -> setAction(context, "OpenNotificationCenter");
            case 2 -> setAction(context, "ClearMemory");
            case 3 -> setAction(context, "ToggleColorInversion");
            case 4 -> setAction(context, "LockScreen");
            case 5 -> setAction(context, "GoToSleep");
            case 6 -> setAction(context, "ScreenCapture");
            case 7 -> setAction(context, "OpenRecents");
            case 8 -> setAction(context, "OpenVolumeDialog");
            case 12 -> setAction(context, "OpenPowerMenu");
            case 13 -> launchAppIntent(context, key, skipLock);
            /*
            case 3: return expandEQS(context);
            case 6: return takeScreenshot(context);
            case 7: return openRecents(context);
            case 8: return launchAppIntent(context, key, skipLock);
            case 9: return launchShortcutIntent(context, key, skipLock);
            case 20: return launchActivityIntent(context, key, skipLock);
            case 10: return toggleThis(context, Helpers.getSharedIntPref(context, key + "_toggle", 0));
            case 11: return switchToPrevApp(context);
            case 12: return openPowerMenu(context);
            case 15: return goBack(context);
            case 16: return simulateMenu(context);
            case 18: return volumeUp(context);
            case 19: return volumeDown(context);
            case 21: return switchKeyboard(context);
            case 22: return switchOneHandedLeft(context);
            case 23: return switchOneHandedRight(context);
            case 24: return forceClose(context);*/
            default -> false;
        };
    }


    // Actions
    public static boolean setAction(Context context, String action) {
        try {
            context.sendBroadcast(new Intent(ACTION_PREFIX + action));
            return true;
        } catch (Throwable t) {
            LogD("GlobalActions", "setAction", t);
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
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        boolean isMusicActive = am.isMusicActive();
        boolean isMusicActiveRemotely = (Boolean) XposedHelpers.callMethod(am, "isMusicActiveRemotely");
        boolean isAllowed = isMusicActive || isMusicActiveRemotely;
        if (!isAllowed) {
            long mCurrentTime = currentTimeMillis();
            long mLastPauseTime = Settings.System.getLong(mContext.getContentResolver(), "last_music_paused_time", mCurrentTime);
            if (mCurrentTime - mLastPauseTime < 10 * 60 * 1000) {
                isAllowed = true;
            }
        }
        return isAllowed;
    }

    public static void sendDownUpKeyEvent(Context mContext, int keyCode, boolean vibrate) {
        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        am.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));

        if (vibrate && PrefsUtils.getSharedBoolPrefs(mContext, "prefs_key_controls_volumemedia_vibrate", true)) ;
        /*Helpers.performStrongVibration(mContext, PrefsUtils.getSharedBoolPrefs(mContext, "prefa_key_controls_volumemedia_vibrate_ignore", false));*/
    }

    public static boolean launchAppIntent(Context context, String key, boolean skipLock) {
        return launchIntent(context, getIntent(context, key, IntentType.APP, skipLock));
    }

    public static boolean launchIntent(Context context, Intent intent) {
        if (intent == null) {
            return false;
        }
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
            if (intentType == IntentType.APP) {
                prefs += "_app";
            } else if (intentType == IntentType.ACTIVITY) {
                prefs += "_activity";
            } else if (intentType == IntentType.SHORTCUT) {
                prefs += "_shortcut_intent";
            }

            String prefValue = PrefsUtils.getSharedStringPrefs(context, prefs, null);
            if (prefValue == null) return null;

            Intent intent = new Intent();
            if (intentType == IntentType.SHORTCUT) {
                intent = Intent.parseUri(prefValue, 0);
            } else {
                String[] pkgAppArray = prefValue.split("\\|");
                if (pkgAppArray.length < 2) {
                    return null;
                }
                ComponentName name = new ComponentName(pkgAppArray[0], pkgAppArray[1]);
                intent.setComponent(name);
                int user = PrefsUtils.getSharedIntPrefs(context, prefs + "_user", 0);
                if (user != 0) {
                    intent.putExtra("user", user);
                }
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
            LogD("GlobalActions", "getIntent", t);
            return null;
        }
    }
}
