package com.fan.common.logviewer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogEntry {
    private final long mTimestamp;
    private final String mLevel;
    private final String mModule;
    private final String mMessage;
    private final String mTag;
    private final boolean mNewLine;

    private static final SimpleDateFormat sTimeFormat =
        new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());

    public LogEntry(String level, String module, String message, String tag, boolean newLine) {
        this.mTimestamp = System.currentTimeMillis();
        this.mLevel = level;
        this.mModule = module;
        this.mMessage = message;
        this.mTag = tag;
        this.mNewLine = newLine;
    }

    // Getters
    public long getTimestamp() {
        return mTimestamp;
    }

    public String getLevel() {
        return mLevel;
    }

    public String getModule() {
        return mModule;
    }

    public String getMessage() {
        return mMessage;
    }

    public String getTag() {
        return mTag;
    }

    public boolean isNewLine() {
        return mNewLine;
    }

    public String getFormattedTime() {
        return sTimeFormat.format(new Date(mTimestamp));
    }

    public int getColor() {
        return switch (mLevel) {
            case "V" -> 0xFF909090; // VERBOSE - Gray
            case "D" -> 0xFF2196F3; // DEBUG - Blue
            case "I" -> 0xFF4CAF50; // INFO - Green
            case "W" -> 0xFFFFC107; // WARN - Amber
            case "E" -> 0xFFF44336; // ERROR - Red
            default -> 0xFF000000;  // Black
        };
    }
}
