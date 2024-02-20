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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.utils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {
    public static boolean findFile(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static boolean findFile(File file) {
        return file.exists();
    }

    public static boolean findFile(File[] files) {
        for (File file : files) {
            return file.exists();
        }
        return false;
    }

    public static boolean findFile(String file, String filter, Pattern pattern) {
        if (findFile(file)) {
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
}
