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

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LogDao {

    /**
     * 1. 写入：插入单条 (App 运行时产生)
     */
    @Insert
    void insert(LogEntry entry);

    /**
     * 2. 批量写入：用于 Xposed 日志同步 (性能更高)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LogEntry> entries);

    /**
     * 3. UI 列表核心查询：
     * 支持模块筛选、级别过滤、关键词搜索，单页查看时放宽到最近 20000 条
     */
    @Query("SELECT * FROM logs WHERE module = :module " +
        "AND (:level = 'ALL' OR level = :level) " +
        "AND (:keyword = '' OR message LIKE '%' || :keyword || '%' OR tag LIKE '%' || :keyword || '%') " +
        "ORDER BY timestamp DESC LIMIT 1000")
    List<LogEntry> queryLogs(String module, String level, String keyword);

    /**
     * 4. 清理功能：按模块清空日志
     */
    @Query("DELETE FROM logs WHERE module = :module")
    void deleteByModule(String module);

    /**
     * 5. 维护功能：防止数据库过大，只保留最近的 50000 条
     */
    @Query("DELETE FROM logs WHERE id NOT IN (SELECT id FROM logs ORDER BY timestamp DESC LIMIT 50000)")
    void autoTrim();

    /**
     * 彻底清空所有日志
     */
    @Query("DELETE FROM logs")
    void clearAll();

    /**
     * 获取指定模块下所有的不重复标签 (用于填充 UI 上的下拉列表)
     * @param module "App" 或 "Xposed"
     */
    @Query("SELECT DISTINCT tag FROM logs WHERE module = :module AND tag IS NOT NULL ORDER BY tag ASC")
    List<String> getDistinctTags(String module);

    /**
     * 四参数核心查询：支持 模块 + 级别 + 标签 + 关键词
     * 处理逻辑：如果 level 是 'ALL'，或者 tag 为空，则跳过该条件
     */
    @Query("SELECT * FROM logs WHERE module = :module " +
        "AND (:level = 'ALL' OR level = :level) " +
        "AND (:tag = '' OR tag = :tag) " +
        "AND (:keyword = '' OR message LIKE '%' || :keyword || '%' OR tag LIKE '%' || :keyword || '%') " +
        "ORDER BY timestamp DESC LIMIT 1000")
    List<LogEntry> queryLogs(String module, String level, String tag, String keyword);

    @Query("SELECT * FROM logs ORDER BY timestamp ASC")
    List<LogEntry> getAllLogsForExport();

    @Query("SELECT * FROM logs WHERE module = :module ORDER BY timestamp ASC")
    List<LogEntry> getLogsByModuleForExport(String module);
}
