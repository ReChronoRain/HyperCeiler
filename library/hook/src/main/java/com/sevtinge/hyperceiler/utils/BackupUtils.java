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
package com.sevtinge.hyperceiler.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BackupUtils {
    public static final int CREATE_DOCUMENT_CODE = 255774;
    public static final int OPEN_DOCUMENT_CODE = 277451;
    public static final String BACKUP_FILE_NAME = "HyperCeiler_settings_backup";

    public static void backup(Activity activity) {
        @SuppressLint("SimpleDateFormat") String backupFileName = BACKUP_FILE_NAME + new SimpleDateFormat("_yyyy-MM-dd-HH:mm:ss").format(new java.util.Date());
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, backupFileName);
        activity.startActivityForResult(intent, CREATE_DOCUMENT_CODE);
    }

    public static void restore(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        activity.startActivityForResult(intent, OPEN_DOCUMENT_CODE);
    }

    public static void handleCreateDocument(Activity activity, @Nullable Uri data) throws IOException, JSONException {
        if (data == null) return;
        OutputStream outputStream = activity.getContentResolver().openOutputStream(data);
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, ?> entry : PrefsUtils.mSharedPreferences.getAll().entrySet()) {
            jsonObject.put(entry.getKey(), entry.getValue());
        }
        bufferedWriter.write(jsonObject.toString());
        bufferedWriter.close();
    }

    public static void handleReadDocument(Activity activity, @Nullable Uri data) throws IOException, JSONException {
        if (data == null) return;
        SharedPreferences.Editor edit = PrefsUtils.mSharedPreferences.edit();
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
            Object value = jsonObject.get(key);
            // https://stackoverflow.com/a/78608931
            //noinspection IfCanBeSwitch
            if (value instanceof String) {
                if (((String) value).contains("[") && ((String) value).contains("]")) {
                    value = ((String) value).replace("[", "").replace("]", "").replace(" ", "");
                    String[] array = ((String) value).split(",");
                    List<String> list = Arrays.asList(array);
                    Set<String> stringSet = new HashSet<>(list);
                    edit.putStringSet(key, stringSet);
                } else {
                    edit.putString(key, (String) value);
                }
            } else if (value instanceof Boolean) {
                edit.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                edit.putInt(key, (Integer) value);
            }
        }
        bufferedReader.close();
        edit.apply();
    }
}
