package com.sevtinge.hyperceiler.utils.search;


import android.text.TextUtils;

import com.sevtinge.hyperceiler.data.ModData;

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
