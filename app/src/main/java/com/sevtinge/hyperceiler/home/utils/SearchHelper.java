package com.sevtinge.hyperceiler.home.utils;

import com.sevtinge.hyperceiler.common.model.adapter.ModSearchAdapter;
import com.sevtinge.hyperceiler.common.model.data.ModData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SearchHelper {

    public static final List<ModData> allModsList = new ArrayList<>();

    public static List<ModData> doSearchSync(String query, boolean isChina) {
        String filterStr = query.toLowerCase().trim();
        List<ModData> resultList = new ArrayList<>();
        HashSet<String> addedKeys = new HashSet<>();

        if (isChina) {
            // 模拟原本的 findList 逻辑：支持多字分散搜索
            for (int i = 0; i < filterStr.length(); i++) {
                String sub = String.valueOf(filterStr.charAt(i));
                for (ModData data : allModsList) {
                    if (data.title.toLowerCase().contains(sub) && !addedKeys.contains(data.key)) {
                        resultList.add(data);
                        addedKeys.add(data.key);
                    }
                }
            }
            // 排序
            resultList.sort(new ModSearchAdapter.ModDataComparator(filterStr));
        } else {
            // 普通搜索
            for (ModData data : allModsList) {
                if (data.title.toLowerCase().contains(filterStr)) {
                    resultList.add(data);
                }
            }
        }
        return resultList;
    }

}
