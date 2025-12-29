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
package com.sevtinge.hyperceiler.hooker.systemui;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

import fan.preference.ColorPickerPreference;
import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;

public class MediaCardSettings extends DashboardFragment implements Preference.OnPreferenceChangeListener {

    DropDownPreference mMediaBackgroundMode;
    SwitchPreference mColorAnim;
    SwitchPreference mInverseColor;
    SeekBarPreferenceCompat mBlurRadius;
    DropDownPreference mAlbumMode;
    SwitchPreference mOptAlbum;
    DropDownPreference mProgressMode;
    DropDownPreference mProgressThumbMode;
    SeekBarPreferenceCompat mProgressModeThickness;
    SeekBarPreferenceCompat mProgressModeCornerRadius;
    ColorPickerPreference mSliderColor;
    ColorPickerPreference mProgressBarColor;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_control_center_media_cards;
    }

    @Override
    public void initPrefs() {
        mMediaBackgroundMode = findPreference("prefs_key_system_ui_control_center_media_control_background_mode");
        mColorAnim = findPreference("prefs_key_system_ui_control_center_media_control_control_color_anim");
        mInverseColor = findPreference("prefs_key_system_ui_control_center_media_control_inverse_color");
        mBlurRadius = findPreference("prefs_key_system_ui_control_center_media_control_panel_background_blur");

        mAlbumMode = findPreference("prefs_key_system_ui_control_center_media_control_media_album_mode");
        mOptAlbum = findPreference("prefs_key_system_ui_control_center_media_control_album_picture_rounded_corners");

        mProgressMode = findPreference("prefs_key_system_ui_control_center_media_control_progress_mode");
        mProgressThumbMode = findPreference("prefs_key_system_ui_control_center_media_control_progress_thumb_mode");
        mProgressModeThickness = findPreference("prefs_key_system_ui_control_center_media_control_progress_thickness");
        mProgressModeCornerRadius = findPreference("prefs_key_system_ui_control_center_media_control_progress_corner_radius");
        mSliderColor = findPreference("prefs_key_system_ui_control_center_media_control_seekbar_thumb_color");
        mProgressBarColor = findPreference("prefs_key_system_ui_control_center_media_control_seekbar_color");

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

        int progressModeValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_control_center_media_control_progress_mode", "0"));
        mProgressModeThickness.setVisible(progressModeValue == 2);
        mProgressModeCornerRadius.setVisible(progressModeValue == 2);
        mSliderColor.setVisible(mediaBackgroundModeValue != 5 && progressModeValue != 2);
        mProgressBarColor.setVisible(mediaBackgroundModeValue != 5);

        mMediaBackgroundMode.setOnPreferenceChangeListener(this);
        mProgressMode.setOnPreferenceChangeListener(this);

        if (!isMoreHyperOSVersion(3f)) {
            int mediaAlbumModeValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_control_center_media_control_media_album_mode", "0"));
            mOptAlbum.setVisible(mediaAlbumModeValue != 2);

            mAlbumMode.setOnPreferenceChangeListener(this);
        } else {
            setFuncHint(mOptAlbum, 2);
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mMediaBackgroundMode) {
            setMediaBackgroundMode(Integer.parseInt((String) o));
        } else if (preference == mAlbumMode) {
            if (!isMoreHyperOSVersion(3f)) {
                mOptAlbum.setVisible(Integer.parseInt((String) o) != 2);
            }
        } else if (preference == mProgressMode) {
            setCanBeVisibleProgressMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setMediaBackgroundMode(int mode) {
        mColorAnim.setVisible(mode != 0 && mode != 5);
        mInverseColor.setVisible(mode == 4);
        mBlurRadius.setVisible(mode == 2);
        mProgressBarColor.setVisible(mode != 5);
    }

    private void setCanBeVisibleProgressMode(int mode) {
        int mediaBackgroundModeValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_control_center_media_control_background_mode", "0"));

        mProgressModeThickness.setVisible(mode == 2);
        mProgressModeCornerRadius.setVisible(mode == 2);
        mSliderColor.setVisible(mode != 2 && mediaBackgroundModeValue != 5);
    }
}
