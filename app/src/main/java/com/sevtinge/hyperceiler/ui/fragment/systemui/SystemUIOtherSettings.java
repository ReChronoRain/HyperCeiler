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
package com.sevtinge.hyperceiler.ui.fragment.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMiuiVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.view.View;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.DropDownPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SwitchPreference;

public class SystemUIOtherSettings extends SettingsPreferenceFragment {

    DropDownPreference mChargeAnimationStyle;
    PreferenceCategory mChargeAnimationTitle;
    PreferenceCategory mMonetOverlay;
    SwitchPreference mOriginCharge;
    SwitchPreference mMiuiMultiWinSwitch;
    SwitchPreference mBottomBar;
    SwitchPreference mVolume;
    SwitchPreference mDisableBluetoothRestrict; // 禁用蓝牙临时关闭

    @Override
    public int getContentResId() {
        return R.xml.system_ui_other;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
            getResources().getString(R.string.system_ui),
            "com.android.systemui"
        );
    }

    @Override
    public void initPrefs() {
        mChargeAnimationStyle = findPreference("prefs_key_system_ui_charge_animation_style");
        mChargeAnimationTitle = findPreference("prefs_key_system_ui_statusbar_charge_animation_title");
        mMonetOverlay = findPreference("prefs_key_system_ui_monet");
        mOriginCharge = findPreference("prefs_key_system_ui_origin_charge_animation");
        mDisableBluetoothRestrict = findPreference("prefs_key_system_ui_disable_bluetooth_restrict");
        mMiuiMultiWinSwitch = findPreference("prefs_key_system_ui_disable_miui_multi_win_switch");
        mBottomBar = findPreference("prefs_key_system_ui_disable_bottombar");
        mVolume = findPreference("prefs_key_system_ui_disable_volume");

        mChargeAnimationTitle.setVisible(!isMoreHyperOSVersion(1f));
        mMonetOverlay.setVisible(!isAndroidVersion(30));
        mOriginCharge.setVisible(isAndroidVersion(31));
        mDisableBluetoothRestrict.setVisible(isMiuiVersion(14f) && isMoreAndroidVersion(31));
        mMiuiMultiWinSwitch.setVisible(isMoreHyperOSVersion(1f) && isMoreAndroidVersion(34));
        mBottomBar.setVisible(isMoreHyperOSVersion(1f) && isMoreAndroidVersion(34));

        mVolume.setOnPreferenceChangeListener(
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
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
            }
        );
    }
}
