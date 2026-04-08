/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.appbase.input;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;

import java.util.Collections;
import java.util.Set;

public final class InputMethodConfig {
    public static final String PREF_IME_STYLE = "various_unlock_ime_style";
    public static final String PREF_IME_TARGET_APPS = "various_unlock_ime_apps";
    public static final String PREF_IME_SHOW_ALL = "various_unlock_ime_show_all";
    public static final String PREF_AOSP_IME_NAV_BAR_LAYOUT_START = "various_aosp_ime_nav_bar_layout_start";
    public static final String PREF_AOSP_IME_NAV_BAR_LAYOUT_END = "various_aosp_ime_nav_bar_layout_end";
    public static final String PREF_AOSP_IME_NAV_BAR_LAYOUT_HANDLE = "various_aosp_ime_nav_bar_layout_handle";
    private static final String PREF_LEGACY_MIUI_IME_UNLOCK = "various_unlock_ime";
    private static final String PREF_LEGACY_AOSP_IME = "various_aosp_ime";
    private static final String PREF_LEGACY_AOSP_IME_APPS = "various_aosp_ime_apps";
    public static final String NAV_BAR_BUTTON_HIDE_IME = "back";
    public static final String NAV_BAR_BUTTON_HOME_HANDLE = "home_handle";
    public static final String NAV_BAR_BUTTON_IME_SWITCHER = "ime_switcher";
    private static final String DEFAULT_AOSP_IME_NAV_BAR_LAYOUT_HANDLE =
        NAV_BAR_BUTTON_HIDE_IME + "[70AC];" + NAV_BAR_BUTTON_HOME_HANDLE + ";" +
            NAV_BAR_BUTTON_IME_SWITCHER + "[70AC]";

    public static final int IME_STYLE_OFF = 0;
    public static final int IME_STYLE_MIUI = 1;
    public static final int IME_STYLE_AOSP = 2;

    private InputMethodConfig() {
    }

    public static boolean shouldHookMiuiIme(@Nullable String packageName) {
        if (!hasImeStylePreference()) {
            return isFeatureEnabled(PREF_LEGACY_MIUI_IME_UNLOCK) &&
                isSelectedPackage(PREF_IME_TARGET_APPS, packageName);
        }
        return isMiuiImeStyle() && isSelectedInputMethodPackage(packageName);
    }

    public static boolean shouldHookAospIme(@Nullable String packageName) {
        if (!hasImeStylePreference()) {
            return isFeatureEnabled(PREF_LEGACY_AOSP_IME) &&
                isSelectedPackage(PREF_LEGACY_AOSP_IME_APPS, packageName);
        }
        return isAospImeStyle() && isSelectedInputMethodPackage(packageName);
    }

    public static boolean isMiuiImeUnlockPackage(@Nullable String packageName) {
        if (!hasImeStylePreference()) {
            return isSelectedPackage(PREF_IME_TARGET_APPS, packageName);
        }
        return isMiuiImeStyle() && isSelectedInputMethodPackage(packageName);
    }

    public static boolean isAospImePackage(@Nullable String packageName) {
        if (!hasImeStylePreference()) {
            return isSelectedPackage(PREF_LEGACY_AOSP_IME_APPS, packageName);
        }
        return isAospImeStyle() && isSelectedInputMethodPackage(packageName);
    }

    public static int getImeStyle() {
        String style = PrefsBridge.getString(PREF_IME_STYLE, "");
        if (style != null && !style.isEmpty()) {
            try {
                return Integer.parseInt(style);
            } catch (NumberFormatException ignored) {
                return IME_STYLE_OFF;
            }
        }

        if (isFeatureEnabled(PREF_LEGACY_MIUI_IME_UNLOCK)) {
            return IME_STYLE_MIUI;
        }
        if (isFeatureEnabled(PREF_LEGACY_AOSP_IME)) {
            return IME_STYLE_AOSP;
        }
        return IME_STYLE_OFF;
    }

    public static boolean isMiuiImeStyle() {
        return getImeStyle() == IME_STYLE_MIUI;
    }

