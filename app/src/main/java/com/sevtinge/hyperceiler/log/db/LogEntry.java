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

    // 无参构造函数 (Room 需要)
    @Ignore
    public LogEntry() {}

    @Ignore
    public LogEntry(String module, String level, String tag, String message) {
        this(module, level, tag, message, System.currentTimeMillis());
    }

    public LogEntry(String module, String level, String tag, String message, long timestamp) {
        this.module = module;
        this.level = level;
        this.tag = tag;
        this.message = message;
        this.timestamp = timestamp;
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

    public String getFormattedTime() {
        return getDateTimeFormat(new Date(this.timestamp));
    }

    private String getDateTimeFormat(Date date) {
        SimpleDateFormat sDateTimeFormat = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
        return sDateTimeFormat.format(date);
    }
}

