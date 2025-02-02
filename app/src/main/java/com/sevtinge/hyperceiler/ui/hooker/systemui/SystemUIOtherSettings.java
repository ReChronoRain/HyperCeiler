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
package com.sevtinge.hyperceiler.ui.hooker.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.ComponentName;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.hooker.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.ToastHelper;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.utils.shell.ShellPackageManager;

import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;

public class SystemUIOtherSettings extends DashboardFragment
        implements Preference.OnPreferenceChangeListener{
    SwitchPreference mDisableInfinitymodeGesture;
    SwitchPreference mVolume;
    SwitchPreference mPower;
    SwitchPreference mFuckSG;
    SwitchPreference mTimer;
    SwitchPreference mCollpasedColumnPress;
    // 数据显示
    DropDownPreference mPctStyle;
    SwitchPreference mBrightness1;
    SwitchPreference mVolume1;
    SwitchPreference mBrightness2;
    SwitchPreference mVolume2;
    SeekBarPreferenceCompat mShowPctTop;
    SwitchPreference mShowPctBlur;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_other;
    }

    @Override
    public void initPrefs() {
        int mPct = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_system_ui_others_pct_style", "0"));

        mDisableInfinitymodeGesture = findPreference("prefs_key_system_ui_disable_infinitymode_gesture");
        mVolume = findPreference("prefs_key_system_ui_disable_volume");
        mPower = findPreference("prefs_key_system_ui_disable_power");
        mFuckSG = findPreference("prefs_key_system_ui_move_log_to_miui");
        mTimer = findPreference("prefs_key_system_ui_volume_timer");
        mCollpasedColumnPress = findPreference("prefs_key_system_ui_volume_collpased_column_press");

        mPctStyle = findPreference("prefs_key_system_ui_others_pct_style");
        mBrightness1 = findPreference("prefs_key_system_ui_control_center_qs_brightness_top_value_show");
        mVolume1 = findPreference("prefs_key_system_ui_control_center_qs_volume_top_value_show");
        mBrightness2 = findPreference("prefs_key_system_cc_volume_showpct_title");
        mVolume2 = findPreference("prefs_key_system_showpct_title");
        mShowPctTop = findPreference("prefs_key_system_ui_others_showpct_top");
        mShowPctBlur = findPreference("prefs_key_system_showpct_use_blur");

        mDisableInfinitymodeGesture.setVisible(isPad());

        if (isMoreHyperOSVersion(2f)) {
            mFuckSG.setVisible(true);
            mTimer.setVisible(false);
            mCollpasedColumnPress.setVisible(true);

            setStyleMode(mPct);
            mPctStyle.setOnPreferenceChangeListener(this);
        } else {
            mFuckSG.setVisible(false);
            mTimer.setVisible(true);
            mCollpasedColumnPress.setVisible(false);

            mPctStyle.setVisible(false);
            mBrightness1.setVisible(false);
            mVolume1.setVisible(false);
        }

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

        mPower.setOnPreferenceChangeListener((preference, o) -> {
            boolean value = (boolean) o;
            boolean result = true;

            ComponentName componentName = new ComponentName("miui.systemui.plugin",
                "miui.systemui.globalactions.GlobalActionsPlugin");
            try {
                PackageManager packageManager = requireContext().getPackageManager();
                if (value) {
                    packageManager.setComponentEnabledSetting(componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
                } else {
                    packageManager.setComponentEnabledSetting(componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
                }
            } catch (Exception e) {
                result = ShellPackageManager.enableOrDisable(componentName, !value);
            } finally {
                if (!result) {
                    ToastHelper.makeText(requireContext(),
                        getString(R.string.preference_enable_failed, preference.getTitle()));
                }
            }
            return result;
        });
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mPctStyle) {
            setStyleMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setStyleMode(int mode) {
        mBrightness1.setVisible(mode == 1);
        mVolume1.setVisible(mode == 1);
        mBrightness2.setVisible(mode == 2);
        mVolume2.setVisible(mode == 2);
        mShowPctTop.setVisible(mode == 2);
        mShowPctBlur.setVisible(mode == 2);
    }
}
