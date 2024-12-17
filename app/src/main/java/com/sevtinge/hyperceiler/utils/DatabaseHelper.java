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

import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

public class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * 一定要使用try-catch块包围，因为永远不知道会出现什么奇奇怪怪的问题
     */
    public DatabaseHelper(Context context, String databaseName) {
        super(context, databaseName, null, getUserVersion(context, databaseName));
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
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

    public static int getUserVersion(Context context, String databaseName) {
        int userVersion = 1;
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = SQLiteDatabase.openDatabase(databaseName, null, SQLiteDatabase.OPEN_READONLY);
            cursor = db.rawQuery("PRAGMA user_version;", null);
            if (cursor != null && cursor.moveToFirst()) {
                userVersion = cursor.getInt(0);
            }
        } catch (Exception e) {
            XposedLogUtils.logD("DatabaseHelper", e.toString());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return userVersion;
    }
}

