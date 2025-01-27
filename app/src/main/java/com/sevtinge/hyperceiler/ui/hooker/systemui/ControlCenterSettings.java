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

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.SeekBar;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.prefs.RecommendPreference;
import com.sevtinge.hyperceiler.ui.base.sub.AppPicker;
import com.sevtinge.hyperceiler.ui.base.sub.SubPickerActivity;
import com.sevtinge.hyperceiler.ui.hooker.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import fan.preference.SeekBarPreferenceCompat;

public class ControlCenterSettings extends DashboardFragment {

    Preference mExpandNotification;
    PreferenceCategory mOldCCGrid;
    SwitchPreference mSwitchCCAN;
    SwitchPreference mSpotlightNotifColorMix;
    SeekBarPreferenceCompat mNewCCGrid;
    SeekBarPreferenceCompat mNewCCGridColumns;
    SwitchPreference mRedirectNotice;
    SwitchPreference mShadeHeaderBlur;
    RecommendPreference mRecommend;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_control_center;
    }

    @Override
    public void initPrefs() {
        mOldCCGrid = findPreference("prefs_key_system_ui_controlcenter_old");
        mExpandNotification = findPreference("prefs_key_system_ui_control_center_expand_notification");
        mNewCCGrid = findPreference("prefs_key_system_control_center_cc_rows");
        mNewCCGridColumns = findPreference("prefs_key_system_control_center_cc_columns");
        mSwitchCCAN = findPreference("prefs_key_system_ui_control_center_switch_cc_and_notification");
        mRedirectNotice = findPreference("prefs_key_system_ui_control_center_redirect_notice");
        mSpotlightNotifColorMix = findPreference("prefs_key_system_ui_control_center_opt_notification_element_background_color");
        mShadeHeaderBlur = findPreference("prefs_key_system_ui_shade_header_gradient_blur");

        mExpandNotification.setOnPreferenceClickListener(
                preference -> {
                    Intent intent = new Intent(getActivity(), SubPickerActivity.class);
                    intent.putExtra("mode", AppPicker.LAUNCHER_MODE);
                    intent.putExtra("key", preference.getKey());
                    startActivity(intent);
                    return true;
                }
        );

        if (isMoreHyperOSVersion(1f)) {
            mNewCCGrid.setVisible(false);
            mNewCCGridColumns.setVisible(false);
        }

        if (isMoreHyperOSVersion(2f)) {
            mOldCCGrid.setVisible(false);
            mSwitchCCAN.setVisible(false);
            mSpotlightNotifColorMix.setVisible(isMoreAndroidVersion(35));
            mShadeHeaderBlur.setVisible(isMoreAndroidVersion(35));
        } else {
            mOldCCGrid.setVisible(true);
            mSwitchCCAN.setVisible(true);
            mSpotlightNotifColorMix.setVisible(false);
            mShadeHeaderBlur.setVisible(false);
        }
        mRedirectNotice.setVisible(!isMoreAndroidVersion(35));

        ((SeekBarPreferenceCompat) findPreference("prefs_key_system_control_center_old_qs_grid_columns")).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                if (progress < 3) progress = 5;
                try {
                    Settings.Secure.putInt(requireActivity().getContentResolver(), "sysui_qqs_count", progress);
                } catch (Throwable t) {
                    AndroidLogUtils.logD("SeekBarPreferenceCompat", "onProgressChanged -> system_control_center_old_qs_grid_columns", t);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Bundle args1 = new Bundle();
        mRecommend = new RecommendPreference(requireContext());
        getPreferenceScreen().addPreference(mRecommend);

        args1.putString(":settings:fragment_args_key", "prefs_key_new_clock_status");
        mRecommend.addRecommendView(getString(R.string.system_ui_statusbar_clock_title),
                null,
                StatusBarSettings.class,
                args1,
                R.string.system_ui_statusbar_title
        );

    }
}
