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
package com.sevtinge.hyperceiler.ui.fragment.app;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isHyperOSVersion;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.activity.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.preference.ColorPickerPreference;
import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;

public class PersonalAssistantFragment extends DashboardFragment
    implements Preference.OnPreferenceChangeListener {

    DropDownPreference mBlurBackground;
    SeekBarPreferenceCompat mBlurRadius;
    ColorPickerPreference mBlurColor;
    DropDownPreference mBlurBackgroundStyle;
    SeekBarPreferenceCompat mTvNotifWidth;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.personal_assistant;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.personal_assistant),
            "com.miui.personalassistant"
        );
    }

    @Override
    public void initPrefs() {
        int mBlurMode = Integer.parseInt(PrefsUtils.getSharedStringPrefs(getContext(), "prefs_key_personal_assistant_value", "0"));
        mBlurBackground = findPreference("prefs_key_personal_assistant_value");
        mBlurBackgroundStyle = findPreference("prefs_key_personal_assistant_value");
        mBlurRadius = findPreference("prefs_key_personal_assistant_blurradius");
        mBlurColor = findPreference("prefs_key_personal_assistant_color");
        mTvNotifWidth = findPreference("prefs_key_personal_assistant_set_tv_notif_info_max_width");

        mBlurBackground.setVisible(!isAndroidVersion(30)); // 负一屏背景设置
        mBlurRadius.setVisible(!isAndroidVersion(30));
        mBlurColor.setVisible(!isAndroidVersion(30));
        mTvNotifWidth.setVisible(isHyperOSVersion(1f));

        setBlurMode(mBlurMode);
        mBlurBackgroundStyle.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
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
