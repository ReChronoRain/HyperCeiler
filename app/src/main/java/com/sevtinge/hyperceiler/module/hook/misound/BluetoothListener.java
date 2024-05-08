package com.sevtinge.hyperceiler.module.hook.misound;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;

import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.FieldData;
import org.luckypray.dexkit.result.MethodData;

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
        ClassData classData = DexKit.getDexKitBridge().findClass(FindClass.create().matcher(ClassMatcher.create()
                .usingStrings("Creating a DolbyAudioEffect to global output mix!"))).singleOrNull();
        try {
            if (classData == null) {
                logE(TAG, "AudioEffect not found");
            } else {
                findAndHookConstructor(classData.getInstance(lpparam.classLoader),
                        int.class, int.class,
                        new MethodHook() {
                            @Override
                            protected void after(MethodHookParam param) {
                                miDolby = param.thisObject;
                                // logE(TAG, "miDolby: " + miDolby);
                            }
                        }
                );
            }
        } catch (ClassNotFoundException e) {
            logE(TAG, e);
        }
        ClassData classData1 = DexKit.getDexKitBridge().findClass(FindClass.create().matcher(ClassMatcher.create()
                .usingStrings("android.media.audiofx.MiSound"))).singleOrNull();
        try {
            if (classData1 == null) {
                logE(TAG, "MiSound not found");
            } else {
                FieldData fieldData = DexKit.getDexKitBridge().findField(FindField.create().matcher(FieldMatcher.create()
                        .declaredClass(classData1.getInstance(lpparam.classLoader)).type(Object.class)
                )).singleOrNull();
                if (fieldData == null) {
                    logE(TAG, "field not found");
                } else {
                    String name = fieldData.getFieldName();
                    findAndHookConstructor(classData1.getInstance(lpparam.classLoader),
                            int.class, int.class,
                            new MethodHook() {
                                @Override
                                protected void after(MethodHookParam param) {
                                    miAudio = XposedHelpers.getObjectField(param.thisObject, name);
                                    // logE(TAG, "miAudio: " + miAudio);
                                }
                            }
                    );
                }
            }
        } catch (ClassNotFoundException e) {
            logE(TAG, e);
        }
        MethodData methodData = DexKit.getDexKitBridge().findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                        .declaredClass(
                                ClassMatcher.create()
                                        .usingStrings("getEnabledEffect(): both of enabled, force return misound")
                        )
                        .usingStrings("getEnabledEffect(): both of enabled, force return misound")
                )
        ).singleOrNull();
        try {
            if (methodData == null) {
                logE(TAG, "null");
            } else {
                Method method = methodData.getMethodInstance(lpparam.classLoader);
                hookMethod(method,
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
                findAndHookMethod(method.getDeclaringClass(), "onPreferenceChange",
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
                MethodData methodData1 = DexKit.getDexKitBridge().findMethod(FindMethod.create().matcher(
                        MethodMatcher.create().declaredClass(method.getDeclaringClass())
                                .usingStrings("refreshEffectSelectionEnabled(): currEffect "))).singleOrNull();
                hookMethod(methodData1.getMethodInstance(lpparam.classLoader), new MethodHook() {
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
                        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
                        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                        intentFilter.addAction(AudioManager.ACTION_HEADSET_PLUG);
                        application.registerReceiver(new Listener(), intentFilter);
                    }
                }
        );
        // settings get global effect_implementer
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

    private static boolean hasControl(Object o) {
        return (boolean) XposedHelpers.callMethod(o, "hasControl");
    }

    private static boolean isEnable(Object o) {
        return (boolean) XposedHelpers.callMethod(o, "getEnabled");
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
        private static boolean lastDolby;
        private static boolean lastMiui;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        on(context);
                    }
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        over(context);
                    }
                    case AudioManager.ACTION_HEADSET_PLUG -> {
                        init();
                        if (intent.hasExtra("state")) {
                            int state = intent.getIntExtra("state", 0);
                            if (state == 1) {
                                on(context);
                            } else if (state == 0) {
                                over(context);
                            }
                        }
                    }
                }
            }
        }

        private void on(Context context) {
            init();
            lastDolby = setAudio(AudioEffect, miDolby);
            lastMiui = setAudio(MiSound, miAudio);
            String implementer = effectImplementer(context);
            // logE(TAG, "A: " + AudioEffect + " d: " + miDolby + " M: " + MiSound + " a: " + miAudio
            //         + " co: " + hasControl(AudioEffect) + " co1: " + hasControl(MiSound) +
            //         " laD: " + lastDolby + " laM: " + lastMiui + " im: " + implementer);
            if (implementer != null) {
                if ("dolby".equals(implementer)) {
                    lastDolby = true;
                    lastMiui = false;
                } else if ("misound".equals(implementer)) {
                    lastDolby = false;
                    lastMiui = true;
                }
            }
            refresh(context, false, false);
        }

        private void refresh(Context context, boolean dolby, boolean miui) {
            Intent intent = new Intent();
            intent.setAction("miui.intent.action.ACTION_AUDIO_EFFECT_REFRESH");
            intent.putExtra("dolby_active", dolby);
            intent.putExtra("misound_active", miui);
            context.sendBroadcast(intent);
        }

        private void over(Context context) {
            init();
            recoveryAudio(AudioEffect, miDolby, lastDolby);
            recoveryAudio(MiSound, miAudio, lastMiui);
            refresh(context, lastDolby, lastMiui);
            // logE(TAG, "A: " + AudioEffect + " d: " + miDolby + " M: " + MiSound + " a: " + miAudio
            //         + " co: " + hasControl(AudioEffect) + " co1: " + hasControl(MiSound) +
            //         " laD: " + lastDolby + " laM: " + lastMiui);
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

        private void init() {
            if (AudioEffect == null) {
                AudioEffect = getAudioEffect();
            }
            if (MiSound == null) {
                MiSound = getMiSound();
            }
        }
    }
}
