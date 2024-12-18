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

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.List;

public class SQLiteDatabaseHelper {

    public static boolean isDatabaseLocked(SQLiteDatabase db) {
        try {
            Cursor cursor = db.rawQuery("SELECT 1", null);
            if (cursor != null) {
                cursor.close();
            }
            return false;
        } catch (SQLiteDatabaseLockedException e) {
            return true;
        } catch (SQLiteException e) {
            return false;
        }
    }

    @SuppressLint("Range")
    public static List<String> queryList(SQLiteDatabase database, String SELECT, String FROM, String WHERE, String[] queryValue, boolean isPreferenceHeaderUse) {
        List<String> result = new ArrayList<>();
        String query = "SELECT " + SELECT + " FROM " + FROM + " WHERE " + WHERE;
        Cursor cursor = database.rawQuery(query, queryValue);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String get = cursor.getString(cursor.getColumnIndex(SELECT));
                if (isPreferenceHeaderUse && "system".equals(get)) get = "android";
                result.add(get);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return result;
    }

}
