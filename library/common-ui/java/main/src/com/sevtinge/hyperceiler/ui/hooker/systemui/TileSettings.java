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
package com.sevtinge.hyperceiler.ui.hooker.systemui;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getWhoAmI;
import static com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.rootExecCmd;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.hook.utils.KillApp;
import com.sevtinge.hyperceiler.hook.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.hook.utils.devicesdk.TelephonyManager;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;

public class TileSettings extends DashboardFragment implements Preference.OnPreferenceChangeListener {
    SwitchPreference mTaplus;
    SwitchPreference mNewCCGridLabel;
    SwitchPreference mRoundedRect;
    SeekBarPreferenceCompat mRoundedRectRadius;
    SeekBarPreferenceCompat mSunshineModeHighBrightness;
    DropDownPreference mFiveG;
    DropDownPreference mSunshineMode;
    DropDownPreference mSunshineModeHigh;
    Handler handler;

    int mMaxBrightness = 0;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.system_ui_control_center_tiles;
    }

    @Override
    public void initPrefs() {
        mFiveG = findPreference("prefs_key_system_control_center_5g_new_tile");
        mRoundedRect = findPreference("prefs_key_system_ui_control_center_rounded_rect");
        mRoundedRectRadius = findPreference("prefs_key_system_ui_control_center_rounded_rect_radius");
        mTaplus = findPreference("prefs_key_security_center_taplus");
        mNewCCGridLabel = findPreference("prefs_key_system_control_center_qs_tile_label");
        mSunshineMode = findPreference("prefs_key_system_control_center_sunshine_new_mode");
        mSunshineModeHigh = findPreference("prefs_key_system_control_center_sunshine_new_mode_high");
        mSunshineModeHighBrightness = findPreference("prefs_key_system_control_center_sunshine_mode_brightness");
        handler = new Handler(Looper.getMainLooper());

        try {
            mMaxBrightness = Integer.parseInt(rootExecCmd("cat /sys/class/backlight/panel0-backlight/max_brightness"));
        } catch (Exception ignore) {}

        mTaplus.setOnPreferenceChangeListener(
                (preference, o) -> {
                    killTaplus();
                    return true;
                }
        );

        if (getWhoAmI().equals("root") && mMaxBrightness > 2048) {
            mSunshineModeHigh.setVisible(true);
            mSunshineMode.setVisible(false);
            mSunshineModeHigh.setOnPreferenceChangeListener(this);
            mSunshineModeHighBrightness.setMaxValue(mMaxBrightness);
        } else {
            mSunshineMode.setVisible(true);
            mSunshineModeHigh.setVisible(false);
            mSunshineModeHighBrightness.setVisible(false);
        }

        mRoundedRectRadius.setVisible(PrefsUtils.getSharedBoolPrefs(getContext(), "prefs_key_system_ui_control_center_rounded_rect", false));
        mNewCCGridLabel.setVisible(false);
        mFiveG.setVisible(TelephonyManager.getDefault().isFiveGCapable());
        mSunshineModeHighBrightness.setVisible(Integer.parseInt(PrefsUtils.mSharedPreferences.getString("prefs_key_system_control_center_sunshine_new_mode_high", "0")) == 3);;

        mRoundedRect.setOnPreferenceChangeListener(this);

    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mRoundedRect) {
            setCanBeVisibleRoundedRect((Boolean) o);
        } else if (preference == mSunshineModeHigh) {
            setCanBeVisibleSunshineBrightness(Integer.parseInt((String) o));
        }
        return true;
    }

    public void killTaplus() {
        ThreadPoolManager.getInstance().submit(() -> handler.post(() ->
                KillApp.killApps("com.miui.contentextension")));
    }

    private void setCanBeVisibleRoundedRect(boolean mode) {
        mRoundedRectRadius.setVisible(mode);
    }

    private void setCanBeVisibleSunshineBrightness(int mode) {
        mSunshineModeHighBrightness.setVisible(mode == 3);
    }

}
