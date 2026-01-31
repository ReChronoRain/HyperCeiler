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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.control;

import static com.sevtinge.hyperceiler.libhook.rules.systemframework.others.AutoEffectSwitchForSystem.getEarPhoneStateFinal;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.DOLBY_PARAM_DAP_ON;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.DOLBY_SET_PARAM_ID;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_DOLBY;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_DOLBY_CONTROL;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_MISOUND;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_MISOUND_CONTROL;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_NONE;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_SPATIAL_AUDIO;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_SURROUND;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.MISOUND_PARAM_3D_SURROUND;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.MISOUND_PARAM_ENABLE;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.RESULT_SUCCESS;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.callMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.callStaticMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findClass;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findClassIfExists;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findMethodBestMatch;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.hookMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.newInstance;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.returnConstant;

import android.content.Context;

import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.DeviceEffectMemory.EffectState;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.callback.IControlForSystem;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * 非 FW 模式下控制音效
 * 直接操作 AudioEffect 实例来控制音效
 *
 * @author 焕晨HChen
 */
public class AudioEffectControlForSystem extends BaseEffectControl implements IControlForSystem {

    private static final String TAG = "AudioEffectControlForSystem";

    // 类引用
    private Class<?> mAudioManagerClass;
    private Class<?> mMiSoundClass;
    private Class<?> mDolbyClass;

    // 使用原子引用保证线程安全
    private final AtomicReference<Object> mDolbyInstanceRef = new AtomicReference<>();
    private final AtomicReference<Object> mMiSoundInstanceRef = new AtomicReference<>();
    private final AtomicBoolean mIsIntactDolbyClass = new AtomicBoolean(false);

    // 上一次的状态
    private volatile boolean mLastDolbyEnable;
    private volatile boolean mLastMiSoundEnable;
    private volatile boolean mLastSpatializerEnable;
    private volatile boolean mLast3dSurroundEnable;

    // 锁，用于保护音效操作
    private final ReentrantLock mEffectLock = new ReentrantLock();

    @Override
    public void init() {
        initClasses();
        hookAudioEffectSetEnabled();
        hookSpatializerSetEnabled();
        hookMiSound3dSurround();
    }

    /**
     * 初始化所需的类引用
     */
    private void initClasses() {
        mAudioManagerClass = findClass("android.media.AudioManager");
        mMiSoundClass = findClass("android.media.audiofx.MiSound");

        // 尝试加载完整的 Dolby 类，如果不存在则使用备用类
        Class<?> dolbyClass = findClassIfExists("com.dolby.dax.DolbyAudioEffect");
        if (dolbyClass != null) {
            mDolbyClass = dolbyClass;mIsIntactDolbyClass.set(true);
        } else {
            mDolbyClass = findClass("com.android.server.audio.dolbyeffect.DolbyEffectController$DolbyAudioEffectHelper");
            mIsIntactDolbyClass.set(false);
        }

        XposedLog.d(TAG, "initClasses: dolbyClass=" + mDolbyClass + ", isIntact=" + mIsIntactDolbyClass.get());
    }

