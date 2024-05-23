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
package com.sevtinge.hyperceiler.ui.sub.home.anim;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import fan.preference.SeekBarPreferenceCompat;

public class HomeTitleAnim8Settings extends SettingsPreferenceFragment {

    SeekBarPreferenceCompat mDRCX;
    SeekBarPreferenceCompat mSRCX;
    SeekBarPreferenceCompat mDRCY;
    SeekBarPreferenceCompat mSRCY;
    SeekBarPreferenceCompat mDRW;
    SeekBarPreferenceCompat mSRW;
    SeekBarPreferenceCompat mDRR;
    SeekBarPreferenceCompat mSRR;

    @Override
    public int getContentResId() {
        return R.xml.home_title_anim_8;
    }

    @Override
    public void initPrefs() {
        mDRCX = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_CENTERX_8");
        mSRCX = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_CENTERX_8");
        mDRCY = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_CENTERY_8");
        mSRCY = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_CENTERY_8");
        mDRW = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_WIDTH_8");
        mSRW = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_WIDTH_8");
        mDRR = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_RATIO_8");
        mSRR = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_RATIO_8");

        if (isMoreHyperOSVersion(1f)) {
            mDRCX.setDefaultValue(990);
            mSRCX.setDefaultValue(180);
            mDRCY.setDefaultValue(990);
            mSRCY.setDefaultValue(180);
            mDRW.setDefaultValue(990);
            mSRW.setDefaultValue(180);
            mDRR.setDefaultValue(990);
            mSRR.setDefaultValue(180);
        } else {
            mDRCX.setDefaultValue(990);
            mSRCX.setDefaultValue(180);
            mDRCY.setDefaultValue(990);
            mSRCY.setDefaultValue(180);
            mDRW.setDefaultValue(990);
            mSRW.setDefaultValue(180);
            mDRR.setDefaultValue(990);
            mSRR.setDefaultValue(180);
        }
    }

}
