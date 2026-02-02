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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.moduleload;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.newInstance;
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

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

// android
@SuppressLint("UnspecifiedRegisterReceiverFlag")
public class GlobalActions extends BaseHook {

    @Override
    public void init() {
        setupGlobalActions();
        setupRestartActions();
    }

    // 统一常量管理
    static final class GlobalActionConstants {
        static final String ACTION_TOGGLE_COLOR_INVERSION = ACTION_PREFIX + "ToggleColorInversion";
        static final String ACTION_LOCK_SCREEN = ACTION_PREFIX + "LockScreen";
        static final String ACTION_GO_TO_SLEEP = ACTION_PREFIX + "GoToSleep";
        static final String ACTION_SCREEN_CAPTURE = ACTION_PREFIX + "ScreenCapture";
        static final String ACTION_OPEN_POWER_MENU = ACTION_PREFIX + "OpenPowerMenu";
        static final String ACTION_LAUNCH_INTENT = ACTION_PREFIX + "LaunchIntent";
        static final String ACTION_RESTART_APPS = ACTION_PREFIX + "RestartApps";
        private GlobalActionConstants() {}
    }

    // GlobalActions
    public void setupGlobalActions() {
        hookAllConstructors("com.android.server.accessibility.AccessibilityManagerService", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Context mGlobalContext = (Context) param.getArgs()[0];
                IntentFilter mFilter = new IntentFilter();
                // Actions
                mFilter.addAction(GlobalActionConstants.ACTION_TOGGLE_COLOR_INVERSION);
                mFilter.addAction(GlobalActionConstants.ACTION_LOCK_SCREEN);
                mFilter.addAction(GlobalActionConstants.ACTION_GO_TO_SLEEP);
                mFilter.addAction(GlobalActionConstants.ACTION_SCREEN_CAPTURE);
                mFilter.addAction(GlobalActionConstants.ACTION_OPEN_POWER_MENU);
                mFilter.addAction(GlobalActionConstants.ACTION_LAUNCH_INTENT);
                mGlobalContext.registerReceiver(mGlobalReceiver, mFilter, Context.RECEIVER_EXPORTED);
            }
        });
    }

    @SuppressLint("UnsafeIntentLaunch")
    private final BroadcastReceiver mGlobalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (action == null) return;

                Class<?> clsWMG = findClass("android.view.WindowManagerGlobal", null);
                Object wms = callStaticMethod(clsWMG, "getWindowManagerService");

                if (GlobalActionConstants.ACTION_TOGGLE_COLOR_INVERSION.equals(action)) {
                    handleToggleColorInversion(context);
                } else if (GlobalActionConstants.ACTION_LOCK_SCREEN.equals(action)) {
                    callMethod(context.getSystemService(Context.POWER_SERVICE), "goToSleep", SystemClock.uptimeMillis());
                    callMethod(wms, "lockNow", (Object) null);
                } else if (GlobalActionConstants.ACTION_GO_TO_SLEEP.equals(action)) {
                    callMethod(context.getSystemService(Context.POWER_SERVICE), "goToSleep", SystemClock.uptimeMillis());
                } else if (GlobalActionConstants.ACTION_SCREEN_CAPTURE.equals(action)) {
                    context.sendBroadcast(new Intent("android.intent.action.CAPTURE_SCREENSHOT"));
                } else if (GlobalActionConstants.ACTION_OPEN_POWER_MENU.equals(action)) {
                    callMethod(wms, "showGlobalActions");
                } else if (GlobalActionConstants.ACTION_LAUNCH_INTENT.equals(action)) {
                    handleLaunchIntent(context, intent);
                }
            } catch (Throwable t) {
                AndroidLog.w(TAG, "system", "onReceive", t);
            }
        }
    };

    private void handleToggleColorInversion(Context context) {
        int opt = Settings.Secure.getInt(context.getContentResolver(), "accessibility_display_inversion_enabled", 0);
        int conflictProp = (int) proxySystemProperties("getInt", "ro.df.effect.conflict", 0, null);
        int conflictProp2 = (int) proxySystemProperties("getInt", "ro.vendor.df.effect.conflict", 0, null);
        boolean hasConflict = conflictProp == 1 || conflictProp2 == 1;
        Object dfMgr = callStaticMethod(findClass("miui.hardware.display.DisplayFeatureManager", null), "getInstance");
        if (hasConflict && opt == 0) {
            callMethod(dfMgr, "setScreenEffect", 15,1);
        }Settings.Secure.putInt(context.getContentResolver(), "accessibility_display_inversion_enabled", opt == 0 ? 1 : 0);if (hasConflict && opt != 0) {
            callMethod(dfMgr, "setScreenEffect", 15, 0);
        }
    }

    private void handleLaunchIntent(Context context, Intent intent) {
        Intent launchIntent = intent.getParcelableExtra("intent", Intent.class);
        if (launchIntent == null) return;

        int user = 0;
        if (launchIntent.hasExtra("user")) {
            user = launchIntent.getIntExtra("user", 0);
            launchIntent.removeExtra("user");
        }

        if (user != 0) {
            callMethod(context, "startActivityAsUser", launchIntent, newInstance(UserHandle.class, user));
        } else {
            context.startActivity(launchIntent);
        }
    }


    /**
     * RestartActions
     */
    public void setupRestartActions() {
        hookAllMethods("com.android.server.policy.PhoneWindowManager", "init", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                IntentFilter intentfilter = new IntentFilter();
                intentfilter.addAction(GlobalActionConstants.ACTION_RESTART_APPS);
                mContext.registerReceiver(mRestartReceiver, intentfilter, Context.RECEIVER_NOT_EXPORTED);
            }
        });
    }

    private static void forceStopPackage(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        callMethod(am, "forceStopPackage", packageName);
    }

    private static final BroadcastReceiver mRestartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                if (action == null) return;

                if (GlobalActionConstants.ACTION_RESTART_APPS.equals(action)) {
                    forceStopPackage(context, intent.getStringExtra("packageName"));
                }
            } catch (Exception e) {
                AndroidLog.e("GlobalActions", "system", null, e);
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
        int action = mPrefsMap.getInt(key + "_action", 0);
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
            AndroidLog.w("GlobalActions", "system", "setAction", t);
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
        boolean isMusicActiveRemotely = (Boolean) callMethod(am, "isMusicActiveRemotely");
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

        if (vibrate && mPrefsMap.getBoolean("controls_volumemedia_vibrate", true)) ;
        /*Helpers.performStrongVibration(mContext, PrefsUtils.getSharedBoolPrefs(mContext, "prefa_key_controls_volumemedia_vibrate_ignore", false));*/
    }

    public static boolean launchAppIntent(Context context, String key, boolean skipLock) {
        return launchIntent(context, getIntent(context, key, IntentType.APP, skipLock));
    }

    public static boolean launchIntent(Context context, Intent intent) {
        if (intent == null) {
            return false;
        }
        Intent bIntent = new Intent(GlobalActionConstants.ACTION_LAUNCH_INTENT);
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

            String prefValue = mPrefsMap.getString(prefs, null);
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
                int user = mPrefsMap.getInt(prefs + "_user", 0);
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
            AndroidLog.w("GlobalActions", "system", "getIntent", t);
            return null;
        }
    }
}
