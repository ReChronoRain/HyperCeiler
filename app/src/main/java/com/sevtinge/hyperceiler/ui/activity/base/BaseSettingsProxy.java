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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import fan.appcompat.app.AppCompatActivity;

public abstract class BaseSettingsProxy {

    AppCompatActivity mActivity;

    public abstract void setupContentView();

    public abstract void handleIntent(Intent intent);

    public abstract void initView(Bundle bundle);

    public abstract boolean onBackPressed();

    public abstract void onCreateOptionsMenu(Menu menu);

    public abstract void onDestroyView();

    public abstract void onOptionsItemSelected(MenuItem menuItem);

    public abstract void onPause();

    public abstract void onPrepareOptionsMenu(Menu menu);

    public abstract void onResume();

    public abstract String getInitialFragmentName(Intent intent);

    public abstract Fragment getTargetFragment(Context context, String initialFragmentName, Bundle savedInstanceState);

    public abstract Bundle getArguments(Intent intent);

    public abstract void onStartSettingsForArguments(Class<?> cls, Preference preference, boolean isAddPreferenceKey);
}
