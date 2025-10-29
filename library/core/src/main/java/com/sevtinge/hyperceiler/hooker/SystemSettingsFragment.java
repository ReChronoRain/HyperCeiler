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
package com.sevtinge.hyperceiler.hooker;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;

import android.os.Bundle;
import android.widget.SeekBar;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.common.prefs.RecommendPreference;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.hook.utils.ToastHelper;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils;

import fan.preference.SeekBarPreferenceCompat;

public class SystemSettingsFragment extends DashboardFragment {
    SwitchPreference mUiMode;
    RecommendPreference mRecommend;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_settings;
    }

    @Override
    public void initPrefs() {
        mUiMode = findPreference("prefs_key_system_settings_unlock_ui_mode");

        mUiMode.setVisible(isPad());

        Bundle args1 = new Bundle();
        mRecommend = new RecommendPreference(requireContext());
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
            private int lastProgress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lastProgress = progress;
                // 如果不是用户手动滑动（如通过对话框改变数值），也立即设置
                if (!fromUser) {
                    setAnimator(lastProgress, name);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setAnimator(lastProgress, name);
            }
        });
    }

    public void setAnimator(int i, String name) {
        float mFloat = ((float) i) / 100;
        try {
            // Settings.Global.putFloat(requireContext().getContentResolver(), name, mFloat);
            ShellUtils.rootExecCmd("settings put global " + name + " " + mFloat);
            AndroidLogUtils.logI("setAnimator", "set: " + name + " float: " + mFloat + " success");
        } catch (Throwable e) {
            ToastHelper.makeText(getContext(), getString(R.string.system_settings_set_failed_toast, name, String.valueOf(mFloat)));
            AndroidLogUtils.logE("setAnimator", "set: " + name + " float: " + mFloat, e);
        }
    }
}
