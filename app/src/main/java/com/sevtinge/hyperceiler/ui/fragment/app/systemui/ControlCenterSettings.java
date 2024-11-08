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

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.prefs.RecommendPreference;
import com.sevtinge.hyperceiler.ui.SubPickerActivity;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.ui.fragment.sub.AppPicker;
import com.sevtinge.hyperceiler.utils.KillApp;
import com.sevtinge.hyperceiler.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.utils.devicesdk.TelephonyManager;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.ColorPickerPreference;
import fan.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import fan.preference.SeekBarPreferenceCompat;
import androidx.preference.SwitchPreference;

public class ControlCenterSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    Preference mExpandNotification;
    PreferenceCategory mMusic;
    PreferenceCategory mCard;
    SwitchPreference mNotice;
    SwitchPreference mNoticex;
    SeekBarPreferenceCompat mNewCCGrid;
    SeekBarPreferenceCompat mNewCCGridColumns;
    SwitchPreference mNewCCGridLabel;
    DropDownPreference mFiveG;
    DropDownPreference mBluetoothSytle;
    SwitchPreference mRoundedRect;
    SeekBarPreferenceCompat mRoundedRectRadius;
    SwitchPreference mThemeBlur;
    DropDownPreference mProgressMode;
    SeekBarPreferenceCompat mProgressModeThickness;
    SeekBarPreferenceCompat mProgressModeCornerRadius;
    ColorPickerPreference mSliderColor;
    ColorPickerPreference mProgressBarColor;
    SwitchPreference mRedirectNotice;

    SwitchPreference mTaplus;
    SwitchPreference mNotifrowmenu;
    RecommendPreference mRecommend;
    Handler handler;

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
        mExpandNotification = findPreference("prefs_key_system_ui_control_center_expand_notification");
        mNewCCGrid = findPreference("prefs_key_system_control_center_cc_rows");
        mNewCCGridColumns = findPreference("prefs_key_system_control_center_cc_columns");
        mNewCCGridLabel = findPreference("prefs_key_system_control_center_qs_tile_label");
        mNotice = findPreference("prefs_key_n_enable");
        mNoticex = findPreference("prefs_key_n_enable_fix");
        mBluetoothSytle = findPreference("prefs_key_system_ui_control_center_cc_bluetooth_tile_style");
        mFiveG = findPreference("prefs_key_system_control_center_5g_new_tile");
        mRoundedRect = findPreference("prefs_key_system_ui_control_center_rounded_rect");
        mRoundedRectRadius = findPreference("prefs_key_system_ui_control_center_rounded_rect_radius");
        mTaplus = findPreference("prefs_key_security_center_taplus");
        mThemeBlur = findPreference("prefs_key_system_ui_control_center_unlock_blur_supported");
        mNotifrowmenu = findPreference("prefs_key_system_ui_control_center_notifrowmenu");
        mProgressMode = findPreference("prefs_key_system_ui_control_center_media_control_progress_mode");
        mProgressModeThickness = findPreference("prefs_key_system_ui_control_center_media_control_progress_thickness");
        mProgressModeCornerRadius = findPreference("prefs_key_system_ui_control_center_media_control_progress_corner_radius");
        mSliderColor = findPreference("prefs_key_system_ui_control_center_media_control_seekbar_thumb_color");
        mProgressBarColor = findPreference("prefs_key_system_ui_control_center_media_control_seekbar_color");
        mRedirectNotice = findPreference("prefs_key_system_ui_control_center_redirect_notice");
        handler = new Handler();

        mExpandNotification.setOnPreferenceClickListener(
                preference -> {
                    Intent intent = new Intent(getActivity(), SubPickerActivity.class);
                    intent.putExtra("mode", AppPicker.LAUNCHER_MODE);
                    intent.putExtra("key", preference.getKey());
                    startActivity(intent);
                    return true;
                }
        );

        mTaplus.setOnPreferenceChangeListener(
                (preference, o) -> {
                    killTaplus();
                    return true;
                }
        );

        if (isMoreHyperOSVersion(1f)) {
            mNewCCGrid.setVisible(false);
            mCard.setVisible(false);
            mNewCCGridColumns.setVisible(false);
            mNewCCGridLabel.setVisible(false);
            mNotice.setVisible(false);
            mBluetoothSytle.setVisible(false);
            mNotifrowmenu.setVisible(false);
            mMusic.setVisible(true);
            mThemeBlur.setVisible(true);
            mRoundedRectRadius.setVisible(PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_system_ui_control_center_rounded_rect", false));
        } else {
            mNewCCGrid.setVisible(true);
            mCard.setVisible(true);
            mNewCCGridColumns.setVisible(true);
            mNewCCGridLabel.setVisible(true);
            mNotice.setVisible(true);
            mBluetoothSytle.setVisible(true);
            mNotifrowmenu.setVisible(true);
            mMusic.setVisible(false);
            mThemeBlur.setVisible(false);
            mRoundedRectRadius.setVisible(false);
        }
        mRedirectNotice.setVisible(!isMoreHyperOSVersion(2f));
        mFiveG.setVisible(TelephonyManager.getDefault().isFiveGCapable());
        mProgressModeThickness.setVisible(Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_system_ui_control_center_media_control_progress_mode", "0")) == 2);
        mProgressModeCornerRadius.setVisible(Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_system_ui_control_center_media_control_progress_mode", "0")) == 2);
        mSliderColor.setVisible(Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_system_ui_control_center_media_control_progress_mode", "0")) != 2);
        mProgressBarColor.setVisible(Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_system_ui_control_center_media_control_progress_mode", "0")) != 2);

        mRoundedRect.setOnPreferenceChangeListener(this);
        mProgressMode.setOnPreferenceChangeListener(this);

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

    public void killTaplus() {
        ThreadPoolManager.getInstance().submit(() -> handler.post(() ->
                KillApp.killApps("com.miui.contentextension")));
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mRoundedRect) {
            setCanBeVisibleRoundedRect((Boolean) o);
        } else if (preference == mProgressMode) {
            setCanBeVisibleProgressMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setCanBeVisibleRoundedRect(boolean mode) {
        mRoundedRectRadius.setVisible(mode && isMoreHyperOSVersion(1f));
    }

    private void setCanBeVisibleProgressMode(int mode) {
        mProgressModeThickness.setVisible(mode == 2);
        mProgressModeCornerRadius.setVisible(mode == 2);
        mSliderColor.setVisible(mode != 2);
        mProgressBarColor.setVisible(mode != 2);
    }
}
