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
