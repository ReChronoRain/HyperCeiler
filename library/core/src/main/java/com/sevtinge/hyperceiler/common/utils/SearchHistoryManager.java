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
package com.sevtinge.hyperceiler.common.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public final class SearchHistoryManager {

    private static final String PREFS_NAME = "search_history";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_SIZE = 20;

    private SearchHistoryManager() {}

    public static List<String> getHistory(Context context) {
        String raw = getPrefs(context).getString(KEY_HISTORY, "");
        List<String> list = new ArrayList<>();
        if (!raw.isEmpty()) {
            for (String s : raw.split("\n")) {
                if (!s.isEmpty()) list.add(s);
            }
        }
        return list;
    }

    public static void addHistory(Context context, String query) {
        if (query == null || query.trim().isEmpty()) return;
        query = query.trim();
        List<String> list = getHistory(context);
        list.remove(query);
        list.addFirst(query);
        if (list.size() > MAX_SIZE) list = list.subList(0, MAX_SIZE);
        getPrefs(context).edit().putString(KEY_HISTORY, String.join("\n", list)).apply();
    }

    public static void clear(Context context) {
        getPrefs(context).edit().remove(KEY_HISTORY).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
