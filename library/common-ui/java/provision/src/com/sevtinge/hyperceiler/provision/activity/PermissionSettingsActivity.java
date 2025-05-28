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

import android.os.Handler;

import androidx.fragment.app.Fragment;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.provision.fragment.PermissionSettingsFragment;

public class PermissionSettingsActivity extends BaseActivity {

    private Handler mBottomHandler = new Handler();

    @Override
    protected int getLogoDrawableId() {
        return 0;
    }

    @Override
    protected int getPreviewDrawable() {
        return R.drawable.provision_service_state;
    }

    @Override
    protected int getTitleStringId() {
        return R.string.provision_permission_settings_title;
    }

    @Override
    protected CharSequence getListDescCharSequence() {
        return null;
    }

    @Override
    protected Fragment getFragment() {
        return new PermissionSettingsFragment();
    }

    @Override
    protected String getFragmentTag() {
        return PermissionSettingsFragment.class.getSimpleName();
    }

    @Override
    public void onNextAminStart() {
        super.onNextAminStart();
        setResult(-1);
        finish();
    }

    public void enableBtnClick() {
        addClickable(false);
        mBottomHandler.postDelayed(() -> addClickable(true), 1000L);
    }

    public void addClickable(boolean clickable) {
        if (mConfirmButton != null) {
            mConfirmButton.setAlpha(clickable ? NO_ALPHA : HALF_ALPHA);
            mConfirmButton.setClickable(clickable);
        }
        if (mNewBackBtn != null) {
            mNewBackBtn.setAlpha(clickable ? NO_ALPHA : HALF_ALPHA);
            mNewBackBtn.setClickable(clickable);
        }
    }

}
