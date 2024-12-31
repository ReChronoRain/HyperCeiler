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

import com.hchen.hooktool.tool.CoreTool;

public class ContentModel {
    public static ClassLoader classLoader;
    public String content;
    public long time;
    public int type;
    public String determineContent;
    public String deviceId;
    public String deviceName;
    public String deviceType;
    public boolean isAcrossDevices;
    public boolean isShow = true;
    public boolean isTemp;

    public ContentModel(String content, int type, long time) {
        this.content = content;
        this.type = type;
        this.time = time;
    }

    public static Object createContentModel(String content, int type, long time) {
        return CoreTool.newInstance("com.miui.inputmethod.ClipboardContentModel", classLoader,
                content, type, time);
    }

    public static boolean putContent(Object data, String content) {
        return CoreTool.setField(data, "content", content);
    }

    public static boolean putType(Object data, int type) {
        return CoreTool.setField(data, "type", type);
    }

    public static boolean putTime(Object data, long time) {
        return CoreTool.setField(data, "time", time);
    }

    public static String getContent(Object data) {
        return (String) CoreTool.getField(data, "content");
    }

    public static int getType(Object data) {
        return (int) CoreTool.getField(data, "type");
    }

    public static long getTime(Object data) {
        return (long) CoreTool.getField(data, "time");
    }
}
