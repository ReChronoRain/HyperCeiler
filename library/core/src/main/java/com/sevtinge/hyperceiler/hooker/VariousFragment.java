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

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreSmallVersion;
import static com.sevtinge.hyperceiler.sub.SubPickerActivity.IME_MODE;

import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.libhook.appbase.input.InputMethodConfig;
import com.sevtinge.hyperceiler.libhook.utils.api.ThreadPoolManager;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;
import com.sevtinge.hyperceiler.sub.SubPickerActivity;
import com.sevtinge.hyperceiler.utils.PackagesUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import fan.preference.DropDownPreference;

public class VariousFragment extends DashboardFragment {
    private static final String PREF_IME_STYLE = "prefs_key_various_unlock_ime_style";
    private static final String PREF_IME_TARGET_APPS = "prefs_key_various_unlock_ime_apps";
    private static final String PREF_IME_SHOW_ALL = "prefs_key_various_unlock_ime_show_all";
    private static final String PREF_AOSP_IME_NAV_BAR_LAYOUT_START = "prefs_key_various_aosp_ime_nav_bar_layout_start";
    private static final String PREF_AOSP_IME_NAV_BAR_LAYOUT_END = "prefs_key_various_aosp_ime_nav_bar_layout_end";
    private static final String PREF_LEGACY_MIUI_ENABLE = "prefs_key_various_unlock_ime";
    private static final String PREF_LEGACY_AOSP_ENABLE = "prefs_key_various_aosp_ime";
    private static final String PREF_LEGACY_AOSP_TARGET_APPS = "prefs_key_various_aosp_ime_apps";

    private static final int IME_STYLE_OFF = 0;
    private static final int IME_STYLE_MIUI = 1;
    private static final int IME_STYLE_AOSP = 2;

