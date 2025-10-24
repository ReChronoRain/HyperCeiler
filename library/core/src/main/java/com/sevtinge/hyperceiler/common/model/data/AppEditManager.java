package com.sevtinge.hyperceiler.common.model.data;

import android.util.Log;
import android.widget.TextView;

import com.sevtinge.hyperceiler.common.callback.IEditCallback;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppEditManager implements IEditCallback {
    private static final String RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String SEPARATOR = "฿";
    private static final Pattern EDIT_PATTERN = Pattern.compile(".*" + SEPARATOR + "(.*)" + SEPARATOR + ".*");

    private final String mKey;
    private Set<String> mSelectedApps;
    private final List<AppArrayList> mAppLists = new ArrayList<>();
    private final Random mRandom = new Random();

    public AppEditManager(String key) {
        mKey = key;
        loadFromSharedPreferences();
    }

    public String getEdit(String packageName) {
        if (packageName == null) return "";

        for (String edit : mSelectedApps) {
            if (edit != null && edit.contains(packageName + SEPARATOR)) {
                Matcher matcher = EDIT_PATTERN.matcher(edit);
                if (matcher.find()) {
                    String editText = matcher.group(1);
                    updateAppListEdit(packageName, editText);
                    return editText;
                }
            }
        }
        return "";
    }

    public void addOrUpdateApp(AppData appData, TextView appNameView) {
        if (appData == null || appNameView == null) return;

        for (AppArrayList appList : mAppLists) {
            if (appList.mPackageName.equals(appData.packageName)) {
                if (appList.mAppName != appNameView) {
                    appList.mAppName = appNameView;
                }
                return;
            }
        }
        mAppLists.add(new AppArrayList(appData.packageName, null, appNameView));
    }

    @Override
    public void editCallback(String label, String packageName, String newEdit) {
        if (packageName == null || newEdit == null) return;

        AppArrayList targetApp = findAppByPackageName(packageName);
        if (targetApp != null) {
            String lastEdit = targetApp.mEdit;

            if (newEdit.equals(label)) {
                targetApp.mEdit = null;
                if (targetApp.mAppName != null) {
                    targetApp.mAppName.setText(label);
                }
                removeEditFromStorage(packageName, packageName);
            } else {
                targetApp.mEdit = newEdit;
                if (targetApp.mAppName != null) {
                    targetApp.mAppName.setText(newEdit);
                }
                updateEditInStorage(packageName, lastEdit, newEdit);
            }
        }
    }

    private AppArrayList findAppByPackageName(String packageName) {
        for (AppArrayList app : mAppLists) {
            if (app.mPackageName.equals(packageName)) {
                return app;
            }
        }
        return null;
    }

    private void updateAppListEdit(String packageName, String editText) {
        for (AppArrayList app : mAppLists) {
            if (app.mPackageName.equals(packageName)) {
                app.mEdit = editText;
                break;
            }
        }
    }

    private void updateEditInStorage(String packageName, String lastEdit, String newEdit) {
        if (lastEdit != null) {
            removeEditFromStorage(packageName, lastEdit);
        }

        String randomSuffix = generateRandomString(5);
        String newRecord = packageName + SEPARATOR + newEdit + SEPARATOR + randomSuffix;
        mSelectedApps.add(newRecord);
        saveToSharedPreferences();
    }

    private void removeEditFromStorage(String packageName, String editToRemove) {
        Set<String> toRemove = new HashSet<>();
        for (String item : mSelectedApps) {
            if (item != null && item.contains(packageName) && item.contains(editToRemove)) {
                toRemove.add(item);
            }
        }
        mSelectedApps.removeAll(toRemove);
        saveToSharedPreferences();
    }

    private void loadFromSharedPreferences() {
        try {
            mSelectedApps = new LinkedHashSet<>(
                PrefsUtils.mSharedPreferences.getStringSet(mKey, new LinkedHashSet<>()));
        } catch (Exception e) {
            Log.e("AppEditManager", "Error loading from shared preferences", e);
            mSelectedApps = new LinkedHashSet<>();
        }
    }

    private void saveToSharedPreferences() {
        try {
            PrefsUtils.mSharedPreferences.edit()
                .putStringSet(mKey, new LinkedHashSet<>(mSelectedApps))
                .apply();
        } catch (Exception e) {
            Log.e("AppEditManager", "Error saving to shared preferences", e);
        }
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_CHARS.charAt(mRandom.nextInt(RANDOM_CHARS.length())));
        }
        return sb.toString();
    }

    public static class AppArrayList {
        public String mPackageName;
        public String mEdit;
        public TextView mAppName;

        public AppArrayList(String packageName, String edit, TextView appName) {
            mPackageName = packageName;
            mEdit = edit;
            mAppName = appName;
        }
    }
}
