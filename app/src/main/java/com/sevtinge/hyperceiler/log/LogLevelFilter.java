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
package com.sevtinge.hyperceiler.log;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.sevtinge.hyperceiler.R;

public enum LogLevelFilter {
    ALL("ALL", R.string.log_filter_all),
    DEBUG("D", R.string.log_level_debug),
    INFO("I", R.string.log_level_info),
    WARN("W", R.string.log_level_warn),
    ERROR("E", R.string.log_level_error);

    private final String value;
    private final int titleResId;

    LogLevelFilter(String value, @StringRes int titleResId) {
        this.value = value;
        this.titleResId = titleResId;
    }

    @NonNull
    public String getValue() {
        return value;
    }

    @NonNull
    public String getTitle(@NonNull Context context) {
        return context.getString(titleResId);
    }

    public static boolean isAll(String value) {
        return ALL.value.equals(value);
    }

    @NonNull
    public static String[] getTitles(@NonNull Context context) {
        LogLevelFilter[] filters = values();
        String[] titles = new String[filters.length];
        for (int i = 0; i < filters.length; i++) {
            titles[i] = filters[i].getTitle(context);
        }
        return titles;
    }

    @NonNull
    public static LogLevelFilter fromPos(int pos) {
        LogLevelFilter[] filters = values();
        if (pos >= 0 && pos < filters.length) {
            return filters[pos];
        }
        return ALL;
    }
}
