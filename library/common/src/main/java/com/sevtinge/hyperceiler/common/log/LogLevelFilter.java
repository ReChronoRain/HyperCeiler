package com.sevtinge.hyperceiler.common.log;

public enum LogLevelFilter {
    ALL("ALL", "全部级别", 5, 0xFF757575),
    DEBUG("D", "Debug", 4, 0xFF1976D2),
    INFO("I", "Info", 3, 0xFF388E3C),
    WARN("W", "Warn", 2, 0xFFFFA000),
    ERROR("E", "Error", 1, 0xFFD32F2F);

    private final String value;  // 对应数据库里的 "D", "I" 等
    private final String title;  // 显示在菜单上的文字
    private final int level;     // 权重
    private final int color;     // UI 颜色

    LogLevelFilter(String value, String title, int level, int color) {
        this.value = value;
        this.title = title;
        this.level = level;
        this.color = color;
    }

    public String getValue() { return value; }
    public int getColor() { return color; }

    public static String[] getTitles() {
        String[] titles = new String[values().length];
        for (int i = 0; i < values().length; i++) titles[i] = values()[i].title;
        return titles;
    }

    public static LogLevelFilter fromPos(int pos) {
        if (pos >= 0 && pos < values().length) return values()[pos];
        return ALL;
    }
}
