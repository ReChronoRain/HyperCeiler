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
package com.sevtinge.hyperceiler.ui.app.home.anim;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.app.dashboard.DashboardFragment;

import fan.preference.SeekBarPreferenceCompat;

public class HomeTitleAnim5Settings extends DashboardFragment {
    SeekBarPreferenceCompat mDRCX;
    SeekBarPreferenceCompat mSRCX;
    SeekBarPreferenceCompat mDRCY;
    SeekBarPreferenceCompat mSRCY;
    SeekBarPreferenceCompat mDRW;
    SeekBarPreferenceCompat mSRW;
    SeekBarPreferenceCompat mDRR;
    SeekBarPreferenceCompat mSRR;
    SeekBarPreferenceCompat mDR;
    SeekBarPreferenceCompat mSR;
    SeekBarPreferenceCompat mDA;
    SeekBarPreferenceCompat mSA;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_title_anim_5;
    }

    @Override
    public void initPrefs() {
        mDRCX = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_CENTERX_5");
        mSRCX = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_CENTERX_5");
        mDRCY = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_CENTERY_5");
        mSRCY = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_CENTERY_5");
        mDRW = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_WIDTH_5");
        mSRW = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_WIDTH_5");
        mDRR = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_RATIO_5");
        mSRR = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_RATIO_5");
        mDR = findPreference("prefs_key_home_title_custom_anim_param_damping_RADIUS_5");
        mSR = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RADIUS_5");
        mDA = findPreference("prefs_key_home_title_custom_anim_param_damping_ALPHA_5");
        mSA = findPreference("prefs_key_home_title_custom_anim_param_stiffness_ALPHA_5");

        mDRCX.setDefaultValue(880);
        mSRCX.setDefaultValue(460);
        mDRCY.setDefaultValue(880);
        mSRCY.setDefaultValue(460);
        mDRW.setDefaultValue(850);
        mSRW.setDefaultValue(460);
        mDRR.setDefaultValue(1000);
        mSRR.setDefaultValue(350);
        mDR.setDefaultValue(1000);
        mSR.setDefaultValue(350);
        mDA.setDefaultValue(1000);
        mSA.setDefaultValue(400);

    }
}
