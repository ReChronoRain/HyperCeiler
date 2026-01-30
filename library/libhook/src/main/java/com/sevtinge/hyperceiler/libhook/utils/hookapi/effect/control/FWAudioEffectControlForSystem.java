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
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_DOLBY;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_MISOUND;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_NONE;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_SPATIAL_AUDIO;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_SURROUND;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.mEffectArray;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.callMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.callStaticMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findClass;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getObjectField;

import android.content.Context;
import android.provider.Settings;

import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.callback.IControlForSystem;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.ArrayList;
import java.util.Arrays;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * 从系统层面控制音效
 *
 * @author 焕晨HChen
 */
public class FWAudioEffectControlForSystem extends BaseEffectControl implements IControlForSystem {
    public static final String TAG = "FWAudioEffectControlForSystem";
    private Context mContext;
    private Object mPresenter = null;
    private Object mCenter = null;
    public static final ArrayList<String> mLastEffectList = new ArrayList<>();

    public void init() {
        findAndHookMethod("android.media.audiofx.AudioEffectCenter",
                "getInstance",
                Context.class, new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        mCenter = param.getResult();
                        if (mCenter == null) return;
                        mPresenter = getObjectField(mCenter, "mPresenter");
                    }
                }
        );

        findAndHookMethod("android.media.audiofx.AudioEffectCenter",
                "setEffectActive",
                String.class, boolean.class,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        if (getEarPhoneStateFinal()) {
                            XposedLog.d(TAG, "earphone is connection, skip set effect: " + param.getArgs()[0] + "!!");
                            param.setResult(null);
                        }
                    }
                }
        );

        findAndHookMethod("android.media.audiofx.AudioEffectCenter",
                "release",
                new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        mPresenter = null;
                        mCenter = null;

                        XposedLog.d(TAG, "AudioEffectCenter release...");
                    }
                }
        );
    }

    public void setContext(Context context) {
        mContext = context;
    }

    private void getInstanceIfNeed() {
        if (mCenter == null)
            if (mContext != null)
                callStaticMethod(
                    findClass("android.media.audiofx.AudioEffectCenter"),
                    "getInstance",
                    mContext
                );
    }

    private boolean isEffectSupported(String effect) {
        getInstanceIfNeed();
        if (mCenter != null)
            return (boolean) callMethod(mCenter, "isEffectSupported", effect);
        return false;
    }

    private boolean isEffectAvailable(String effect) {
        getInstanceIfNeed();
        if (mCenter != null)
            return (boolean) callMethod(mCenter, "isEffectAvailable", effect);
        return false;
    }

    private boolean isEffectActive(String effect) {
        getInstanceIfNeed();
        if (mCenter != null)
            return (boolean) callMethod(mCenter, "isEffectActive", effect);
        return false;
    }

    private void setEffectActive(String effect, boolean active) {
        getInstanceIfNeed();
        if (mPresenter != null) {
            switch (effect) {
                case EFFECT_DOLBY -> {
                    if (active)
                        callMethod(mPresenter, "setDolbyActive");
                }
                case EFFECT_MISOUND -> {
                    if (active)
                        callMethod(mPresenter, "setMiSoundActive");
                }
                case EFFECT_SPATIAL_AUDIO -> {
                    callMethod(mPresenter, "setSpatialAudioActive", active);
                }
                case EFFECT_SURROUND -> {
                    callMethod(mPresenter, "setSurroundActive", active);
                }
            }
        }
    }

    @Override
    void updateEffectMap() {
        mEffectSupportMap.clear();
        Arrays.stream(mEffectArray).forEach(s ->
                mEffectSupportMap.put(s, String.valueOf(isEffectSupported(s))));

        mEffectAvailableMap.clear();
        Arrays.stream(mEffectArray).forEach(s ->
                mEffectAvailableMap.put(s, String.valueOf(isEffectAvailable(s))));

        mEffectActiveMap.clear();
        Arrays.stream(mEffectArray).forEach(s ->
                mEffectActiveMap.put(s, String.valueOf(isEffectActive(s))));

        XposedLog.d(TAG, "updateEffectMap: mCenter: " + mCenter + ", mPresenter: " + mPresenter +
                ", mEffectSupportMap: " + mEffectSupportMap + ", mEffectAvailableMap: " + mEffectAvailableMap + ", mEffectActiveMap: " + mEffectActiveMap);
    }

    @Override
    public void updateLastEffectState() {
        mLastEffectList.clear();
        Arrays.stream(mEffectArray).forEach(s -> {
            if (isEffectActive(s))
                mLastEffectList.add(s);
        });
    }

    @Override
    public void setEffectToNone(Context context) {
        if (mPresenter == null) return;
        callMethod(mPresenter, "setEffectDeactivate");

        if (context != null)
            Settings.Global.putString(context.getContentResolver(), "effect_implementer", EFFECT_NONE);
    }

    @Override
    public void resetAudioEffect() {
        if (mLastEffectList.isEmpty()) {
            if (isEffectSupported(EFFECT_DOLBY) && isEffectAvailable(EFFECT_DOLBY)) {
                setEffectActive(EFFECT_DOLBY, true);
            } else if (isEffectSupported(EFFECT_MISOUND) && isEffectAvailable(EFFECT_MISOUND)) {
                setEffectActive(EFFECT_MISOUND, true);
            }

            if (isEffectSupported(EFFECT_SPATIAL_AUDIO) && isEffectAvailable(EFFECT_SPATIAL_AUDIO))
                setEffectActive(EFFECT_SPATIAL_AUDIO, true);
            if (isEffectSupported(EFFECT_SURROUND) && isEffectAvailable(EFFECT_SURROUND))
                setEffectActive(EFFECT_SURROUND, true);
            return;
        }
        mLastEffectList.forEach(s -> setEffectActive(s, true));
        mLastEffectList.clear();
    }

    @Override
    public void dumpAudioEffectState() {
        StringBuilder builder = new StringBuilder();
        Arrays.stream(mEffectArray).forEach(s ->
                builder.append(s).append(": ").append(isEffectActive(s)).append(", "));
        XposedLog.d(TAG, builder.toString());
    }
}
