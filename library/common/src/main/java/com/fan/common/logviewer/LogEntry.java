/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.fan.common.logviewer;

import android.annotation.SuppressLint;

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
    private final int mUid;
    private final int mPid;
    private final int mTid;

    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat sTimeFormat =
        new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat sDateTimeFormat =
        new SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.getDefault());
    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat sLogFileFormat =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());

    public LogEntry(String level, String module, String message, String tag, boolean newLine) {
        this(System.currentTimeMillis(), level, module, message, tag, newLine,
            android.os.Process.myUid(), android.os.Process.myPid(), android.os.Process.myTid());
    }

    public LogEntry(long timestamp, String level, String module, String message, String tag, boolean newLine) {
        this(timestamp, level, module, message, tag, newLine, 0, 0, 0);
    }

    public LogEntry(long timestamp, String level, String module, String message, String tag, boolean newLine,
                    int uid, int pid, int tid) {
        this.mTimestamp = timestamp;
        this.mLevel = level;
        this.mModule = module;
        this.mMessage = message;
        this.mTag = tag;
        this.mNewLine = newLine;
        this.mUid = uid;
        this.mPid = pid;
        this.mTid = tid;
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

    public int getUid() {
        return mUid;
    }

    public int getPid() {
        return mPid;
    }

    public int getTid() {
        return mTid;
    }

    public String getFormattedTime() {
        Date logDate = new Date(mTimestamp);
        long now = System.currentTimeMillis();
        // 如果是今天，只显示时间；否则显示日期+时间
        if (isSameDay(logDate, now)) {
            return sTimeFormat.format(logDate);
        } else {
            return sDateTimeFormat.format(logDate);
        }
    }

    public String getLogFileFormattedTime() {
        return sLogFileFormat.format(new Date(mTimestamp));
    }

    private static boolean isSameDay(Date d1, long currentTimeMillis) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return dayFormat.format(d1).equals(dayFormat.format(new Date(currentTimeMillis)));
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

    public String toLogFileLine() {
        return String.format(Locale.US, "[ %s %8d:%6d:%6d %s/%-12s ] %s",
            getLogFileFormattedTime(),
            mUid,
            mPid,
            mTid,
            mLevel,
            mTag,
            mMessage);
    }
}
