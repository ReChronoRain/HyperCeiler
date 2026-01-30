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

import static com.sevtinge.hyperceiler.libhook.rules.misound.NewAudioEffectControl.getField;
import static com.sevtinge.hyperceiler.libhook.rules.misound.NewAutoSEffSwitch.getEarPhoneStateFinal;
import static com.sevtinge.hyperceiler.libhook.rules.misound.NewAutoSEffSwitch.isSupportFW;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.mEffectArray;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.callMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findClass;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.hookMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.newInstance;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.KtHelpUtilsKt.hook;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import com.sevtinge.hyperceiler.libhook.IEffectInfo;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

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
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class NewFWAudioEffectControl {
    public static final String TAG = "NewFWAudioEffectControl";
    private Object mEffectSelectionPrefs;
    private Object mPreference;
    public IEffectInfo mIEffectInfo;

    public void init() {
        EzxHelpUtils.findAndHookMethod("android.media.audiofx.AudioEffectCenter",
                "setEffectActive",
                String.class, boolean.class,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        if (getEarPhoneStateFinal()) {
                            XposedLog.d(TAG, "com.miui.misound", "earphone is connection, skip set effect: " + param.getArgs()[0] + "!!");
                            param.setResult(null);
                        }
                    }
                }
        );

        try {
            Class<?> activityClass = DexKit.findMember("AVDolby", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    ClassData classData = bridge.findClass(FindClass.create()
                        .matcher(
                            ClassMatcher.create().usingStrings("refreshOnEffectChangeBroadcast AV Dolby: ")
                        )
                    ).singleOrThrow(() -> new IllegalStateException(TAG + ": Cannot found Class AV Dolby"));
                    return classData;
                }
            });
            Method refresh = DexKit.findMember("AVDolby2", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                            .declaredClass(activityClass)
                            .usingStrings("refreshOnEffectChangeBroadcast AV Dolby: ")
                        )
                    ).singleOrThrow(() -> new IllegalStateException(TAG + ": Cannot found AV Dolby2"));
                    return methodData;
                }
            });
            Method create = activityClass.getDeclaredMethod("onCreatePreferences", Bundle.class, String.class);
            Method onResume = activityClass.getDeclaredMethod("onResume");
            Field prefsField = DexKit.findMember("preference2", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                            .declaredClass(activityClass)
                            .type(findClass("miuix.preference.DropDownPreference"))
                        )
                    ).singleOrThrow(() -> new IllegalStateException(TAG + ": Cannot found field preference2"));
                    return fieldData;
                }
            });

            Class<?> preferenceCategoryClass = findClass("miuix.preference.PreferenceCategory");
            Class<?> preferenceClass = findClass("androidx.preference.Preference");
            hook(create, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) throws IllegalAccessException {
                    Context context = (Context) callMethod(param.getThisObject(), "requireContext");
                    Object preferenceScreen = callMethod(param.getThisObject(), "getPreferenceScreen");
                    Object preferenceCategory = newInstance(preferenceCategoryClass, context, null);
                    callMethod(preferenceCategory, "setTitle", "HyperCeiler (AutoSEffSwitch)");
                    callMethod(preferenceCategory, "setKey", "auto_effect_switch");

                    mPreference = newInstance(preferenceClass, context, null);
                    // callMethod(mPreference, "setTitle", "基本信息:");
                    callMethod(mPreference, "setKey", "auto_effect_switch_pref");
                    updateAutoSEffSwitchInfo();

                    callMethod(preferenceScreen, "addPreference", preferenceCategory);
                    callMethod(preferenceCategory, "addPreference", mPreference);
                    XposedLog.d(TAG, "com.miui.misound", "create pref category: " + preferenceCategory);

                    mEffectSelectionPrefs = getField(param.getThisObject(), prefsField);
                    updateEffectSelectionState();
                }
            });

            IMethodHook hook = new IMethodHook() {
                @Override
                public void after(AfterHookParam param) throws IllegalAccessException {
                    mEffectSelectionPrefs = getField(param.getThisObject(), prefsField);
                    updateEffectSelectionState();
                    updateAutoSEffSwitchInfo();
                }
            };

            hookMethod(refresh, hook);
            hookMethod(onResume, hook);
        } catch (Throwable e) {
            XposedLog.e(TAG, "com.miui.misound", e);
        }
    }

    public void updateEffectSelectionState() {
        if (mEffectSelectionPrefs == null) return;
        if (getEarPhoneStateFinal()) {
            callMethod(mEffectSelectionPrefs, "setEnabled", false);
            XposedLog.d(TAG, "com.miui.misound", "Disable effect selection: " + mEffectSelectionPrefs);
        } else
            callMethod(mEffectSelectionPrefs, "setEnabled", true);
    }

    private void updateAutoSEffSwitchInfo() {
        if (mPreference == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("isSupport FW: ").append(isSupportFW()).append("\n");
        sb.append("isEarPhoneConnection: ").append(getEarPhoneStateFinal()).append("\n");

        if (mIEffectInfo != null) {
            try {
                Map<String, String> map = mIEffectInfo.getEffectSupportMap();
                sb.append("\n# Effect Support Info:\n");
                Arrays.stream(mEffectArray).forEach(s -> {
                    String state = map.get(s);
                    sb.append("isSupport").append(s.substring(0, 1).toUpperCase())
                            .append(s.substring(1).toLowerCase())
                            .append(": ").append(state).append("\n");
                });
            } catch (RemoteException e) {
                XposedLog.e(TAG, "com.miui.misound", e);
            }

            try {
                Map<String, String> map = mIEffectInfo.getEffectAvailableMap();
                sb.append("\n# Effect Available Info:\n");
                Arrays.stream(mEffectArray).forEach(s -> {
                    String state = map.get(s);
                    sb.append("isAvailable").append(s.substring(0, 1).toUpperCase())
                            .append(s.substring(1).toLowerCase())
                            .append(": ").append(state).append("\n");
                });
            } catch (RemoteException e) {
                XposedLog.e(TAG, "com.miui.misound", e);
            }

            try {
                Map<String, String> map = mIEffectInfo.getEffectActiveMap();
                XposedLog.d(TAG, "com.miui.misound", "Effect Active Info: " + map);
                sb.append("\n# Effect Active Info:\n");
                Arrays.stream(mEffectArray).forEach(s -> {
                    String state = map.get(s);
                    sb.append("isActive").append(s.substring(0, 1).toUpperCase())
                            .append(s.substring(1).toLowerCase())
                            .append(": ").append(state).append("\n");
                });
            } catch (RemoteException e) {
                XposedLog.e(TAG, "com.miui.misound", e);
            }
        }

        callMethod(mPreference, "setSummary", sb.toString());
    }
}
