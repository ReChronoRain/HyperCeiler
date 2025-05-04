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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.utils.api.effect.control;

import static com.hchen.hooktool.log.XposedLog.logI;
import static com.hchen.hooktool.core.CoreTool.*;
import static com.hchen.hooktool.helper.MethodHelper.*;
import static com.sevtinge.hyperceiler.hook.module.hook.systemframework.AutoEffectSwitchForSystem.getEarPhoneStateFinal;
import static com.sevtinge.hyperceiler.hook.utils.api.effect.EffectItem.EFFECT_DOLBY;
import static com.sevtinge.hyperceiler.hook.utils.api.effect.EffectItem.EFFECT_DOLBY_CONTROL;
import static com.sevtinge.hyperceiler.hook.utils.api.effect.EffectItem.EFFECT_MISOUND;
import static com.sevtinge.hyperceiler.hook.utils.api.effect.EffectItem.EFFECT_MISOUND_CONTROL;
import static com.sevtinge.hyperceiler.hook.utils.api.effect.EffectItem.EFFECT_SPATIAL_AUDIO;
import static com.sevtinge.hyperceiler.hook.utils.api.effect.EffectItem.EFFECT_SURROUND;

import android.content.Context;

import com.hchen.hooktool.hook.IHook;
import com.sevtinge.hyperceiler.hook.utils.api.effect.callback.IControlForSystem;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XC_MethodReplacement;

import java.util.Objects;
import java.util.UUID;

/**
 * 非 FW 模式下控制音效
 *
 * @author 焕晨HChen
 */
public class AudioEffectControlForSystem extends BaseEffectControl implements IControlForSystem {
    public static final String TAG = "AudioEffectControlForSystem";
    private static final UUID mDolbyUUID = UUID.fromString("9d4921da-8225-4f29-aefa-39537a04bcaa");
    private static final UUID mMiSoundUUID = UUID.fromString("5b8e36a5-144a-4c38-b1d7-0002a5d5c51b");
    private Class<?> mAudioManagerClass = null;
    private Class<?> mMiSoundClass = null;
    private Class<?> mDolbyClass = null;
    private boolean isIntactDolbyClass = false;
    private Object mDolbyInstance = null;
    private Object mMiSoundInstance = null;
    private boolean mLastDolbyEnable = false;
    private boolean mLastMiSoundEnable = false;
    private boolean mLastSpatializerEnable = false;
    private boolean mLast3dSurroundEnable = false;

    public void init() {
        mAudioManagerClass = findClass("android.media.AudioManager");
        mMiSoundClass = findClass("android.media.audiofx.MiSound");
        if (existsClass("com.dolby.dax.DolbyAudioEffect")) {
            mDolbyClass = findClass("com.dolby.dax.DolbyAudioEffect");
            isIntactDolbyClass = true;
        } else
            mDolbyClass = findClass("com.android.server.audio.dolbyeffect.DolbyEffectController$DolbyAudioEffectHelper");

        hookMethod("android.media.audiofx.AudioEffect",
                "setEnabled",
                boolean.class,
                new IHook() {
                    @Override
                    public void before() {
                        observeCall();
                        if (!getEarPhoneStateFinal()) return;

                        if (mDolbyInstance != null) {
                            if (Objects.equals(mDolbyInstance, thisObject())) {
                                logI(TAG, "earphone is connection, skip set dolby effect!!");
                                setResult(0); // SUCCESS
                                return;
                            }
                        }

                        if (mMiSoundInstance != null) {
                            if (Objects.equals(mMiSoundInstance, thisObject())) {
                                logI(TAG, "earphone is connection, skip set misound effect!!");
                                setResult(0); // SUCCESS
                            }
                        }
                    }
                }
        );

        hookMethod("android.media.Spatializer",
                "setEnabled",
                boolean.class,
                new IHook() {
                    @Override
                    public void before() {
                        if (getEarPhoneStateFinal()) {
                            logI(TAG, "earphone is connection, skip set spatializer effect!!");
                            returnNull();
                        }
                    }
                }
        );

        hookMethod("android.media.audiofx.MiSound",
                "set3dSurround",
                int.class,
                new IHook() {
                    @Override
                    public void before() {
                        if (getEarPhoneStateFinal()) {
                            logI(TAG, "earphone is connection, skip set 3dSurround effect!!");
                            returnNull();
                        }
                    }
                }
        );
    }

    // -------- Effect Utils --------
    private Object initEffectInstance(Object instance, Class<?> cls) {
        if (cls == null) return null;
        if (instance != null) {
            if (hasControl(instance)) return instance;
            callMethod(instance, "release");
        }
        return newInstance(cls, 0, 0);
    }

    private boolean hasControl(Object instance) {
        if (instance == null) return false;
        return (boolean) callMethod(instance, "hasControl");
    }

    private void setEnableEffect(Object instance, boolean enable) {
        if (instance == null || instance.getClass().getSuperclass() == null) return;
        callMethod(instance, "checkState", "setEnabled()");
        XposedBridge.hookMethod(findMethodPro(instance.getClass()).withSuper(true).withMethodName("native_setEnabled").single(), XC_MethodReplacement.returnConstant(enable)); // super private
    }

    // -------- Dolby --------
    private void setEnableDolbyEffect(boolean enable) {
        if (mDolbyClass == null) return;
        mDolbyInstance = initEffectInstance(mDolbyInstance, mDolbyClass);

        if (isIntactDolbyClass) {
            callMethod(mDolbyInstance, "setBoolParam", 0, enable);
        } else {
            byte[] bArr = new byte[12];
            int int32ToByteArray = int32ToByteArray(0, bArr, 0);
            int32ToByteArray(enable ? 1 : 0, bArr, int32ToByteArray + int32ToByteArray(1, bArr, int32ToByteArray));
            callMethod(mDolbyInstance, "checkReturnValue", callMethod(mDolbyInstance, "setParameter", 5, bArr));
        }

        setEnableEffect(mDolbyInstance, enable);
    }

