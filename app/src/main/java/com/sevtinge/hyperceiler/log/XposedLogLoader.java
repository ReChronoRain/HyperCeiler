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
package com.sevtinge.hyperceiler.log;

import android.content.Context;
import android.util.Log;

import com.sevtinge.hyperceiler.log.db.LogDao;
import com.sevtinge.hyperceiler.log.db.LogEntry;
import com.sevtinge.hyperceiler.log.db.LogRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XposedLogLoader {
    private static final String TAG = "XposedLogLoader";

    // 预设路径：LSPosed 的模块日志文件 (通常需要 Root 权限)
    private static final String SYSTEM_LSPD_LOG_PATH = "/data/adb/lspd/log/modules/com.sevtinge.hyperceiler.log";

    // 正则表达式：匹配 [I/Tag]: Message 格式
    private static final Pattern LOG_PATTERN = Pattern.compile("\\[(V|D|I|W|E|C)/(.*?)\\]: (.*)");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    /**
     * 获取 App 私有目录下的日志副本路径
     */
    private static File getLocalLogFile(Context context) {
        // 存储在 /data/user/0/com.sevtinge.hyperceiler/files/log/xposed_sync.log
        File logDir = new File(context.getFilesDir(), "log");
        if (!logDir.exists()) logDir.mkdirs();
        return new File(logDir, "xposed_sync.log");
    }

    /**
     * 第一步：将系统日志文件拷贝到私有目录
     */
    private static boolean copyXposedLog(Context context) {
        File source = new File(SYSTEM_LSPD_LOG_PATH);
        File dest = getLocalLogFile(context);

        if (!source.exists()) {
            Log.e(TAG, "Source log file not found: " + SYSTEM_LSPD_LOG_PATH);
            return false;
        }

        try (FileChannel srcChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
            destChannel.transferFrom(srcChannel, 0, srcChannel.size());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy log file. Root permission might be needed.", e);
            return false;
        }
    }

    /**
     * 第二步：同步到数据库 (核心方法)
     */
    public static void syncLogsToDatabase(Context context) {
        new Thread(() -> {
            // 1. 拷贝文件
            if (!copyXposedLog(context)) return;

            // 2. 准备数据库
            LogDao dao = LogRepository.getInstance().getDao();
            long lastTs = dao.getLastXposedTimestamp();
            List<LogEntry> batch = new ArrayList<>();

            // 3. 解析并入库
            try (BufferedReader reader = new BufferedReader(new FileReader(getLocalLogFile(context)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LogEntry entry = parseLine(line);
                    // 增量插入：只插入比数据库里更新的日志
                    if (entry != null && entry.getTimestamp() > lastTs) {
                        batch.add(entry);
                    }

                    if (batch.size() >= 500) {
                        dao.insertAll(batch);
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    dao.insertAll(batch);
                }
                Log.i(TAG, "Sync to database complete.");
            } catch (IOException e) {
                Log.e(TAG, "Read sync file failed", e);
            }
        }).start();
    }

    /**
     * 正则解析行逻辑
     */
    private static LogEntry parseLine(String line) {
        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.find()) {
            String level = matcher.group(1);
            String tag = matcher.group(2);
            String message = matcher.group(3);

            // 构造 LogEntry，标记为 Xposed 模块
            return new LogEntry("Xposed", level, tag, message);
        }
        return null;
    }

    /**
     * 清空操作：清空库和本地副本
     */
    public static void clearAll(Context context) {
        new Thread(() -> {
            LogRepository.getInstance().getDao().deleteByModule("Xposed");
            File local = getLocalLogFile(context);
            if (local.exists()) local.delete();
        }).start();
    }

    /**
     * 导出操作
     */
    public static void exportLogs(Context context, File targetFile) {
        new Thread(() -> {
            List<LogEntry> logs = LogRepository.getInstance().getDao().getAllLogsForExport("Xposed");
            try (FileWriter writer = new FileWriter(targetFile)) {
                writer.write("--- HyperCeiler Log Export ---\n");
                for (LogEntry entry : logs) {
                    String time = DATE_FORMAT.format(new Date(entry.getTimestamp()));
                    writer.write(String.format("[%s] [%s/%s]: %s\n", time, entry.getLevel(), entry.getTag(), entry.getMessage()));
                }
            } catch (IOException e) {
                Log.e(TAG, "Export failed", e);
            }
        }).start();
    }
}
