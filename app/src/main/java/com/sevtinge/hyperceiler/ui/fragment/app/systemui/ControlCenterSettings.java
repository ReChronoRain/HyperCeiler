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

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.SeekBar;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.prefs.RecommendPreference;
import com.sevtinge.hyperceiler.ui.activity.SubPickerActivity;
import com.sevtinge.hyperceiler.ui.activity.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.ui.fragment.sub.AppPicker;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;

public class ControlCenterSettings extends DashboardFragment {

    Preference mExpandNotification;
    Preference mMusic;
    PreferenceCategory mCard;
    PreferenceCategory mOldCCGrid;
    SwitchPreference mNotice;
    SwitchPreference mNoticex;
    SwitchPreference mSwitchCCAN;
    SwitchPreference mSpotlightNotifColorMix;
    SeekBarPreferenceCompat mNewCCGrid;
    SeekBarPreferenceCompat mNewCCGridColumns;
    DropDownPreference mBluetoothSytle;
    SwitchPreference mThemeBlur;
    SwitchPreference mRedirectNotice;
    SwitchPreference mShadeHeaderBlur;
    SwitchPreference mNotifrowmenu;
    RecommendPreference mRecommend;
    SwitchPreference mBrightness;
    DropDownPreference mBrightnessValue;
    SwitchPreference mVolume;
    DropDownPreference mVolumeValue;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_control_center;
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
        mMusic = findPreference("prefs_key_system_ui_control_center_media_control_media_custom");
        mCard = findPreference("prefs_key_system_ui_controlcenter_card");
        mOldCCGrid = findPreference("prefs_key_system_ui_controlcenter_old");
        mExpandNotification = findPreference("prefs_key_system_ui_control_center_expand_notification");
        mNewCCGrid = findPreference("prefs_key_system_control_center_cc_rows");
        mNewCCGridColumns = findPreference("prefs_key_system_control_center_cc_columns");
        mNotice = findPreference("prefs_key_n_enable");
        mNoticex = findPreference("prefs_key_n_enable_fix");
        mSwitchCCAN = findPreference("prefs_key_system_ui_control_center_switch_cc_and_notification");
        mBluetoothSytle = findPreference("prefs_key_system_ui_control_center_cc_bluetooth_tile_style");
        mThemeBlur = findPreference("prefs_key_system_ui_control_center_unlock_blur_supported");
        mNotifrowmenu = findPreference("prefs_key_system_ui_control_center_notifrowmenu");
        mRedirectNotice = findPreference("prefs_key_system_ui_control_center_redirect_notice");
        mSpotlightNotifColorMix = findPreference("prefs_key_system_ui_control_center_opt_notification_element_background_color");
        mShadeHeaderBlur = findPreference("prefs_key_system_ui_shade_header_gradient_blur");
        mBrightness = findPreference("prefs_key_system_ui_control_center_qs_brightness_top_value_show");
        mBrightnessValue = findPreference("prefs_key_system_ui_control_center_qs_brightness_top_value_show_value");
        mVolume = findPreference("prefs_key_system_ui_control_center_qs_volume_top_value_show");
        mVolumeValue = findPreference("prefs_key_system_ui_control_center_qs_volume_top_value_show_value");

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
            mCard.setVisible(false);
            mNewCCGridColumns.setVisible(false);
            mNotice.setVisible(false);
            mBluetoothSytle.setVisible(false);
            mNotifrowmenu.setVisible(false);
            mMusic.setVisible(true);
            mThemeBlur.setVisible(true);
        } else {
            mNewCCGrid.setVisible(true);
            mCard.setVisible(true);
            mNewCCGridColumns.setVisible(true);
            mNotice.setVisible(true);
            mBluetoothSytle.setVisible(true);
            mNotifrowmenu.setVisible(true);
            mMusic.setVisible(false);
            mThemeBlur.setVisible(false);
        }

        if (isMoreHyperOSVersion(2f)) {
            mOldCCGrid.setVisible(false);
            mSwitchCCAN.setVisible(false);
            mSpotlightNotifColorMix.setVisible(isMoreAndroidVersion(35));
            mShadeHeaderBlur.setVisible(isMoreAndroidVersion(35));
            mBrightness.setVisible(true);
            mBrightnessValue.setVisible(true);
            mVolume.setVisible(true);
            mVolumeValue.setVisible(true);
        } else {
            mOldCCGrid.setVisible(true);
            mSwitchCCAN.setVisible(true);
            mSpotlightNotifColorMix.setVisible(false);
            mShadeHeaderBlur.setVisible(false);
            mBrightness.setVisible(false);
            mBrightnessValue.setVisible(false);
            mVolume.setVisible(false);
            mVolumeValue.setVisible(false);
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
        mRecommend = new RecommendPreference(getContext());
        getPreferenceScreen().addPreference(mRecommend);

        if (isMoreHyperOSVersion(1f))
            args1.putString(":settings:fragment_args_key", "prefs_key_new_clock_status");
        else args1.putString(":settings:fragment_args_key", "prefs_key_old_clock_status");
        mRecommend.addRecommendView(getString(R.string.system_ui_statusbar_clock_title),
                null,
                StatusBarSettings.class,
                args1,
                R.string.system_ui_statusbar_title
        );

    }
}
