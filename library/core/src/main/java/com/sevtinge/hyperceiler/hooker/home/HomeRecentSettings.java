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
package com.sevtinge.hyperceiler.hooker.home;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isHyperOSVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.sub.SubPickerActivity.ALL_APPS_MODE;

import android.content.Intent;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.rules.home.recent.HomeRecentsHapticOS4;
import com.sevtinge.hyperceiler.sub.SubPickerActivity;

import java.util.Map;

import fan.preference.DropDownPreference;
import fan.preference.SeekBarPreferenceCompat;

public class HomeRecentSettings extends DashboardFragment {

    Preference mHideRecentCard;
    SeekBarPreferenceCompat mTaskViewHeight;
    SwitchPreference mShowLaunch;
    SwitchPreference mHideWorldCirculate;
    SwitchPreference mHideFreeform;
    SwitchPreference mUnlockPin;
    SwitchPreference mShowMenInfo;
    SwitchPreference mHideCleanIcon;
    SwitchPreference mNotHideCleanIcon;
    SwitchPreference mFixCardTitlePadding;
    private static final String HIDE_CLEAR = "prefs_key_home_layout_recents_hide_clear";
    private static final String DISABLE_CLEAR = "prefs_key_home_layout_recents_no_clear";

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.home_recent;
    }

    @Override
    public void initPrefs() {
        findPreference("home_recent_haptic_category").setVisible(isHyperOSVersion(4f));
        DropDownPreference hapticType = findPreference("prefs_key_home_recent_card_haptic_type");
        hapticType.setOnPreferenceChangeListener((preference, value) -> {
            int type = Integer.parseInt((String) value);
            int strength = PrefsBridge.getInt("home_recent_card_haptic_strength", 50);
            if (!HomeRecentsHapticOS4.perform(requireContext().getApplicationContext(), type, strength, 1)) {
                Toast.makeText(requireContext(), R.string.home_recent_card_haptic_preview_unavailable, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        SwitchPreference scrollHaptic = findPreference("prefs_key_home_recent_card_haptic");
        SwitchPreference closeHaptic = findPreference("prefs_key_home_recent_card_close_haptic");
        Preference hapticStrength = findPreference("prefs_key_home_recent_card_haptic_strength");
        hapticType.setEnabled(scrollHaptic.isChecked() || closeHaptic.isChecked());
        hapticStrength.setEnabled(scrollHaptic.isChecked() || closeHaptic.isChecked());
        scrollHaptic.setOnPreferenceChangeListener((preference, value) -> {
            boolean enabled = (Boolean) value || closeHaptic.isChecked();
            hapticType.setEnabled(enabled);hapticStrength.setEnabled(enabled);return true;
        });
        closeHaptic.setOnPreferenceChangeListener((preference, value) -> {
            boolean enabled = (Boolean) value || scrollHaptic.isChecked();
            hapticType.setEnabled(enabled);hapticStrength.setEnabled(enabled);return true;
        });
        mShowLaunch = findPreference("prefs_key_home_recent_show_launch");
        mHideWorldCirculate = findPreference("prefs_key_home_recent_hide_world_circulate");
        mHideFreeform = findPreference("prefs_key_home_recent_hide_freeform");
        mUnlockPin = findPreference("prefs_key_home_recent_unlock_pin");
        mHideRecentCard = findPreference("prefs_key_home_recent_hide_card");
        mTaskViewHeight = findPreference("prefs_key_home_recent_task_view_height");
        mShowMenInfo = findPreference("prefs_key_home_recent_show_memory_info");
        mHideCleanIcon = findPreference("prefs_key_home_recent_hide_clean_up");
        mNotHideCleanIcon = findPreference("prefs_key_always_show_clean_up");
        mFixCardTitlePadding = findPreference("prefs_key_home_recent_fix_card_title_padding");

        // Both switches are read while the launcher builds a card header, so an already built
        // card keeps its old header until it is rebuilt. Re-entering the recents view rebuilds
        // the cards, which is why the summaries ask for that instead of a launcher restart.
        SwitchPreference hideText = findPreference("prefs_key_home_recent_hide_text");
        hideText.setVisible(isHyperOSVersion(4f));
        mTaskViewHeight.setVisible(isPad());
        mShowMenInfo.setVisible(isPad());
        mFixCardTitlePadding.setVisible(!isPad());

        if (isHyperOSVersion(4f)) {
            mHideRecentCard.setSummary(R.string.home_recent_hide_card_os4_desc);
            // setFuncHint/setPreVisible also erase stored keys and replace descriptions.
            // OS4's temporary gate must be presentation-only.
            mUnlockPin.setVisible(false);
        } else if (isMoreHyperOSVersion(3f)) {
            setFuncHint(mShowLaunch, 1);
            setFuncHint(mHideWorldCirculate, isPad() ? 1 : 2);
            setFuncHint(mHideFreeform, 1);
            setPreVisible(mUnlockPin, false);
        }

        mHideRecentCard.setOnPreferenceClickListener(
                preference -> {
                    Intent intent = new Intent(getActivity(), SubPickerActivity.class);
                    intent.putExtra("mode", ALL_APPS_MODE);
                    intent.putExtra("key", preference.getKey());
                    startActivity(intent);
                    return true;
                }
        );

        if (isHyperOSVersion(4f)) {
            initClearActionForOS4();
        } else {
            // Preserve the original switch and dependency behavior on other OS versions.
            mHideCleanIcon.setOnPreferenceChangeListener((preference, o) -> {
                if (!(boolean) o) {
                    mNotHideCleanIcon.setChecked(false);
                }
                return true;
            });
        }
        HomeOS4AdaptationGate.apply(getPreferenceScreen(), getPreferenceScreenResId());
    }

    private void initClearActionForOS4() {
        mHideCleanIcon.setVisible(false);
        mNotHideCleanIcon.setVisible(false);
        DropDownPreference action = findPreference("prefs_key_home_recent_clear_action_os4");
        action.setVisible(true);

        // Display the existing native flags; no third persisted mode or provider/ABI migration.
        // If an older installation has both flags on, native behavior already prioritizes disable.
        Map<String, ?> values = PrefsBridge.getAll();
        boolean hidden = Boolean.TRUE.equals(values.get(HIDE_CLEAR));
        boolean disabled = Boolean.TRUE.equals(values.get(DISABLE_CLEAR));
        action.setValue(disabled ? "2" : hidden ? "1" : "0");
        action.setOnPreferenceChangeListener((preference, value) -> {
            if (!(value instanceof String mode)) return false;
            // Clear the incompatible flag first so a refresh cannot observe two enabled modes.
            switch (mode) {
                case "0" -> {
                    PrefsBridge.putBoolean(HIDE_CLEAR, false);
                    PrefsBridge.putBoolean(DISABLE_CLEAR, false);
                }
                case "1" -> {
                    PrefsBridge.putBoolean(DISABLE_CLEAR, false);
                    PrefsBridge.putBoolean(HIDE_CLEAR, true);
                }
                case "2" -> {
                    PrefsBridge.putBoolean(HIDE_CLEAR, false);
                    PrefsBridge.putBoolean(DISABLE_CLEAR, true);
                }
                default -> { return false; }
            }
            return true;
        });
    }
}
