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

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static boolean mkdirs(String path) {
        File file = new File(path);
        return mkdirs(file);
    }

    public static boolean mkdirs(File file) {
        if (file.exists()) {
            return true;
        }
        return file.mkdirs();
    }

    public static boolean touch(String path) {
        File file = new File(path);
        File parentDir = file.getParentFile();
        if (parentDir == null || !mkdirs(parentDir)) {
            return false;
        }
        if (!file.exists()) {
            try {
                return file.createNewFile();
            } catch (IOException e) {
                XposedLogUtils.logE(ITAG.TAG, "touch: " + e);
            }
        }
        return true;
    }

    public static boolean exists(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static boolean exists(File file) {
        return file.exists();
    }

    public static boolean exists(String file, String filter, Pattern pattern) {
        if (exists(file)) {
            File f = new File(file);
            File[] files = f.listFiles();
            if (files != null) {
                for (File file1 : files) {
                    if (file1.getName().contains(filter)) {
                        Matcher matcher = pattern.matcher(file1.getName());
                        if (matcher.find()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static void setPermission(String paths) {
        Path filePath = Paths.get(paths);

        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(filePath);

            permissions.add(PosixFilePermission.OTHERS_READ);
            permissions.add(PosixFilePermission.OTHERS_WRITE);
            permissions.add(PosixFilePermission.GROUP_READ);
            permissions.add(PosixFilePermission.GROUP_WRITE);

            Files.setPosixFilePermissions(filePath, permissions);
        } catch (IOException e) {
            AndroidLogUtils.logE(TAG, "set permission: " + e);
        }
    }

    // 写入文件
    public static boolean write(String path, String content) {
        return write(path, content, false);
    }

    public static boolean write(String path, String content, boolean mode) {
        File file = new File(path);
        if (!touch(path)) {
            return false;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, mode))) {
            writer.write(content);
            return true;
        } catch (IOException e) {
            XposedLogUtils.logE(ITAG.TAG, "writeToFile: " + e);
            return false;
        }
    }

    // 读取文件
    @Nullable
    public static String read(String path) {
        File file = new File(path);
        if (!exists(file)) {
            return null;
        }
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        } catch (IOException e) {
            XposedLogUtils.logE(ITAG.TAG, "readFromFile: " + e);
            return null;
        }
        return content.toString();
    }
}
