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

import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logI;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileHelper {
    public static String TAG = "FileHelper";

    public static boolean exists(String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent == null) {
            logE(TAG, "path: " + path + " parent is null");
            return false;
        }
        if (!parent.exists()) {
            if (parent.mkdirs()) {
                logI(TAG, "success to mkdirs: " + parent);
            } else {
                logE(TAG, "failed to mkdirs: " + parent);
                return false;
            }
        }
        if (file.exists()) {
            return true;
        } else {
            try {
                if (file.createNewFile()) {
                    return true;
                }
            } catch (IOException e) {
                logE(TAG, e);
            }
        }
        return false;
    }

    public static void write(String path, String str) {
        if (str == null) {
            logE(TAG, "str is null?? are you sure? path: " + path);
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(path, false))) {
            writer.write(str);
        } catch (IOException e) {
            logE(TAG, e);
        }
    }

    public static String read(String path) {
        try (BufferedReader reader = new BufferedReader(
                new FileReader(path))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (IOException e) {
            logE(TAG, e);
            return "";
        }
    }

    public static boolean empty(String path) {
        String result = read(path);
        return result.isEmpty() || result.equals("[]");
    }
}
