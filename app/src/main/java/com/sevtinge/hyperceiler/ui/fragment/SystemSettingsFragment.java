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
package com.sevtinge.hyperceiler.ui.fragment;

import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreMiuiVersion;

import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.SeekBar;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.prefs.RecommendPreference;
import com.sevtinge.hyperceiler.ui.base.BaseSettingsActivity;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import moralnorm.preference.SeekBarPreferenceEx;
import moralnorm.preference.SwitchPreference;

public class SystemSettingsFragment extends SettingsPreferenceFragment {
    SwitchPreference mNewNfc; // 新版 NFC 界面
    SwitchPreference mAreaScreenshot; // 区域截屏
    SwitchPreference mHighMode; // 极致模式
    SwitchPreference mNoveltyHaptic; // 新版触感调节页面
    SwitchPreference mPad; // 解锁平板分区
    SwitchPreference mNotice; // 重要通知程度
    SwitchPreference mUiMode;
    RecommendPreference mRecommend;

    @Override
    public int getContentResId() {
        return R.xml.system_settings;
    }

    @Override
    public View.OnClickListener addRestartListener() {
        return view -> ((BaseSettingsActivity) getActivity()).showRestartDialog(
                getResources().getString(R.string.system_settings),
                "com.android.settings"
        );
    }

    @Override
    public void initPrefs() {
        mHighMode = findPreference("prefs_key_system_settings_develop_speed_mode");
        mAreaScreenshot = findPreference("prefs_key_system_settings_area_screenshot");
        mNewNfc = findPreference("prefs_key_system_settings_new_nfc_page");
        mNoveltyHaptic = findPreference("prefs_key_system_settings_novelty_haptic");
        mPad = findPreference("prefs_key_system_settings_enable_pad_area");
        mNotice = findPreference("prefs_key_system_settings_more_notification_settings");
        mUiMode = findPreference("prefs_key_system_settings_unlock_ui_mode");

        mUiMode.setVisible(isPad());
        mHighMode.setVisible(!isAndroidVersion(30));
        mAreaScreenshot.setVisible(isAndroidVersion(30));
        mNewNfc.setVisible((isMoreMiuiVersion(14f) || isMoreHyperOSVersion(1f)) && isMoreAndroidVersion(33));
        mNoveltyHaptic.setVisible((isMoreMiuiVersion(14f) || isMoreHyperOSVersion(1f)) && isMoreAndroidVersion(31));
        mPad.setVisible(isPad());

        if (isMoreHyperOSVersion(1f)) {
            mNotice.setSummary(R.string.system_settings_more_notification_settings_summary);
        }

        Bundle args1 = new Bundle();
        mRecommend = new RecommendPreference(getContext());
        getPreferenceScreen().addPreference(mRecommend);

        args1.putString(":settings:fragment_args_key", "prefs_key_mi_settings_show_fps");
        mRecommend.addRecommendView(getString(R.string.mi_settings_show_fps),
                null,
                MiSettingsFragment.class,
                args1,
                R.string.mi_settings
        );

        animationScale();
    }

    public void animationScale() {
        SeekBarPreferenceEx seekBarPreferenceWn = findPreference("prefs_key_system_settings_window_animation_scale");
        setOnSeekBarChangeListener(seekBarPreferenceWn, "window_animation_scale");

        SeekBarPreferenceEx seekBarPreferenceTr = findPreference("prefs_key_system_settings_transition_animation_scale");
        setOnSeekBarChangeListener(seekBarPreferenceTr, "transition_animation_scale");

        SeekBarPreferenceEx seekBarPreferenceAn = findPreference("prefs_key_system_settings_animator_duration_scale");
        setOnSeekBarChangeListener(seekBarPreferenceAn, "animator_duration_scale");
    }

    public void setOnSeekBarChangeListener(SeekBarPreferenceEx mySeekBarPreference, String name) {
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
