package com.sevtinge.hyperceiler.model.adapter;

import android.graphics.drawable.Drawable;
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
import java.util.Objects;
import java.util.Set;

import fan.recyclerview.card.CardGroupAdapter;

public class AppDataAdapter extends CardGroupAdapter<AppViewHolder> {

    private static final int DEFAULT_ICON_SIZE_DP = 40;

    private final String mKey;
    private final int mMode;
    private final AppEditManager mEditManager;
    private final List<AppData> mAppDataList;
    private final Set<String> mSelectedPackages = new LinkedHashSet<>();

    private boolean mDeferredSelectionMode;
    private OnItemClickListener mOnItemClickListener;

    public AppDataAdapter(List<AppData> appInfoList, String key, int mode) {
        mKey = key;
        mMode = mode;
        mEditManager = key != null ? new AppEditManager(key) : null;
        mAppDataList = appInfoList != null ? new ArrayList<>(appInfoList) : new ArrayList<>();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_app_list, parent, false);
        return new AppViewHolder(itemView);
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
        if (position >= 0 && position < mAppDataList.size()) {
            holder.bind(mAppDataList.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return mAppDataList.size();
    }

    public void setData(List<AppData> data) {
        List<AppData> newList = data != null ? new ArrayList<>(data) : new ArrayList<>();
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(
            new AppDiffCallback(mAppDataList, newList)
        );
        mAppDataList.clear();
        mAppDataList.addAll(newList);
        result.dispatchUpdatesTo(this);
    }

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

    public void refreshSelections() {
        Set<String> selectedApps = getCurrentSelectedPackages();
        for (AppData appData : mAppDataList) {
            appData.isSelected = selectedApps.contains(appData.packageName);
        }
        notifyDataSetChanged();
    }

    public void toggleSelection(int position) {
        if (position < 0 || position >= mAppDataList.size()) {
            return;
        }

        AppData appData = mAppDataList.get(position);
        appData.isSelected = !appData.isSelected;
        updateSelectedPackages(appData);
        notifyItemChanged(position);
    }

    public void editCallback(String label, String packageName, String edit) {
        if (mEditManager == null) {
            return;
        }

        mEditManager.editCallback(label, packageName, edit);
        for (int i = 0; i < mAppDataList.size(); i++) {
            if (Objects.equals(packageName, mAppDataList.get(i).packageName)) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    private Set<String> getCurrentSelectedPackages() {
        if (mDeferredSelectionMode) {
            return new LinkedHashSet<>(mSelectedPackages);
        }
        if (mKey != null) {
            return new LinkedHashSet<>(PrefsBridge.getStringSet(mKey));
        }
        return new LinkedHashSet<>();
    }

    private void updateSelectedPackages(AppData appData) {
        if (mDeferredSelectionMode) {
            updatePackageSet(mSelectedPackages, appData);
            return;
        }
        if (mKey == null) {
            return;
        }

        Set<String> selectedApps = new LinkedHashSet<>(PrefsBridge.getStringSet(mKey));
        updatePackageSet(selectedApps, appData);
        PrefsBridge.putByApp(mKey, selectedApps);
    }

    private static void updatePackageSet(Set<String> packages, AppData appData) {
        if (appData.isSelected) {
            packages.add(appData.packageName);
        } else {
            packages.remove(appData.packageName);
        }
    }

    private boolean isToggleSelectionMode() {
        return switch (mMode) {
            case SubPickerActivity.LAUNCHER_MODE,
                 SubPickerActivity.APP_OPEN_MODE,
                 SubPickerActivity.PROCESS_TEXT_MODE,
                 SubPickerActivity.ALL_APPS_MODE,
                 SubPickerActivity.IME_MODE,
                 SubPickerActivity.SCOPE_MODE -> true;
            default -> false;
        };
    }

    private boolean shouldShowCheckbox() {
        return isToggleSelectionMode() || mMode == SubPickerActivity.LAUNCHER_PICK_MODE;
    }

    public interface OnItemClickListener {
        void onItemClick(View itemView, AppData appData, int position);
    }

    private static class AppDiffCallback extends DiffUtil.Callback {
        private final List<AppData> mOldList;
        private final List<AppData> mNewList;

        AppDiffCallback(List<AppData> oldList, List<AppData> newList) {
            mOldList = oldList;
            mNewList = newList;
        }

        @Override
        public int getOldListSize() {
            return mOldList.size();
        }

        @Override
        public int getNewListSize() {
            return mNewList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldPosition, int newPosition) {
            return Objects.equals(
                mOldList.get(oldPosition).packageName,
                mNewList.get(newPosition).packageName
            );
        }

        @Override
        public boolean areContentsTheSame(int oldPosition, int newPosition) {
            AppData oldItem = mOldList.get(oldPosition);
            AppData newItem = mNewList.get(newPosition);
            return oldItem.isSelected == newItem.isSelected
                && Objects.equals(oldItem.label, newItem.label);
        }
    }

    public class AppViewHolder extends RecyclerView.ViewHolder {

        private final ImageView mAppIcon;
        private final TextView mAppName;
        private final CheckBox mSelectCheckbox;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            mAppIcon = itemView.findViewById(android.R.id.icon);
            mAppName = itemView.findViewById(android.R.id.title);
            mSelectCheckbox = itemView.findViewById(android.R.id.checkbox);
            setupClickListeners();
        }

        public void bind(AppData appInfo) {
            if (appInfo == null) {
                return;
            }

            updateAppName(appInfo);
            updateAppIcon(appInfo);
            updateCheckbox(appInfo);
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
            String packageName = appInfo.packageName;
            mAppIcon.setTag(packageName);

            if (appInfo.icon != null) {
                mAppIcon.setImageDrawable(appInfo.icon);
                return;
            }
            if (packageName == null || packageName.isEmpty()) {
                mAppIcon.setImageResource(android.R.drawable.sym_def_app_icon);
                return;
            }

            int iconSize = getTargetIconSize();
            Drawable cached = AppIconCache.getCached(itemView.getContext(), packageName, iconSize);
            if (cached != null) {
                appInfo.icon = cached;
                mAppIcon.setImageDrawable(cached);
                return;
            }

            mAppIcon.setImageResource(android.R.drawable.sym_def_app_icon);
            AppIconCache.loadIconAsync(itemView.getContext(), packageName, iconSize, icon -> {
                if (icon != null && Objects.equals(packageName, mAppIcon.getTag())) {
                    appInfo.icon = icon;
                    mAppIcon.setImageDrawable(icon);
                }
            });
        }

        private int getTargetIconSize() {
            ViewGroup.LayoutParams params = mAppIcon.getLayoutParams();
            int size = params != null ? Math.max(params.width, params.height) : 0;
            if (size > 0) {
                return size;
            }
            return Math.round(
                itemView.getResources().getDisplayMetrics().density * DEFAULT_ICON_SIZE_DP
            );
        }

        private void updateCheckbox(AppData appInfo) {
            boolean showCheckbox = shouldShowCheckbox();
            mSelectCheckbox.setVisibility(showCheckbox ? View.VISIBLE : View.GONE);
            if (showCheckbox) {
                mSelectCheckbox.setChecked(appInfo.isSelected);
            }
        }

        private void setupClickListeners() {
            itemView.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= mAppDataList.size()) {
                    return;
                }

                AppData appData = mAppDataList.get(position);
                if (isToggleSelectionMode()) {
                    toggleSelection(position);
                } else if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(view, appData, position);
                }
            });

            mSelectCheckbox.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION || position >= mAppDataList.size()) {
                    return;
                }

                if (mMode == SubPickerActivity.LAUNCHER_PICK_MODE) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(
                            view,
                            mAppDataList.get(position),
                            position
                        );
                    }
                } else {
                    toggleSelection(position);
                }
            });
        }
    }
}