    private boolean isEnabledDolbyEffect() {
        if (mDolbyClass == null) return false;
        mDolbyInstance = initEffectInstance(mDolbyInstance, mDolbyClass);

        if (isIntactDolbyClass) {
            return (boolean) callMethod(mDolbyInstance, "getBoolParam", 0);
        } else {
            byte[] baValue = new byte[12];
            int32ToByteArray(0, baValue, 0);
            callMethod(mDolbyInstance, "checkReturnValue", callMethod(mDolbyInstance, "getParameter", 5, baValue));
            int en = byteArrayToInt32(baValue);
            return en > 0;
        }
    }

    private int int32ToByteArray(int src, byte[] dst, int index) {
        return (int) callStaticMethod(mDolbyClass, "int32ToByteArray", src, dst, index);
    }

    private static int byteArrayToInt32(byte[] ba) {
        return ((ba[3] & 255) << 24) | ((ba[2] & 255) << 16) | ((ba[1] & 255) << 8) | (ba[0] & 255);
    }

    // -------- MiSound --------
    private void setEnableMiSound(boolean enable) {
        if (mMiSoundInstance == null) return;
        mMiSoundInstance = initEffectInstance(mMiSoundInstance, mMiSoundClass);

        callMethod(mMiSoundInstance, "checkStatus", callMethod(mMiSoundInstance, "setParameter", 25, enable ? 1 : 0));
        setEnableEffect(mMiSoundInstance, enable);
    }

    private boolean isEnabledMiSound() {
        if (mMiSoundClass == null) return false;
        mMiSoundInstance = initEffectInstance(mMiSoundInstance, mMiSoundClass);

        return (boolean) callMethod(mMiSoundInstance, "getEnabled");
    }

    // -------- Spatializer --------
    private boolean isAvailableSpatializer() {
        if (mAudioManagerClass == null) return false;
        Object sService = callStaticMethod(mAudioManagerClass, "getService");
        return (boolean) callMethod(sService, "isSpatializerAvailable");
    }

    private void setEnableSpatializer(boolean enable) {
        if (mAudioManagerClass == null) return;
        if (!isAvailableSpatializer()) return;
        Object sService = callStaticMethod(mAudioManagerClass, "getService");
        callMethod(sService, "setSpatializerEnabled", enable);
    }

    private boolean isEnabledSpatializer() {
        if (mAudioManagerClass == null) return false;
        Object sService = callStaticMethod(mAudioManagerClass, "getService");
        return (boolean) callMethod(sService, "isSpatializerEnabled");
    }

    // -------- 3d Surround --------
    private void setEnable3dSurround(boolean enable) {
        if (mMiSoundInstance == null) return;
        callMethod(mMiSoundInstance, "checkStatus", callMethod(mMiSoundInstance, "setParameter", 20, enable ? 1 : 0));
    }

    private boolean isEnabled3dSurround() {
        if (mMiSoundInstance == null) return false;
        int[] value = new int[1];
        callMethod(mMiSoundInstance, "checkStatus", callMethod(mMiSoundInstance, "getParameter", 20, value));
        return value[0] == 1;
    }

    // -------- Control --------
    @Override
    void updateEffectMap() {
        mEffectHasControlMap.clear();
        mEffectHasControlMap.put(EFFECT_DOLBY_CONTROL, String.valueOf(hasControl(mDolbyInstance)));
        mEffectHasControlMap.put(EFFECT_MISOUND_CONTROL, String.valueOf(hasControl(mMiSoundInstance)));

        mEffectEnabledMap.clear();
        mEffectEnabledMap.put(EFFECT_DOLBY, String.valueOf(isEnabledDolbyEffect()));
        mEffectEnabledMap.put(EFFECT_MISOUND, String.valueOf(isEnabledMiSound()));
        mEffectEnabledMap.put(EFFECT_SPATIAL_AUDIO, String.valueOf(isEnabledSpatializer()));
        mEffectEnabledMap.put(EFFECT_SURROUND, String.valueOf(isEnabled3dSurround()));
    }

    @Override
    public void updateLastEffectState() {
        mLastDolbyEnable = isEnabledDolbyEffect();
        mLastMiSoundEnable = isEnabledMiSound();
        mLastSpatializerEnable = isEnabledSpatializer();
        mLast3dSurroundEnable = isEnabled3dSurround();

        logI(TAG, "updateLastEffectState: mLastDolbyEnable: " + mLastDolbyEnable + ", mLastMiSoundEnable: " + mLastMiSoundEnable
                + ", mLastSpatializerEnable: " + mLastSpatializerEnable + ", mLast3dSurroundEnable: " + mLast3dSurroundEnable);
    }

    @Override
    public void setEffectToNone(Context context) {
        setEnableDolbyEffect(false);
        setEnableMiSound(false);
        setEnableSpatializer(false);
        setEnable3dSurround(false);
    }

    @Override
    public void resetAudioEffect() {
        if (mLastMiSoundEnable) {
            setEnableDolbyEffect(false);
            setEnableMiSound(true);
            setEnable3dSurround(mLast3dSurroundEnable);
        } else {
            setEnableDolbyEffect(true);
            setEnableMiSound(false);
        }
        setEnableSpatializer(mLastSpatializerEnable);
    }

    @Override
    public void dumpAudioEffectState() {
        logI(TAG, "dolby: " + isEnabledDolbyEffect() + ", misound: " + isEnabledMiSound() +
                ", spatializer: " + isEnabledSpatializer() + ", 3dSurround: " + isEnabled3dSurround());
    }
}

