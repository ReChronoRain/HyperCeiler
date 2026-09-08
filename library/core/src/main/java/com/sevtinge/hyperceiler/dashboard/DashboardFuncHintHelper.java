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
package com.sevtinge.hyperceiler.dashboard;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.core.R;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class DashboardFuncHintHelper {
    private static final String TAG = "DashboardFuncHintHelper";
    private static final long UNKNOWN_VERSION_CODE = -1L;

    private final DashboardFragment mHost;
    private final DashboardPreferencePageLockHelper mPageLockHelper;
    private final Map<String, Long> mVersionCodeCache = new ConcurrentHashMap<>();

    DashboardFuncHintHelper(@NonNull DashboardFragment host, @NonNull DashboardPreferencePageLockHelper pageLockHelper) {
        mHost = host;
        mPageLockHelper = pageLockHelper;
    }

    boolean setFuncHint(@NonNull Preference preference, int value, @Nullable String pkgName, long... ranges) {
        VersionRange[] parsedRanges = parseRanges(ranges);
        long versionCode = getVersionCode(pkgName);
        return applyRule(FuncHintRule.of(preference, value, DashboardFragment.APP_MATCH_OUT_OF_RANGE, parsedRanges), versionCode);
    }

    boolean setFuncHint(@NonNull Preference preference, int value, int matchMode, @Nullable String pkgName, long... ranges) {
        VersionRange[] parsedRanges = parseRanges(ranges);
        long versionCode = getVersionCode(pkgName);
        return applyRule(FuncHintRule.of(preference, value, matchMode, parsedRanges), versionCode);
    }

    boolean setFuncHint(@NonNull Preference preference, int value, @Nullable String pkgName, @Nullable VersionRange... ranges) {
        long versionCode = getVersionCode(pkgName);
        return applyRule(FuncHintRule.of(preference, value, DashboardFragment.APP_MATCH_OUT_OF_RANGE, ranges), versionCode);
    }

    boolean setFuncHint(@NonNull Preference preference, int value, int matchMode, @Nullable String pkgName, @Nullable VersionRange... ranges) {
        long versionCode = getVersionCode(pkgName);
        return applyRule(FuncHintRule.of(preference, value, matchMode, ranges), versionCode);
    }

    void setFuncHints(@Nullable String pkgName, @NonNull FuncHintRule... rules) {
        long versionCode = getVersionCode(pkgName);
        for (FuncHintRule rule : rules) {
            if (rule == null || rule.preference == null) continue;
            applyRule(rule, versionCode);
        }
    }

    private boolean applyRule(@NonNull FuncHintRule rule, long versionCode) {
        Preference preference = rule.preference;
        if (versionCode == UNKNOWN_VERSION_CODE) {
            mPageLockHelper.lockWithVersionUnavailableWarning();
            disablePreference(preference);
            return false;
        }

        boolean inRange = isInVersionRange(versionCode, rule.versionRanges);
        boolean shouldDisable = (rule.matchMode == DashboardFragment.APP_MATCH_IN_RANGE) == inRange;
        if (!shouldDisable) {
            return preference.isEnabled();
        }

        disablePreference(preference);
        applySummary(preference, rule.value);
        return false;
    }

    private void applySummary(@NonNull Preference preference, int value) {
        switch (value) {
            case DashboardFragment.APP_HINT_UNSUPPORTED ->
                preference.setSummary(R.string.unsupported_app_version_func);
            case DashboardFragment.APP_HINT_SUPPORTED ->
                preference.setSummary(R.string.supported_app_version_func);
            default ->
                throw new IllegalStateException("Unexpected app version summary value: " + value);
        }
    }

    private void disablePreference(@NonNull Preference preference) {
        String key = preference.getKey();
        if (!TextUtils.isEmpty(key)) {
            mHost.cleanKey(key);
        }
        preference.setEnabled(false);
    }

    private long getVersionCode(@Nullable String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            return UNKNOWN_VERSION_CODE;
        }
        Long cached = mVersionCodeCache.get(pkgName);
        if (cached != null) {
            return cached;
        }

        long versionCode = queryVersionCode(pkgName);
        mVersionCodeCache.put(pkgName, versionCode);
        return versionCode;
    }

    private long queryVersionCode(@NonNull String pkgName) {
        try {
            boolean isDebugMode = mHost.getSharedPreferences().getBoolean("prefs_key_development_debug_mode", false);
            int debugVersionCode = mHost.getSharedPreferences().getInt("debug_choose_" + pkgName, 0);
            if (debugVersionCode <= 0) {
                debugVersionCode = mHost.getSharedPreferences().getInt("prefs_key_debug_choose_" + pkgName, 0);
            }
            if (isDebugMode && debugVersionCode > 0) {
                return debugVersionCode;
            }

            PackageInfo packageInfo = mHost.requireContext().getPackageManager().getPackageInfo(pkgName, PackageManager.MATCH_ALL);
            long versionCode = packageInfo.getLongVersionCode();
            return versionCode > 0 ? versionCode : UNKNOWN_VERSION_CODE;
        } catch (PackageManager.NameNotFoundException e) {
            AndroidLog.w(TAG, "Failed to find package while querying version: " + pkgName, e);
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to query package version: " + pkgName, e);
        }
        return UNKNOWN_VERSION_CODE;
    }

    private boolean isInVersionRange(long versionCode, @Nullable VersionRange... ranges) {
        if (ranges == null || ranges.length == 0) return true;
        for (VersionRange range : ranges) {
            if (range == null) continue;
            boolean matchMin = range.minVersionCode <= 0 || versionCode >= range.minVersionCode;
            boolean matchMax = range.maxVersionCode <= 0 || versionCode <= range.maxVersionCode;
            if (matchMin && matchMax) return true;
        }
        return false;
    }

    static FuncHintRule rule(@NonNull Preference preference, int value, @Nullable VersionRange... ranges) {
        return FuncHintRule.of(preference, value, DashboardFragment.APP_MATCH_OUT_OF_RANGE, ranges);
    }

    static FuncHintRule rule(@NonNull Preference preference, int value, int matchMode, @Nullable VersionRange... ranges) {
        return FuncHintRule.of(preference, value, matchMode, ranges);
    }

    static VersionRange range(long minVersionCode, long maxVersionCode) {
        return VersionRange.between(minVersionCode, maxVersionCode);
    }

    static VersionRange atLeast(long minVersionCode) {
        return VersionRange.atLeast(minVersionCode);
    }

    static VersionRange atMost(long maxVersionCode) {
        return VersionRange.atMost(maxVersionCode);
    }

    private static VersionRange[] parseRanges(@Nullable long... ranges) {
        if (ranges == null || ranges.length == 0) {
            return new VersionRange[0];
        }
        if (ranges.length % 2 != 0) {
            throw new IllegalStateException("Version ranges must be min/max pairs.");
        }

        VersionRange[] parsed = new VersionRange[ranges.length / 2];
        int idx = 0;
        for (int i = 0; i < ranges.length; i += 2) {
            parsed[idx++] = VersionRange.between(ranges[i], ranges[i + 1]);
        }
        return parsed;
    }

    public static final class FuncHintRule {
        final Preference preference;
        final int value;
        final int matchMode;
        final VersionRange[] versionRanges;

        private FuncHintRule(@NonNull Preference preference, int value, int matchMode, @Nullable VersionRange[] versionRanges) {
            this.preference = preference;
            this.value = value;
            this.matchMode = matchMode;
            this.versionRanges = versionRanges;
        }

        public static FuncHintRule of(@NonNull Preference preference, int value, long... ranges) {
            return of(preference, value, DashboardFragment.APP_MATCH_OUT_OF_RANGE, parseRanges(ranges));
        }

        public static FuncHintRule of(@NonNull Preference preference, int value, @Nullable VersionRange... ranges) {
            return of(preference, value, DashboardFragment.APP_MATCH_OUT_OF_RANGE, ranges);
        }

        public static FuncHintRule of(@NonNull Preference preference, int value, int matchMode, long... ranges) {
            return of(preference, value, matchMode, parseRanges(ranges));
        }

        public static FuncHintRule of(@NonNull Preference preference, int value, int matchMode, @Nullable VersionRange... ranges) {
            return new FuncHintRule(preference, value, matchMode, ranges);
        }
    }

    public static final class VersionRange {
        final long minVersionCode;
        final long maxVersionCode;

        private VersionRange(long minVersionCode, long maxVersionCode) {
            this.minVersionCode = minVersionCode;
            this.maxVersionCode = maxVersionCode;
        }

        public static VersionRange between(long minVersionCode, long maxVersionCode) {
            return new VersionRange(minVersionCode, maxVersionCode);
        }

        public static VersionRange atLeast(long minVersionCode) {
            return new VersionRange(minVersionCode, 0);
        }

        public static VersionRange atMost(long maxVersionCode) {
            return new VersionRange(0, maxVersionCode);
        }
    }
}
