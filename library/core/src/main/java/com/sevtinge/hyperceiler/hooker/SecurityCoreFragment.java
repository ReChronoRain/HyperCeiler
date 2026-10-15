/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hooker;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;

public class SecurityCoreFragment extends DashboardFragment {
    @Override
    public int getPreferenceScreenResId() {
        return R.xml.security_core;
    }

    @Override
    public void initPrefs() {
        SwitchPreference power = findPreference("prefs_key_securitycore_power_rear_code_enable");
        Preference code = findPreference("prefs_key_securitycore_power_rear_code_action");
        Preference back = findPreference("prefs_key_securitycore_back_tap_rear_alipay_enable");
        if (power != null && code != null) {
            power.setEnabled(Build.VERSION.SDK_INT == 37);
            code.setVisible(power.isChecked());
            power.setOnPreferenceChangeListener((preference, value) -> {
                code.setVisible((boolean) value);
                return true;
            });
        }
        if (back != null) back.setEnabled(Build.VERSION.SDK_INT == 37);
        bindSettings("rear_alipay_power_settings", "com.miui.miinput.gesture.powerkey.DoubleClickPowerKeySettingsActivity");
        bindSettings("rear_alipay_back_settings", "com.miui.miinput.gesture.backtap.BackTapSettingsActivity");
    }

    private void bindSettings(String key, String activity) {
        Preference preference = findPreference(key);
        if (preference == null) return;
        preference.setOnPreferenceClickListener(ignored -> {
            try {
                startActivity(new Intent().setComponent(new ComponentName("com.miui.securitycore", activity)));
            } catch (ActivityNotFoundException | SecurityException error) {
                AndroidLog.e("SecurityCoreFragment", "Cannot open native gesture settings", error);
                Toast.makeText(requireContext(), R.string.start_failed, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }
}
