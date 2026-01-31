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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hooker;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.SETTINGS_KEY_CONFIG_DEFAULT_EFFECT;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.SETTINGS_KEY_CONFIG_LOCK_SELECTION;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.effect.EffectItem.SETTINGS_KEY_CONFIG_REMEMBER_DEVICE;
import static com.sevtinge.hyperceiler.libhook.utils.shell.ShellUtils.rootExecCmd;

import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;

import fan.preference.DropDownPreference;

public class MiSoundFragment extends DashboardFragment {

    private static final String TAG = "MiSoundFragment";

    SwitchPreference mLockSelection;
    SwitchPreference mRememberDevices;
    DropDownPreference mDefaultEffect;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.misound;
    }

    @Override
    public void initPrefs() {
        mLockSelection = findPreference("prefs_key_misound_bluetooth_lock_selection");
        mRememberDevices = findPreference("prefs_key_misound_bluetooth_remember_device");
        mDefaultEffect = findPreference("prefs_key_misound_bluetooth_default_effect");

        if (mLockSelection != null) {
            mLockSelection.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean value = (Boolean) newValue;
                putGlobalInt(SETTINGS_KEY_CONFIG_LOCK_SELECTION, value ? 1 : 0);
                return true;
            });
        }

        if (mRememberDevices != null) {
            mRememberDevices.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean value = (Boolean) newValue;
                putGlobalInt(SETTINGS_KEY_CONFIG_REMEMBER_DEVICE, value ? 1 : 0);
                return true;
            });
        }

        if (mDefaultEffect != null) {
            mDefaultEffect.setOnPreferenceChangeListener((preference, newValue) -> {
                String value = (String) newValue;
                putGlobalString(SETTINGS_KEY_CONFIG_DEFAULT_EFFECT, value);
                return true;
            });
        }

        syncAllConfigToSettings();
    }

    private void syncAllConfigToSettings() {
        if (mLockSelection != null) {
            putGlobalInt(SETTINGS_KEY_CONFIG_LOCK_SELECTION, mLockSelection.isChecked() ? 1 : 0);
        }
        if (mRememberDevices != null) {
            putGlobalInt(SETTINGS_KEY_CONFIG_REMEMBER_DEVICE, mRememberDevices.isChecked() ? 1 : 0);
        }
        if (mDefaultEffect != null && mDefaultEffect.getValue() != null) {
            putGlobalString(SETTINGS_KEY_CONFIG_DEFAULT_EFFECT, mDefaultEffect.getValue());
        }
    }

    private void putGlobalInt(String key, int value) {
        String cmd = "settings put global " + key + " " + value;
        rootExecCmd(cmd);
        AndroidLog.d(TAG, "Executed: " + cmd);
    }

    private void putGlobalString(String key, String value) {
        String cmd = "settings put global " + key + " \"" + value + "\"";
        rootExecCmd(cmd);
        AndroidLog.d(TAG, "Executed: " + cmd);
    }
}

