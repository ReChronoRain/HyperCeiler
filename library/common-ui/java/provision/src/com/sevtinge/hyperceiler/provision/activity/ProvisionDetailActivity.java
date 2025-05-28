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

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.sevtinge.hyperceiler.provision.utils.IOnFocusListener;

import fan.appcompat.app.AppCompatActivity;

public abstract class ProvisionDetailActivity extends AppCompatActivity {

    protected Fragment mFragment;
    protected FragmentManager mFragmentManager;

    protected abstract Fragment getFragment();
    protected abstract String getFragmentTag();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentManager = getSupportFragmentManager();
        mFragment = mFragmentManager.findFragmentByTag(getFragmentTag());
        if (mFragment == null) {
            mFragment = getFragment();
            setFragment(android.R.id.content, mFragment, getFragmentTag());
        }
        setupView();
    }

    protected void setupView() {}

    protected void setFragment(@IdRes int containerViewId, @NonNull Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(containerViewId, fragment, tag)
                .commit();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mFragment instanceof IOnFocusListener) {
            ((IOnFocusListener) mFragment).onWindowFocusChanged(hasFocus);
        }
    }
}
