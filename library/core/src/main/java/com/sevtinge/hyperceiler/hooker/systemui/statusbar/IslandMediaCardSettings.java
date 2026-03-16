package com.sevtinge.hyperceiler.hooker.systemui.statusbar;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;

public class IslandMediaCardSettings extends DashboardFragment implements Preference.OnPreferenceChangeListener {

    DropDownPreference mMediaBackgroundMode;
    SwitchPreference mColorAnim;
    SwitchPreference mInverseColor;
    DropDownPreference mAmbientLight;
    SwitchPreference mAmbientLightOpt;
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
        return R.xml.system_ui_status_bar_island_media_card;
    }

    @Override
    public void initPrefs() {
        mMediaBackgroundMode = findPreference("prefs_key_system_ui_island_media_control_background_mode");
        mColorAnim = findPreference("prefs_key_system_ui_island_media_control_control_color_anim");
        mInverseColor = findPreference("prefs_key_system_ui_island_media_control_inverse_color");
        mAmbientLight = findPreference("prefs_key_system_ui_island_media_control_ambient_light");
        mAmbientLightOpt = findPreference("prefs_key_system_ui_island_media_control_ambient_light_opt");
        mBlurRadius = findPreference("prefs_key_system_ui_island_media_control_panel_background_blur");

        mProgressOn = findPreference("prefs_key_system_ui_island_media_control_progress_on");
        mProgressMode = findPreference("prefs_key_system_ui_island_media_control_progress_mode");
        mProgressThumbMode = findPreference("prefs_key_system_ui_island_media_control_progress_thumb_mode");
        mProgressModeThickness = findPreference("prefs_key_system_ui_island_media_control_progress_thickness");
        mProgressModeCornerRadius = findPreference("prefs_key_system_ui_island_media_control_progress_corner_radius");
        mProgressComet = findPreference("prefs_key_system_ui_island_media_control_progress_comet");
        mProgressRound = findPreference("prefs_key_system_ui_island_media_control_progress_round");

        int mediaBackgroundModeValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_island_media_control_background_mode", "0"));
        mColorAnim.setVisible(mediaBackgroundModeValue != 0);
        mInverseColor.setVisible(mediaBackgroundModeValue == 4);
        mBlurRadius.setVisible(mediaBackgroundModeValue == 2);

        int ambientLightValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_island_media_control_ambient_light", "0"));
        mAmbientLight.setVisible(mediaBackgroundModeValue == 0);
        mAmbientLightOpt.setVisible(mediaBackgroundModeValue == 0 && ambientLightValue == 2);

        boolean progressOn = getSharedPreferences().getBoolean("prefs_key_system_ui_island_media_control_progress_on", false);
        int progressModeValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_island_media_control_progress_mode", "0"));
        setProgressVisibility(progressOn, progressModeValue);

        mMediaBackgroundMode.setOnPreferenceChangeListener(this);
        mAmbientLight.setOnPreferenceChangeListener(this);
        mProgressOn.setOnPreferenceChangeListener(this);
        mProgressMode.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mMediaBackgroundMode) {
            setMediaBackgroundMode(Integer.parseInt((String) o));
        } else if (preference == mAmbientLight) {
            int ambientLightValue = Integer.parseInt((String) o);
            int mediaBackgroundModeValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_island_media_control_background_mode", "0"));
            mAmbientLightOpt.setVisible(mediaBackgroundModeValue == 0 && ambientLightValue == 2);
        } else if (preference == mProgressOn) {
            boolean enabled = (boolean) o;
            int progressModeValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_island_media_control_progress_mode", "0"));
            setProgressVisibility(enabled, progressModeValue);
        } else if (preference == mProgressMode) {
            setCanBeVisibleProgressMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setMediaBackgroundMode(int mode) {
        mColorAnim.setVisible(mode != 0 && mode != 5);
        mInverseColor.setVisible(mode == 4);
        mBlurRadius.setVisible(mode == 2);

        int ambientLightValue = Integer.parseInt(getSharedPreferences().getString("prefs_key_system_ui_island_media_control_ambient_light", "0"));
        mAmbientLight.setVisible(mode == 0);
        mAmbientLightOpt.setVisible(mode == 0 && ambientLightValue == 2);
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
        mProgressThumbMode.setVisible(progressMode != 0);
    }
}

