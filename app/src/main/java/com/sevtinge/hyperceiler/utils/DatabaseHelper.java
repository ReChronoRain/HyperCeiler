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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    // private static final String DATABASE_NAME = "/data/adb/lspd/config/module_config.db";
    private static final int DATABASE_VERSION = 3;

    public DatabaseHelper(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 这里只是示例，如果需要创建表，写入创建表的 SQL 语句
        db.execSQL("CREATE TABLE modules (id INTEGER PRIMARY KEY, module_pkg_name TEXT, mid INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS modules");
        onCreate(db);
    }

    /**
     * 执行自定义查询
     * @param tableName  表名
     * @param columns    需要查询的列（传 null 查询所有列）
     * @param selection  查询条件，例如 "column_name = ?"
     * @param selectionArgs 查询条件参数
     * @param orderBy    排序依据
     * @return 查询结果的 Cursor
     */
    public Cursor customQuery(String tableName, String[] columns, String selection, String[] selectionArgs, String orderBy) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(tableName, columns, selection, selectionArgs, null, null, orderBy);
    }
}

