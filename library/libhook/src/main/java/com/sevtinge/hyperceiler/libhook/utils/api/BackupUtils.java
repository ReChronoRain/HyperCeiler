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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.api;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BackupUtils {
    public static final int CREATE_DOCUMENT_CODE = 255774;
    public static final int OPEN_DOCUMENT_CODE = 277451;
    public static final String BACKUP_FILE_NAME = "HyperCeiler_settings_backup";

    // 获取备份用的 Intent
    public static Intent getCreateDocumentIntent() {
        @SuppressLint("SimpleDateFormat")
        String backupFileName = BACKUP_FILE_NAME + new SimpleDateFormat("_yyyy-MM-dd-HH:mm:ss").format(new Date());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, backupFileName);
        return intent;
    }

    // 获取恢复用的 Intent
    public static Intent getOpenDocumentIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        return intent;
    }


    /**
     * 执行备份：强制从物理句柄读取，确保文件不为空
     */
    public static void handleCreateDocument(Context context, @Nullable Uri data) throws IOException, JSONException {
        if (data == null) return;

        // 关键：强制获取物理文件中的所有配置
        Map<String, ?> allEntries = PrefsBridge.getAll();
        AndroidLog.d("Backup", "开始备份，读取到项数: " + allEntries.size());

        try (OutputStream os = context.getContentResolver().openOutputStream(data);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {

            JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                if ("prefs_key_allow_hook".equals(key)) continue;

                Object value = entry.getValue();
                // 针对 StringSet 做特殊 JSON 处理，存为 JSONArray
                if (value instanceof Set) {
                    jsonObject.put(key, new JSONArray((Set<?>) value));
                } else {
                    jsonObject.put(key, value);
                }
            }

            writer.write(jsonObject.toString(4)); // 使用缩进增加可读性
            writer.flush();
        }
    }

    /**
     * 执行恢复：双写到物理和远程
     */
    public static void handleReadDocument(Context context, @Nullable Uri data) throws IOException, JSONException {
        if (data == null) return;

        StringBuilder sb = new StringBuilder();
        try (InputStream is = context.getContentResolver().openInputStream(data);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        JSONObject jsonObject = new JSONObject(sb.toString());
        Iterator<String> keys = jsonObject.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            if ("prefs_key_allow_hook".equals(key)) continue;

            Object value = jsonObject.get(key);

            // 自动识别并还原类型
            if (value instanceof JSONArray) {
                Set<String> set = new HashSet<>();
                JSONArray array = (JSONArray) value;
                for (int i = 0; i < array.length(); i++) set.add(array.getString(i));
                PrefsBridge.putStringSet(key, set);
            } else if (value instanceof Boolean) {
                PrefsBridge.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                PrefsBridge.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                PrefsBridge.putLong(key, (Long) value);
            } else if (value instanceof String) {
                PrefsBridge.putString(key, (String) value);
            }
        }
    }

    public static void handleCreateDocument(Activity activity, @Nullable Uri data) throws IOException, JSONException {
        if (data == null) return;
        OutputStream outputStream = activity.getContentResolver().openOutputStream(data);
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, ?> entry : PrefsBridge.getAll().entrySet()) {
            if ("prefs_key_allow_hook".equals(entry.getKey())) {
                continue;
            }
            jsonObject.put(entry.getKey(), entry.getValue());
        }
        bufferedWriter.write(jsonObject.toString());
        bufferedWriter.close();
    }

    public static void handleReadDocument(Activity activity, @Nullable Uri data) throws IOException, JSONException {
        if (data == null) return;
        InputStream inputStream = activity.getContentResolver().openInputStream(data);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line = bufferedReader.readLine();
        while (line != null) {
            stringBuilder.append(line);
            line = bufferedReader.readLine();
        }
        String read = stringBuilder.toString();
        JSONObject jsonObject = new JSONObject(read);
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if ("prefs_key_allow_hook".equals(key)) {
                continue;
            }
            Object value = jsonObject.get(key);
            // https://stackoverflow.com/a/78608931
            //noinspection IfCanBeSwitch
            if (value instanceof String) {
                if (((String) value).contains("[") && ((String) value).contains("]")) {
                    value = ((String) value).replace("[", "").replace("]", "").replace(" ", "");
                    String[] array = ((String) value).split(",");
                    List<String> list = Arrays.asList(array);
                    Set<String> stringSet = new HashSet<>(list);
                    PrefsBridge.putStringSet(key, stringSet);
                } else {
                    PrefsBridge.putString(key, (String) value);
                }
            } else if (value instanceof Boolean) {
                PrefsBridge.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                PrefsBridge.putInt(key, (Integer) value);
            }
        }
        bufferedReader.close();
    }
}
