package com.sevtinge.hyperceiler.module.base.dexkit;

import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logD;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logI;

import org.json.JSONArray;
import org.json.JSONException;

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

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class DexKitCacheFile {

    static String TAG = "DexKitCacheFile";

    public static String getFilePath(XC_LoadPackage.LoadPackageParam loadPackageParam, String callingClassName, String tag) {
        return "/data/user/0/" + loadPackageParam.packageName + "/cache/HyperCeiler_" + callingClassName + "_" + tag + "_DexKit_Cache.dat";
    }

    public static void checkFile(XC_LoadPackage.LoadPackageParam loadPackageParam, String callingClassName, String tag) {
        String path = getFilePath(loadPackageParam, callingClassName, tag);
        File file = new File(path);
        File parentDir = file.getParentFile();
        if (parentDir == null) {
            logE(TAG, "parentDir is null: " + path);
        }
        if (parentDir != null && !parentDir.exists()) {
            if (parentDir.mkdirs()) {
                logI(TAG, "mkdirs: " + parentDir);
            } else {
                logE(TAG, "mkdirs: " + parentDir);
            }
        }
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    writeFile(loadPackageParam, new JSONArray(), callingClassName, tag);
                    setPermission(path);
                    logI(TAG, "createNewFile: " + file);
                } else {
                    logE(TAG, "createNewFile: " + file);
                }
            } catch (IOException e) {
                logE(TAG, "createNewFile: " + e);
            }
        } else {
            setPermission(path);
        }
    }

    public static void writeFile(XC_LoadPackage.LoadPackageParam loadPackageParam, JSONArray jsonArray, String callingClassName, String tag) {
        String path = getFilePath(loadPackageParam, callingClassName, tag);
        if (jsonArray == null) {
            logE(TAG, "write json is null");
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new
                FileWriter(path, false))) {
            writer.write(jsonArray.toString());
        } catch (IOException e) {
            logE(TAG, "writeFile: " + e);
        }
    }

    public static boolean isEmptyFile(XC_LoadPackage.LoadPackageParam loadPackageParam, String callingClassName, String tag) {
        if(!isFileExists(loadPackageParam, callingClassName, tag)){
            JSONArray jsonArray = readFile(loadPackageParam, callingClassName, tag);
            return jsonArray.length() == 0;
        }
        return false;
    }

    public static boolean isFileExists(XC_LoadPackage.LoadPackageParam loadPackageParam, String callingClassName, String tag) {
        File file = new File(getFilePath(loadPackageParam, callingClassName, tag));
        return file.exists();
    }

    public static JSONArray readFile(XC_LoadPackage.LoadPackageParam loadPackageParam, String callingClassName, String tag) {
        String path = getFilePath(loadPackageParam, callingClassName, tag);
        try (BufferedReader reader = new BufferedReader(new
                FileReader(path))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            String jsonString = builder.toString();
            if (jsonString.isEmpty()) {
                jsonString = "[]";
            }
            return new JSONArray(jsonString);
        } catch (IOException | JSONException e) {
            logE(TAG, "readFile: " + e);
        }
        return new JSONArray();
    }

    public static boolean resetFile(XC_LoadPackage.LoadPackageParam loadPackageParam, String callingClassName, String tag) {
        // 清空文件内容
        writeFile(loadPackageParam, new JSONArray(), callingClassName, tag);
        return isEmptyFile(loadPackageParam, callingClassName, tag);
    }

    public static void setPermission(String paths) {
        // 指定文件的路径
        Path filePath = Paths.get(paths);

        try {
            // 获取当前文件的权限
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(filePath);

            // 添加世界可读写权限
            permissions.add(PosixFilePermission.OTHERS_READ);
            permissions.add(PosixFilePermission.OTHERS_WRITE);
            permissions.add(PosixFilePermission.GROUP_READ);
            permissions.add(PosixFilePermission.GROUP_WRITE);

            // 设置新的权限
            Files.setPosixFilePermissions(filePath, permissions);
        } catch (IOException e) {
            logE(TAG, "setPermission: " + e);
        }
    }

}
