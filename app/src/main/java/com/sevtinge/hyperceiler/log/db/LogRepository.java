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

import android.content.Context;

import androidx.room.Room;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.log.XposedLogLoader;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 日志仓库 - 整个 App 唯一的数据库访问点
 */
public class LogRepository {

    private static final String TAG = "LogRepository";
    private static final String DATABASE_NAME = "hyperceiler_logs.db";
    private static volatile LogRepository sInstance;

    private final LogDao mLogDao;
    private final Context mAppContext;
    // 专用线程池，处理数据库的写入和删除，避免阻塞主线程
    private final ExecutorService mIoExecutor;

    private LogRepository(Context context) {
        // 1. 初始化 Room 数据库 (全项目仅此一处)
        LogDatabase db = Room.databaseBuilder(
                context.getApplicationContext(),
                LogDatabase.class,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // 版本升级时如果字段不匹配，直接重建表
            .build();

        mLogDao = db.logDao();
        mAppContext = context;
        mIoExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "LogDbThread");
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
    }

    /**
     * 在 Application 中调用
     */
    public static void init(Context context) {
        if (sInstance == null) {
            synchronized (LogRepository.class) {
                if (sInstance == null) {
                    sInstance = new LogRepository(context);
                }
            }
        }
    }

    public static LogRepository getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("LogRepository must be initialized in Application first!");
        }
        return sInstance;
    }

    /**
     * 获取 DAO 接口
     */
    public LogDao getDao() {
        return mLogDao;
    }


    /**
     * 异步插入单条日志
     */
    public void insertLog(LogEntry entry) {
        mIoExecutor.execute(() -> mLogDao.insert(entry));
    }

    /**
     * 异步清空指定模块日志
     */
    public void deleteLogsByModule(String module) {
        mIoExecutor.execute(() -> mLogDao.deleteByModule(module));
    }

    /**
     * 异步清空所有日志
     */
    public void clearAllLogs() {
        mIoExecutor.execute(mLogDao::clearAll);
    }

    /**
     * 自动裁剪日志，防止数据库过大 (保留最近 5000 条)
     */
    public void autoTrim() {
        mIoExecutor.execute(mLogDao::autoTrim);
    }

    /**
     * 获取用于 IO 操作的线程池
     */
    public ExecutorService getIoExecutor() {
        return mIoExecutor;
    }

    public List<LogEntry> getLogsByModuleForExportSync(String module) {
        try {
            return mIoExecutor.submit(() -> mLogDao.getLogsByModuleForExport(module))
                .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to query logs for export: " + module, e);
            return Collections.emptyList();
        }
    }

    public List<LogEntry> getLogsByModulePageForExportSync(String module, int limit, int offset) {
        try {
            return mIoExecutor.submit(() -> mLogDao.getLogsByModulePageForExport(module, limit, offset))
                .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to query paged logs for export: " + module, e);
            return Collections.emptyList();
        }
    }

    public List<LogEntry> getLogsByModuleAndSourceGroupPageForExportSync(
        String module,
        String sourceGroup,
        int limit,
        int offset
    ) {
        try {
            return mIoExecutor.submit(() -> mLogDao.getLogsByModuleAndSourceGroupPageForExport(module, sourceGroup, limit, offset))
                .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to query paged logs for export: " + module + ", " + sourceGroup, e);
            return Collections.emptyList();
        }
    }

    /**
     * 执行同步操作（将文件搬运到数据库）
     */
    public void syncXposedLogs() {
        mIoExecutor.execute(() -> {
            XposedLogLoader.syncLogsToDatabase(mAppContext);
        });
    }

    /**
     * 清理指定模块的日志
     */
    public void clearLogs(String module) {
        mIoExecutor.execute(() -> {
            if (module == null) {
                mLogDao.clearAll();
            } else {
                mLogDao.deleteByModule(module);
            }
        });
    }

    /**
     * 自动裁剪数据库，防止无限增长
     */
    public void trimDatabase() {
        mIoExecutor.execute(() -> mLogDao.autoTrim());
    }

    public Context getContext() {
        return mAppContext;
    }
}
