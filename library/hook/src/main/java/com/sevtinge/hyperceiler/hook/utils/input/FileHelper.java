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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.utils.input;

import static com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logI;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

/**
 * 文件读写系统
 *
 * @author 焕晨HChen
 */
public class FileHelper {
    public static String TAG = "FileHelper";

    public static boolean exists(@NonNull String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent == null) {
            logE(TAG, "Parent must not be null!! path: " + path);
            return false;
        }

        if (!parent.exists()) {
            if (parent.mkdirs()) {
                logI(TAG, "Success to mkdirs: " + parent);
            } else {
                logE(TAG, "Failed to mkdirs: " + parent);
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

    public static void write(@NonNull String path, @NonNull String content) {
        if (Objects.isNull(content)) content = "";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, false))) {
            writer.write(content);
        } catch (IOException e) {
            logE(TAG, e);
        }
    }

    @NonNull
    public static String read(@NonNull String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
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

    public static boolean isEmpty(@NonNull String path) {
        String data = read(path);
        return data.isEmpty() || Objects.equals(data, "[]");
    }
}
