package com.sevtinge.hyperceiler.common.model.adapter;

import static com.sevtinge.hyperceiler.sub.SubPickerActivity.APP_OPEN_MODE;
import static com.sevtinge.hyperceiler.sub.SubPickerActivity.INPUT_MODE;
import static com.sevtinge.hyperceiler.sub.SubPickerActivity.LAUNCHER_MODE;
import static com.sevtinge.hyperceiler.sub.SubPickerActivity.PROCESS_TEXT_MODE;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.common.callback.IEditCallback;
import com.sevtinge.hyperceiler.common.model.data.AppData;
import com.sevtinge.hyperceiler.common.model.adapter.AppDataAdapter.AppViewHolder;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fan.recyclerview.card.CardGroupAdapter;

public class AppDataAdapter extends CardGroupAdapter<AppViewHolder>
    implements IEditCallback {

    private final String TAG = "AppDataAdapter";

    private final String mKey;
    private final int mMode;

    private Set<String> selectedApps;
    private List<AppData> mAppDataList;
    private List<AppData> mAppInfoListNew;
    public ArrayList<AppArrayList> appLists = new ArrayList<>();

    private OnItemClickListener mOnItemClickListener;

    public AppDataAdapter(List<AppData> appInfoList, String key, int mode) {
        mKey = key;
        mMode = mode;
        mAppDataList = new ArrayList<>(appInfoList);
        mAppInfoListNew = new ArrayList<>(appInfoList);
        getShared();
    }

    @Override
    public void setHasStableIds() {

    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AppViewHolder(LayoutInflater.from(parent.getContext()).inflate(
            R.layout.item_app_list, parent, false));
    }

    @Override
    public int getItemViewGroup(int position) {
        return 0;
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppData appInfo = mAppInfoListNew.get(position);
        holder.bind(appInfo, position);
    }

    @Override
    public int getItemCount() {
        return mAppInfoListNew.size();
    }

    /**
     * Update list data for search function.
     *
     * @param data Filtered application list data.
     */
    public void updateData(List<AppData> data) {
        mAppInfoListNew.clear();
        mAppInfoListNew.addAll(data);
        notifyDataSetChanged();
    }

    /**
     * Reset the list data to the original data to clear the search results.
     */
    public void resetData() {
        updateData(mAppDataList);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(View itemView, AppData appData, int position);
    }

    public class AppViewHolder extends RecyclerView.ViewHolder {

        private ImageView appIcon;
        private TextView appName;
        private TextView appEdit;
        private CheckBox mSelecte;
        private View mItemView;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            mItemView = itemView;
            appIcon = itemView.findViewById(android.R.id.icon);
            appName = itemView.findViewById(android.R.id.title);
            appEdit = itemView.findViewById(android.R.id.summary);
            mSelecte = itemView.findViewById(android.R.id.checkbox);
        }

        public void bind(AppData appInfo, int position) {
            if (mMode == INPUT_MODE) {
                String edit = getEdit(appInfo.packageName);
                if (!edit.isEmpty()) {
                    appName.setText(edit);
                } else {
                    appName.setText(appInfo.label);
                }
            } else {
                appName.setText(appInfo.label);
            }

            appIcon.setImageDrawable(appInfo.icon);

            // appEdit.setText(appInfo.packageName);
            mSelecte.setChecked(shouldSelect(appInfo.packageName));
            mSelecte.setVisibility(mMode == LAUNCHER_MODE ||
                mMode == APP_OPEN_MODE ||
                mMode == PROCESS_TEXT_MODE ?
                View.VISIBLE : View.GONE);

            // Log.e(TAG, "getView: " + appInfo.label, null);
            if (mMode == INPUT_MODE) {
                addApp(appInfo, appName);
            }

            mItemView.setOnClickListener(v -> {
                if (position != RecyclerView.NO_POSITION) {
                    mOnItemClickListener.onItemClick(v, mAppInfoListNew.get(position), position);
                }
            });
        }
    }

    // 以下方法保持不变，从原来的 AppDataAdapter 复制过来
    public String getEdit(String packageName) {
        String string1 = "";
        String string2 = "";
        Pattern pattern = Pattern.compile(".*฿(.*)฿.*");
        for (String edit : selectedApps) {
            if (edit.contains(packageName + "฿")) {
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
        return string2;
    }

    public void addApp(AppData appData, TextView appName) {
        boolean appExists = false;
        if (appLists.isEmpty()) {
            appLists.add(new AppArrayList(appData.packageName, null, appName));
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
            getShared();
            String randomString = generateRandomString(5);
            selectedApps.add(packageName + "฿" + edit + "฿" + randomString);
            putShared();
        }
    }

    public void getShared() {
        selectedApps = new LinkedHashSet<>(PrefsUtils.mSharedPreferences.getStringSet(mKey, new LinkedHashSet<>()));
    }

    public void putShared() {
        PrefsUtils.mSharedPreferences.edit().putStringSet(mKey, selectedApps).apply();
    }

    public void deleteAll() {
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
                }
            }
        }
        if (!deleteAll.isEmpty()) {
            deleteAll.forEach(selectedApps::remove);
            putShared();
        }
    }

    private String generateRandomString(int length) {
        String possibleCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder randomStringBuilder = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(possibleCharacters.length());
            char randomChar = possibleCharacters.charAt(randomIndex);
            randomStringBuilder.append(randomChar);
        }
        return randomStringBuilder.toString();
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
