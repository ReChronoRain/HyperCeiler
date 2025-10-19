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
package com.sevtinge.hyperceiler.common.utils.search;

import android.text.TextUtils;

import com.sevtinge.hyperceiler.common.model.data.ModData;

public class SearchResultItem implements Comparable<SearchResultItem> {

    public static final int SEARCH_ITEM_NORMAL = 0;
    public static final int SEARCH_EMPTY = 1;
    public static final int SEARCH_SEPARATE_APP = 2;
    public static final int SEARCH_CATEGORY = 3;
    public static final int SEARCH_FOOTER = 4;

    public String group;

    public String title;
    public String breadcrumbs;
    public String key;
    public ModData.ModCat cat;
    public String sub;
    public int order;
    public String fragment;
    public int catTitleResId;

    public final int type;
    public static final SearchResultItem EMPTY = new SearchResultItem(SEARCH_EMPTY);
    public static final SearchResultItem CATEGORY = new SearchResultItem(SEARCH_CATEGORY);
    public static final SearchResultItem FOOTER = new SearchResultItem(SEARCH_FOOTER);

    public SearchResultItem(int type) {
        this.type = type;
    }

    public String getGroupInfo(String str, String str2) {
        String str3 = !TextUtils.isEmpty(str) ? str.split("/")[0] : null;
        if (!TextUtils.isEmpty(str3)) {
            return str3;
        }
        if (!TextUtils.isEmpty(str2)) {
            str3 = str2.split("/")[0];
        }
        if (!TextUtils.isEmpty(str3)) {
            return str3;
        }
        throw new RuntimeException("group is null: " + title);
    }

    @Override
    public int compareTo(SearchResultItem o) {
        return 0;
    }
}
