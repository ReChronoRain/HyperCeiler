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
package com.sevtinge.hyperceiler.ui.activity.base;

import static com.sevtinge.hyperceiler.utils.PersistConfig.isNeedGrayView;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;

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
        if (isNeedGrayView) {
            View decorView = getWindow().getDecorView();
            Paint paint = new Paint();
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0);
            paint.setColorFilter(new ColorMatrixColorFilter(cm));
            decorView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        }
        registerObserver();
    }

    private void registerObserver() {
        PrefsUtils.registerOnSharedPreferenceChangeListener(getApplicationContext());
        Helpers.fixPermissionsAsync(getApplicationContext());
        Helpers.registerFileObserver(getApplicationContext());
    }
}
