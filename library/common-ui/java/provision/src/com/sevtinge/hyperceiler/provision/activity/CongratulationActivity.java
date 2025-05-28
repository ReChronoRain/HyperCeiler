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

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.sevtinge.hyperceiler.provision.fragment.CongratulationFragment;

public class CongratulationActivity extends ProvisionDetailActivity {

    private boolean mIsDisableBack = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsDisableBack = getIntent().getBooleanExtra("extra_disable_back", false);
    }

    @Override
    protected Fragment getFragment() {
        return new CongratulationFragment();
    }

    @Override
    protected String getFragmentTag() {
        return CongratulationFragment.class.getSimpleName();
    }

    @Override
    public void onBackPressed() {
        if (!mIsDisableBack) {
            setResult(0);
            super.onBackPressed();
        }
    }
}
