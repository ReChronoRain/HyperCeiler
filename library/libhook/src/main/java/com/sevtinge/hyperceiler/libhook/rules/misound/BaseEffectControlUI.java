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
import static com.sevtinge.hyperceiler.libhook.rules.misound.NewAutoSEffSwitch.isSupportFW;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.callMethod;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findClass;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.newInstance;

import android.content.Context;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.libhook.IEffectInfo;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

/**
 * 音效控制 UI 基类
 * 提供 Preference 创建和更新的公共逻辑
 *
 * @author 焕晨HChen
 */
public abstract class BaseEffectControlUI {

    protected static final String TAG = "BaseEffectControlUI";

    // Preference 相关
    protected final AtomicReference<Object> mPreferenceRef = new AtomicReference<>();
    protected final AtomicReference<Object> mEffectSelectionPrefsRef = new AtomicReference<>();
    protected final AtomicReference<IEffectInfo> mEffectInfoRef = new AtomicReference<>();

    // 类引用
    protected Class<?> mPreferenceCategoryClass;
    protected Class<?> mPreferenceClass;

    /**
     * 初始化
     */
    public abstract void init();

    /**
     * 设置 IEffectInfo
     */
    public void setEffectInfo(IEffectInfo effectInfo) {
        mEffectInfoRef.set(effectInfo);
    }

    /**
     * 耳机状态变化回调
     */
    public void onEarphoneStateChanged() {
        updateEffectSelectionState();
        updateAutoSEffSwitchInfo();
    }

    /**
     * 初始化 Preference 类
     */
    protected void initPreferenceClasses() {
        mPreferenceCategoryClass = findClass("miuix.preference.PreferenceCategory");
        mPreferenceClass = findClass("androidx.preference.Preference");
    }

    /**
     * 创建信息 Preference
     */
    protected void createInfoPreference(Object thisObject) {
        try {
            Context context = (Context) callMethod(thisObject, "requireContext");
            Object preferenceScreen = callMethod(thisObject, "getPreferenceScreen");

            if (context == null || preferenceScreen == null) {
                XposedLog.w(TAG, "Context or PreferenceScreen is null");
                return;
            }

            // 创建 PreferenceCategory
            Object preferenceCategory = newInstance(mPreferenceCategoryClass, context, null);
            callMethod(preferenceCategory, "setTitle", "HyperCeiler (AutoSEffSwitch)");
            callMethod(preferenceCategory, "setKey", "auto_effect_switch");

            // 创建信息 Preference
            Object preference = newInstance(mPreferenceClass, context, null);
            callMethod(preference, "setKey", "auto_effect_switch_pref");
            mPreferenceRef.set(preference);

            // 添加到界面
            callMethod(preferenceScreen, "addPreference", preferenceCategory);
            callMethod(preferenceCategory, "addPreference", preference);

            // 更新信息
            updateAutoSEffSwitchInfo();
            XposedLog.d(TAG, "Info preference created");

            // 添加到界面
            callMethod(preferenceScreen, "addPreference", preferenceCategory);
            callMethod(preferenceCategory, "addPreference", preference);// 更新信息
            updateAutoSEffSwitchInfo();XposedLog.d(TAG, "Info preference created");
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to create info preference", e);
        }
    }

    /**
     * 更新音效选择控件状态
     */
    public void updateEffectSelectionState() {
        Object effectSelectionPrefs = mEffectSelectionPrefsRef.get();
        if (effectSelectionPrefs == null) return;

        try {
            // 检查是否启用了锁定选择功能
            boolean lockEnabled = isLockSelectionEnabled();
            boolean earphoneConnected = getEarPhoneStateFinal();

            // 只有在启用锁定且耳机连接时才禁用选择
            boolean enabled = !(lockEnabled && earphoneConnected);

            callMethod(effectSelectionPrefs, "setEnabled", enabled);
            XposedLog.d(TAG, "Effect selection enabled: " + enabled +
                " (lockEnabled=" + lockEnabled + ", earphoneConnected=" + earphoneConnected + ")");
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to update effect selection state", e);
        }
    }

    /**
     * 更新自动切换信息显示
     */
    protected void updateAutoSEffSwitchInfo() {
        Object preference = mPreferenceRef.get();
        if (preference == null) return;

        try {
            String summary = buildInfoSummary();
            callMethod(preference, "setSummary", summary);
        } catch (Exception e) {
            XposedLog.e(TAG, "Failed to update info", e);
        }
    }

    /**
     * 构建信息摘要
     */
    protected abstract String buildInfoSummary();

    /**
     * 构建基本信息
     */
    protected StringBuilder buildBasicInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("isSupport FW: ").append(isSupportFW()).append("\n");
        sb.append("isEarPhoneConnection: ").append(getEarPhoneStateFinal()).append("\n");
        sb.append("isLockSelection: ").append(isLockSelectionEnabled()).append("\n");
        return sb;
    }

    /**
     * 安全获取 Map 数据
     */
    protected Map<String, String> safeGetMap(MapSupplier supplier) {
        IEffectInfo effectInfo = mEffectInfoRef.get();
        if (effectInfo == null) return null;

        try {
            return supplier.get(effectInfo);
        } catch (RemoteException e) {
            XposedLog.e(TAG, "Failed to get map from IEffectInfo", e);
            return null;
        }
    }

    /**
     * 安全获取字段值
     */
    protected static Object getFieldValue(@NonNull Object instance, @NonNull Field field) {
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (IllegalAccessException e) {
            XposedLog.e(TAG, "Failed to get field value", e);
            return null;
        }
    }

    /**
     * Hook onCreatePreferences 方法
     */
    protected IMethodHook createOnCreatePreferencesHook(Field effectSelectionField) {
        return new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                initPreferenceClasses();
                createInfoPreference(param.getThisObject());

                Object effectSelection = getFieldValue(param.getThisObject(), effectSelectionField);
                mEffectSelectionPrefsRef.set(effectSelection);
                updateEffectSelectionState();
            }
        };
    }

    /**
     * Hook onResume 方法
     */
    protected IMethodHook createOnResumeHook(Field effectSelectionField) {
        return new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Object effectSelection = getFieldValue(param.getThisObject(), effectSelectionField);
                mEffectSelectionPrefsRef.set(effectSelection);
                updateEffectSelectionState();
                updateAutoSEffSwitchInfo();
            }
        };
    }

    /**
     * Map 提供者接口
     */
    @FunctionalInterface
    protected interface MapSupplier {
        Map<String, String> get(IEffectInfo effectInfo) throws RemoteException;
    }
}
