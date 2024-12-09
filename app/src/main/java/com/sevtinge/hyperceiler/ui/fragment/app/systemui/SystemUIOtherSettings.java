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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.ui.fragment.app.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMiuiVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.ComponentName;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.dashboard.DashboardFragment;

import fan.preference.DropDownPreference;

public class SystemUIOtherSettings extends DashboardFragment {

    DropDownPreference mChargeAnimationStyle;
    PreferenceCategory mChargeAnimationTitle;
    SwitchPreference mMiuiMultiWinSwitch;
    SwitchPreference mMiuiMultiWinSwitchRemove;
    SwitchPreference mDisableInfinitymodeGesture;
    SwitchPreference mBottomBar;
    SwitchPreference mVolume;
    SwitchPreference mPower;
    SwitchPreference mDisableBluetoothRestrict; // 禁用蓝牙临时关闭
    SwitchPreference mPctUseBlur;
    SwitchPreference mShowPct;
    SwitchPreference mFuckSG;
    SwitchPreference mTimer;
    SwitchPreference mPluginThemeBlur;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_other;
    }

    @Override
    public void initPrefs() {
        mChargeAnimationStyle = findPreference("prefs_key_system_ui_charge_animation_style");
        mChargeAnimationTitle = findPreference("prefs_key_system_ui_statusbar_charge_animation_title");
        mDisableBluetoothRestrict = findPreference("prefs_key_system_ui_disable_bluetooth_restrict");
        mMiuiMultiWinSwitch = findPreference("prefs_key_system_ui_disable_miui_multi_win_switch");
        mMiuiMultiWinSwitchRemove = findPreference("prefs_key_system_ui_remove_miui_multi_win_switch");
        mDisableInfinitymodeGesture = findPreference("prefs_key_system_ui_disable_infinitymode_gesture");
        mBottomBar = findPreference("prefs_key_system_ui_disable_bottombar");
        mVolume = findPreference("prefs_key_system_ui_disable_volume");
        mPower = findPreference("prefs_key_system_ui_disable_power");
        mPctUseBlur = findPreference("prefs_key_system_showpct_use_blur");
        mShowPct = findPreference("prefs_key_system_showpct_title");
        mFuckSG = findPreference("prefs_key_system_ui_move_log_to_miui");
        mTimer = findPreference("prefs_key_system_ui_volume_timer");
        mPluginThemeBlur = findPreference("prefs_key_system_ui_other_default_plugin_theme");

        mChargeAnimationTitle.setVisible(!isMoreHyperOSVersion(1f));
        mDisableBluetoothRestrict.setVisible(isMiuiVersion(14f) && !isMoreHyperOSVersion(1f));
        mMiuiMultiWinSwitch.setVisible(isMoreHyperOSVersion(1f) && isMoreAndroidVersion(34));
        mMiuiMultiWinSwitchRemove.setVisible(isMoreHyperOSVersion(1f) && isMoreAndroidVersion(34) && isPad());
        mDisableInfinitymodeGesture.setVisible(isMoreHyperOSVersion(1f) && isMoreAndroidVersion(34) && isPad());
        mBottomBar.setVisible(isMoreHyperOSVersion(1f) && isMoreAndroidVersion(34));
        mPctUseBlur.setVisible(isMoreHyperOSVersion(1f));
        mShowPct.setVisible(!isMoreHyperOSVersion(1f));
        mFuckSG.setVisible(isMoreHyperOSVersion(2f));
        mTimer.setVisible(!isMoreAndroidVersion(35));
        mPluginThemeBlur.setVisible(isMoreHyperOSVersion(1f));

        mVolume.setOnPreferenceChangeListener(
                (preference, o) -> {
                    ComponentName componentName = new ComponentName("miui.systemui.plugin",
                            "miui.systemui.volume.VolumeDialogPlugin");
                    PackageManager packageManager = getContext().getPackageManager();
                    if ((boolean) o) {
                        packageManager.setComponentEnabledSetting(componentName,
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                PackageManager.DONT_KILL_APP);
                    } else {
                        packageManager.setComponentEnabledSetting(componentName,
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                PackageManager.DONT_KILL_APP);
                    }
                    return true;
                }
        );

        mPower.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
                ComponentName componentName = new ComponentName("miui.systemui.plugin",
                        "miui.systemui.globalactions.GlobalActionsPlugin");
                PackageManager packageManager = getContext().getPackageManager();
                if ((boolean) o) {
                    packageManager.setComponentEnabledSetting(componentName,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                } else {
                    packageManager.setComponentEnabledSetting(componentName,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                }
                return true;
            }
        });
    }
}
