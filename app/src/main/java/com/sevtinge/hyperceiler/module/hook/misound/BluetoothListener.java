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
package com.sevtinge.hyperceiler.module.hook.misound;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.app.Application;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothLeAudio;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

import de.robv.android.xposed.XposedHelpers;

public class BluetoothListener extends BaseHook {
    private static final String TAG = "BluetoothListener";
    private static Object miDolby = null;
    private static Object miAudio = null;
    private static String uuid = "";
    private String mode = null;

    @Override
    public void init() throws NoSuchMethodException {
        uuid = mPrefsMap.getString("misound_bluetooth_uuid", "");
        Class<?> clazz1 = (Class<?>) DexKit.getDexKitBridge("CreateDolbyAudioEffectClazz", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData clazzData = bridge.findClass(FindClass.create()
                        .matcher(ClassMatcher.create().usingStrings("Creating a DolbyAudioEffect to global output mix!"))).singleOrNull();
                return clazzData.getInstance(lpparam.classLoader);
            }
        });
        if (clazz1 == null) {
            logE(TAG, "AudioEffect not found");
        } else {
            findAndHookConstructor(clazz1,
                    int.class, int.class,
                    new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) {
                            miDolby = param.thisObject;
                            // logD(TAG, "miDolby: " + miDolby);
                        }
                    }
            );
        }
        Class<?> clazz2 = (Class<?>) DexKit.getDexKitBridge("MiSoundClazz", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                ClassData clazzData = bridge.findClass(FindClass.create()
                        .matcher(ClassMatcher.create()
                                .usingStrings("android.media.audiofx.MiSound"))).singleOrNull();
                return clazzData.getInstance(lpparam.classLoader);
            }
        });
        if (clazz2 == null) {
            logE(TAG, "MiSound not found");
        } else {
            Field field = (Field) DexKit.getDexKitBridge("Field", new IDexKit() {
                @Override
                public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    FieldData fieldData = bridge.findField(FindField.create()
                            .matcher(FieldMatcher.create()
                                    .declaredClass(clazz2).type(Object.class)
                            )).singleOrNull();
                    return fieldData.getFieldInstance(lpparam.classLoader);
                }
            });
            if (field == null) {
                logE(TAG, "field not found");
            } else {
                String name = field.getName();
                findAndHookConstructor(clazz2,
                        int.class, int.class,
                        new MethodHook() {
                            @Override
                            protected void after(MethodHookParam param) {
                                miAudio = XposedHelpers.getObjectField(param.thisObject, name);
                                // logD(TAG, "miAudio: " + miAudio);
                            }
                        }
                );
            }
        }
        Method method1 = (Method) DexKit.getDexKitBridge("GetEnabledEffect", new IDexKit() {
            @Override
            public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .declaredClass(
                                        ClassMatcher.create()
                                                .usingStrings("getEnabledEffect(): both of enabled, force return misound")
                                )
                                .usingStrings("getEnabledEffect(): both of enabled, force return misound")
                        )).singleOrNull();
                return methodData.getMethodInstance(lpparam.classLoader);
            }
        });
        try {
            if (method1 == null) {
                logE(TAG, "null");
            } else {
                hookMethod(method1,
                        new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) {
                                Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getActivity");
                                boolean isBluetoothA2dpOn = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).isBluetoothA2dpOn();
                                boolean isWiredHeadsetOn = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).isWiredHeadsetOn();
                                if (isBluetoothA2dpOn || isWiredHeadsetOn) {
                                    if (mode == null) mode = "none";
                                }
                                if (mode != null) {
                                    param.setResult(mode);
                                }
                            }
                        }
                );
                findAndHookMethod(method1.getDeclaringClass(), "onPreferenceChange",
                        "androidx.preference.Preference", Object.class,
                        new MethodHook() {
                            @Override
                            protected void before(MethodHookParam param) {
                                Object o = param.args[1];
                                if (o instanceof String) {
                                    if ("none".equals(o) || "dolby".equals(o) || "misound".equals(o))
                                        mode = (String) o;
                                }
                            }
                        }
                );
                Method method2 = (Method) DexKit.getDexKitBridge("RefreshEffectSelectionEnabled", new IDexKit() {
                    @Override
                    public AnnotatedElement dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                        MethodData methodData = bridge.findMethod(FindMethod.create()
                                .matcher(MethodMatcher.create().declaredClass(method1.getDeclaringClass())
                                        .usingStrings("refreshEffectSelectionEnabled(): currEffect "))).singleOrNull();
                        return methodData.getMethodInstance(lpparam.classLoader);
                    }
                });
                hookMethod(method2, new MethodHook() {
                            @Override
                            protected void after(MethodHookParam param) {
                                mode = null;
                            }
                        }
                );
            }
        } catch (Throwable e) {
            logE(TAG, e);
        }
        findAndHookMethod("com.miui.misound.MiSoundApplication", "onCreate",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        Application application = (Application) param.thisObject;
                        IntentFilter intentFilter = new IntentFilter();
                        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
                        intentFilter.addAction(BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED);
                        intentFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);
                        application.registerReceiver(new Listener(), intentFilter);
                    }
                }
        );
    }

    private static String effectImplementer(Context context) {
        return Settings.Global.getString(context.getContentResolver(), "effect_implementer");
    }

    private static Object getAudioEffect() {
        Class<?> AudioEffect = XposedHelpers.findClassIfExists("android.media.audiofx.AudioEffect", ClassLoader.getSystemClassLoader());
        if (AudioEffect == null) return null;
        return getAudio(AudioEffect);
    }

    private static Object getMiSound() {
        Class<?> MiSound = XposedHelpers.findClassIfExists("android.media.audiofx.MiSound", ClassLoader.getSystemClassLoader());
        if (MiSound == null) return null;
        return XposedHelpers.newInstance(MiSound, 1, 0);
    }

    public static Object getSpatializer(Context context) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) return null;
            return AudioManager.class.getMethod("getSpatializer").invoke(audioManager);
        } catch (Exception e) {
            logE(TAG, "Failed to get Spatializer", e);
            return null;
        }
    }

    private static boolean hasControl(Object o) {
        return (boolean) XposedHelpers.callMethod(o, "hasControl");
    }

    private static boolean isEnable(Object o) {
        if (o.getClass().getName().equals("android.media.Spatializer"))
            return (boolean) XposedHelpers.callMethod(o, "isEnabled");
        else return (boolean) XposedHelpers.callMethod(o, "getEnabled");
    }

    private static void setEnable(Object o, boolean value) {
        XposedHelpers.callMethod(o, "setEnabled", value);
    }

    @NonNull
    private static Object getAudio(Class<?> AudioEffect) {
        // Class<?> DolbyAudioEffectHelper = findClassIfExists("com.android.server.audio.dolbyeffect.DolbyEffectController$DolbyAudioEffectHelper",
        //         ClassLoader.getSystemClassLoader());
        // logE(TAG, "DolbyAudioEffectHelper: " + DolbyAudioEffectHelper);
        // UUID dolby = (UUID) XposedHelpers.getStaticObjectField(
        //         DolbyAudioEffectHelper, "EFFECT_TYPE_DOLBY_AUDIO_PROCESSING");
        UUID EFFECT_TYPE_NULL = (UUID) XposedHelpers.getStaticObjectField(AudioEffect, "EFFECT_TYPE_NULL");
        UUID dolby;
        if (uuid.isEmpty()) {
            dolby = UUID.fromString("9d4921da-8225-4f29-aefa-39537a04bcaa");
        } else {
            dolby = UUID.fromString(uuid);
        }
        return XposedHelpers.newInstance(AudioEffect, EFFECT_TYPE_NULL, dolby, 0, 0);
    }

    public static class Listener extends BroadcastReceiver {
        private static Object AudioEffect = null;
        private static Object MiSound = null;
        private static Object Spatializer = null;
        private static boolean lastDolby;
        private static boolean lastMiui;
        private static boolean lastSpatial;
        private static boolean isInitialized = false;
        private static boolean isLeAudioConnected = false;
        private static boolean isForceSpatialOn = mPrefsMap.getBoolean("misound_spatial");

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                logD(TAG, "action: " + action);
                init(context);
                switch (action) {
                    case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        int state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
                        logD(TAG, "state: " + state);
                        switch (state) {
                            case 0 -> over(context);
                            case 2 -> on(context);
                        }
                    }
                    case BluetoothLeAudio.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED -> {
                        int state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
                        logD(TAG, "state: " + state);
                        if (isLeAudioConnected) {
                            isLeAudioConnected = false;
                        } else {
                            switch (state) {
                                case 0 -> over(context);
                                case 2 -> on(context);
                            }
                            isLeAudioConnected = true;
                        }
                    }
                    case AudioManager.ACTION_HEADSET_PLUG -> {
                        if (intent.hasExtra("state")) {
                            int state = intent.getIntExtra("state", 0);
                            logD(TAG, "state: " + state);
                            switch (state) {
                                case 0 -> {
                                    if (isInitialized) {
                                        over(context);
                                    } else {
                                        // 用于修复音质音效在第一次连接蓝牙耳机时发送错误广播的问题
                                        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                                        if (audioManager.isBluetoothA2dpOn()) {
                                            on(context);
                                        }
                                        isInitialized = true;
                                    }
                                }
                                case 1 -> on(context);
                            }
                        }
                    }
                }
            }
        }

        private void on(Context context) {
            init(context);
            lastDolby = setAudio(AudioEffect, miDolby);
            lastMiui = setAudio(MiSound, miAudio);
            lastSpatial = isEnable(Spatializer);
            String implementer = effectImplementer(context);
            if (implementer != null) {
                if ("dolby".equals(implementer)) {
                    lastDolby = true;
                    lastMiui = false;
                } else if ("misound".equals(implementer)) {
                    lastDolby = false;
                    lastMiui = true;
                }
            }
            setEnable(Spatializer, false);
            refresh(context, false, false, false);
        }

        private void over(Context context) {
            init(context);
            recoveryAudio(AudioEffect, miDolby, lastDolby);
            recoveryAudio(MiSound, miAudio, lastMiui);
            if (isForceSpatialOn) {
                setEnable(Spatializer, true);
            } else {
                setEnable(Spatializer, lastSpatial);
            }
            refresh(context, lastDolby, lastMiui, lastSpatial);
        }

        private void refresh(Context context, boolean dolby, boolean miui, boolean spatial) {
            Intent intent = new Intent();
            intent.setAction(isMoreAndroidVersion(35) ? "miui.intent.action.ACTION_SYSTEM_UI_DOLBY_EFFECT_SWITCH" : "miui.intent.action.ACTION_AUDIO_EFFECT_REFRESH");
            intent.setPackage("com.miui.misound");
            intent.putExtra("dolby_active", dolby);
            intent.putExtra("misound_active", miui);
            intent.putExtra("spatial_active", spatial);
            context.sendBroadcast(intent);
            logD(TAG, " dolby: " + dolby + " miui: " + miui + " spatial: " + spatial);
        }

        private static boolean setAudio(Object audio, Object otherAudio) {
            boolean last;
            if (audio != null) {
                if (hasControl(audio)) {
                    last = isEnable(audio);
                    setEnable(audio, false);
                    return last;
                } else if (otherAudio != null) {
                    if (hasControl(otherAudio)) {
                        last = isEnable(otherAudio);
                        setEnable(otherAudio, false);
                        return last;
                    }
                }
            }
            return false;
        }

        private static void recoveryAudio(Object audio, Object otherAudio, boolean last) {
            if (audio != null) {
                if (last != isEnable(audio)) {
                    if (hasControl(audio)) {
                        setEnable(audio, last);
                    } else if (otherAudio != null) {
                        if (hasControl(otherAudio)) {
                            setEnable(otherAudio, last);
                        }
                    }
                }
            }
        }

        private void init(Context context) {
            if (AudioEffect == null) {
                AudioEffect = getAudioEffect();
            }
            if (MiSound == null) {
                MiSound = getMiSound();
            }
            if (Spatializer == null) {
                Spatializer = getSpatializer(context);
            }
        }
    }
}
