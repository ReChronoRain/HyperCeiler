package com.sevtinge.hyperceiler.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.utils.SettingLauncherHelper;
import com.sevtinge.hyperceiler.dashboard.SubSettings;
import com.sevtinge.hyperceiler.search.data.ModEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fan.recyclerview.card.CardGroupAdapter;

public class SearchResultAdapter extends CardGroupAdapter<SearchResultAdapter.ModSearchViewHolder> {

    // 数据源切换为 Room 实体类
    private final List<ModEntity> modsList = new ArrayList<>();
    private String filterString = "";
    private boolean isChina;
    private OnItemClickListener mItemClickListener;
    private final Map<String, Drawable> iconCache = new HashMap<>();

    public interface OnItemClickListener {
        void onItemClick(View view, ModEntity ad);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    /**
     * 关键方法：供 HomePageFragment 中的 SearchHandler 调用
     */
    @SuppressLint("NotifyDataSetChanged")
    public void refresh(List<ModEntity> newData, String query, boolean isChina) {
        this.modsList.clear();
        if (newData != null) {
            this.modsList.addAll(newData);
        }
        this.filterString = query;
        this.isChina = isChina;
        notifyDataSetChanged();
        updateGroupInfo();
    }

    @NonNull
    @Override
    public ModSearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(com.sevtinge.hyperceiler.core.R.layout.item_search_result, parent, false);
        return new ModSearchViewHolder(view);
    }

    @Override
    public void setHasStableIds() {

    }

    @Override
    public int getItemViewGroup(int position) {
        if (position < 0 || position >= modsList.size()) return 0;
        String group = modsList.get(position).groupName;
        if (group == null || group.isEmpty()) return 0;
        return group.hashCode();
    }

    private boolean isFirstInGroup(int position) {
        if (position == 0) return true;
        return getItemViewGroup(position) != getItemViewGroup(position - 1);
    }

    @Override
    public void onBindViewHolder(@NonNull ModSearchViewHolder holder, int position) {
        ModEntity ad = modsList.get(position);
        boolean firstInGroup = isFirstInGroup(position);
        holder.bind(ad, isChina, filterString, iconCache, firstInGroup, v -> {
            onItemClickListener(v.getContext(), ad);
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(v, ad);
            }
        });
    }

    private void onItemClickListener(Context context, ModEntity ad) {
        if (ad == null) return;
        Bundle args = new Bundle();
        args.putString(":settings:fragment_args_key", ad.key);
        args.putInt(":settings:fragment_resId", ad.xmlResId);
        SettingLauncherHelper.onStartSettingsForArguments(
            context,
            SubSettings.class,
            ad.fragment,
            args,
            ad.catTitleResId
        );
    }

    @Override
    public int getItemCount() {
        return modsList.size();
    }



    public static class ModSearchViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mIcon;
        private final TextView mName;
        private final TextView mPackageName;

        public ModSearchViewHolder(View itemView) {
            super(itemView);
            mIcon = itemView.findViewById(android.R.id.icon);
            mName = itemView.findViewById(android.R.id.title);
            mPackageName = itemView.findViewById(android.R.id.summary);
        }

        public void bind(ModEntity ad, boolean isChina, String query,
                         Map<String, Drawable> iconCache, boolean isFirstInGroup,
                         View.OnClickListener listener) {
            mName.setText(getHighlightedText(ad.title, query, isChina));

            // 隐藏单层 breadcrumbs（与 groupName 相同时不显示）
            String bc = ad.breadcrumbs;
            if (bc != null && bc.contains("/")) {
                mPackageName.setText(bc);
                mPackageName.setVisibility(View.VISIBLE);
            } else {
                mPackageName.setVisibility(View.GONE);
            }

            // 仅组内第一个条目显示图标
            if (isFirstInGroup) {
                String groupName = ad.groupName;
                if (groupName != null && !groupName.isEmpty()) {
                    Drawable icon = iconCache.get(groupName);
                    if (icon == null) {
                        String pkg = SearchHelper.getGroupPackageMap().get(groupName);
                        if (pkg != null) {
                            try {
                                icon = itemView.getContext().getPackageManager().getApplicationIcon(pkg);
                                iconCache.put(groupName, icon);
                            } catch (PackageManager.NameNotFoundException e) {
                                // 应用未安装，使用 prefs_main 中定义的图标
                                icon = loadFallbackIcon(groupName);
                                if (icon != null) iconCache.put(groupName, icon);
                            }
                        } else {
                            // 没有包名映射（如"其他"），使用 prefs_main 中定义的图标
                            icon = loadFallbackIcon(groupName);
                            if (icon != null) iconCache.put(groupName, icon);
                        }
                    }
                    if (icon != null) {
                        mIcon.setImageDrawable(icon);
                        mIcon.setVisibility(View.VISIBLE);
                    } else {
                        mIcon.setVisibility(View.INVISIBLE);
                    }
                } else {
                    mIcon.setVisibility(View.INVISIBLE);
                }
            } else {
                mIcon.setVisibility(View.INVISIBLE);
            }

            itemView.setOnClickListener(listener);
        }

        private Drawable loadFallbackIcon(String groupName) {
            Integer iconResId = SearchHelper.getGroupIconMap().get(groupName);
            if (iconResId != null && iconResId > 0) {
                try {
                    return androidx.core.content.ContextCompat.getDrawable(itemView.getContext(), iconResId);
                } catch (Exception ignored) {}
            }
            return null;
        }

        // 提取出的文字高亮逻辑
        private Spannable getHighlightedText(String text, String query, boolean isChina) {
            Spannable spannable = new SpannableString(text);
            if (TextUtils.isEmpty(query)) return spannable;

            String lText = text.toLowerCase();
            String lQuery = query.toLowerCase();

            if (isChina) {
                // 逐字匹配高亮 (旧版逻辑)
                for (char c : lQuery.toCharArray()) {
                    int start = lText.indexOf(String.valueOf(c));
                    if (start >= 0) {
                        spannable.setSpan(new ForegroundColorSpan(SearchHelper.MARK_COLOR_VIBRANT),
                            start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            } else {
                // 连续匹配高亮
                int start = lText.indexOf(lQuery);
                if (start >= 0) {
                    spannable.setSpan(new ForegroundColorSpan(SearchHelper.MARK_COLOR_VIBRANT),
                        start, start + lQuery.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            return spannable;
        }
    }
}