    SwitchPreference mClipboard;
    SwitchPreference mClipboardClear;
    SwitchPreference mShowAllImeList;
    DropDownPreference mImeStyle;
    DropDownPreference mAospImeNavBarLayoutStart;
    DropDownPreference mAospImeNavBarLayoutEnd;
    Preference mImeTargetApps;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.various;
    }

    @Override
    public void initPrefs() {
        migrateLegacyInputMethodPrefs();
        mClipboard = findPreference("prefs_key_sogou_xiaomi_clipboard");
        mClipboardClear = findPreference("prefs_key_add_clipboard_clear");
        mImeStyle = findPreference(PREF_IME_STYLE);
        mImeTargetApps = findPreference(PREF_IME_TARGET_APPS);
        mShowAllImeList = findPreference(PREF_IME_SHOW_ALL);
        mAospImeNavBarLayoutStart = findPreference(PREF_AOSP_IME_NAV_BAR_LAYOUT_START);
        mAospImeNavBarLayoutEnd = findPreference(PREF_AOSP_IME_NAV_BAR_LAYOUT_END);

        if (isMoreSmallVersion(200, 2f)) {
            setFuncHint(mClipboardClear, 2);
        }

        if (mClipboard != null) {
            mClipboard.setOnPreferenceChangeListener((preference, o) -> {
                restartClipboardInputMethods();
                return true;
            });
        }

        if (mImeStyle != null) {
            mImeStyle.setValue(getImeStyleValue());
            mImeStyle.setOnPreferenceChangeListener((preference, newValue) -> {
                updateInputMethodPreferenceState(parseImeStyleValue(newValue));
                return true;
            });
        }

        if (mShowAllImeList != null) {
            mShowAllImeList.setChecked(getSharedPreferences().getBoolean(PREF_IME_SHOW_ALL, false));
        }

        if (mAospImeNavBarLayoutStart != null) {
            mAospImeNavBarLayoutStart.setOnPreferenceChangeListener((preference, newValue) -> {
                String value = String.valueOf(newValue);
                syncAospImeNavBarLayoutHandle(PREF_AOSP_IME_NAV_BAR_LAYOUT_START, value);
                return true;
            });
        }

        if (mAospImeNavBarLayoutEnd != null) {
            mAospImeNavBarLayoutEnd.setOnPreferenceChangeListener((preference, newValue) -> {
                String value = String.valueOf(newValue);
                syncAospImeNavBarLayoutHandle(PREF_AOSP_IME_NAV_BAR_LAYOUT_END, value);
                return true;
            });
        }

        if (mImeTargetApps != null) {
            mImeTargetApps.setOnPreferenceClickListener(preference -> {
                openImePicker(preference);
                return true;
            });
        }
        updateInputMethodPreferenceState(getImeStyle());
        updateInputMethodSummary();
        syncAospImeNavBarLayoutHandle(null, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateInputMethodPreferenceState(getImeStyle());
        updateInputMethodSummary();
        syncAospImeNavBarLayoutHandle(null, null);
    }

    private void restartClipboardInputMethods() {
        ThreadPoolManager.getInstance().submit(() ->
            AppsTool.killApps(
                "com.sohu.inputmethod.sogou.xiaomi",
                "com.sohu.inputmethod.sogou",
                "com.baidu.input_mi",
                "com.baidu.input"
            )
        );
    }

    private void openImePicker(Preference preference) {
        Intent intent = new Intent(getActivity(), SubPickerActivity.class);
        intent.putExtra("mode", IME_MODE);
        intent.putExtra("key", preference.getKey());
        startActivity(intent);
    }

    private void updateInputMethodSummary() {
        if (mImeTargetApps == null || getContext() == null) {
            return;
        }

        Set<String> packages = getInputMethodTargets();
        if (packages == null || packages.isEmpty()) {
            mImeTargetApps.setSummary(R.string.various_unlock_ime_apps_desc);
            return;
        }

        if (packages.size() == 1) {
            String packageName = packages.iterator().next();
            String label = PackagesUtils.getAppLabel(getContext(), packageName);
            mImeTargetApps.setSummary(label != null ? label : packageName);
            return;
        }

        mImeTargetApps.setSummary(getString(R.string.various_input_method_apps_count, packages.size()));
    }

    private void updateInputMethodPreferenceState(int imeStyle) {
        if (mImeTargetApps != null) {
            mImeTargetApps.setVisible(imeStyle != IME_STYLE_OFF);
        }
        if (mShowAllImeList != null) {
            mShowAllImeList.setVisible(imeStyle == IME_STYLE_MIUI);
        }
        if (mAospImeNavBarLayoutStart != null) {
            mAospImeNavBarLayoutStart.setVisible(imeStyle == IME_STYLE_AOSP);
        }
        if (mAospImeNavBarLayoutEnd != null) {
            mAospImeNavBarLayoutEnd.setVisible(imeStyle == IME_STYLE_AOSP);
        }
    }

    private void syncAospImeNavBarLayoutHandle(String changedKey, String newValue) {
        String start = mAospImeNavBarLayoutStart != null ? mAospImeNavBarLayoutStart.getValue() : InputMethodConfig.NAV_BAR_BUTTON_HIDE_IME;
        String end = mAospImeNavBarLayoutEnd != null ? mAospImeNavBarLayoutEnd.getValue() : InputMethodConfig.NAV_BAR_BUTTON_IME_SWITCHER;

        if (PREF_AOSP_IME_NAV_BAR_LAYOUT_START.equals(changedKey)) {
            start = newValue;
        } else if (PREF_AOSP_IME_NAV_BAR_LAYOUT_END.equals(changedKey)) {
            end = newValue;
        }

        if (start == null || start.isBlank()) {
            start = InputMethodConfig.NAV_BAR_BUTTON_HIDE_IME;
        }
        if (end == null || end.isBlank()) {
            end = InputMethodConfig.NAV_BAR_BUTTON_IME_SWITCHER;
        }

        PrefsBridge.putString(
            InputMethodConfig.PREF_AOSP_IME_NAV_BAR_LAYOUT_HANDLE,
            start + "[70AC];" + InputMethodConfig.NAV_BAR_BUTTON_HOME_HANDLE + ";" + end + "[70AC]"
        );
    }

    private void updateDropDownSummary(DropDownPreference preference, String value) {
        if (preference == null || value == null) {
            return;
        }

        int index = preference.findIndexOfValue(value);
        CharSequence[] entries = preference.getEntries();
        CharSequence entry = index >= 0 && entries != null && index < entries.length ? entries[index] : null;
        if (entry != null && !entry.isEmpty()) {
            preference.setSummary(entry);
        }
    }

    private void migrateLegacyInputMethodPrefs() {
        SharedPreferences preferences = getSharedPreferences();
        if (preferences == null || preferences.contains(PREF_IME_STYLE)) {
            return;
        }

        boolean legacyMiuiEnabled = preferences.getBoolean(PREF_LEGACY_MIUI_ENABLE, false);
        boolean legacyAospEnabled = preferences.getBoolean(PREF_LEGACY_AOSP_ENABLE, false);
        LinkedHashSet<String> inputMethodTargets = new LinkedHashSet<>(
            preferences.getStringSet(PREF_IME_TARGET_APPS, Collections.emptySet()));
        LinkedHashSet<String> legacyAospTargets = new LinkedHashSet<>(
            preferences.getStringSet(PREF_LEGACY_AOSP_TARGET_APPS, Collections.emptySet()));

        int migratedStyle = IME_STYLE_OFF;
        if (legacyMiuiEnabled) {
            migratedStyle = IME_STYLE_MIUI;
        } else if (legacyAospEnabled) {
            migratedStyle = IME_STYLE_AOSP;
            if (inputMethodTargets.isEmpty() && !legacyAospTargets.isEmpty()) {
                inputMethodTargets.addAll(legacyAospTargets);
            }
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_IME_STYLE, String.valueOf(migratedStyle));
        if (legacyMiuiEnabled) {
            editor.putBoolean(PREF_IME_SHOW_ALL, true);
        }
        if (!inputMethodTargets.isEmpty()) {
            editor.putStringSet(PREF_IME_TARGET_APPS, inputMethodTargets);
        }
        editor.remove(PREF_LEGACY_MIUI_ENABLE);
        editor.remove(PREF_LEGACY_AOSP_ENABLE);
        editor.remove(PREF_LEGACY_AOSP_TARGET_APPS);
        editor.apply();
    }

    private int getImeStyle() {
        SharedPreferences preferences = getSharedPreferences();
        if (preferences == null) {
            return IME_STYLE_OFF;
        }
        return parseImeStyleValue(preferences.getString(PREF_IME_STYLE, String.valueOf(IME_STYLE_OFF)));
    }

    private String getImeStyleValue() {
        return String.valueOf(getImeStyle());
    }

    private int parseImeStyleValue(Object value) {
        if (value == null) {
            return IME_STYLE_OFF;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return IME_STYLE_OFF;
        }
    }

    private Set<String> getInputMethodTargets() {
        SharedPreferences preferences = getSharedPreferences();
        if (preferences == null) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(preferences.getStringSet(PREF_IME_TARGET_APPS, Collections.emptySet()));
    }
}
