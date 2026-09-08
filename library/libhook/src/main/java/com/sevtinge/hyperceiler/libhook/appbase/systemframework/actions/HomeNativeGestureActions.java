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
package com.sevtinge.hyperceiler.libhook.appbase.systemframework.actions;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findField;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findMethodExactIfExists;
import static io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClass;
import static io.github.kyuubiran.ezxhelper.core.util.ClassUtil.loadClassOrNull;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sevtinge.hyperceiler.common.log.AndroidLog;

public final class HomeNativeGestureActions {
    private static final String TAG = "HomeNativeGestureActions";
    private static final String XIAOAI_PACKAGE = "com.miui.voiceassist";
    private static final String XIAOAI_SERVICE = "com.xiaomi.voiceassistant.VoiceService";
    private static final String MIUI_TOGGLE_ACTION = "com.miui.app.ExtraStatusBarManager.action_TRIGGER_TOGGLE";
    private static final String MIUI_TOGGLE_EXTRA_ID = "com.miui.app.ExtraStatusBarManager.extra_TOGGLE_ID";
    private static final String ENTITY_VOICE_ASSISTANT = "VoiceAssistant";
    private static final String ENTITY_VOICE_ASSISTANT_SCREEN_RECOGNIZER = "VoiceAssistantScreenRecognizer";
    private static final String MIUI_HOME_TRIGGER_FROM = "MiuiHome";
    private static final String NAV_LONG_PRESS = "NavLongPress";
    private static final String HOME_SEARCH = "home_search";
    private static final String START_SCREEN_RECOGNITION = "start_screen_recognition";
    private static final String LONG_PRESS_FULLSCREEN_GESTURE_LINE = "long_press_fullscreen_gesture_line";
    private static final int MIUI_TOGGLE_LOCK_SCREEN = 10;
    private static final int CTS_ENTRY_POINT_GESTURE = 1;
    private static final int CTS_SOURCE_FLAG = 7;
    private static final String CTS_CALLER = "hyperOS_home";

    private HomeNativeGestureActions() {
    }

    public static boolean lockScreen(Context context) {
        try {
            context.sendBroadcast(new Intent(MIUI_TOGGLE_ACTION).putExtra(MIUI_TOGGLE_EXTRA_ID, MIUI_TOGGLE_LOCK_SCREEN));
            return true;
        } catch (Throwable t) {
            AndroidLog.w(TAG, "system", "lockScreen", t);
            return false;
        }
    }

    public static boolean launchSuperXiaoAi(Context context) {
        return launchVoiceAssistant(context, ENTITY_VOICE_ASSISTANT);
    }

    public static boolean launchSuperXiaoAiScreenRecognizer(Context context) {
        return launchVoiceAssistant(context, ENTITY_VOICE_ASSISTANT_SCREEN_RECOGNIZER);
    }

    public static boolean launchGoogleVoiceAssistant(Context context) {
        try {
            performShortHaptic(context);
            Object wrapper = getAssistantProxy(context);
            if (wrapper == null) {
                return false;
            }

            Bundle bundle = new Bundle();
            bundle.putInt("triggered_by", 83);
            bundle.putInt("invocation_type", 1);
            findMethodExactIfExists(wrapper.getClass(), "onAssistantGestureCompletion").invoke(wrapper);
            findMethodExactIfExists(wrapper.getClass(), "startAssistant", Bundle.class).invoke(wrapper, bundle);
            return true;
        } catch (Throwable t) {
            AndroidLog.w(TAG, "google", "launchGoogleVoiceAssistant", t);
            return false;
        }
    }