    /**
     * Hook AudioEffect.setEnabled 方法
     */
    private void hookAudioEffectSetEnabled() {
        findAndHookMethod("android.media.audiofx.AudioEffect", "setEnabled",
            boolean.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (!getEarPhoneStateFinal()) return;

                    Object thisObject = param.getThisObject();
                    Object dolbyInstance = mDolbyInstanceRef.get();
                    Object miSoundInstance = mMiSoundInstanceRef.get();

                    if (dolbyInstance != null && Objects.equals(dolbyInstance, thisObject)) {
                        XposedLog.d(TAG, "Earphone connected, skip setting dolby effect");
                        param.setResult(RESULT_SUCCESS);
                        return;
                    }

                    if (miSoundInstance != null && Objects.equals(miSoundInstance, thisObject)) {
                        XposedLog.d(TAG, "Earphone connected, skip setting misound effect");
                        param.setResult(RESULT_SUCCESS);}
                }
            }
        );
    }

    /**
     * Hook Spatializer.setEnabled 方法
     */
    private void hookSpatializerSetEnabled() {
        findAndHookMethod("android.media.Spatializer",
            "setEnabled",
            boolean.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (getEarPhoneStateFinal()) {
                        XposedLog.d(TAG, "Earphone connected, skip setting spatializer effect");
                        param.setResult(null);
                    }
                }
            }
        );
    }

    /**
     * Hook MiSound.set3dSurround 方法
     */
    private void hookMiSound3dSurround() {
        findAndHookMethod("android.media.audiofx.MiSound",
            "set3dSurround",
            int.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (getEarPhoneStateFinal()) {
                        XposedLog.d(TAG, "Earphone connected, skip setting 3dSurround effect");
                        param.setResult(null);
                    }
                }
            }
        );
    }

    // ==================== Effect Instance Management ====================

    /**
     * 获取或创建音效实例
     */
    private Object getOrCreateEffectInstance(AtomicReference<Object> instanceRef, Class<?> cls) {
        if (cls == null) return null;

        Object currentInstance = instanceRef.get();
        if (currentInstance != null && hasControl(currentInstance)) {
            return currentInstance;
        }

        mEffectLock.lock();
        try {
            currentInstance = instanceRef.get();
            if (currentInstance != null && hasControl(currentInstance)) {
                return currentInstance;
            }

            Object newInstance = createEffectInstance(cls);
            if (newInstance == null) {
                XposedLog.w(TAG, "Failed to create effect instance for: " + cls.getName());
                return currentInstance;
            }

            releaseEffectInstance(currentInstance);
            instanceRef.set(newInstance);
            return newInstance;

        } finally {
            mEffectLock.unlock();
        }
    }

    /**
     * 创建音效实例
     */
    private Object createEffectInstance(Class<?> cls) {
        try {
            return newInstance(cls, 0, 0);
        } catch (Exception e) {
            XposedLog.e(TAG, "createEffectInstance failed", e);
            return null;
        }
    }

    /**
     * 释放音效实例
     */
    private void releaseEffectInstance(Object instance) {
        if (instance == null) return;
        try {
            callMethod(instance, "release");
        } catch (Exception e) {
            XposedLog.w(TAG, "releaseEffectInstance failed", e);
        }
    }

    /**
     * 检查是否拥有控制权
     */
    private boolean hasControl(Object instance) {
        if (instance == null) return false;
        try {
            Object result = callMethod(instance, "hasControl");
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 设置音效启用状态（通过 Hook native 方法）
     */
    private void setEnableEffect(Object instance, boolean enable) {
        if (instance == null) return;

        Class<?> superClass = instance.getClass().getSuperclass();
        if (superClass == null) return;

        try {
            callMethod(instance, "checkState", "setEnabled()");
            hookMethod(
                findMethodBestMatch(instance.getClass(), "native_setEnabled"),
                returnConstant(enable)
            );
        } catch (Exception e) {
            XposedLog.e(TAG, "setEnableEffect failed", e);
        }
    }

    // ==================== Dolby Control ====================

    /**
     * 设置 Dolby 音效启用状态
     */
    private void setEnableDolbyEffect(boolean enable) {
        if (mDolbyClass == null) return;

        mEffectLock.lock();
        try {
            Object instance = getOrCreateEffectInstance(mDolbyInstanceRef, mDolbyClass);
            if (instance == null) return;

            if (mIsIntactDolbyClass.get()) {
                setDolbyEnableIntact(instance, enable);
            } else {
                setDolbyEnableFallback(instance, enable);
            }
            setEnableEffect(instance, enable);XposedLog.d(TAG, "setEnableDolbyEffect: " + enable);

        } catch (UnsupportedOperationException e) {
            XposedLog.e(TAG, "setEnableDolbyEffect: UnsupportedOperationException", e);
        } catch (Exception e) {
            XposedLog.e(TAG, "setEnableDolbyEffect: Exception", e);
        } finally {
            mEffectLock.unlock();
        }
    }

    /**
     * 使用完整 Dolby 类设置启用状态
     */
    private void setDolbyEnableIntact(Object instance, boolean enable) {
        callMethod(instance, "setBoolParam", DOLBY_PARAM_DAP_ON, enable);
    }

    /**
     * 使用备用方式设置 Dolby 启用状态
     */
    private void setDolbyEnableFallback(Object instance, boolean enable) {
        byte[] bArr = new byte[12];
        int offset = int32ToByteArray(DOLBY_PARAM_DAP_ON, bArr, 0);
        offset = int32ToByteArray(1, bArr, offset);
        int32ToByteArray(enable ? 1 : 0, bArr, offset);
        Object result = callMethod(instance, "setParameter", DOLBY_SET_PARAM_ID, bArr);
        callMethod(instance, "checkReturnValue", result);
    }

    /**
     * 获取 Dolby 音效启用状态
     */
    private boolean isEnabledDolbyEffect() {
        if (mDolbyClass == null) return false;

        try {
            Object instance = getOrCreateEffectInstance(mDolbyInstanceRef, mDolbyClass);
            if (instance == null) return false;

            if (mIsIntactDolbyClass.get()) {
                Object result = callMethod(instance, "getBoolParam", DOLBY_PARAM_DAP_ON);
                return result instanceof Boolean && (Boolean) result;
            } else {
                byte[] baValue = new byte[12];
                int32ToByteArray(DOLBY_PARAM_DAP_ON, baValue, 0);
                Object result = callMethod(instance, "getParameter", DOLBY_SET_PARAM_ID, baValue);
                callMethod(instance, "checkReturnValue", result);
                return byteArrayToInt32(baValue) > 0;
            }
        } catch (Exception e) {
            XposedLog.e(TAG, "isEnabledDolbyEffect failed", e);
            return false;
        }
    }

    /**
     * 将 int32 转换为字节数组
     */
    private int int32ToByteArray(int src, byte[] dst, int index) {
        try {
            Object result = callStaticMethod(mDolbyClass, "int32ToByteArray", src, dst, index);
            return result instanceof Integer ? (Integer) result : 0;
        } catch (Exception e) {
            dst[index] = (byte) (src & 0xFF);
            dst[index + 1] = (byte) ((src >> 8) & 0xFF);
            dst[index + 2] = (byte) ((src >> 16) & 0xFF);
            dst[index + 3] = (byte) ((src >> 24) & 0xFF);
            return 4;
        }
    }

    /**
     * 将字节数组转换为 int32
     */
    private static int byteArrayToInt32(byte[] ba) {
        return ((ba[3] & 0xFF) << 24) |
            ((ba[2] & 0xFF) << 16) |
            ((ba[1] & 0xFF) << 8) |
            (ba[0] & 0xFF);
    }

    // ==================== MiSound Control ====================

    /**
     * 设置 MiSound 音效启用状态
     */
    private void setEnableMiSound(boolean enable) {
        if (mMiSoundClass == null) return;

        mEffectLock.lock();
        try {
            Object instance = getOrCreateEffectInstance(mMiSoundInstanceRef, mMiSoundClass);
            if (instance == null) return;

            Object result = callMethod(instance, "setParameter", MISOUND_PARAM_ENABLE, enable ? 1 : 0);
            callMethod(instance, "checkStatus", result);
            setEnableEffect(instance, enable);
            XposedLog.d(TAG, "setEnableMiSound: " + enable);

        } catch (UnsupportedOperationException e) {
            XposedLog.e(TAG, "setEnableMiSound: UnsupportedOperationException", e);
        } catch (Exception e) {
            XposedLog.e(TAG, "setEnableMiSound: Exception", e);
        } finally {
            mEffectLock.unlock();
        }
    }

    /**
     * 获取 MiSound 音效启用状态
     */
    private boolean isEnabledMiSound() {
        if (mMiSoundClass == null) return false;

        try {
            Object instance = getOrCreateEffectInstance(mMiSoundInstanceRef, mMiSoundClass);
            if (instance == null) return false;

            Object result = callMethod(instance, "getEnabled");
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            XposedLog.e(TAG, "isEnabledMiSound failed", e);
            return false;
        }
    }

    // ==================== Spatializer Control ====================

    /**
     * 检查 Spatializer 是否可用
     */
    private boolean isAvailableSpatializer() {
        if (mAudioManagerClass == null) return false;

        try {
            Object sService = callStaticMethod(mAudioManagerClass, "getService");
            if (sService == null) return false;

            Object result = callMethod(sService, "isSpatializerAvailable");
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            XposedLog.e(TAG, "isAvailableSpatializer failed", e);
            return false;
        }
    }

    /**
     * 设置 Spatializer 启用状态
     */
    private void setEnableSpatializer(boolean enable) {
        if (mAudioManagerClass == null || !isAvailableSpatializer()) return;

        try {
            Object sService = callStaticMethod(mAudioManagerClass, "getService");
            if (sService != null) {
                callMethod(sService, "setSpatializerEnabled", enable);XposedLog.d(TAG, "setEnableSpatializer: " + enable);
            }
        } catch (Exception e) {
            XposedLog.e(TAG, "setEnableSpatializer failed", e);
        }
    }

    /**
     * 获取 Spatializer 启用状态
     */
    private boolean isEnabledSpatializer() {
        if (mAudioManagerClass == null) return false;

        try {
            Object sService = callStaticMethod(mAudioManagerClass, "getService");
            if (sService == null) return false;

            Object result = callMethod(sService, "isSpatializerEnabled");
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            XposedLog.e(TAG, "isEnabledSpatializer failed", e);
            return false;
        }
    }

    // ==================== 3D Surround Control ====================

    /**
     * 设置 3D 环绕音效启用状态
     */
    private void setEnable3dSurround(boolean enable) {
        Object instance = mMiSoundInstanceRef.get();
        if (instance == null) return;

        try {
            Object result = callMethod(instance, "setParameter", MISOUND_PARAM_3D_SURROUND, enable ? 1 : 0);
            callMethod(instance, "checkStatus", result);
            XposedLog.d(TAG, "setEnable3dSurround: " + enable);
        } catch (Exception e) {
            XposedLog.e(TAG, "setEnable3dSurround failed", e);
        }
    }

    /**
     * 获取 3D 环绕音效启用状态
     */
    private boolean isEnabled3dSurround() {
        Object instance = mMiSoundInstanceRef.get();
        if (instance == null) return false;

        try {
            int[] value = new int[1];
            Object result = callMethod(instance, "getParameter", MISOUND_PARAM_3D_SURROUND, value);
            callMethod(instance, "checkStatus", result);
            return value[0] == 1;
        } catch (Exception e) {
            XposedLog.e(TAG, "isEnabled3dSurround failed", e);
            return false;
        }
    }

    // ==================== IControlForSystem Implementation ====================

    @Override
    protected void updateEffectMap() {
        mEffectHasControlMap.clear();
        putBoolean(mEffectHasControlMap, EFFECT_DOLBY_CONTROL, hasControl(mDolbyInstanceRef.get()));
        putBoolean(mEffectHasControlMap, EFFECT_MISOUND_CONTROL, hasControl(mMiSoundInstanceRef.get()));

        mEffectEnabledMap.clear();
        putBoolean(mEffectEnabledMap, EFFECT_DOLBY, isEnabledDolbyEffect());
        putBoolean(mEffectEnabledMap, EFFECT_MISOUND, isEnabledMiSound());
        putBoolean(mEffectEnabledMap, EFFECT_SPATIAL_AUDIO, isEnabledSpatializer());
        putBoolean(mEffectEnabledMap, EFFECT_SURROUND, isEnabled3dSurround());
    }

    @Override
    public void updateLastEffectState() {
        mLastDolbyEnable = isEnabledDolbyEffect();
        mLastMiSoundEnable = isEnabledMiSound();
        mLastSpatializerEnable = isEnabledSpatializer();
        mLast3dSurroundEnable = isEnabled3dSurround();

        XposedLog.d(TAG, "updateLastEffectState: dolby=" + mLastDolbyEnable +
            ", misound=" + mLastMiSoundEnable +
            ", spatializer=" + mLastSpatializerEnable +
            ", 3dSurround=" + mLast3dSurroundEnable);
    }

    @Override
    public EffectState getCurrentEffectState() {
        String mainEffect;
        if (isEnabledDolbyEffect()) {
            mainEffect = EFFECT_DOLBY;
        } else if (isEnabledMiSound()) {
            mainEffect = EFFECT_MISOUND;
        } else {
            mainEffect = EFFECT_NONE;
        }

        return new EffectState(mainEffect, isEnabledSpatializer(), isEnabled3dSurround());
    }

    @Override
    public void applyEffectState(EffectState state) {
        if (state == null) return;

        XposedLog.d(TAG, "applyEffectState: " + state);

        // 先关闭所有主音效
        setEnableDolbyEffect(false);
        setEnableMiSound(false);

        // 应用主音效
        switch (state.mainEffect) {
            case EFFECT_DOLBY -> setEnableDolbyEffect(true);
            case EFFECT_MISOUND -> {
                setEnableMiSound(true);
                setEnable3dSurround(state.surround);
            }
            case EFFECT_NONE -> {
                // 保持关闭
            }
        }

        // 应用空间音频
        setEnableSpatializer(state.spatialAudio);}

    @Override
    public void setEffectToNone(Context context) {
        setEnableDolbyEffect(false);
        setEnableMiSound(false);
        setEnableSpatializer(false);setEnable3dSurround(false);
        XposedLog.d(TAG, "setEffectToNone completed");
    }

    @Override
    public void resetAudioEffect() {
        if (mLastMiSoundEnable) {
            setEnableDolbyEffect(false);
            setEnableMiSound(true);
            setEnable3dSurround(mLast3dSurroundEnable);
        } else {
            setEnableDolbyEffect(mLastDolbyEnable);
            setEnableMiSound(false);
        }
        setEnableSpatializer(mLastSpatializerEnable);
        XposedLog.d(TAG, "resetAudioEffect completed");
    }

    @Override
    public void dumpAudioEffectState() {
        XposedLog.d(TAG, "AudioEffect State: dolby=" + isEnabledDolbyEffect() +
            ", misound=" + isEnabledMiSound() +
            ", spatializer=" + isEnabledSpatializer() +
            ", 3dSurround=" + isEnabled3dSurround());
    }
}
