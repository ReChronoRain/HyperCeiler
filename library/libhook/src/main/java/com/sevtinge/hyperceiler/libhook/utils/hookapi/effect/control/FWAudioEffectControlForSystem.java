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
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_ARRAY;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_DOLBY;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_MISOUND;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_NONE;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_SPATIAL_AUDIO;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_SURROUND;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.SETTINGS_KEY_EFFECT_IMPLEMENTER;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.callMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.callStaticMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findClass;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getObjectField;

import android.content.Context;
import android.provider.Settings;

import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.DeviceEffectMemory.EffectState;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.callback.IControlForSystem;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * FW 模式下控制音效
 * 通过 AudioEffectCenter 系统 API 控制音效
 *
 * @author 焕晨HChen
 */
public class FWAudioEffectControlForSystem extends BaseEffectControl implements IControlForSystem {

    private static final String TAG = "FWAudioEffectControlForSystem";

    // 使用原子引用保证线程安全
    private final AtomicReference<Context> mContextRef = new AtomicReference<>();
    private final AtomicReference<Object> mPresenterRef = new AtomicReference<>();
    private final AtomicReference<Object> mCenterRef = new AtomicReference<>();
    // 使用线程安全的列表存储上一次的音效状态
    private final CopyOnWriteArrayList<String> mLastEffectList = new CopyOnWriteArrayList<>();

    @Override
    public void init() {
        hookAudioEffectCenterGetInstance();
        hookAudioEffectCenterSetEffectActive();
        hookAudioEffectCenterRelease();
    }

