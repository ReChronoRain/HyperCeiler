package com.sevtinge.hyperceiler.logviewer;

public class LogXposedParseHelper {

    /**
     * 解析 Xposed 日志消息用于显示
     * 兼容新旧格式：
     *   旧版: [HyperCeiler][I][pkg][ClassName]: detail message
     *   新版: [pkg][ClassName]: detail message
     * @return [primary, secondary]
     */
    public static String[] parseXposedDisplay(String message, String level) {
        if (message == null) return new String[]{null, ""};

        // Crash：去掉所有 [...] 前缀
        if ("C".equals(level)) {
            String stripped = message.replaceFirst("^(?:\\[[^\\]]+\\])+:\\s*", "");
            return new String[]{null, stripped};
        }

        // 找 "]: " 分割点
        int idx = message.indexOf("]: ");
        if (idx == -1) return new String[]{null, message};

        String brackets = message.substring(0, idx + 1);
        String rest = message.substring(idx + 3);

        // 提取最后一个 [xxx] 作为主要内容
        int lastOpen = brackets.lastIndexOf('[');
        int lastClose = brackets.lastIndexOf(']');
        String primary = (lastOpen >= 0 && lastClose > lastOpen)
            ? brackets.substring(lastOpen + 1, lastClose) : null;

        return new String[]{primary, rest};
    }

    public static int getLevelBadgeColor(String level) {
        return switch (level) {
            case "C" -> 0xFFD32F2F;
            case "E" -> 0x40F44336;
            case "W" -> 0x40FFC107;
            case "I" -> 0x404CAF50;
            case "D" -> 0x402196F3;
            default -> 0x40909090;
        };
    }

    public static int getLevelTextColor(String level) {
        return switch (level) {
            case "C" -> 0xFFFFFFFF;
            case "E" -> 0xFFF44336;
            case "W" -> 0xFFFF8F00;
            case "I" -> 0xFF388E3C;
            case "D" -> 0xFF1976D2;
            default -> 0xFF757575;
        };
    }
}
