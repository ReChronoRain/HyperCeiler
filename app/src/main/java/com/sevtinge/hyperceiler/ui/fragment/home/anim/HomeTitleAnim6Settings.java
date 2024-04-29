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
package com.sevtinge.hyperceiler.ui.fragment.home.anim;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

import moralnorm.preference.SeekBarPreferenceEx;

public class HomeTitleAnim6Settings extends SettingsPreferenceFragment {
    SeekBarPreferenceEx mDRCX;
    SeekBarPreferenceEx mSRCX;
    SeekBarPreferenceEx mDRCY;
    SeekBarPreferenceEx mSRCY;
    SeekBarPreferenceEx mDRW;
    SeekBarPreferenceEx mSRW;
    SeekBarPreferenceEx mDRR;
    SeekBarPreferenceEx mSRR;
    SeekBarPreferenceEx mDR;
    SeekBarPreferenceEx mSR;
    SeekBarPreferenceEx mDA;
    SeekBarPreferenceEx mSA;

    @Override
    public int getContentResId() {
        return R.xml.home_title_anim_6;
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
        mDRCX = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_CENTERX_6");
        mSRCX = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_CENTERX_6");
        mDRCY = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_CENTERY_6");
        mSRCY = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_CENTERY_6");
        mDRW = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_WIDTH_6");
        mSRW = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_WIDTH_6");
        mDRR = findPreference("prefs_key_home_title_custom_anim_param_damping_RECT_RATIO_6");
        mSRR = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RECT_RATIO_6");
        mDR = findPreference("prefs_key_home_title_custom_anim_param_damping_RADIUS_6");
        mSR = findPreference("prefs_key_home_title_custom_anim_param_stiffness_RADIUS_6");
        mDA = findPreference("prefs_key_home_title_custom_anim_param_damping_ALPHA_6");
        mSA = findPreference("prefs_key_home_title_custom_anim_param_stiffness_ALPHA_6");

        if (isMoreHyperOSVersion(1f)) {
            mDRCX.setDefaultValue(950);
            mSRCX.setDefaultValue(378);
            mDRCY.setDefaultValue(950);
            mSRCY.setDefaultValue(378);
            mDRW.setDefaultValue(900);
            mSRW.setDefaultValue(405);
            mDRR.setDefaultValue(950);
            mSRR.setDefaultValue(333);
            mDR.setDefaultValue(990);
            mSR.setDefaultValue(180);
            mDA.setDefaultValue(990);
            mSA.setDefaultValue(378);
        } else {
            mDRCX.setDefaultValue(950);
            mSRCX.setDefaultValue(378);
            mDRCY.setDefaultValue(950);
            mSRCY.setDefaultValue(378);
            mDRW.setDefaultValue(900);
            mSRW.setDefaultValue(405);
            mDRR.setDefaultValue(950);
            mSRR.setDefaultValue(333);
            mDR.setDefaultValue(990);
            mSR.setDefaultValue(180);
            mDA.setDefaultValue(990);
            mSA.setDefaultValue(378);
        }
    }
}
