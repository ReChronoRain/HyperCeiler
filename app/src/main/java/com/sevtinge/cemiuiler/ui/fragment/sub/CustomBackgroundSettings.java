package com.sevtinge.cemiuiler.ui.fragment.sub;

import android.os.Bundle;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.ColorPickerPreference;
import moralnorm.preference.Preference;
import moralnorm.preference.SeekBarPreference;
import moralnorm.preference.SwitchPreference;

public class CustomBackgroundSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    private SeekBarPreference mBlurRadius;
    private SwitchPreference mBlurEnabled;

    private ColorPickerPreference mColor;
    private SeekBarPreference mColorAlpha;
    private SeekBarPreference mCornerRadius;

    private String mBlurRadiusKey;
    private String mBlurEnabledKey;

    private String mColorKey;
    private String mColorAlphaKey;
    private String mCornerRadiusKey;

    @Override
    public int getContentResId() {
        return R.xml.various_background;
    }

    @Override
    public void initPrefs() {
        Bundle args = getArguments();
        String mKey = args.getString("key");

        mBlurRadius = findPreference("prefs_key_custom_background_blur_radius");
        mBlurEnabled = findPreference("prefs_key_custom_background_blur_enabled");

        mColor = findPreference("prefs_key_custom_background_color");
        mColorAlpha = findPreference("prefs_key_custom_background_color_alpha");
        mCornerRadius = findPreference("prefs_key_custom_background_corner_radius");

        mBlurEnabledKey = mKey + "_blur_enabled";
        mBlurRadiusKey = mKey + "_blur_radius";

        mColorKey = mKey + "_color";
        mColorAlphaKey = mKey + "_color_alpha";
        mCornerRadiusKey = mKey + "_corner_radius";

        mBlurEnabled.setChecked(isBackgroundBlurEnabled());

        mColorAlpha.setVisible(false);
        mColor.saveValue(getBackgroundColor());
        setSeekBarPreferenceValue(mColorAlpha, mColorAlphaKey, 120);
        setSeekBarPreferenceValue(mCornerRadius, mCornerRadiusKey, 18);
        setSeekBarPreferenceValue(mBlurRadius, mBlurRadiusKey, 60);

        mBlurRadius.setOnPreferenceChangeListener(this);
        mBlurEnabled.setOnPreferenceChangeListener(this);

        mColor.setOnPreferenceChangeListener(this);
        mColorAlpha.setOnPreferenceChangeListener(this);
        mCornerRadius.setOnPreferenceChangeListener(this);
    }


    private boolean isBackgroundBlurEnabled() {
        return hasKey(mBlurEnabledKey) && PrefsUtils.getSharedBoolPrefs(getContext(), mBlurEnabledKey, false);
    }

    private int getBackgroundColor() {
        if (hasKey(mColorKey)) {
            return PrefsUtils.getSharedIntPrefs(getContext(), mColorKey, 2113929215);
        } else {
            return 2113929215;
        }
    }

    private void setSeekBarPreferenceValue(SeekBarPreference preference, String key, int defValue) {
        if (hasKey(key)) {
            preference.setValue(PrefsUtils.getSharedIntPrefs(getContext(), key, defValue));
        } else if (preference.isVisible()) {
            PrefsUtils.mSharedPreferences.edit().putInt(key, defValue).apply();
            preference.setValue(defValue);
        }
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mBlurEnabled) {
            setBlurEnabled(!mBlurEnabled.isChecked());
        } else if (preference == mBlurRadius) {
            setBackgroundBlurRadius((int) o);
        } else if (preference == mColor) {
            setBackgroundColor((int) o);
        } else if (preference == mColorAlpha) {
            setBackgroundColorAlpha((int) o);
        } else if (preference == mCornerRadius) {
            setBackgroundCornerRadius((int) o);
        }
        return false;
    }

    private void setBlurEnabled(boolean isBlurEnabled) {
        mBlurEnabled.setChecked(isBlurEnabled);
        PrefsUtils.mSharedPreferences.edit().putBoolean(mBlurEnabledKey, isBlurEnabled).apply();
        if (isBlurEnabled) {
            setSeekBarPreferenceValue(mBlurRadius, mBlurRadiusKey, 60);
            /*setSeekBarPreferenceValue(mBgAlpha, mBgAlphaKey, 120);*/
        }
    }

    private void setBackgroundBlurRadius(int value) {
        mBlurRadius.setValue(value);
        PrefsUtils.mSharedPreferences.edit().putInt(mBlurRadiusKey, value).apply();
    }

    private void setBackgroundColor(int value) {
        PrefsUtils.mSharedPreferences.edit().putInt(mColorKey, value).apply();
    }

    private void setBackgroundColorAlpha(int value) {
        mColorAlpha.setValue(value);
        PrefsUtils.mSharedPreferences.edit().putInt(mColorAlphaKey, value).apply();
    }

    private void setBackgroundCornerRadius(int value) {
        mCornerRadius.setValue(value);
        PrefsUtils.mSharedPreferences.edit().putInt(mCornerRadiusKey, value).apply();
    }
}