    public static boolean launchGoogleCircleToSearchFromHome(Context context) {
        try {
            performShortHaptic(context);
            Context homeContext = resolveHomeApplicationContext(context);
            ClassLoader classLoader = homeContext.getClassLoader();
            Class<?> helperClass = loadClassOrNull("com.miui.home.recents.cts.CircleToSearchHelper", classLoader);
            if (helperClass == null) {
                Class<?> starterClass = loadClass("com.miui.home.recents.cts.CircleToSearchStarter", classLoader);
                return (Boolean) findMethodExactIfExists(starterClass, "invokeOmni", Context.class, int.class)
                    .invoke(null, homeContext, CTS_ENTRY_POINT_GESTURE);
            }

            try {
                return (Boolean) findMethodExactIfExists(helperClass, "invokeCircleToSearch", Context.class, long.class, int.class)
                    .invoke(null, homeContext, 0L, CTS_ENTRY_POINT_GESTURE);
            } catch (Throwable ignored) {
                return (Boolean) findMethodExactIfExists(helperClass, "invokeCircleToSearch", Context.class, int.class, int.class)
                    .invoke(null, homeContext, 0, CTS_ENTRY_POINT_GESTURE);
            }
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean triggerCircleToSearchViaVoiceInteraction() {
        try {
            Bundle bundle = new Bundle();
            bundle.putLong("invocation_time_ms", SystemClock.elapsedRealtime());
            bundle.putInt("omni.entry_point", CTS_ENTRY_POINT_GESTURE);

            Class<?> serviceManagerClass = loadClass("android.os.ServiceManager", null);
            IBinder binder = (IBinder) findMethodExactIfExists(serviceManagerClass, "getService", String.class)
                .invoke(null, "voiceinteraction");
            if (binder == null) {
                return false;
            }

            Class<?> stubClass = loadClass("com.android.internal.app.IVoiceInteractionManagerService$Stub", null);
            Object service = findMethodExactIfExists(stubClass, "asInterface", IBinder.class).invoke(null, binder);
            if (service == null) {
                return false;
            }

            try {
                Object result = service.getClass().getMethod(
                    "showSessionFromSession",
                    IBinder.class,
                    Bundle.class,
                    int.class,
                    String.class
                ).invoke(service, null, bundle, CTS_SOURCE_FLAG, CTS_CALLER);
                return !(result instanceof Boolean) || (Boolean) result;
            } catch (NoSuchMethodException ignored) {
                Object result = service.getClass().getMethod(
                    "showSessionFromSession",
                    IBinder.class,
                    Bundle.class,
                    int.class
                ).invoke(service, null, bundle, CTS_SOURCE_FLAG);
                return !(result instanceof Boolean) || (Boolean) result;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean launchVoiceAssistant(Context context, String entityType) {
        try {
            Intent intent = createVoiceAssistantIntent(context, entityType);
            resolveHomeApplicationContext(context).startForegroundService(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            AndroidLog.w(TAG, XIAOAI_PACKAGE, "launchVoiceAssistant", e);
            return false;
        } catch (Throwable t) {
            AndroidLog.w(TAG, XIAOAI_PACKAGE, "launchVoiceAssistant", t);
            return false;
        }
    }

    private static Intent createVoiceAssistantIntent(Context context, String entityType) {
        Intent intent = new Intent(Intent.ACTION_ASSIST);
        VoiceAssistantConfig config = getVoiceAssistantConfig(context);
        applyVoiceAssistantComponent(intent, config);

        if (ENTITY_VOICE_ASSISTANT_SCREEN_RECOGNIZER.equals(entityType)) {
            intent.putExtra("triggerType", NAV_LONG_PRESS);
            intent.putExtra("triggerFrom", MIUI_HOME_TRIGGER_FROM);
            applyVoiceAssistantStartFrom(intent, config, LONG_PRESS_FULLSCREEN_GESTURE_LINE);
            intent.putExtra("voice_assist_function_key", START_SCREEN_RECOGNITION);
        } else {
            applyVoiceAssistantStartFrom(intent, config, HOME_SEARCH);
            intent.putExtra("from", "large");
        }
        return intent;
    }

    private static void applyVoiceAssistantComponent(Intent intent, VoiceAssistantConfig config) {
        if (config != null && config.pkgName != null && config.clazzName != null) {
            intent.setClassName(config.pkgName, config.clazzName);
            return;
        }
        intent.setComponent(new ComponentName(XIAOAI_PACKAGE, XIAOAI_SERVICE));
    }

    private static void applyVoiceAssistantStartFrom(Intent intent, VoiceAssistantConfig config, String fromKey) {
        if (fromKey == null || fromKey.isEmpty()) {
            return;
        }
        String startFromKey = config != null ? config.startFromKey : null;
        if (startFromKey == null || startFromKey.isEmpty()) {
            startFromKey = "voice_assist_start_from_key";
        }
        intent.putExtra(startFromKey, fromKey);
    }

    private static VoiceAssistantConfig getVoiceAssistantConfig(Context context) {
        try {
            String config = Settings.Secure.getString(context.getContentResolver(), "entity_config_key_voice_assistant");
            if (config == null || config.isEmpty()) {
                return null;
            }
            JsonObject json = new Gson().fromJson(config, JsonObject.class);
            if (json == null) {
                return null;
            }

            VoiceAssistantConfig result = new VoiceAssistantConfig();
            if (json.has("pkgName") && !json.get("pkgName").isJsonNull()) {
                result.pkgName = json.get("pkgName").getAsString();
            }
            if (json.has("clazzName") && !json.get("clazzName").isJsonNull()) {
                result.clazzName = json.get("clazzName").getAsString();
            }
            if (json.has("startFromKey") && !json.get("startFromKey").isJsonNull()) {
                result.startFromKey = json.get("startFromKey").getAsString();
            }
            return result;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Context resolveHomeApplicationContext(Context context) {
        try {
            ClassLoader classLoader = context.getClassLoader();
            Class<?> applicationClass = loadClass("com.miui.home.launcher.Application", classLoader);
            Object application = findMethodExactIfExists(applicationClass, "getInstance").invoke(null);
            if (application instanceof Context applicationContext) {
                return applicationContext.getApplicationContext();
            }
        } catch (Throwable t) {
        }
        return context.getApplicationContext();
    }

    private static Object getAssistantProxy(Context context) {
        try {
            Context homeContext = resolveHomeApplicationContext(context);
            ClassLoader classLoader = homeContext.getClassLoader();

            Class<?> miuiWrapperClass = loadClassOrNull("com.miui.home.recents.MiuiSystemUiProxyWrapper", classLoader);
            if (miuiWrapperClass != null) {
                Object instance = findField(miuiWrapperClass, "INSTANCE").get(null);
                if (instance != null) {
                    return instance;
                }
            }

            Class<?> wrapperClass = loadClassOrNull("com.miui.home.recents.SystemUiProxyWrapper", classLoader);
            if (wrapperClass == null) {
                return null;
            }

            Object instanceHolder = findField(wrapperClass, "INSTANCE").get(null);
            if (instanceHolder == null) {
                return null;
            }
            if (wrapperClass.isInstance(instanceHolder)) {
                return instanceHolder;
            }
            return findMethodExactIfExists(instanceHolder.getClass(), "getNoCreate").invoke(instanceHolder);
        } catch (Throwable t) {
            return null;
        }
    }

    @SuppressLint("MissingPermission")
    private static void performShortHaptic(Context context) {
        try {
            Vibrator vibrator = context.getSystemService(Vibrator.class);
            if (vibrator == null || !vibrator.hasVibrator()) {
                return;
            }
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
        } catch (Throwable t) {
        }
    }

    private static final class VoiceAssistantConfig {
        String pkgName;
        String clazzName;
        String startFromKey;
    }
}