    /**
     * Hook AudioEffectCenter.getInstance 方法
     */
    private void hookAudioEffectCenterGetInstance() {
        findAndHookMethod("android.media.audiofx.AudioEffectCenter",
            "getInstance",
            Context.class,
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    Object center = param.getResult();
                    mCenterRef.set(center);
                    if (center != null) {
                        Object presenter = getObjectField(center, "mPresenter");
                        mPresenterRef.set(presenter);
                        XposedLog.d(TAG, "AudioEffectCenter instance obtained: " + center);
                    }
                }
            }
        );
    }

    /**
     * Hook AudioEffectCenter.setEffectActive 方法
     */
    private void hookAudioEffectCenterSetEffectActive() {
        findAndHookMethod("android.media.audiofx.AudioEffectCenter",
            "setEffectActive",
            String.class, boolean.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (getEarPhoneStateFinal()) {
                        String effect = (String) param.getArgs()[0];
                        XposedLog.d(TAG, "Earphone connected, skip setting effect: " + effect);
                        param.setResult(null);
                    }
                }
            }
        );
    }

    /**
     * Hook AudioEffectCenter.release 方法
     */
    private void hookAudioEffectCenterRelease() {
        findAndHookMethod("android.media.audiofx.AudioEffectCenter",
            "release",
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    mPresenterRef.set(null);
                    mCenterRef.set(null);
                    XposedLog.d(TAG, "AudioEffectCenter released");
                }
            }
        );
    }

    @Override
    public void setContext(Context context) {
        mContextRef.set(context);
    }

    /**
     * 确保 AudioEffectCenter 实例存在
     */
    private void ensureInstance() {
        if (mCenterRef.get() != null) return;

        Context context = mContextRef.get();
        if (context != null) {
            try {
                Class<?> centerClass = findClass("android.media.audiofx.AudioEffectCenter");
                callStaticMethod(centerClass, "getInstance", context);
            } catch (Exception e) {
                XposedLog.e(TAG, "ensureInstance failed", e);
            }
        }
    }

    // ==================== Effect Query Methods ====================

    private boolean isEffectSupported(String effect) {
        ensureInstance();
        Object center = mCenterRef.get();
        if (center == null) return false;

        try {
            Object result = callMethod(center, "isEffectSupported", effect);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            XposedLog.e(TAG, "isEffectSupported failed for: " + effect, e);
            return false;
        }
    }

    private boolean isEffectAvailable(String effect) {
        ensureInstance();
        Object center = mCenterRef.get();
        if (center == null) return false;

        try {
            Object result = callMethod(center, "isEffectAvailable", effect);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            XposedLog.e(TAG, "isEffectAvailable failed for: " + effect, e);
            return false;
        }
    }

    private boolean isEffectActive(String effect) {
        ensureInstance();
        Object center = mCenterRef.get();
        if (center == null) return false;

        try {
            Object result = callMethod(center, "isEffectActive", effect);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            XposedLog.e(TAG, "isEffectActive failed for: " + effect, e);
            return false;
        }
    }

    // ==================== Effect Control Methods ====================

    private void setEffectActive(String effect, boolean active) {
        ensureInstance();
        Object presenter = mPresenterRef.get();
        if (presenter == null) {
            XposedLog.w(TAG, "setEffectActive: presenter is null");
            return;
        }

        try {
            switch (effect) {
                case EFFECT_DOLBY -> {
                    if (active) {
                        callMethod(presenter, "setDolbyActive");
                    }
                }
                case EFFECT_MISOUND -> {
                    if (active) {
                        callMethod(presenter, "setMiSoundActive");
                    }
                }
                case EFFECT_SPATIAL_AUDIO -> callMethod(presenter, "setSpatialAudioActive", active);
                case EFFECT_SURROUND -> callMethod(presenter, "setSurroundActive", active);
                default -> XposedLog.w(TAG, "Unknown effect type: " + effect);
            }
            XposedLog.d(TAG, "setEffectActive: " + effect + " = " + active);
        } catch (Exception e) {
            XposedLog.e(TAG, "setEffectActive failed for: " + effect, e);
        }
    }

    // ==================== IControlForSystem Implementation ====================

    @Override
    protected void updateEffectMap() {
        mEffectSupportMap.clear();
        mEffectAvailableMap.clear();
        mEffectActiveMap.clear();
        Arrays.stream(EFFECT_ARRAY).forEach(effect -> {
            putBoolean(mEffectSupportMap, effect, isEffectSupported(effect));
            putBoolean(mEffectAvailableMap, effect, isEffectAvailable(effect));
            putBoolean(mEffectActiveMap, effect, isEffectActive(effect));
        });

        XposedLog.d(TAG, "updateEffectMap: center=" + mCenterRef.get() +
            ", presenter=" + mPresenterRef.get() +
            ", support=" + mEffectSupportMap +
            ", available=" + mEffectAvailableMap +
            ", active=" + mEffectActiveMap);
    }

    @Override
    public void updateLastEffectState() {
        mLastEffectList.clear();

        Arrays.stream(EFFECT_ARRAY).forEach(effect -> {
            if (isEffectActive(effect)) {
                mLastEffectList.add(effect);
            }
        });

        XposedLog.d(TAG, "updateLastEffectState: " + mLastEffectList);
    }

    @Override
    public EffectState getCurrentEffectState() {
        String mainEffect;
        if (isEffectActive(EFFECT_DOLBY)) {
            mainEffect = EFFECT_DOLBY;
        } else if (isEffectActive(EFFECT_MISOUND)) {
            mainEffect = EFFECT_MISOUND;
        } else {
            mainEffect = EFFECT_NONE;
        }

        return new EffectState(mainEffect, isEffectActive(EFFECT_SPATIAL_AUDIO), isEffectActive(EFFECT_SURROUND));
    }

    @Override
    public void applyEffectState(EffectState state) {
        if (state == null) return;

        XposedLog.d(TAG, "applyEffectState: " + state);

        Object presenter = mPresenterRef.get();
        if (presenter == null) {
            XposedLog.w(TAG, "applyEffectState: presenter is null");
            return;
        }

        try {
            // 先关闭所有主音效
            callMethod(presenter, "setEffectDeactivate");

            // 应用主音效
            switch (state.mainEffect) {
                case EFFECT_DOLBY -> setEffectActive(EFFECT_DOLBY, true);
                case EFFECT_MISOUND -> {
                    setEffectActive(EFFECT_MISOUND, true);
                    setEffectActive(EFFECT_SURROUND, state.surround);
                }
                case EFFECT_NONE -> {
                    // 保持关闭
                }
            }

            // 应用空间音频
            setEffectActive(EFFECT_SPATIAL_AUDIO, state.spatialAudio);
        } catch (Exception e) {
            XposedLog.e(TAG, "applyEffectState failed", e);
        }
    }

    @Override
    public void setEffectToNone(Context context) {
        Object presenter = mPresenterRef.get();
        if (presenter == null) {
            XposedLog.w(TAG, "setEffectToNone: presenter is null");
            return;
        }

        try {
            callMethod(presenter, "setEffectDeactivate");
        } catch (Exception e) {
            XposedLog.e(TAG, "setEffectDeactivate failed", e);
        }

        // 更新系统设置
        if (context != null) {
            try {
                Settings.Global.putString(
                    context.getContentResolver(), SETTINGS_KEY_EFFECT_IMPLEMENTER, EFFECT_NONE
                );
            } catch (Exception e) {
                XposedLog.e(TAG, "Failed to update settings", e);
            }
        }

        XposedLog.d(TAG, "setEffectToNone completed");
    }

    @Override
    public void resetAudioEffect() {
        if (mLastEffectList.isEmpty()) {
            resetToDefaultEffect();
        } else {
            mLastEffectList.forEach(effect -> setEffectActive(effect, true));
            mLastEffectList.clear();
        }

        XposedLog.d(TAG, "resetAudioEffect completed");
    }

    private void resetToDefaultEffect() {
        if (isEffectSupported(EFFECT_DOLBY) && isEffectAvailable(EFFECT_DOLBY)) {
            setEffectActive(EFFECT_DOLBY, true);
        } else if (isEffectSupported(EFFECT_MISOUND) && isEffectAvailable(EFFECT_MISOUND)) {
            setEffectActive(EFFECT_MISOUND, true);
        }

        if (isEffectSupported(EFFECT_SPATIAL_AUDIO) && isEffectAvailable(EFFECT_SPATIAL_AUDIO)) {
            setEffectActive(EFFECT_SPATIAL_AUDIO, true);
        }

        if (isEffectSupported(EFFECT_SURROUND) && isEffectAvailable(EFFECT_SURROUND)) {
            setEffectActive(EFFECT_SURROUND, true);
        }
    }

    @Override
    public void dumpAudioEffectState() {
        StringBuilder builder = new StringBuilder("AudioEffect State: ");
        Arrays.stream(EFFECT_ARRAY).forEach(effect ->
            builder.append(effect).append("=").append(isEffectActive(effect)).append(", ")
        );
        XposedLog.d(TAG, builder.toString());
    }
}