    public static boolean isAospImeStyle() {
        return getImeStyle() == IME_STYLE_AOSP;
    }

    public static boolean shouldShowAllImeList() {
        if (!hasImeStylePreference()) {
            return isFeatureEnabled(PREF_LEGACY_MIUI_IME_UNLOCK);
        }
        return isMiuiImeStyle() && PrefsBridge.getBoolean(PREF_IME_SHOW_ALL, false);
    }

    public static boolean shouldHookMiuiImeListInSystem() {
        if (!hasImeStylePreference()) {
            return isFeatureEnabled(PREF_LEGACY_MIUI_IME_UNLOCK) &&
                !getSelectedPackages(PREF_IME_TARGET_APPS).isEmpty();
        }
        return shouldShowAllImeList() && !getSelectedInputMethodPackages().isEmpty();
    }

    public static boolean shouldHookAospImeInSystem() {
        if (!hasImeStylePreference()) {
            return isFeatureEnabled(PREF_LEGACY_AOSP_IME) &&
                !getSelectedPackages(PREF_LEGACY_AOSP_IME_APPS).isEmpty();
        }
        return isAospImeStyle() && !getSelectedInputMethodPackages().isEmpty();
    }

    @NonNull
    public static String getAospImeNavBarLayoutStart() {
        String start = PrefsBridge.getString(PREF_AOSP_IME_NAV_BAR_LAYOUT_START, NAV_BAR_BUTTON_HIDE_IME);
        return start != null && !start.isBlank() ? start : NAV_BAR_BUTTON_HIDE_IME;
    }

    @NonNull
    public static String getAospImeNavBarLayoutEnd() {
        String end = PrefsBridge.getString(PREF_AOSP_IME_NAV_BAR_LAYOUT_END, NAV_BAR_BUTTON_IME_SWITCHER);
        return end != null && !end.isBlank() ? end : NAV_BAR_BUTTON_IME_SWITCHER;
    }

    @NonNull
    public static String getAospImeNavBarLayoutHandle() {
        String handle = PrefsBridge.getString(PREF_AOSP_IME_NAV_BAR_LAYOUT_HANDLE, "");
        if (handle != null && !handle.isBlank()) {
            return handle;
        }
        String start = getAospImeNavBarLayoutStart();
        String end = getAospImeNavBarLayoutEnd();
        if (!start.isBlank() && !end.isBlank()) {
            return start + "[70AC];" + NAV_BAR_BUTTON_HOME_HANDLE + ";" + end + "[70AC]";
        }
        return DEFAULT_AOSP_IME_NAV_BAR_LAYOUT_HANDLE;
    }

    public static boolean isSelectedInputMethodPackage(@Nullable String packageName) {
        return isSelectedPackage(PREF_IME_TARGET_APPS, packageName);
    }

    @NonNull
    public static Set<String> getSelectedInputMethodPackages() {
        return getSelectedPackages(PREF_IME_TARGET_APPS);
    }

    public static boolean isSelectedPackage(@NonNull String prefKey, @Nullable String packageName) {
        String normalizedPackage = normalizePackageName(packageName);
        return normalizedPackage != null && getSelectedPackages(prefKey).contains(normalizedPackage);
    }

    @NonNull
    public static Set<String> getSelectedPackages(@NonNull String prefKey) {
        Set<String> packages = PrefsBridge.getStringSet(prefKey);
        return packages != null ? packages : Collections.emptySet();
    }

    @Nullable
    public static String normalizePackageName(@Nullable String packageNameOrId) {
        if (packageNameOrId == null || packageNameOrId.isEmpty()) {
            return null;
        }
        int separator = packageNameOrId.indexOf('/');
        return separator >= 0 ? packageNameOrId.substring(0, separator) : packageNameOrId;
    }

    private static boolean isFeatureEnabled(@NonNull String prefKey) {
        return PrefsBridge.getBoolean(prefKey);
    }

    private static boolean hasImeStylePreference() {
        String style = PrefsBridge.getString(PREF_IME_STYLE, "");
        return style != null && !style.isEmpty();
    }
}
