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
package com.sevtinge.hyperceiler.ui.sub;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.ColorPickerPreference;
import fan.preference.DropDownPreference;
import fan.preference.Preference;
import fan.preference.SeekBarPreferenceCompat;

public class PersonalAssistantFragment extends DashboardFragment
    implements Preference.OnPreferenceChangeListener {

    DropDownPreference mBlurBackground;
    SeekBarPreferenceCompat mBlurRadius;
    ColorPickerPreference mBlurColor;
    DropDownPreference mBlurBackgroundStyle;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.personal_assistant;
    }

    @Override
    public void initPrefs() {
        int mBlurMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_personal_assistant_value", "0"));
        mBlurBackground = findPreference("prefs_key_personal_assistant_value");
        mBlurBackgroundStyle = findPreference("prefs_key_personal_assistant_value");
        mBlurRadius = findPreference("prefs_key_personal_assistant_blurradius");
        mBlurColor = findPreference("prefs_key_personal_assistant_color");

        mBlurBackground.setVisible(!isAndroidVersion(30)); // 负一屏背景设置
        mBlurRadius.setVisible(!isAndroidVersion(30));
        mBlurColor.setVisible(!isAndroidVersion(30));

        setBlurMode(mBlurMode);
        mBlurBackgroundStyle.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference == mBlurBackgroundStyle) {
            setBlurMode(Integer.parseInt((String) o));
        }
        return true;
    }

    private void setBlurMode(int mode) {
        mBlurRadius.setVisible(mode == 2);
        mBlurColor.setVisible(mode == 2);
    }
}
