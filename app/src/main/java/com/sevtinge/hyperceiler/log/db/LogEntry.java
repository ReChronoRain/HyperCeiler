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
package com.sevtinge.hyperceiler.log.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(
    tableName = "logs",
    indices = {
        @Index("module"),
        @Index("timestamp")
    }
)
public class LogEntry {
    public static final String SOURCE_GROUP_CURRENT = "log";
    public static final String SOURCE_GROUP_OLD = "log.old";

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "module")
    private String module; // "App" 或 "Xposed"

    @ColumnInfo(name = "level")
    private String level; // V, D, I, W, E, C

    @ColumnInfo(name = "tag")
    private String tag;

    @ColumnInfo(name = "message")
    private String message;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "source_group", defaultValue = "'log'")
    private String sourceGroup;

    @ColumnInfo(name = "process_ids")
    private String processIds;

    // 无参构造函数 (Room 需要)
    @Ignore
    public LogEntry() {}

    @Ignore
    public LogEntry(String module, String level, String tag, String message) {
        this(module, level, tag, message, System.currentTimeMillis(), SOURCE_GROUP_CURRENT, "");
    }

    @Ignore
    public LogEntry(String module, String level, String tag, String message, long timestamp) {
        this(module, level, tag, message, timestamp, SOURCE_GROUP_CURRENT, "");
    }

    @Ignore
    public LogEntry(String module, String level, String tag, String message, long timestamp, String sourceGroup) {
        this(module, level, tag, message, timestamp, sourceGroup, "");
    }

    public LogEntry(String module, String level, String tag, String message, long timestamp, String sourceGroup, String processIds) {
        this.module = module;
        this.level = level;
        this.tag = tag;
        this.message = message;
        this.timestamp = timestamp;
        this.sourceGroup = sourceGroup == null || sourceGroup.isEmpty() ? SOURCE_GROUP_CURRENT : sourceGroup;
        this.processIds = processIds == null ? "" : processIds;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public String getModule() {
        return module;
    }

    public String getLevel() {
        return level;
    }

    public String getTag() {
        return tag;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getSourceGroup() {
        return sourceGroup;
    }

    public String getProcessIds() {
        return processIds;
    }

    public String getFormattedTime() {
        return getDateTimeFormat(new Date(this.timestamp));
    }

    private String getDateTimeFormat(Date date) {
        SimpleDateFormat sDateTimeFormat = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
        return sDateTimeFormat.format(date);
    }
}
