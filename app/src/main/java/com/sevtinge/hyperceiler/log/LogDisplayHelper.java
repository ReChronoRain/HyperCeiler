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

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.R;

public final class LogDisplayHelper {

    private static final String APP_PACKAGE = "com.sevtinge.hyperceiler";
    public static final String OTHER_TAG_VALUE = "Other";
    private static final String META_PREFIX = "[[HC_META uidPid=";
    private static final String META_SUFFIX = "]] ";

    private LogDisplayHelper() {}

    /**
     * 解析 Xposed 日志消息用于显示
     * 兼容新旧格式：
     * 旧版: [HyperCeiler][I][pkg][ClassName]: detail message
     * 新版: [pkg][ClassName]: detail message
     * @return [primary, secondary]
     */
    @NonNull
    public static String[] parseXposedDisplay(String message, String level) {
        message = stripMetaPrefix(message);
        if (message == null) {
            return new String[]{null, ""};
        }

        if ("C".equals(level)) {
            String stripped = message.replaceFirst("^(?:\\[[^\\]]+\\])+:\\s*", "");
            return new String[]{null, stripped};
        }

        int idx = message.indexOf("]: ");
        if (idx == -1) {
            return new String[]{null, message};
        }

        String brackets = message.substring(0, idx + 1);
        String rest = message.substring(idx + 3);

        int lastOpen = brackets.lastIndexOf('[');
        int lastClose = brackets.lastIndexOf(']');
        String primary = (lastOpen >= 0 && lastClose > lastOpen)
            ? brackets.substring(lastOpen + 1, lastClose) : null;

        return new String[]{primary, rest};
    }

    @NonNull
    public static String stripMetaPrefix(String message) {
        if (message == null || !message.startsWith(META_PREFIX)) {
            return message == null ? "" : message;
        }
        int end = message.indexOf(META_SUFFIX);
        if (end == -1) {
            return message;
        }
        return message.substring(end + META_SUFFIX.length());
    }

    @NonNull
    public static String withUidPidMeta(String message, String uidPid) {
        if (message == null || message.isEmpty() || uidPid == null || uidPid.isEmpty()) {
            return message == null ? "" : message;
        }
        if (message.startsWith(META_PREFIX)) {
            return message;
        }
        return META_PREFIX + uidPid + META_SUFFIX + message;
    }

    @NonNull
    public static String extractUidPid(String message) {
        if (message == null || !message.startsWith(META_PREFIX)) {
            return "";
        }
        int end = message.indexOf(META_SUFFIX);
        if (end == -1) {
            return "";
        }
        return message.substring(META_PREFIX.length(), end);
    }

    @ColorRes
    public static int getLevelBadgeColorRes(String level) {
        return switch (level) {
            case "C" -> R.color.log_level_badge_bg_crash;
            case "E" -> R.color.log_level_badge_bg_error;
            case "W" -> R.color.log_level_badge_bg_warn;
            case "I" -> R.color.log_level_badge_bg_info;
            case "D" -> R.color.log_level_badge_bg_debug;
            default -> R.color.log_level_badge_bg_default;
        };
    }

    @ColorRes
    public static int getLevelTextColorRes(String level) {
        return switch (level) {
            case "C" -> R.color.log_level_badge_text_crash;
            case "E" -> R.color.log_level_badge_text_error;
            case "W" -> R.color.log_level_badge_text_warn;
            case "I" -> R.color.log_level_badge_text_info;
            case "D" -> R.color.log_level_badge_text_debug;
            default -> R.color.log_level_badge_text_default;
        };
    }

    @NonNull
    public static String getDisplayLevel(String level) {
        return switch (level) {
            case "C" -> "CRASH";
            case "E" -> "ERROR";
            case "W" -> "WARN";
            case "I" -> "INFO";
            case "D" -> "DEBUG";
            default -> level == null ? "" : level;
        };
    }

    @NonNull
    public static String getListTitle(String module, String tag, String message, String level) {
        if (!"Xposed".equals(module)) {
            return tag == null ? "" : tag;
        }
        String[] parsed = parseXposedDisplay(message, level);
        String primary = parsed[0];
        if (primary == null || primary.isEmpty()) {
            primary = "HyperCeiler";
        }
        return primary;
    }

    @NonNull
    public static String getListSubtitle(String module, String tag, String message) {
        if (!"Xposed".equals(module)) {
            return "";
        }
        String target = extractTargetPackage(stripMetaPrefix(message));
        if (target == null || target.isEmpty()) {
            return tag == null ? "" : tag;
        }
        return APP_PACKAGE + "  ->  " + target;
    }

    @NonNull
    public static String getListMessage(String module, String message, String level) {
        if (!"Xposed".equals(module) || message == null) {
            return message == null ? "" : message;
        }
        return parseXposedDisplay(message, level)[1];
    }

    @NonNull
    public static String getExportMessage(String module, String message) {
        if (!"Xposed".equals(module)) {
            return message == null ? "" : message;
        }
        return stripMetaPrefix(message);
    }

    private static String extractTargetPackage(String message) {
        if (message == null || !message.startsWith("[")) {
            return null;
        }
        int end = message.indexOf(']');
        if (end <= 1) {
            return null;
        }
        String candidate = message.substring(1, end);
        return candidate.contains(".") ? candidate : null;
    }
}
