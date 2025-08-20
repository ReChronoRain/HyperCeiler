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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Pair;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class XmlResourceParserHelper {

    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    // 缓存解析结果（key：xmlResId，value：解析出的键值对列表）
    private static final Map<Integer, List<Pair<String, String>>> mXmlParseCache = new HashMap<>();

    /**
     * 处理XML资源（带缓存）
     */
    public static void processCachedXmlResource(Resources res, int xmlResId, BiConsumer<String, String> processor) throws XmlPullParserException, IOException {
        // 先查缓存，命中则直接处理
        List<Pair<String, String>> cachedData = mXmlParseCache.get(xmlResId);
        if (cachedData != null) {
            for (Pair<String, String> pair : cachedData) {
                processor.accept(pair.first, pair.second);
            }
            return;
        }

        // 未命中缓存，解析XML并缓存结果
        List<Pair<String, String>> dataList = new ArrayList<>();
        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int event = xml.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    String name = xml.getName();
                    // 只处理目标标签，减少无效判断
                    if ("SwitchPreference".equals(name)) {
                        String key = xml.getAttributeValue(ANDROID_NS, "key");
                        if (key != null) {
                            // 过滤无效key
                            dataList.add(new Pair<>(key, null));
                            processor.accept(key, null);
                        }
                    } else if (isPreferenceHeaderTag(xml)) {
                        String key = xml.getAttributeValue(ANDROID_NS, "key");
                        String summary = xml.getAttributeValue(ANDROID_NS, "summary");
                        if (key != null) { // 过滤无效key
                            dataList.add(new Pair<>(key, summary));
                            processor.accept(key, summary);
                        }
                    }
                }
                event = xml.next();
            }
            // 缓存解析结果（仅缓存有效数据）
            mXmlParseCache.put(xmlResId, dataList);
        }
    }

    private static boolean isPreferenceHeaderTag(android.content.res.XmlResourceParser xml) {
        return "com.sevtinge.hyperceiler.common.prefs.PreferenceHeader".equals(xml.getName());
    }
}
