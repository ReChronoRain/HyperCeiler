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
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.EFFECT_ARRAY;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookMethod;
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
import java.util.Arrays;
import java.util.Map;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * FW 模式下的音效控制 UI
 *
 * @author 焕晨HChen
 */
public class NewFWAudioEffectControl extends BaseEffectControlUI {

    private static final String TAG = "NewFWAudioEffectControl";

    @Override
    public void init() {
        hookAudioEffectCenter();
        hookAVDolbyActivity();
    }

    /**
     * 判断是否应该阻止音效切换
     * 只有在启用锁定且耳机连接时才阻止
     */
    private static boolean shouldBlockEffectSwitch() {
        return isLockSelectionEnabled() && getEarPhoneStateFinal();
    }

    /**
     * Hook AudioEffectCenter.setEffectActive
     */
    private void hookAudioEffectCenter() {
        findAndHookMethod("android.media.audiofx.AudioEffectCenter",
            "setEffectActive",
            String.class, boolean.class,
            new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (shouldBlockEffectSwitch()) {
                        String effect = (String) param.getArgs()[0];
                        XposedLog.d(TAG, "Lock enabled and earphone connected, skip setting effect: " + effect);
                        param.setResult(null);
                    }
                }
            }
        );
    }

    /**
     * Hook AV Dolby 设置界面
     */
    private void hookAVDolbyActivity() {
        try {
            // 查找 Activity 类
            Class<?> activityClass = DexKit.findMember("AVDolby", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    return bridge.findClass(FindClass.create()
                        .matcher(ClassMatcher.create().usingStrings("refreshOnEffectChangeBroadcast AV Dolby: "))
                    ).singleOrThrow(() -> new IllegalStateException("Cannot find AVDolby class"));
                }
            });

            // 查找刷新方法
            Method refreshMethod = DexKit.findMember("AVDolby2", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    return bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                            .declaredClass(activityClass)
                            .usingStrings("refreshOnEffectChangeBroadcast AV Dolby: ")
                        )
                    ).singleOrThrow(() -> new IllegalStateException("Cannot find AVDolby refresh method"));
                }
            });

            // 查找音效选择字段
            Field prefsField = DexKit.findMember("preference2", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    return bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                            .declaredClass(activityClass)
                            .type(findClass("miuix.preference.DropDownPreference"))
                        )
                    ).singleOrThrow(() -> new IllegalStateException("Cannot find preference field"));
                }
            });

            // Hook 方法
            Method onCreate = activityClass.getDeclaredMethod("onCreatePreferences", Bundle.class, String.class);
            Method onResume = activityClass.getDeclaredMethod("onResume");

            hook(onCreate, createOnCreatePreferencesHook(prefsField));

            // 创建通用的刷新 Hook
            IMethodHook refreshHook = new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    Object effectSelection = getFieldValue(param.getThisObject(), prefsField);
                    mEffectSelectionPrefsRef.set(effectSelection);
                    updateEffectSelectionState();
                    updateAutoSEffSwitchInfo();
                }
            };

            hook(refreshMethod, refreshHook);
            hook(onResume, refreshHook);

        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to hook AVDolby activity", e);
        }
    }

    @Override
    protected String buildInfoSummary() {
        StringBuilder sb = buildBasicInfo();

        // 支持状态信息
        Map<String, String> supportMap = safeGetMap(IEffectInfo::getEffectSupportMap);
        if (supportMap != null) {
            sb.append("\n# Effect Support Info:\n");
            appendEffectStates(sb, supportMap, "isSupport");
        }

        // 可用状态信息
        Map<String, String> availableMap = safeGetMap(IEffectInfo::getEffectAvailableMap);
        if (availableMap != null) {
            sb.append("\n# Effect Available Info:\n");
            appendEffectStates(sb, availableMap, "isAvailable");
        }

        // 激活状态信息
        Map<String, String> activeMap = safeGetMap(IEffectInfo::getEffectActiveMap);
        if (activeMap != null) {
            sb.append("\n# Effect Active Info:\n");
            appendEffectStates(sb, activeMap, "isActive");
            XposedLog.d(TAG, "Effect Active Info: " + activeMap);
        }

        return sb.toString();
    }

    /**
     * 追加音效状态信息
     */
    private void appendEffectStates(StringBuilder sb, Map<String, String> map, String prefix) {
        Arrays.stream(EFFECT_ARRAY).forEach(effect -> {
            String state = map.get(effect);
            String capitalizedEffect = capitalizeFirst(effect);
            sb.append(prefix).append(capitalizedEffect).append(": ").append(state).append("\n");
        });
    }

    /**
     * 首字母大写
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
