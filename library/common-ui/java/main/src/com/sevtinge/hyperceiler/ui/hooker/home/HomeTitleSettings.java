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
package com.sevtinge.hyperceiler.ui.hooker.home;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isHyperOSVersion;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.common.prefs.RecommendPreference;
import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.ui.sub.AppPickerFragment;
import com.sevtinge.hyperceiler.ui.sub.SubPickerActivity;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

import fan.preference.SeekBarPreferenceCompat;

public class HomeTitleSettings extends DashboardFragment {

    SwitchPreference mDisableMonoChrome;
    SwitchPreference mDisableMonetColor;
    SwitchPreference mDisableHideTheme;
    SwitchPreference mIconSize;
    SeekBarPreferenceCompat mTitleFontSize;
    Preference mIconTitleCustomization;
    RecommendPreference mRecommend;
    PreferenceCategory mAppBlur;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_title;
    }

    @Override
    public void initPrefs() {
        mIconTitleCustomization = findPreference("prefs_key_home_title_title_icontitlecustomization");
        mDisableMonoChrome = findPreference("prefs_key_home_other_icon_mono_chrome");
        mAppBlur = findPreference("prefs_key_home_title_app_blur_hyper");
        mIconSize = findPreference("prefs_key_home_title_icon_size_enable");
        mTitleFontSize = findPreference("prefs_key_home_drawer_title_font_size");

        mDisableMonoChrome.setVisible(isMoreAndroidVersion(33));
        mDisableMonoChrome.setOnPreferenceChangeListener((preference, o) -> true);
        mDisableMonetColor = findPreference("prefs_key_home_other_icon_monet_color");
        mDisableMonetColor.setVisible(isMoreAndroidVersion(33));
        mDisableMonetColor.setOnPreferenceChangeListener((preference, o) -> true);
        mDisableHideTheme = findPreference("prefs_key_home_title_disable_hide_theme");
        mDisableHideTheme.setVisible(isPad());
        mAppBlur.setVisible(isHyperOSVersion(1f));
        mIconSize.setVisible(isHyperOSVersion(2f));
        mTitleFontSize.setVisible(isMoreHyperOSVersion(2f));

        mIconTitleCustomization.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SubPickerActivity.class);
            intent.putExtra("mode", AppPickerFragment.INPUT_MODE);
            intent.putExtra("key", preference.getKey());
            startActivity(intent);
            return true;
        });


        Bundle args1 = new Bundle();
        Bundle args2 = new Bundle();
        mRecommend = new RecommendPreference(getContext());
        getPreferenceScreen().addPreference(mRecommend);

        args1.putString(":settings:fragment_args_key", "prefs_key_home_other_shortcut_background_blur");
        mRecommend.addRecommendView(getString(R.string.home_other_shortcut_background_blur),
                null,
                HomeOtherSettings.class,
                args1,
                R.string.home_other
        );

        args2.putString(":settings:fragment_args_key", "prefs_key_home_other_all_hide_app_activity");
        mRecommend.addRecommendView(getString(R.string.home_other_app_icon_hide),
                null,
                HomeOtherSettings.class,
                args2,
                R.string.home_other
        );
    }
}
