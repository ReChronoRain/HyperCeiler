package com.sevtinge.hyperceiler.home.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchHistorySPUtils {
    private final SharedPreferences mSharedPreferences;
    private static final String DELIMITER = "¶";

    public SearchHistorySPUtils(Context context, String name) {
        mSharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public void saveDataList(String key, List<?> list) {
        if (list == null || list.isEmpty()) return;

        String joinedString = TextUtils.join(DELIMITER, list);

        SharedPreferences.Editor edit = mSharedPreferences.edit();
        edit.putString(key, joinedString);
        edit.apply();
    }

    public List<String> loadDataList(String key) {
        String serialized = mSharedPreferences.getString(key, "");
        if (TextUtils.isEmpty(serialized)) {
            return new ArrayList<>();
        }

        String[] parts = serialized.split(DELIMITER);
        return new ArrayList<>(Arrays.asList(parts));
    }

    public void removeDateList(String key) {
        mSharedPreferences.edit().remove(key).apply();
    }
}
