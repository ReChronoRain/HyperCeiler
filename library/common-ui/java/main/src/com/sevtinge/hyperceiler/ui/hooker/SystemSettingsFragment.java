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
package com.sevtinge.hyperceiler.ui.hooker;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;

import android.os.Bundle;
import android.provider.Settings;
import android.widget.SeekBar;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.common.prefs.RecommendPreference;
import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;

import fan.preference.SeekBarPreferenceCompat;

public class SystemSettingsFragment extends DashboardFragment {
    SwitchPreference mPad; // 解锁平板分区
    SwitchPreference mUiMode;
    RecommendPreference mRecommend;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_settings;
    }

    @Override
    public void initPrefs() {
        mPad = findPreference("prefs_key_system_settings_enable_pad_area");
        mUiMode = findPreference("prefs_key_system_settings_unlock_ui_mode");

        mUiMode.setVisible(isPad());
        mPad.setVisible(isPad());

        Bundle args1 = new Bundle();
        mRecommend = new RecommendPreference(getContext());
        getPreferenceScreen().addPreference(mRecommend);

        args1.putString(":settings:fragment_args_key", "prefs_key_mi_settings_show_fps");
        args1.putInt(":settings:fragment_resId", R.xml.mi_settings);
        mRecommend.addRecommendView(getString(R.string.mi_settings_show_fps),
                null,
                DashboardFragment.class,
                args1,
                R.string.mi_settings
        );

        animationScale();
    }

    public void animationScale() {
        SeekBarPreferenceCompat seekBarPreferenceWn = findPreference("prefs_key_system_settings_window_animation_scale");
        setOnSeekBarChangeListener(seekBarPreferenceWn, "window_animation_scale");

        SeekBarPreferenceCompat seekBarPreferenceTr = findPreference("prefs_key_system_settings_transition_animation_scale");
        setOnSeekBarChangeListener(seekBarPreferenceTr, "transition_animation_scale");

        SeekBarPreferenceCompat seekBarPreferenceAn = findPreference("prefs_key_system_settings_animator_duration_scale");
        setOnSeekBarChangeListener(seekBarPreferenceAn, "animator_duration_scale");
    }

    public void setOnSeekBarChangeListener(SeekBarPreferenceCompat mySeekBarPreference, String name) {
        mySeekBarPreference.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setAnimator(progress, name);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public void setAnimator(int i, String name) {
        float mFloat = ((float) i) / 100;
        try {
            Settings.Global.putFloat(getContext().getContentResolver(), name, mFloat);
        } catch (Throwable e) {
            AndroidLogUtils.logE("setAnimator", "set: " + name + " float: " + mFloat, e);
        }
    }
}
