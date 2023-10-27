package com.sevtinge.hyperceiler.utils;

import com.sevtinge.hyperceiler.expansionpacks.utils.KS2Utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.Nullable;

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
import java.util.Iterator;
import java.util.Map;

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
        bufferedWriter.write(KS2Utils.encrypted(jsonObject.toString(), "\u0040\u0037\u0037\u0034\u0039\u0031"));
        bufferedWriter.close();
    }

    public static void handleReadDocument(Activity activity, @Nullable Uri data) throws IOException, JSONException {
        if (data == null) return;
        SharedPreferences.Editor edit = PrefsUtils.mSharedPreferences.edit();
        InputStream inputStream = activity.getContentResolver().openInputStream(data);
        String documentContent = inputStream2String(inputStream);
        String decryptedContent = KS2Utils.decrypted(documentContent, "\u0040\u0037\u0037\u0034\u0039\u0031");
        JSONObject jsonObject = new JSONObject(decryptedContent);
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);
            if (value instanceof String) {
                edit.putString(key, (String) value);
            } else if (value instanceof Boolean) {
                edit.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                edit.putInt(key, (Integer) value);
            }
        }
        edit.apply();
    }

    private static String inputStream2String(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        bufferedReader.close();
        return stringBuilder.toString();
    }
}
