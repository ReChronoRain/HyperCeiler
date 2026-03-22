package com.sevtinge.hyperceiler.model.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.model.adapter.AppDataAdapter.AppViewHolder;
import com.sevtinge.hyperceiler.model.data.AppData;
import com.sevtinge.hyperceiler.model.data.AppEditManager;
import com.sevtinge.hyperceiler.sub.SubPickerActivity;
import com.sevtinge.hyperceiler.utils.AppIconCache;

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
    private final Set<String> mSelectedPackages = new LinkedHashSet<>();
    private boolean mDeferredSelectionMode = false;
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
     * 使用 DiffUtil 进行差量更新，替代 notifyDataSetChanged()
     */
    public void setData(List<AppData> data) {
        List<AppData> newList = (data != null) ? data : new ArrayList<>();
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new AppDiffCallback(mAppDataList, newList));
        mAppDataList.clear();
        mAppDataList.addAll(newList);
        result.dispatchUpdatesTo(this);
    }

    /**
     * 获取当前数据
     */
    public List<AppData> getData() {
        return new ArrayList<>(mAppDataList);
    }

    public void setDeferredSelectionMode(boolean deferredSelectionMode) {
        mDeferredSelectionMode = deferredSelectionMode;
    }

    public void setSelectedPackages(Set<String> selectedPackages) {
        mSelectedPackages.clear();
        if (selectedPackages != null) {
            mSelectedPackages.addAll(selectedPackages);
        }
        refreshSelections();
    }

    public Set<String> getSelectedPackages() {
        return new LinkedHashSet<>(mSelectedPackages);
    }

    /**
     * 刷新选中状态
     */
    public void refreshSelections() {
        Set<String> selectedApps;
        if (mDeferredSelectionMode) {
            selectedApps = new LinkedHashSet<>(mSelectedPackages);
        } else if (mKey != null) {
            selectedApps = new LinkedHashSet<>(PrefsBridge.getStringSet(mKey));
        } else {
            selectedApps = new LinkedHashSet<>();
        }

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
        if (position < 0 || position >= mAppDataList.size()) {
            return;
        }

        AppData appData = mAppDataList.get(position);
        appData.isSelected = !appData.isSelected;

        if (mDeferredSelectionMode) {
            if (appData.isSelected) {
                mSelectedPackages.add(appData.packageName);
            } else {
                mSelectedPackages.remove(appData.packageName);
            }
        } else if (mKey != null) {
            // 更新 SharedPreferences
            Set<String> selectedApps = new LinkedHashSet<>(
                PrefsBridge.getStringSet(mKey));

            if (appData.isSelected) {
                selectedApps.add(appData.packageName);
            } else {
                selectedApps.remove(appData.packageName);
            }

            PrefsBridge.putByApp(mKey, selectedApps);
        }

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

    /**
     * DiffUtil.Callback 实现，基于 packageName 判断是否同一项
     */
    private static class AppDiffCallback extends DiffUtil.Callback {
        private final List<AppData> oldList;
        private final List<AppData> newList;

        AppDiffCallback(List<AppData> oldList, List<AppData> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }

        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            String oldPkg = oldList.get(oldPos).packageName;
            String newPkg = newList.get(newPos).packageName;
            if (oldPkg == null || newPkg == null) return oldPkg == newPkg;
            return oldPkg.equals(newPkg);
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            AppData o = oldList.get(oldPos);
            AppData n = newList.get(newPos);
            return o.isSelected == n.isSelected
                && String.valueOf(o.label).equals(String.valueOf(n.label));
        }
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

        /**
         * 异步加载图标，使用 AppIconCache 缓存
         */
        private void updateAppIcon(AppData appInfo) {
            if (appInfo.icon != null) {
                mAppIcon.setImageDrawable(appInfo.icon);
                mAppIcon.setTag(appInfo.packageName);
                return;
            }

            // 先尝试从缓存获取
            android.graphics.drawable.Drawable cached = AppIconCache.getCached(appInfo.packageName);
            if (cached != null) {
                mAppIcon.setImageDrawable(cached);
                return;
            }

            // 设置占位图，避免闪烁
            mAppIcon.setImageResource(android.R.drawable.sym_def_app_icon);

            // 记录当前 packageName 防止复用错位
            mAppIcon.setTag(appInfo.packageName);

            AppIconCache.loadIconAsync(mItemView.getContext(), appInfo.packageName, icon -> {
                // 检查 ViewHolder 是否已被复用
                if (appInfo.packageName.equals(mAppIcon.getTag())) {
                    if (icon != null) {
                        mAppIcon.setImageDrawable(icon);
                    }
                }
            });
        }

        private void updateCheckboxVisibility(AppData appInfo) {
            boolean shouldShowCheckbox = (mMode == SubPickerActivity.LAUNCHER_MODE ||
                mMode == SubPickerActivity.APP_OPEN_MODE ||
                mMode == SubPickerActivity.PROCESS_TEXT_MODE ||
                mMode == SubPickerActivity.ALL_APPS_MODE ||
                mMode == SubPickerActivity.SCOPE_MODE);

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
                        mMode == SubPickerActivity.PROCESS_TEXT_MODE ||
                        mMode == SubPickerActivity.ALL_APPS_MODE ||
                        mMode == SubPickerActivity.SCOPE_MODE) {
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
