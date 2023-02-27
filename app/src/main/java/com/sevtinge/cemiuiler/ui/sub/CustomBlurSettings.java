package com.sevtinge.cemiuiler.ui.sub;

import android.os.Bundle;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.base.SubFragment;
import com.sevtinge.cemiuiler.utils.PrefsUtils;

import moralnorm.preference.ColorPickerPreference;
import moralnorm.preference.PreferenceCategory;
import moralnorm.preference.SeekBarPreference;
import moralnorm.preference.SwitchPreference;

public class CustomBlurSettings extends SubFragment {

    private Bundle args;
    private String mKey = null;
    private SwitchPreference mBlurEnabled;
    private PreferenceCategory mBlurCustomCat;

    private SeekBarPreference mBlurRadius;
    private ColorPickerPreference mBgColor;
    private SeekBarPreference mBgCornerRadius;
    /*private SeekBarPreference mBgAlpha;*/

    private String mBlurEnabledKey;
    private String mBlurRadiusKey;
    private String mBgCornerRadiusKey;
    private String mBgAlphaKey;
    private String mBgColorKey;


    @Override
    public int getContentResId() {
        return R.xml.various_custom_blur;
    }

    @Override
    public void initPrefs() {
        args = getActivity().getIntent().getExtras();
        mKey = args.getString("key");
        mBlurEnabled = findPreference("prefs_key_various_blur_enabled");
        mBlurCustomCat = findPreference("prefs_key_various_blur_custom");

        mBlurEnabledKey = mKey + "_blur_enabled";
        mBlurRadiusKey = mKey + "_blur_radius";

        mBgColorKey = mKey + "_bg_color";
        mBgCornerRadiusKey = mKey + "_bg_corner_radius";
        /*mBgAlphaKey = mKey + "_bg_alpha";*/

        mBlurRadius = findPreference("prefs_key_various_blur_radius");

        mBgColor = findPreference("prefs_key_various_background_color");
        mBgCornerRadius = findPreference("prefs_key_various_background_corner_radius");
        /*mBgAlpha = findPreference("prefs_key_various_background_alpha");*/

        mBlurEnabled.setChecked(getBlurEnabled());

        if (PrefsUtils.mSharedPreferences.contains(mBlurEnabledKey)) {
            setSeekBarPreferenceValue(mBlurRadius, mBlurRadiusKey, 60);
            setSeekBarPreferenceValue(mBgCornerRadius, mBgCornerRadiusKey, 30);
            /*getBlurRadius(mBgAlpha, mBgAlphaKey, 120);*/
            mBgColor.saveValue(getBgColor());
        }

        mBlurEnabled.setOnPreferenceChangeListener((preference, o) -> {
            setBlurEnabled(!mBlurEnabled.isChecked());
            return true;
        });

        mBlurRadius.setOnPreferenceChangeListener((preference, o) -> {
            setBlurRadius((int)o);
            return true;
        });

        mBgCornerRadius.setOnPreferenceChangeListener((preference, o) -> {
            setBgCornerRadius((int)o);
            return true;
        });

        /*mBgAlpha.setOnPreferenceChangeListener((preference, o) -> {
            setBgAlpha((int)o);
            return true;
        });*/

        mBgColor.setOnPreferenceChangeListener((preference, o) -> {
            setBgColor((int) o);
            return true;
        });
    }

    private void setBlurEnabled(boolean isBlurEnabled) {
        mBlurEnabled.setChecked(isBlurEnabled);
        PrefsUtils.mSharedPreferences.edit().putBoolean(mBlurEnabledKey, isBlurEnabled).apply();
        if (isBlurEnabled) {
            setSeekBarPreferenceValue(mBlurRadius, mBlurRadiusKey, 60);
            setSeekBarPreferenceValue(mBgCornerRadius, mBgCornerRadiusKey, 30);
            /*setSeekBarPreferenceValue(mBgAlpha, mBgAlphaKey, 120);*/
        }
    }

    private boolean getBlurEnabled() {
        if (PrefsUtils.mSharedPreferences.contains(mBlurEnabledKey)) {
            return PrefsUtils.getSharedBoolPrefs(getContext(),mBlurEnabledKey,false);
        } else {
            return false;
        }
    }

    private void setBlurRadius(int value) {
        mBlurRadius.setValue(value);
        PrefsUtils.mSharedPreferences.edit().putInt(mBlurRadiusKey, value).apply();
    }

    private void setSeekBarPreferenceValue(SeekBarPreference preference, String key, int defValue) {
        if (PrefsUtils.mSharedPreferences.contains(key)) {
            preference.setValue(PrefsUtils.getSharedIntPrefs(getContext(), key, defValue));
        } else {
            PrefsUtils.mSharedPreferences.edit().putInt(key, defValue).apply();
            preference.setValue(defValue);
        }
    }

    private void setBgCornerRadius(int value) {
        mBgCornerRadius.setValue(value);
        PrefsUtils.mSharedPreferences.edit().putInt(mBgCornerRadiusKey, value).apply();
    }

    /*private void setBgAlpha(int value) {
        mBgAlpha.setValue(value);
        PrefsUtils.mSharedPreferences.edit().putInt(mBgAlphaKey, value).apply();
    }*/


    private void setBgColor(int value) {
        PrefsUtils.mSharedPreferences.edit().putInt(mBgColorKey, value).apply();
    }

    private int getBgColor() {
        if (PrefsUtils.mSharedPreferences.contains(mBgColorKey)) {
            return PrefsUtils.getSharedIntPrefs(getContext(), mBgColorKey,-1);
        } else {
            return -1;
        }
    }
}
