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
import static com.sevtinge.hyperceiler.libhook.rules.misound.NewAutoSEffSwitch.isSupportFW;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.callMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findClass;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.newInstance;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.KtHelpUtilsKt.hook;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.libhook.IEffectInfo;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem;
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
import java.util.Map;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class NewAudioEffectControl {
    public static final String TAG = "NewAudioEffectControl";
    private Object mPreference;
    private Object mEffectSelectionPrefs;
    public IEffectInfo mIEffectInfo;

    public void init() {
        try {
            Method dolbySwitch = DexKit.findMember("setDsOnSafely", new IDexKit() {
                    @Override
                    public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                        MethodData methodData = bridge.findMethod(FindMethod.create()
                            .matcher(MethodMatcher.create()
                                .declaredClass(ClassMatcher.create().usingStrings("setDsOnSafely: enter"))
                                .usingStrings("setDsOnSafely: enter")
                            )
                        ).singleOrThrow(() -> new IllegalStateException(TAG + ": Cannot found setDsOnSafely()"));
                        return methodData;
                    }
            });
            hook(dolbySwitch, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (getEarPhoneStateFinal()) {
                        param.setResult(null);
                        XposedLog.d(TAG, "com.miui.misound", "Don't set dolby mode, in earphone mode!");
                    }
                }
            });

            Method miSoundSwitch = DexKit.findMember("setEffectEnable", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                            .declaredClass(ClassMatcher.create().usingStrings("setEffectEnable() fail, exception: "))
                            .usingStrings("setEffectEnable() fail, exception: ")
                        )
                    ).singleOrThrow(() -> new IllegalStateException(TAG + ": Cannot found setEffectEnable()"));
                    return methodData;
                }
            });
            hook(miSoundSwitch, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    if (getEarPhoneStateFinal()) {
                        param.setResult(null);
                        XposedLog.d(TAG, "com.miui.misound", "Don't set misound mode, in earphone mode!");
                    }
                }
            });

            Class<?> activityClass = DexKit.findMember("spatialAudio", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    ClassData classData = bridge.findClass(FindClass.create()
                        .matcher(
                            ClassMatcher.create().usingStrings("supports spatial audio 3.0 ")
                        )
                    ).singleOrThrow(() -> new IllegalStateException(TAG + ": Cannot found Class spatialAudio"));
                    return classData;
                }
            });
            Method create = activityClass.getDeclaredMethod("onCreatePreferences", Bundle.class, String.class);
            Method onResume = activityClass.getDeclaredMethod("onResume");
            Field effectSelectionField = DexKit.findMember("preference", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    FieldData fieldData = bridge.findField(FindField.create()
                        .matcher(FieldMatcher.create()
                            .declaredClass(activityClass)
                            .type(findClass("miuix.preference.DropDownPreference"))
                            .addReadMethod(MethodMatcher.create()
                                .declaredClass(activityClass)
                                .usingStrings("updateEffectSelectionPreference(): set as ")
                            )
                        )
                    ).singleOrThrow(() -> new IllegalStateException(TAG + ": Cannot found field preference"));
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

                    mEffectSelectionPrefs = getField(param.getThisObject(), effectSelectionField);
                    updateEffectSelectionState();
                }
            });
            hook(onResume, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) throws IllegalAccessException {
                    mEffectSelectionPrefs = getField(param.getThisObject(), effectSelectionField);
                    updateEffectSelectionState();
                    updateAutoSEffSwitchInfo();
                }
            });

            Method broadcastReceiver = DexKit.findMember("onReceive", new IDexKit() {
                @Override
                public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                    MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                            .declaredClass(ClassMatcher.create().usingStrings("onReceive: to refreshEnable"))
                            .usingStrings("onReceive: to refreshEnable")
                        )
                    ).singleOrThrow(() -> new IllegalStateException(TAG + ": Cannot found onReceive()"));
                    return methodData;
                }
            });
            hook(broadcastReceiver, new IMethodHook() {
                @Override
                public void before(BeforeHookParam param) {
                    updateEffectSelectionState();
                    updateAutoSEffSwitchInfo();
                }
            });
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
                Map<String, String> map = mIEffectInfo.getEffectHasControlMap();
                sb.append("\n# Effect Control Info:\n");
                sb.append("hasControlDolby: ").append(map.get(EffectItem.EFFECT_DOLBY_CONTROL)).append("\n");
                sb.append("hasControlMiSound: ").append(map.get(EffectItem.EFFECT_MISOUND_CONTROL)).append("\n");
            } catch (RemoteException e) {
                XposedLog.e(TAG, "com.miui.misound", e);
            }

            try {
                Map<String, String> map = mIEffectInfo.getEffectEnabledMap();
                sb.append("\n# Effect Enable Info: \n");
                sb.append("isEnableDolby: ").append(map.get(EffectItem.EFFECT_DOLBY)).append("\n");
                sb.append("isEnableMiSound: ").append(map.get(EffectItem.EFFECT_MISOUND)).append("\n");
                sb.append("isEnableSpatializer: ").append(map.get(EffectItem.EFFECT_SPATIAL_AUDIO)).append("\n");
                sb.append("isEnable3dSurround: ").append(map.get(EffectItem.EFFECT_SURROUND)).append("\n");
            } catch (RemoteException e) {
                XposedLog.e(TAG, "com.miui.misound", e);
            }
        }

        callMethod(mPreference, "setSummary", sb.toString());
    }

    public static Object getField(@NonNull Object instance, @NonNull Field field) throws IllegalAccessException {
        field.setAccessible(true);
        return field.get(instance);
    }
}
