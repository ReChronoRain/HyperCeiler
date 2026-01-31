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
package com.sevtinge.hyperceiler.libhook.rules.misound;

import static com.sevtinge.hyperceiler.libhook.rules.misound.NewAutoSEffSwitch.getEarPhoneStateFinal;
import static com.sevtinge.hyperceiler.libhook.rules.misound.NewAutoSEffSwitch.isLockSelectionEnabled;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_DOLBY;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_DOLBY_CONTROL;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_MISOUND;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_MISOUND_CONTROL;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_SPATIAL_AUDIO;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_SURROUND;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findClass;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.KtHelpUtilsKt.hook;

import android.os.Bundle;

import com.sevtinge.hyperceiler.libhook.IEffectInfo;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindField;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * 非 FW 模式下的音效控制 UI
 *
 * @author 焕晨HChen
 */
public class NewAudioEffectControl extends BaseEffectControlUI {

    private static final String TAG = "NewAudioEffectControl";

    @Override
    public void init() {
        hookDolbySwitch();
        hookMiSoundSwitch();
        hookSpatialAudioActivity();
    }

    /**
     * 判断是否应该阻止音效切换
     * 只有在启用锁定且耳机连接时才阻止
     */
    private static boolean shouldBlockEffectSwitch() {
        return isLockSelectionEnabled() && getEarPhoneStateFinal();
    }

    /**
     * Hook Dolby 开关
     */
    private void hookDolbySwitch() {
        try {
            Method dolbySwitch = DexKit.findMember("setDsOnSafely", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    return bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                            .declaredClass(ClassMatcher.create().usingStrings("setDsOnSafely: enter"))
                            .usingStrings("setDsOnSafely: enter")
                        )
                    ).singleOrThrow(() -> new IllegalStateException("Cannot find setDsOnSafely()"));
                }
            });
            hook(dolbySwitch, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (shouldBlockEffectSwitch()) {
                        param.setResult(null);
                        XposedLog.d(TAG, "Lock enabled and earphone connected, skip setting Dolby");
                    }
                }
            });
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to hook Dolby switch", e);
        }
    }

    /**
     * Hook MiSound 开关
     */
    private void hookMiSoundSwitch() {
        try {
            Method miSoundSwitch = DexKit.findMember("setEffectEnable", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    return bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                            .declaredClass(ClassMatcher.create().usingStrings("setEffectEnable() fail, exception: "))
                            .usingStrings("setEffectEnable() fail, exception: ")
                        )
                    ).singleOrThrow(() -> new IllegalStateException("Cannot find setEffectEnable()"));
                }
            });

            hook(miSoundSwitch, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (shouldBlockEffectSwitch()) {
                        param.setResult(null);
                        XposedLog.d(TAG, "Lock enabled and earphone connected, skip setting MiSound");
                    }
                }
            });
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to hook MiSound switch", e);
        }
    }

    /**
     * Hook 空间音频设置界面
     */
    private void hookSpatialAudioActivity() {
        try {
            // 查找 Activity 类
            Class<?> activityClass = DexKit.findMember("spatialAudio", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    return bridge.findClass(FindClass.create()
                        .matcher(ClassMatcher.create().usingStrings("supports spatial audio 3.0 "))
                    ).singleOrThrow(() -> new IllegalStateException("Cannot find spatialAudio class"));
                }
            });

            // 查找音效选择字段
            Field effectSelectionField = DexKit.findMember("preference", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    return bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                            .declaredClass(activityClass)
                            .type(findClass("miuix.preference.DropDownPreference"))
                            .addReadMethod(MethodMatcher.create()
                                .declaredClass(activityClass)
                                .usingStrings("updateEffectSelectionPreference(): set as "))
                        )
                    ).singleOrThrow(() -> new IllegalStateException("Cannot find preference field"));
                }
            });

            // Hook 方法
            Method onCreate = activityClass.getDeclaredMethod("onCreatePreferences", Bundle.class, String.class);
            Method onResume = activityClass.getDeclaredMethod("onResume");

            hook(onCreate, createOnCreatePreferencesHook(effectSelectionField));
            hook(onResume, createOnResumeHook(effectSelectionField));// Hook 广播接收器
            hookBroadcastReceiver();
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to hook spatial audio activity", e);
        }
    }

    /**
     * Hook 广播接收器
     */
    private void hookBroadcastReceiver() {
        try {
            Method broadcastReceiver = DexKit.findMember("onReceive", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    return bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                            .declaredClass(ClassMatcher.create().usingStrings("onReceive: to refreshEnable"))
                            .usingStrings("onReceive: to refreshEnable")
                        )
                    ).singleOrThrow(() -> new IllegalStateException("Cannot find onReceive()"));
                }
            });

            hook(broadcastReceiver, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    updateEffectSelectionState();
                    updateAutoSEffSwitchInfo();
                }
            });
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to hook broadcast receiver", e);
        }
    }

    @Override
    protected String buildInfoSummary() {
        StringBuilder sb = buildBasicInfo();

        // 控制权信息
        Map<String, String> controlMap = safeGetMap(IEffectInfo::getEffectHasControlMap);
        if (controlMap != null) {
            sb.append("\n# Effect Control Info:\n");
            sb.append("hasControlDolby: ").append(controlMap.get(EFFECT_DOLBY_CONTROL)).append("\n");
            sb.append("hasControlMiSound: ").append(controlMap.get(EFFECT_MISOUND_CONTROL)).append("\n");
        }

        // 启用状态信息
        Map<String, String> enabledMap = safeGetMap(IEffectInfo::getEffectEnabledMap);
        if (enabledMap != null) {
            sb.append("\n# Effect Enable Info:\n");
            sb.append("isEnableDolby: ").append(enabledMap.get(EFFECT_DOLBY)).append("\n");
            sb.append("isEnableMiSound: ").append(enabledMap.get(EFFECT_MISOUND)).append("\n");
            sb.append("isEnableSpatializer: ").append(enabledMap.get(EFFECT_SPATIAL_AUDIO)).append("\n");
            sb.append("isEnable3dSurround: ").append(enabledMap.get(EFFECT_SURROUND)).append("\n");
        }

        return sb.toString();
    }
}
