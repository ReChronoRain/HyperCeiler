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
package com.sevtinge.hyperceiler.ui.fragment.app.home.anim;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import fan.preference.SeekBarPreferenceCompat;

public class HomeTitleAnim2Settings extends SettingsPreferenceFragment {
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
        return R.xml.home_title_anim_2;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartDialog(
            getResources().getString(R.string.mihome),
            "com.miui.home"
        );
    }

    @Override
    public void initPrefs() {
        mDRCX = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_CENTERX_2");
        mSRCX = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_CENTERX_2");
        mDRCY = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_CENTERY_2");
        mSRCY = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_CENTERY_2");
        mDRW = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_WIDTH_2");
        mSRW = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_WIDTH_2");
        mDRR = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_RATIO_2");
        mSRR = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_RATIO_2");
        mDR = findPreference("prefs_key_home_title_custom_anim_param_damping_RADIUS_2");
        mSR = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RADIUS_2");
        mDA = findPreference("prefs_key_home_title_custom_anim_param_damping_ALPHA_2");
        mSA = findPreference("prefs_key_home_title_custom_anim_param_stiffness_ALPHA_2");

        if (isMoreHyperOSVersion(1f)) {
            mDRCX.setDefaultValue(1000);
            mSRCX.setDefaultValue(330);
            mDRCY.setDefaultValue(1000);
            mSRCY.setDefaultValue(330);
            mDRW.setDefaultValue(1000);
            mSRW.setDefaultValue(330);
            mDRR.setDefaultValue(1000);
            mSRR.setDefaultValue(330);
            mDR.setDefaultValue(1000);
            mSR.setDefaultValue(330);
            mDA.setDefaultValue(1000);
            mSA.setDefaultValue(200);
        } else {
            mDRCX.setDefaultValue(960);
            mSRCX.setDefaultValue(300);
            mDRCY.setDefaultValue(990);
            mSRCY.setDefaultValue(300);
            mDRW.setDefaultValue(960);
            mSRW.setDefaultValue(410);
            mDRR.setDefaultValue(960);
            mSRR.setDefaultValue(340);
            mDR.setDefaultValue(990);
            mSR.setDefaultValue(135);
            mDA.setDefaultValue(990);
            mSA.setDefaultValue(135);
        }
    }
}
