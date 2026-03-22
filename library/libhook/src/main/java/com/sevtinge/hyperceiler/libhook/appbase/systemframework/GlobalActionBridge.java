/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.appbase.systemframework;

import static java.lang.System.currentTimeMillis;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.provider.Settings;
import android.view.KeyEvent;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.StatusBarActionBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

public final class GlobalActionBridge {
    public static final String ACTION_TOGGLE_COLOR_INVERSION = BaseHook.ACTION_PREFIX + "ToggleColorInversion";
    public static final String ACTION_LOCK_SCREEN = BaseHook.ACTION_PREFIX + "LockScreen";
    public static final String ACTION_GO_TO_SLEEP = BaseHook.ACTION_PREFIX + "GoToSleep";
    public static final String ACTION_SCREEN_CAPTURE = BaseHook.ACTION_PREFIX + "ScreenCapture";
    public static final String ACTION_OPEN_POWER_MENU = BaseHook.ACTION_PREFIX + "OpenPowerMenu";
    public static final String ACTION_LAUNCH_INTENT = BaseHook.ACTION_PREFIX + "LaunchIntent";
    public static final String ACTION_RESTART_APPS = BaseHook.ACTION_PREFIX + "RestartApps";

    private GlobalActionBridge() {
    }

    public static boolean handleAction(Context context, String key) {
        return handleAction(context, key, false);
    }

    public static boolean handleAction(Context context, String key, boolean skipLock) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        int action = PrefsBridge.getInt(key + "_action", 0);
        if (action <= 0) {
            return false;
        }
        if (action >= 85 && action <= 88) {
            if (isMediaActionsAllowed(context)) {
                sendDownUpKeyEvent(context, action, false);
            }
            return true;
        }
        return switch (action) {
            case 1 -> StatusBarActionBridge.openNotificationCenter(context);
            case 2 -> StatusBarActionBridge.clearMemory(context);
            case 3 -> sendAction(context, "ToggleColorInversion");
            case 4 -> sendAction(context, "LockScreen");
            case 5 -> sendAction(context, "GoToSleep");
            case 6 -> sendAction(context, "ScreenCapture");
            case 7 -> StatusBarActionBridge.openRecents(context);
            case 8 -> StatusBarActionBridge.openVolumeDialog(context);
            case 12 -> sendAction(context, "OpenPowerMenu");
            case 13 -> launchAppIntent(context, key, skipLock);
            default -> false;
        };
    }

    public static boolean setAction(Context context, String actionSuffix) {
        return sendAction(context, actionSuffix);
    }

    public static boolean sendAction(Context context, String actionSuffix) {
        try {
            context.sendBroadcast(new Intent(BaseHook.ACTION_PREFIX + actionSuffix));
            return true;
        } catch (Throwable t) {
            AndroidLog.w("GlobalActionBridge", "system", "sendAction", t);
            return false;
        }
    }

    public static boolean launchAppIntent(Context context, String key, boolean skipLock) {
        return launchIntent(context, getIntent(key, IntentType.APP, skipLock));
    }

    public static boolean launchIntent(Context context, Intent intent) {
        if (intent == null) {
            return false;
        }
        Intent broadcastIntent = new Intent(ACTION_LAUNCH_INTENT);
        broadcastIntent.putExtra("intent", intent);
        context.sendBroadcast(broadcastIntent);
        return true;
    }

    public static Intent getIntent(String prefs, IntentType intentType, boolean skipLock) {
        try {
            if (intentType == IntentType.APP) {
                prefs += "_app";
            } else if (intentType == IntentType.ACTIVITY) {
                prefs += "_activity";
            } else if (intentType == IntentType.SHORTCUT) {
                prefs += "_shortcut_intent";
            }

            String prefValue = PrefsBridge.getString(prefs, null);
            if (prefValue == null) {
                return null;
            }

            Intent intent = new Intent();
            if (intentType == IntentType.SHORTCUT) {
                intent = Intent.parseUri(prefValue, 0);
            } else {
                String[] pkgAppArray = prefValue.split("\\|");
                if (pkgAppArray.length < 2) {
                    return null;
                }
                intent.setComponent(new ComponentName(pkgAppArray[0], pkgAppArray[1]));
                int user = PrefsBridge.getInt(prefs + "_user", 0);
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
            AndroidLog.w("GlobalActionBridge", "system", "getIntent", t);
            return null;
        }
    }

    public static Intent getIntent(Context context, String prefs, IntentType intentType, boolean skipLock) {
        return getIntent(prefs, intentType, skipLock);
    }

    public static boolean isMediaActionsAllowed(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        boolean isAllowed = audioManager.isMusicActive()
            || (Boolean) BaseHook.callMethod(audioManager, "isMusicActiveRemotely");
        if (!isAllowed) {
            long currentTime = currentTimeMillis();
            long lastPauseTime = Settings.System.getLong(
                context.getContentResolver(),
                "last_music_paused_time",
                currentTime
            );
            if (currentTime - lastPauseTime < 10 * 60 * 1000) {
                isAllowed = true;
            }
        }
        return isAllowed;
    }

    public static void sendDownUpKeyEvent(Context context, int keyCode, boolean vibrate) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        audioManager.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));

        if (vibrate && PrefsBridge.getBoolean("controls_volumemedia_vibrate", true)) {
            // Reserved for future vibration feedback implementation.
        }
    }

    public enum IntentType {
        APP,
        ACTIVITY,
        SHORTCUT
    }
}
