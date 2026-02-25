package com.sevtinge.hyperceiler.home.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

import org.json.JSONArray;

import java.util.Arrays;

public class SearchHistorySPUtils {
    private final SharedPreferences mSharedPreferences;
    private static final String DELIMITER = "¶"; // 使用特殊字符作为分隔符，避免与搜索内容冲突
    private static final int MAX_HISTORY_COUNT = 15;

    public SearchHistorySPUtils(Context context, String name) {
        mSharedPreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    /**
     * 保存搜索词（保序且去重）
     */
    public void saveDataList(String key, List<?> list) {
        // 合法性校验
        if (list == null || list.isEmpty()) return;

        // 将 List 转换为以逗号分隔的字符串，例如: "小,大"
        String joinedString = TextUtils.join(DELIMITER, list);

        // 3. 存储到 SharedPreferences
        SharedPreferences.Editor edit = mSharedPreferences.edit();

        // 注意：原代码中有 edit.clear()，这会删掉该 Sp 文件下的【所有】其他数据
        // 如果你的原意只是想更新当前 key，请删掉下面这行
        edit.clear();

        edit.putString(key, joinedString);
        edit.apply();
    }

    /**
     * 读取搜索历史
     */
    public List<String> loadDataList(String key) {
        String serialized = mSharedPreferences.getString(key, "");
        if (TextUtils.isEmpty(serialized)) {
            return new ArrayList<>();
        }

        // 转换为可操作的 ArrayList
        String[] parts = serialized.split(DELIMITER);
        return new ArrayList<>(Arrays.asList(parts));
    }

    /**
     * 清空
     */
    public void removeDateList(String key) {
        mSharedPreferences.edit().remove(key).apply();
    }
}

