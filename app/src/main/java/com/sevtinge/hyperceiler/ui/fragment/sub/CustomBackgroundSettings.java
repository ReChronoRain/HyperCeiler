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
package com.sevtinge.hyperceiler.ui.fragment.sub;

import android.os.Bundle;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.ColorPickerPreference;
import fan.preference.Preference;
import fan.preference.SeekBarPreference;
import fan.preference.SwitchPreference;

public class CustomBackgroundSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    private String mKey = "";
    private String mCustomBackgroundEnabledKey;
    private String mColorKey;
    private String mCornerRadiusKey;

    private String mBlurEnabledKey;
    private String mBlurRadiusKey;

    private SwitchPreference mCustomEnabledPreference;

    private ColorPickerPreference mColorPickerPreference;
    private SeekBarPreference mCornerRadiusPreference;

    private SwitchPreference mBlurEnabledPreference;
    private SeekBarPreference mBlurRadiusPreference;

    @Override
    public int getContentResId() {
        return R.xml.custom_background;
    }

    @Override
    public void initPrefs() {
        Bundle args = getArguments();

        if (args != null) {
            mKey = args.getString("key");

            mCustomBackgroundEnabledKey = mKey + "_custom_enable";

            mColorKey = mKey + "_color";
            mCornerRadiusKey = mKey + "_corner_radius";

            mBlurEnabledKey = mKey + "_blur_enabled";
            mBlurRadiusKey = mKey + "_blur_radius";
        } else {
            finish();
        }

        mCustomEnabledPreference = findPreference("prefs_key_custom_background_enabled");

        mColorPickerPreference = findPreference("prefs_key_custom_background_color");
        mCornerRadiusPreference = findPreference("prefs_key_custom_background_corner_radius");

        mBlurEnabledPreference = findPreference("prefs_key_custom_background_blur_enabled");
        mBlurRadiusPreference = findPreference("prefs_key_custom_background_blur_radius");

        loadData();

        mCustomEnabledPreference.setOnPreferenceChangeListener(this);

        mColorPickerPreference.setOnPreferenceChangeListener(this);
        mCornerRadiusPreference.setOnPreferenceChangeListener(this);

        mBlurEnabledPreference.setOnPreferenceChangeListener(this);
        mBlurRadiusPreference.setOnPreferenceChangeListener(this);
    }


    private void loadData() {
        mCustomEnabledPreference.setChecked(isCustomEnabled());
        mColorPickerPreference.setColor(getColor(-1));
        mCornerRadiusPreference.setValue(getSeekBarValue(mCornerRadiusKey, 18));
        mBlurEnabledPreference.setChecked(isBackgroundBlurEnabled());
        mBlurRadiusPreference.setValue(getSeekBarValue(mBlurRadiusKey, 60));
    }

    private boolean isCustomEnabled() {
        return hasKey(mCustomBackgroundEnabledKey) && PrefsUtils.getSharedBoolPrefs(getContext(), mCustomBackgroundEnabledKey, false);
    }

    private void setColor() {
        mColorPickerPreference.setColor(getColor(2113929215));
    }

    private int getColor(int defValue) {
        return hasKey(mColorKey) ? PrefsUtils.getSharedIntPrefs(getContext(), mColorKey, defValue) : defValue;
    }

    private boolean isBackgroundBlurEnabled() {
        return hasKey(mBlurEnabledKey) && PrefsUtils.getSharedBoolPrefs(getContext(), mBlurEnabledKey, false);
    }

    private int getSeekBarValue(String key, int defValue) {
        return hasKey(key) ? PrefsUtils.getSharedIntPrefs(getContext(), key, defValue) : defValue;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mCustomEnabledPreference) {
            setCustomEnable((Boolean) o);
        } else if (preference == mColorPickerPreference) {
            setBackgroundColor((int) o);
        } else if (preference == mCornerRadiusPreference) {
            setBackgroundCornerRadius((int) o);
        } else if (preference == mBlurEnabledPreference) {
            setBlurEnabled((Boolean) o);
        } else if (preference == mBlurRadiusPreference) {
            setBackgroundBlurRadius((int) o);
        }
        return false;
    }

    private void setCustomEnable(boolean isCustomEnabled) {
        mCustomEnabledPreference.setChecked(isCustomEnabled);
        PrefsUtils.mSharedPreferences.edit().putBoolean(mCustomBackgroundEnabledKey, isCustomEnabled).apply();
    }

    private void setBackgroundColor(int value) {
        mColorPickerPreference.setColor(value);
        PrefsUtils.mSharedPreferences.edit().putInt(mColorKey, value).apply();
    }

    private void setBackgroundCornerRadius(int value) {
        mCornerRadiusPreference.setValue(value);
        PrefsUtils.mSharedPreferences.edit().putInt(mCornerRadiusKey, value).apply();
    }

    private void setBlurEnabled(boolean isBlurEnabled) {
        mBlurEnabledPreference.setChecked(isBlurEnabled);
        PrefsUtils.mSharedPreferences.edit().putBoolean(mBlurEnabledKey, isBlurEnabled).apply();
    }

    private void setBackgroundBlurRadius(int value) {
        mBlurRadiusPreference.setValue(value);
        PrefsUtils.mSharedPreferences.edit().putInt(mBlurRadiusKey, value).apply();
    }
}
