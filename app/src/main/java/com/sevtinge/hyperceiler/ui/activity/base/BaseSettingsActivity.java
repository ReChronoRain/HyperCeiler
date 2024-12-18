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
package com.sevtinge.hyperceiler.ui.activity.base;

import static com.sevtinge.hyperceiler.utils.GrayViewUtils.isNeedGrayView;

import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import java.util.ArrayList;
import java.util.List;

import fan.appcompat.app.AlertDialog;

public abstract class BaseSettingsActivity extends BaseActivity {

    private String initialFragmentName;
    public Fragment mFragment;
    public static List<BaseSettingsActivity> mActivityList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isNeedGrayView) {
            View decorView = getWindow().getDecorView();
            Paint paint = new Paint();
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0);
            paint.setColorFilter(new ColorMatrixColorFilter(cm));
            decorView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        }
        Intent intent = getIntent();
        initialFragmentName = mProxy.getInitialFragmentName(intent);
        if (TextUtils.isEmpty(initialFragmentName)) {
            initialFragmentName = intent.getStringExtra(":android:show_fragment");
        }
        createUiFromIntent(savedInstanceState, intent);
    }

    protected void createUiFromIntent(Bundle savedInstanceState, Intent intent) {
        mProxy.setupContentView();
        mActivityList.add(this);
        Fragment targetFragment = mProxy.getTargetFragment(this, initialFragmentName, savedInstanceState);
        if (targetFragment != null) {
            targetFragment.setArguments(mProxy.getArguments(intent));
            setFragment(targetFragment);
        }
    }

    public void setFragment(Fragment fragment) {
        mFragment = fragment;
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.frame_content, fragment)
            .commit();
    }

    public Fragment getFragment() {
        return mFragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mFragment != null) {
            mFragment.onCreateOptionsMenu(menu, getMenuInflater());
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (mFragment != null) {
            mFragment.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }

    public void showRestartDialog(String string, String s) {

    }
}
