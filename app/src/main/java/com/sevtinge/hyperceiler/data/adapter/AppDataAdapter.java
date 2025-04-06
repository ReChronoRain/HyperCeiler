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
package com.sevtinge.hyperceiler.data.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.callback.IEditCallback;
import com.sevtinge.hyperceiler.ui.common.model.data.AppData;
import com.sevtinge.hyperceiler.ui.base.sub.AppPicker;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppDataAdapter extends ArrayAdapter<AppData> implements IEditCallback {
    private final String TAG = "AppDataAdapter";
    public ArrayList<AppArrayList> appLists = new ArrayList<>();
    private static List<AppData> appInfoList;
    private Set<String> selectedApps;
    private ImageView appIcon;
    private TextView appName;
    private TextView appEdit;
    private CheckBox mSelecte;
    private final int resourceId;
    private final String mKey;
    private final int mMode;
    private List<AppData> originalAppDataList;

    public AppDataAdapter(@NonNull Context context, int resource, List<AppData> appInfoList, String key, int mode) {
        super(context, resource, appInfoList);
        AppPicker.setEditCallback(this);
        this.resourceId = resource;
        mKey = key;
        mMode = mode;
        this.originalAppDataList = new ArrayList<>(appInfoList);
    }

    /**
     * Update list data for search function.
     *
     * @param data Filtered application list data.
     */
    public void updateData(List<AppData> data) {
        clear();
        addAll(data);
        notifyDataSetChanged();
    }

    /**
     * Reset the list data to the original data to clear the search results.
     */
    public void resetData() {
        updateData(originalAppDataList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        AppData appInfo = getItem(position);
        getShared();
        View view = setNewView(convertView, parent, appInfo);
        assert appInfo != null;
        if (mMode == AppPicker.INPUT_MODE) {
            String edit = getEdit(appInfo.packageName);
            if (!edit.isEmpty()) {
                appName.setText(edit);
            } else {
                appName.setText(appInfo.label);
            }
        } else
            appName.setText(appInfo.label);
        appIcon.setImageBitmap(appInfo.icon);

        // appEdit.setText(appInfo.packageName);
        mSelecte.setChecked(shouldSelect(appInfo.packageName));
        mSelecte.setVisibility(mMode == AppPicker.LAUNCHER_MODE ||
                mMode == AppPicker.APP_OPEN_MODE ||
                mMode == AppPicker.PROCESS_TEXT_MODE ?
                View.VISIBLE : View.GONE);
        // Log.e(TAG, "getView: " + appInfo.label, null);
        return view;
    }

    public String getEdit(String packageName) {
        String string1 = "";
        String string2 = "";
        Pattern pattern = Pattern.compile(".*฿(.*)฿.*");
        // ArrayList<String> have = new ArrayList<>();
        for (String edit : selectedApps) {
            if (edit.contains(packageName + "฿")) {
                // have.add(edit);
                Matcher matcher = pattern.matcher(edit);
                if (matcher.find()) {
                    string2 = matcher.group(1);
                }
            }
        }
        if (string2 != null && !string2.isEmpty()) {
            for (int i = 0; i < appLists.size(); i++) {
                AppArrayList arrayList = appLists.get(i);
                if (arrayList.mPackageName.equals(packageName)) {
                    arrayList.mEdit = string2;
                }
            }
        }
        /*if (have.size() >= 2) {
            for (int i = 0; i < have.size() - 1; i++) {
                selectedApps.remove(have.get(i));
            }
            putShared();
        }*/
        return string2;
    }

    public View setNewView(@Nullable View view, @NonNull ViewGroup parent, AppData appInfo) {
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
        }
        appIcon = view.findViewById(android.R.id.icon);
        appName = view.findViewById(android.R.id.title);
        appEdit = view.findViewById(android.R.id.summary);
        mSelecte = view.findViewById(android.R.id.checkbox);
        if (mMode == 3) {
            addApp(appInfo, appName);
        }
        return view;
    }

    public void addApp(AppData appData, TextView appName) {
        boolean appExists = false;
        if (appLists.isEmpty()) {
            appLists.add(new AppArrayList(appData.packageName, null, appName));
            // Log.e(TAG, "addApp1: " + appData.label, null);
        } else {
            for (int i = 0; i < appLists.size(); i++) {
                AppArrayList appList = appLists.get(i);
                if (appList.mPackageName.equals(appData.packageName)) {
                    if (!appList.mAppName.equals(appName)) {
                        appList.mAppName = appName;
                    }
                    appExists = true;
                    break;
                }
            }
            if (!appExists) {
                appLists.add(new AppArrayList(appData.packageName, null, appName));
                // Log.e(TAG, "addApp: " + appData.label, null);
            }
        }
    }

    public boolean shouldSelect(String pkgName) {
        return (selectedApps.contains(pkgName));
    }

    @Override
    public void editCallback(String label, String packageName, String edit) {
        boolean appExists = false;
        String mLastEdit = null;
        String isOriginal = null;
        for (int i = 0; i < appLists.size(); i++) {
            AppArrayList arrayList = appLists.get(i);
            if (arrayList.mPackageName.equals(packageName)) {
                if (arrayList.mEdit != null) {
                    mLastEdit = arrayList.mEdit;
                }
                if (edit.equals(label)) {
                    isOriginal = packageName;
                    arrayList.mEdit = null;
                } else {
                    arrayList.mEdit = edit;
                }
                arrayList.mAppName.setText(edit);
                appExists = true;
                break;
            }
        }
        if (appExists) {
            if (mLastEdit != null) {
                deleteEdit(packageName, mLastEdit);
            }
            if (isOriginal != null) {
                deleteEdit(packageName, isOriginal);
                return;
            }
            // getShared();
            String randomString = generateRandomString(5);
            selectedApps.add(packageName + "฿" + edit + "฿" + randomString);
            putShared();
        }
    }

    public void getShared() {
        selectedApps = new LinkedHashSet<>(PrefsUtils.mSharedPreferences.getStringSet(mKey, new LinkedHashSet<>()));
        // Log.e(TAG, "getShared: " + mKey, null);
    }

    public void putShared() {
        PrefsUtils.mSharedPreferences.edit().putStringSet(mKey, selectedApps).apply();
    }

    public void deleteAll() {
        // selectedApps.remove(delete);
        Collection<String> deleteAll = new ArrayList<>(selectedApps);
        if (!deleteAll.isEmpty()) {
            deleteAll.forEach(selectedApps::remove);
            putShared();
        }
    }

    public void deleteEdit(String packageName, String lastEdit) {
        Collection<String> deleteAll = new ArrayList<>();
        for (String delete : selectedApps) {
            if (delete.contains(packageName)) {
                if (delete.contains(lastEdit)) {
                    deleteAll.add(delete);
                    // selectedApps.remove(delete);
                    // putShared();
                }
            }
        }
        if (!deleteAll.isEmpty()) {
            deleteAll.forEach(selectedApps::remove);
            putShared();
        }
    }

    private String generateRandomString(int length) {
        // 定义包含所有可能字符的字符串
        String possibleCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

        // 使用StringBuilder构建字符串
        StringBuilder randomStringBuilder = new StringBuilder(length);

        // 使用Random类生成随机索引，并将对应的字符添加到StringBuilder中
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(possibleCharacters.length());
            char randomChar = possibleCharacters.charAt(randomIndex);
            randomStringBuilder.append(randomChar);
        }

        // 将StringBuilder转换为String并返回
        return randomStringBuilder.toString();
    }

    public static class AppArrayList {
        public String mPackageName;
        public String mEdit;

        // public String mLastEdit;

        public TextView mAppName;

        public AppArrayList(String packageName, String edit, TextView appName) {
            mPackageName = packageName;
            mEdit = edit;
            // mLastEdit = lastEdit;
            mAppName = appName;
        }
    }
}
