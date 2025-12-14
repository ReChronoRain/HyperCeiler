package com.sevtinge.hyperceiler.common.model.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.common.model.adapter.AppDataAdapter.AppViewHolder;
import com.sevtinge.hyperceiler.common.model.data.AppData;
import com.sevtinge.hyperceiler.common.model.data.AppEditManager;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.sub.SubPickerActivity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import fan.recyclerview.card.CardGroupAdapter;

public class AppDataAdapter extends CardGroupAdapter<AppViewHolder> {

    private final String TAG = "AppDataAdapter";

    private final String mKey;
    private final int mMode;
    private final AppEditManager mEditManager;

    private final List<AppData> mAppDataList; // 单一数据源
    private OnItemClickListener mOnItemClickListener;

    public AppDataAdapter(List<AppData> appInfoList, String key, int mode) {
        mKey = key;
        mMode = mode;
        mEditManager = (key != null) ? new AppEditManager(key) : null;
        mAppDataList = new ArrayList<>(appInfoList);
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AppViewHolder(LayoutInflater.from(parent.getContext()).inflate(
            R.layout.item_app_list, parent, false));
    }

    @Override
    public void setHasStableIds() {

    }

    @Override
    public int getItemViewGroup(int position) {
        return 0;
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        if (position < 0 || position >= mAppDataList.size()) return;

        AppData appInfo = mAppDataList.get(position);
        holder.bind(appInfo, position);
    }

    @Override
    public int getItemCount() {
        return mAppDataList.size();
    }

    /**
     * 设置数据并刷新
     */
    public void setData(List<AppData> data) {
        if (data == null) {
            mAppDataList.clear();
        } else {
            mAppDataList.clear();
            mAppDataList.addAll(data);
        }
        notifyDataSetChanged();
    }

    /**
     * 获取当前数据
     */
    public List<AppData> getData() {
        return new ArrayList<>(mAppDataList);
    }

    /**
     * 刷新选中状态
     */
    public void refreshSelections() {
        if (mKey == null) return;

        Set<String> selectedApps = new LinkedHashSet<>(
            PrefsUtils.mSharedPreferences.getStringSet(mKey, new LinkedHashSet<>()));

        // 更新所有项目的选中状态
        for (AppData appData : mAppDataList) {
            appData.isSelected = selectedApps.contains(appData.packageName);
        }

        notifyDataSetChanged();
    }

    /**
     * 切换指定位置的选中状态
     */
    public void toggleSelection(int position) {
        if (position < 0 || position >= mAppDataList.size() || mKey == null) {
            return;
        }

        AppData appData = mAppDataList.get(position);
        appData.isSelected = !appData.isSelected;

        // 更新 SharedPreferences
        Set<String> selectedApps = new LinkedHashSet<>(
            PrefsUtils.mSharedPreferences.getStringSet(mKey, new LinkedHashSet<>()));

        if (appData.isSelected) {
            selectedApps.add(appData.packageName);
        } else {
            selectedApps.remove(appData.packageName);
        }

        PrefsUtils.mSharedPreferences.edit().putStringSet(mKey, selectedApps).apply();

        notifyItemChanged(position);
    }

    public void editCallback(String label, String packageName, String edit) {
        if (mEditManager != null) {
            mEditManager.editCallback(label, packageName, edit);
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(View itemView, AppData appData, int position);
    }

    public class AppViewHolder extends RecyclerView.ViewHolder {

        private final ImageView mAppIcon;
        private final TextView mAppName;
        private final TextView mAppEdit;
        private final CheckBox mSelectCheckbox;
        private final View mItemView;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            mItemView = itemView;
            mAppIcon = itemView.findViewById(android.R.id.icon);
            mAppName = itemView.findViewById(android.R.id.title);
            mAppEdit = itemView.findViewById(android.R.id.summary);
            mSelectCheckbox = itemView.findViewById(android.R.id.checkbox);
        }

        public void bind(AppData appInfo, int position) {
            if (appInfo == null) return;

            updateAppName(appInfo);
            updateAppIcon(appInfo);
            updateCheckboxVisibility(appInfo);
            setupEditMode(appInfo);

            setupClickListeners();
        }

        private void updateAppName(AppData appInfo) {
            if (mMode == SubPickerActivity.INPUT_MODE && mEditManager != null) {
                String editText = mEditManager.getEdit(appInfo.packageName);
                mAppName.setText(!editText.isEmpty() ? editText : appInfo.label);
            } else {
                mAppName.setText(appInfo.label);
            }
        }

        private void updateAppIcon(AppData appInfo) {
            if (appInfo.icon != null) {
                mAppIcon.setImageDrawable(appInfo.icon);
            } else {
                mAppIcon.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        }

        private void updateCheckboxVisibility(AppData appInfo) {
            boolean shouldShowCheckbox = (mMode == SubPickerActivity.LAUNCHER_MODE ||
                mMode == SubPickerActivity.APP_OPEN_MODE ||
                mMode == SubPickerActivity.PROCESS_TEXT_MODE || mMode == SubPickerActivity.ALL_APPS_MODE);

            mSelectCheckbox.setVisibility(shouldShowCheckbox ? View.VISIBLE : View.GONE);

            if (shouldShowCheckbox) {
                mSelectCheckbox.setChecked(appInfo.isSelected);
            }
        }

        private void setupEditMode(AppData appInfo) {
            if (mMode == SubPickerActivity.INPUT_MODE && mEditManager != null) {
                mEditManager.addOrUpdateApp(appInfo, mAppName);
            }
        }

        private void setupClickListeners() {
            // Item 点击监听
            mItemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < mAppDataList.size()) {
                    AppData appData = mAppDataList.get(position);

                    if (mMode == SubPickerActivity.LAUNCHER_MODE ||
                        mMode == SubPickerActivity.APP_OPEN_MODE ||
                        mMode == SubPickerActivity.PROCESS_TEXT_MODE) {
                        // 对于选择模式，点击item也切换选中状态
                        toggleSelection(position);
                    } else {
                        // 其他模式调用原来的点击逻辑
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onItemClick(v, appData, position);
                        }
                    }
                }
            });

            // Checkbox 点击监听 - 防止事件冒泡
            mSelectCheckbox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    toggleSelection(position);
                }
            });
        }
    }
}
