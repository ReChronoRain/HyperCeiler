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
package com.sevtinge.hyperceiler.ui.app.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.ComponentName;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.app.dashboard.DashboardFragment;

public class SystemUIOtherSettings extends DashboardFragment {
    SwitchPreference mDisableInfinitymodeGesture;
    SwitchPreference mVolume;
    SwitchPreference mPower;
    SwitchPreference mFuckSG;
    SwitchPreference mTimer;
    SwitchPreference mCollpasedColumnPress;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_other;
    }

    @Override
    public void initPrefs() {
        mDisableInfinitymodeGesture = findPreference("prefs_key_system_ui_disable_infinitymode_gesture");
        mVolume = findPreference("prefs_key_system_ui_disable_volume");
        mPower = findPreference("prefs_key_system_ui_disable_power");
        mFuckSG = findPreference("prefs_key_system_ui_move_log_to_miui");
        mTimer = findPreference("prefs_key_system_ui_volume_timer");
        mCollpasedColumnPress = findPreference("prefs_key_system_ui_volume_collpased_column_press");

        mDisableInfinitymodeGesture.setVisible(isPad());
        mFuckSG.setVisible(isMoreHyperOSVersion(2f));
        mTimer.setVisible(!isMoreAndroidVersion(35));
        mCollpasedColumnPress.setVisible(isMoreHyperOSVersion(2f));

        mVolume.setOnPreferenceChangeListener(
                (preference, o) -> {
                    ComponentName componentName = new ComponentName("miui.systemui.plugin",
                            "miui.systemui.volume.VolumeDialogPlugin");
                    PackageManager packageManager = requireContext().getPackageManager();
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
                PackageManager packageManager = requireContext().getPackageManager();
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
