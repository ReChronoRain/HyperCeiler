package com.sevtinge.hyperceiler.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sevtinge.hyperceiler.common.model.data.ModData;
import com.sevtinge.hyperceiler.common.utils.SettingLauncherHelper;
import com.sevtinge.hyperceiler.dashboard.SubSettings;
import com.sevtinge.hyperceiler.search.SearchHelper;
import com.sevtinge.hyperceiler.search.data.ModEntity;

import java.util.ArrayList;
import java.util.List;

import fan.recyclerview.card.CardGroupAdapter;

public class SearchResultAdapter extends CardGroupAdapter<SearchResultAdapter.ModSearchViewHolder> {

    // 数据源切换为 Room 实体类
    private final List<ModEntity> modsList = new ArrayList<>();
    private String filterString = "";
    private boolean isChina;
    private OnItemClickListener mItemClickListener;

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
        return 0;
    }

    @Override
    public void onBindViewHolder(@NonNull ModSearchViewHolder holder, int position) {
        ModEntity ad = modsList.get(position);
        holder.bind(ad, isChina, filterString, v -> {
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

        public void bind(ModEntity ad, boolean isChina, String query, View.OnClickListener listener) {
            mName.setText(getHighlightedText(ad.title, query, isChina));
            mPackageName.setText(ad.breadcrumbs);
            itemView.setOnClickListener(listener);
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
