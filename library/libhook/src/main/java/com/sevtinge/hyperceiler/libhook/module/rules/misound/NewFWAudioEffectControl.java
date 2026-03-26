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
package com.sevtinge.hyperceiler.hook.module.rules.misound;

import static com.hchen.hooktool.core.CoreTool.callMethod;
import static com.hchen.hooktool.core.CoreTool.findClass;
import static com.hchen.hooktool.core.CoreTool.getField;
import static com.hchen.hooktool.core.CoreTool.hook;
import static com.hchen.hooktool.core.CoreTool.hookAll;
import static com.hchen.hooktool.core.CoreTool.hookMethod;
import static com.hchen.hooktool.core.CoreTool.newInstance;
import static com.sevtinge.hyperceiler.hook.module.rules.misound.NewAutoSEffSwitch.getEarPhoneStateFinal;
import static com.sevtinge.hyperceiler.hook.module.rules.misound.NewAutoSEffSwitch.isSupportFW;
import static com.sevtinge.hyperceiler.hook.utils.api.effect.EffectItem.mEffectArray;
import static com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logI;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import com.hchen.hooktool.hook.IHook;
import com.sevtinge.hyperceiler.hook.IEffectInfo;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.IDexKit;

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
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

public class NewFWAudioEffectControl {
    public static final String TAG = "NewFWAudioEffectControl";
    private Object mEffectSelectionPrefs;
    private Object mPreference;
    public IEffectInfo mIEffectInfo;

    public void init() {
        hookMethod("android.media.audiofx.AudioEffectCenter",
                "setEffectActive",
                String.class, boolean.class,
                new IHook() {
                    @Override
                    public void before() {
                        if (getEarPhoneStateFinal()) {
                            logI(TAG, "earphone is connection, skip set effect: " + getArg(0) + "!!");
                            returnNull();
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
            hook(create, new IHook() {
                @Override
                public void after() {
                    Context context = (Context) callThisMethod("requireContext");
                    Object preferenceScreen = callThisMethod("getPreferenceScreen");
                    Object preferenceCategory = newInstance(preferenceCategoryClass, context, null);
                    callMethod(preferenceCategory, "setTitle", "HyperCeiler (AutoSEffSwitch)");
                    callMethod(preferenceCategory, "setKey", "auto_effect_switch");

                    mPreference = newInstance(preferenceClass, context, null);
                    // callMethod(mPreference, "setTitle", "基本信息:");
                    callMethod(mPreference, "setKey", "auto_effect_switch_pref");
                    updateAutoSEffSwitchInfo();

                    callMethod(preferenceScreen, "addPreference", preferenceCategory);
                    callMethod(preferenceCategory, "addPreference", mPreference);
                    logI(TAG, "create pref category: " + preferenceCategory);

                    mEffectSelectionPrefs = getField(thisObject(), prefsField);
                    updateEffectSelectionState();
                }
            });
            hookAll(new Member[]{refresh, onResume}, new IHook() {
                @Override
                public void after() {
                    mEffectSelectionPrefs = getField(thisObject(), prefsField);
                    updateEffectSelectionState();
                    updateAutoSEffSwitchInfo();
                }
            });
        } catch (Throwable e) {
            logE(TAG, e);
        }
    }

    public void updateEffectSelectionState() {
        if (mEffectSelectionPrefs == null) return;
        if (getEarPhoneStateFinal()) {
            callMethod(mEffectSelectionPrefs, "setEnabled", false);
            logI(TAG, "Disable effect selection: " + mEffectSelectionPrefs);
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
                logE(TAG, e);
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
                logE(TAG, e);
            }

            try {
                Map<String, String> map = mIEffectInfo.getEffectActiveMap();
                logI(TAG, "Effect Active Info: " + map);
                sb.append("\n# Effect Active Info:\n");
                Arrays.stream(mEffectArray).forEach(s -> {
                    String state = map.get(s);
                    sb.append("isActive").append(s.substring(0, 1).toUpperCase())
                            .append(s.substring(1).toLowerCase())
                            .append(": ").append(state).append("\n");
                });
            } catch (RemoteException e) {
                logE(TAG, e);
            }
        }

        callMethod(mPreference, "setSummary", sb.toString());
    }
}
