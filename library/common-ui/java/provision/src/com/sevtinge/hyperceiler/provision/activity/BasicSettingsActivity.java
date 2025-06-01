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
package com.sevtinge.hyperceiler.provision.activity;

import androidx.fragment.app.Fragment;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.provision.fragment.BasicSettingsFragment;

public class BasicSettingsActivity extends BaseActivity {

    @Override
    protected Fragment getFragment() {
        return new BasicSettingsFragment();
    }

    @Override
    protected String getFragmentTag() {
        return BasicSettingsFragment.class.getSimpleName();
    }

    @Override
    protected CharSequence getListDescCharSequence() {
        return null;
    }

    @Override
    protected int getLogoDrawableId() {
        return 0;
    }

    @Override
    protected int getPreviewDrawable() {
        return R.drawable.provision_basic_settings;
    }

    @Override
    protected int getTitleStringId() {
        return R.string.provision_basic_settings_title;
    }

    @Override
    public void onNextAminStart() {
        super.onNextAminStart();
        setResult(-1);
        finish();
    }
}
