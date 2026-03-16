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
package com.sevtinge.hyperceiler.hooker.systemui;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;

public class MediaCardSettings extends DashboardFragment implements Preference.OnPreferenceChangeListener {

    DropDownPreference mMediaBackgroundMode;
    SwitchPreference mColorAnim;
    SwitchPreference mInverseColor;
    SwitchPreference mAmbientLight;
    SwitchPreference mAmbientLightOpt;
    SwitchPreference mAlwaysDark;
    SeekBarPreferenceCompat mBlurRadius;
    SwitchPreference mProgressOn;
    DropDownPreference mProgressMode;
    DropDownPreference mProgressThumbMode;
    SeekBarPreferenceCompat mProgressModeThickness;
    SeekBarPreferenceCompat mProgressModeCornerRadius;
    SwitchPreference mProgressComet;
    SwitchPreference mProgressRound;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_control_center_media_cards;
    }

    @Override
    public void initPrefs() {
        mMediaBackgroundMode = findPreference("prefs_key_system_ui_control_center_media_control_background_mode");
        mColorAnim = findPreference("prefs_key_system_ui_control_center_media_control_control_color_anim");
        mInverseColor = findPreference("prefs_key_system_ui_control_center_media_control_inverse_color");
        mAmbientLight = findPreference("prefs_key_system_ui_control_center_media_control_ambient_light");
        mAmbientLightOpt = findPreference("prefs_key_system_ui_control_center_media_control_ambient_light_opt");
        mAlwaysDark = findPreference("prefs_key_system_ui_control_center_media_control_always_dark");
        mBlurRadius = findPreference("prefs_key_system_ui_control_center_media_control_panel_background_blur");

        mProgressOn = findPreference("prefs_key_system_ui_control_center_media_control_progress_on");
        mProgressMode = findPreference("prefs_key_system_ui_control_center_media_control_progress_mode");
        mProgressThumbMode = findPreference("prefs_key_system_ui_control_center_media_control_progress_thumb_mode");
        mProgressModeThickness = findPreference("prefs_key_system_ui_control_center_media_control_progress_thickness");
        mProgressModeCornerRadius = findPreference("prefs_key_system_ui_control_center_media_control_progress_corner_radius");
        mProgressComet = findPreference("prefs_key_system_ui_control_center_media_control_progress_comet");
        mProgressRound = findPreference("prefs_key_system_ui_control_center_media_control_progress_round");

        int mediaBackgroundModeValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_control_center_media_control_background_mode", "0"));
        if (isMoreHyperOSVersion(3f)) {
            mMediaBackgroundMode.setEntries(R.array.system_ui_control_center_media_control_background_mode_new);
            mMediaBackgroundMode.setEntryValues(R.array.system_ui_control_center_media_control_background_mode_new_value);

            if (mediaBackgroundModeValue == 5) {
                cleanKey("prefs_key_system_ui_control_center_media_control_background_mode");
            }
        } else setPreVisible(mProgressThumbMode, false);
        mColorAnim.setVisible(mediaBackgroundModeValue != 0 && mediaBackgroundModeValue != 5);
        mInverseColor.setVisible(mediaBackgroundModeValue == 4);
        mBlurRadius.setVisible(mediaBackgroundModeValue == 2);

        boolean ambientLightEnabled = getSharedPreferences().getBoolean("prefs_key_system_ui_control_center_media_control_ambient_light", false);
        mAmbientLight.setVisible(mediaBackgroundModeValue == 0);
        mAmbientLightOpt.setVisible(mediaBackgroundModeValue == 0 && ambientLightEnabled);
        mAlwaysDark.setVisible(mediaBackgroundModeValue == 0);

        boolean progressOn = getSharedPreferences().getBoolean("prefs_key_system_ui_control_center_media_control_progress_on", false);
        int progressModeValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_control_center_media_control_progress_mode", "0"));
        setProgressVisibility(progressOn, progressModeValue);

        mMediaBackgroundMode.setOnPreferenceChangeListener(this);
        mProgressOn.setOnPreferenceChangeListener(this);
        mProgressMode.setOnPreferenceChangeListener(this);
        mAmbientLight.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mMediaBackgroundMode) {
            setMediaBackgroundMode(Integer.parseInt((String) o));
        } else if (preference == mProgressOn) {
            boolean enabled = (boolean) o;
            int progressModeValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_control_center_media_control_progress_mode", "0"));
            setProgressVisibility(enabled, progressModeValue);
        } else if (preference == mProgressMode) {
            setCanBeVisibleProgressMode(Integer.parseInt((String) o));
        } else if (preference == mAmbientLight) {
            boolean enabled = (boolean) o;
            mAmbientLightOpt.setVisible(enabled && mAmbientLight.isVisible());
        }
        return true;
    }

    private void setMediaBackgroundMode(int mode) {
        mColorAnim.setVisible(mode != 0 && mode != 5);
        mInverseColor.setVisible(mode == 4);
        mBlurRadius.setVisible(mode == 2);

        boolean ambientLightEnabled = getSharedPreferences().getBoolean("prefs_key_system_ui_control_center_media_control_ambient_light", false);
        mAmbientLight.setVisible(mode == 0);
        mAmbientLightOpt.setVisible(mode == 0 && ambientLightEnabled);
        mAlwaysDark.setVisible(mode == 0);
    }

    private void setProgressVisibility(boolean progressOn, int progressMode) {
        mProgressMode.setVisible(progressOn);
        if (progressOn) {
            setCanBeVisibleProgressMode(progressMode);
        } else {
            mProgressThumbMode.setVisible(false);
            mProgressModeThickness.setVisible(false);
            mProgressModeCornerRadius.setVisible(false);
            mProgressComet.setVisible(false);
            mProgressRound.setVisible(false);
        }
    }

    private void setCanBeVisibleProgressMode(int progressMode) {
        mProgressModeThickness.setVisible(progressMode == 0 || progressMode == 2);
        mProgressModeCornerRadius.setVisible(progressMode == 2);
        mProgressComet.setVisible(progressMode == 2);
        mProgressRound.setVisible(progressMode == 2);
        mProgressThumbMode.setVisible(progressMode != 0 && isMoreHyperOSVersion(3f));
    }
}

