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

import android.os.Bundle;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import fan.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    protected BaseSettingsProxy mProxy;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mProxy = new SettingsProxy(this);
        super.onCreate(savedInstanceState);
        registerObserver();
    }

    private void registerObserver() {
        PrefsUtils.registerOnSharedPreferenceChangeListener(getApplicationContext());
        Helpers.fixPermissionsAsync(getApplicationContext());
        Helpers.registerFileObserver(getApplicationContext());
    }
}
