/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hooker.home;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import fan.preference.DropDownPreference;

public class HomeOtherSettings extends DashboardFragment {

    /**
     * 桌面卸载功能的开关（总开关）。
     */
    private static final String KEY_UNINSTALL_MASTER_SWITCH = "prefs_key_home_other_uninstall_with_default_package_manager";

    /**
     * 桌面卸载器选择项，value 为包名或下面的两个特殊值。
     * <p>
     * Hook 端（UninstallWithDefaultPackageManager）从同一个 key 读结果。
     */
    private static final String KEY_UNINSTALLER = "prefs_key_home_other_uninstaller";

    /**
     * 空字符串：交给系统解析（会按 intent-filter 优先级选 MIUI 包管理器）。
     */
    private static final String VALUE_FOLLOW_SYSTEM = "";

    /**
     * 跟随系统默认安装器：Hook 端在每次卸载时解析当前默认安装器，并用它的卸载 Activity 作为目标。
     */
    private static final String VALUE_FOLLOW_DEFAULT_INSTALLER = "__follow_default_installer__";

    /**
     * 用来探测「谁声明了 {@code package:} 的卸载处理」的示例包名，只需能被 intent-filter 匹配。
     */
    private static final String PROBE_PACKAGE = "com.android.shell";

    /**
     * 用来探测「当前默认安装器」的 apk MIME。
     */
    private static final String MIME_APK = "application/vnd.android.package-archive";

    SwitchPreference mMoveToMinusOneScreen;
    SwitchPreference mWindowedMode;
    SwitchPreference mShareAPK;
    SwitchPreference mEnableMoreSettings;
    SwitchPreference mHideReportText;
    SwitchPreference mDisablePreLoad;


    @Override
    public int getPreferenceScreenResId() {
        if (isMoreHyperOSVersion(3f)) {
            return R.xml.home_other_new;
        }
        return R.xml.home_other;
    }

    @Override
    public void initPrefs() {
        if (isMoreHyperOSVersion(3f)) {
            mMoveToMinusOneScreen = findPreference("prefs_key_home_widget_allow_moved_to_minus_one_screen");
            if (isPad()) setFuncHint(mMoveToMinusOneScreen, 1);
        }

        mWindowedMode = findPreference("prefs_key_home_other_freeform_shortcut_menu");
        mShareAPK = findPreference("prefs_key_home_other_allow_share_apk");
        mHideReportText = findPreference("prefs_key_home_title_hide_report_text");
        mDisablePreLoad = findPreference("prefs_key_home_other_disable_prestart");

        if (isPad()) {
            setFuncHint(mWindowedMode, 2);
            setFuncHint(mShareAPK, 1);
            setFuncHint(mHideReportText, 1);
            setFuncHint(mDisablePreLoad, 1);
        }

        mEnableMoreSettings = findPreference("prefs_key_home_other_mi_pad_enable_more_setting");
        mEnableMoreSettings.setVisible(isPad() && !isMoreHyperOSVersion(3f));

        initUninstallPrefs();
    }

    /**
     * 「卸载器」只在桌面卸载总开关开启时显示，与项目其它联动设置项保持一致的做法。
     */
    private void initUninstallPrefs() {
        DropDownPreference uninstaller = findPreference(KEY_UNINSTALLER);

        if (uninstaller != null) {
            populateUninstallerChoices(uninstaller);
            if (TextUtils.isEmpty(uninstaller.getValue())) {
                uninstaller.setValue(VALUE_FOLLOW_SYSTEM);
            }
        }

        applyUninstallVisibility(uninstaller,
            getSharedPreferences().getBoolean(KEY_UNINSTALL_MASTER_SWITCH, false));

        Preference masterSwitch = findPreference(KEY_UNINSTALL_MASTER_SWITCH);
        if (masterSwitch != null) {
            masterSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                applyUninstallVisibility(uninstaller, Boolean.TRUE.equals(newValue));
                return true;
            });
        }
    }

    /**
     * 按总开关状态收起/展开「卸载器」。
     */
    private void applyUninstallVisibility(Preference uninstaller, boolean visible) {
        if (uninstaller != null) {
            uninstaller.setVisible(visible);
        }
    }

    /**
     * 列出设备上所有能处理 {@code package:} 卸载的应用供选择。
     * <p>
     * 顺序：系统默认 → 跟随默认安装器 → 各卸载器。
     * 需要列出 MIUI 包管理器这类优先级更高的候选，所以不使用 MATCH_DEFAULT_ONLY。
     */
    private void populateUninstallerChoices(DropDownPreference uninstaller) {
        Context context = getContext();
        if (context == null) return;

        PackageManager pm = context.getPackageManager();
        Intent probe = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", PROBE_PACKAGE, null));
        List<ResolveInfo> infos = pm.queryIntentActivities(probe, 0);

        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> values = new ArrayList<>();
        Set<String> seenPackages = new LinkedHashSet<>();

        entries.add(context.getString(R.string.security_uninstaller_system_default));
        values.add(VALUE_FOLLOW_SYSTEM);

        // 跟随默认安装器：这里只展示当前默认安装器的名字，真正的解析在 Hook 端每次卸载时进行。
        entries.add(buildFollowInstallerLabel(context, pm));
        values.add(VALUE_FOLLOW_DEFAULT_INSTALLER);

        if (infos != null) {
            for (ResolveInfo info : infos) {
                if (info.activityInfo == null) continue;
                String pkgName = info.activityInfo.packageName;
                if (TextUtils.isEmpty(pkgName) || !seenPackages.add(pkgName)) continue;
                entries.add(info.loadLabel(pm) + " (" + pkgName + ")");
                values.add(pkgName);
            }
        }

        uninstaller.setEntries(entries.toArray(new CharSequence[0]));
        uninstaller.setEntryValues(values.toArray(new CharSequence[0]));
    }

    /**
     * 「跟随默认安装器（<应用名>）」；解析不到默认安装器时退回不显示应用名的写法。
     */
    private CharSequence buildFollowInstallerLabel(Context context, PackageManager pm) {
        String pkgName = resolveDefaultInstallerPackage(context);
        if (TextUtils.isEmpty(pkgName)) {
            return context.getString(R.string.security_uninstaller_follow_default_installer_unknown);
        }

        CharSequence label = pkgName;
        try {
            label = pm.getApplicationLabel(pm.getApplicationInfo(pkgName, 0));
        } catch (Throwable ignored) {
            // 取不到应用名就直接显示包名。
        }
        return context.getString(R.string.security_uninstaller_follow_default_installer, label);
    }

    /**
     * 系统默认安装器 = 当前处理 apk 安装（{@code application/vnd.android.package-archive}）的默认应用。
     */
    private String resolveDefaultInstallerPackage(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent probe = new Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse("file:///sdcard/__probe__.apk"), MIME_APK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ResolveInfo info = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
        if (info == null || info.activityInfo == null) return null;
        return info.activityInfo.packageName;
    }

}
