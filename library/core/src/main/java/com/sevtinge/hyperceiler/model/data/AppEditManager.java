package com.sevtinge.hyperceiler.model.data;

import com.sevtinge.hyperceiler.callback.IEditCallback;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;

import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public class AppEditManager implements IEditCallback {

    private static final String TAG = "AppEditManager";
    private static final String RANDOM_CHARS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String SEPARATOR = "฿";

    private final String mKey;
    private final Set<String> mSelectedApps = new LinkedHashSet<>();
    private final Random mRandom = new Random();

    public AppEditManager(String key) {
        mKey = key;
        loadFromSharedPreferences();
    }

    public String getEdit(String packageName) {
        if (packageName == null) {
            return "";
        }

        for (String record : mSelectedApps) {
            if (packageName.equals(getPackageName(record))) {
                return getEditText(record);
            }
        }
        return "";
    }

    @Override
    public void editCallback(String label, String packageName, String newEdit) {
        if (packageName == null || newEdit == null) {
            return;
        }

        removeEditFromStorage(packageName);
        if (!newEdit.equals(label)) {
            String randomSuffix = generateRandomString(5);
            mSelectedApps.add(packageName + SEPARATOR + newEdit + SEPARATOR + randomSuffix);
        }
        saveToSharedPreferences();
    }

    private void removeEditFromStorage(String packageName) {
        mSelectedApps.removeIf(record -> packageName.equals(getPackageName(record)));
    }

    private static String getPackageName(String record) {
        if (record == null) {
            return null;
        }
        int separatorIndex = record.indexOf(SEPARATOR);
        return separatorIndex > 0 ? record.substring(0, separatorIndex) : null;
    }

    private static String getEditText(String record) {
        int firstSeparator = record.indexOf(SEPARATOR);
        int lastSeparator = record.lastIndexOf(SEPARATOR);
        if (firstSeparator < 0 || lastSeparator <= firstSeparator) {
            return "";
        }
        return record.substring(firstSeparator + SEPARATOR.length(), lastSeparator);
    }

    private void loadFromSharedPreferences() {
        try {
            mSelectedApps.addAll(PrefsBridge.getStringSet(mKey));
        } catch (Exception e) {
            AndroidLog.e(TAG, "loadFromSharedPreferences failed", e);
        }
    }

    private void saveToSharedPreferences() {
        try {
            PrefsBridge.putByApp(mKey, new LinkedHashSet<>(mSelectedApps));
        } catch (Exception e) {
            AndroidLog.e(TAG, "saveToSharedPreferences failed", e);
        }
    }

    private String generateRandomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(RANDOM_CHARS.charAt(mRandom.nextInt(RANDOM_CHARS.length())));
        }
        return builder.toString();
    }
}
