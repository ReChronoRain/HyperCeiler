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
package com.sevtinge.hyperceiler.ui.fragment.framework;

import android.os.Bundle;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.prefs.RecommendPreference;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.PhoneFragment;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;

public class NetworkSettings extends SettingsPreferenceFragment {
    RecommendPreference mRecommend;

    @Override
    public int getContentResId() {
        return R.xml.framework_phone;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity)getActivity()).showRestartSystemDialog();
    }

    @Override
    public void initPrefs() {

        Bundle args1 = new Bundle();
        mRecommend = new RecommendPreference(getContext());
        getPreferenceScreen().addPreference(mRecommend);

        args1.putString(":settings:fragment_args_key", "prefs_key_phone_additional_network_settings");
        mRecommend.addRecommendView(getString(R.string.phone_additional_network_settings),
                null,
                PhoneFragment.class,
                args1,
                R.string.phone
        );

    }
}
