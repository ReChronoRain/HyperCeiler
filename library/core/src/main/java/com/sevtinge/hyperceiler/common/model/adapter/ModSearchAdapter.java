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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.common.model.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.common.model.data.ModData;
import com.sevtinge.hyperceiler.common.utils.SearchIndexManager;
import com.sevtinge.hyperceiler.common.view.FlowLayout;
import com.sevtinge.hyperceiler.core.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import fan.recyclerview.card.CardGroupAdapter;

public class ModSearchAdapter extends CardGroupAdapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_EMPTY = 1;
    private static final int TYPE_HISTORY = 2;

    private final List<ModData> mItems = new ArrayList<>();
    private final List<Integer> mGroupIndices = new ArrayList<>();
    private final List<Boolean> mIsGroupFirst = new ArrayList<>();
    private final List<String> mHistory = new ArrayList<>();

    private String mQuery = "";
    private boolean mIsChinese = false;
    private boolean mIsEmpty = false;
    private boolean mShowHistory = false;

    private OnItemClickListener mClickListener;
    private OnHistoryClickListener mHistoryClickListener;

    private static final LruCache<String, Drawable> sIconCache = new LruCache<>(64);
    private static final Map<String, String> sGroupToPackage = new LinkedHashMap<>();

    public interface OnItemClickListener {
        void onItemClick(View view, ModData data);
    }

    public interface OnHistoryClickListener {
        void onHistoryClick(String query);
        void onClearHistory();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mClickListener = listener;
    }

    public void setOnHistoryClickListener(OnHistoryClickListener listener) {
        mHistoryClickListener = listener;
    }

    public String getQuery() {
        return mQuery;
    }

    public static void initGroupPackageMap(Map<String, String> map) {
        sGroupToPackage.clear();
        sGroupToPackage.putAll(map);
    }

    private static Drawable getAppIcon(Context context, String groupName) {
        Drawable cached = sIconCache.get(groupName);
        if (cached != null) return cached;

        String packageName = sGroupToPackage.get(groupName);
        if (packageName != null) {
            try {
                PackageManager pm = context.getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
                Drawable icon = info.loadIcon(pm);
                sIconCache.put(groupName, icon);
                return icon;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }

        return null;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submitSearch(Context context, String query, List<String> history) {
        mQuery = query == null ? "" : query;

        mHistory.clear();
        if (history != null) {
            mHistory.addAll(history);
        }

        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        mIsChinese = locale.getLanguage().equals(new Locale("zh").getLanguage());

        mItems.clear();
        mGroupIndices.clear();
        mIsGroupFirst.clear();
        mShowHistory = false;
        mIsEmpty = false;

        if (mQuery.isEmpty()) {
            if (!mHistory.isEmpty()) {
                mShowHistory = true;
                mItems.add(null);
                mGroupIndices.add(0);
                mIsGroupFirst.add(true);
            }
        } else {
            List<ModData> results = SearchIndexManager.search(mQuery, locale);
            if (!results.isEmpty()) {
                LinkedHashMap<String, List<ModData>> groups = new LinkedHashMap<>();
                for (ModData mod : results) {
                    groups.computeIfAbsent(mod.getGroup(), k -> new ArrayList<>()).add(mod);
                }
                int groupIdx = 0;
                for (Map.Entry<String, List<ModData>> entry : groups.entrySet()) {
                    boolean first = true;
                    for (ModData mod : entry.getValue()) {
                        mItems.add(mod);
                        mGroupIndices.add(groupIdx);
                        mIsGroupFirst.add(first);
                        first = false;
                    }
                    groupIdx++;
                }
            } else {
                mIsEmpty = true;
                mItems.add(null);
                mGroupIndices.add(0);
                mIsGroupFirst.add(true);
            }
        }

        updateGroupInfo();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mShowHistory) return TYPE_HISTORY;
        if (mIsEmpty) return TYPE_EMPTY;
        return TYPE_ITEM;
    }

    @Override
    public int getItemViewGroup(int position) {
        if (mShowHistory || mIsEmpty) return CardGroupAdapter.GROUP_ID_NONE;
        return position < mGroupIndices.size() ? mGroupIndices.get(position) : CardGroupAdapter.GROUP_ID_NONE;
    }

    @Override
    public void setHasStableIds() {}

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HISTORY) {
            return new HistoryHolder(inflater.inflate(R.layout.item_search_history, parent, false));
        }
        if (viewType == TYPE_EMPTY) {
            return new EmptyHolder(inflater.inflate(R.layout.item_search_empty, parent, false));
        }
        return new ItemHolder(inflater.inflate(R.layout.item_search_result, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        if (holder instanceof HistoryHolder hh) {
            hh.itemView.setClickable(false);
            hh.itemView.setFocusable(false);
            hh.bind(mHistory, mHistoryClickListener);
        } else if (holder instanceof ItemHolder ih) {
            ModData mod = mItems.get(position);
            boolean isFirst = mIsGroupFirst.get(position);

            if (isFirst) {
                Drawable icon = getAppIcon(ih.itemView.getContext(), mod.getGroup());
                if (icon != null) {
                    ih.icon.setImageDrawable(icon);
                } else {
                    ih.icon.setImageResource(R.drawable.ic_various_new);
                }
                ih.icon.setVisibility(View.VISIBLE);
            } else {
                ih.icon.setVisibility(View.INVISIBLE);
            }

            ih.bind(mod, mIsChinese, mQuery);
            ih.itemView.setOnClickListener(v -> {
                if (mClickListener != null) mClickListener.onItemClick(v, mod);
            });
        }
    }

    static class HistoryHolder extends RecyclerView.ViewHolder {
        final FlowLayout flowLayout;
        final View clearBtn;

        HistoryHolder(View v) {
            super(v);
            flowLayout = v.findViewById(R.id.search_history_flow);
            clearBtn = v.findViewById(R.id.search_history_clear);

            // 移除默认的点击水波纹和卡片背景效果，保持透明
            v.setBackground(null);
            v.setForeground(null);
        }

        void bind(List<String> history, OnHistoryClickListener listener) {
            flowLayout.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            for (String query : history) {
                TextView tag = (TextView) inflater.inflate(R.layout.item_search_history_tag, flowLayout, false);
                tag.setText(query);
                tag.setOnClickListener(v -> {
                    if (listener != null) listener.onHistoryClick(query);
                });
                flowLayout.addView(tag);
            }
            clearBtn.setOnClickListener(v -> {
                if (listener != null) listener.onClearHistory();
            });
        }
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView summary;

        ItemHolder(View v) {
            super(v);
            icon = v.findViewById(android.R.id.icon);
            title = v.findViewById(android.R.id.title);
            summary = v.findViewById(android.R.id.summary);
        }

        void bind(ModData mod, boolean isChinese, String query) {
            String lowerQuery = query.toLowerCase();
            Spannable spannable = new SpannableString(mod.title);

            if (isChinese) {
                for (int i = 0; i < lowerQuery.length(); i++) {
                    String ch = String.valueOf(lowerQuery.charAt(i));
                    int start = mod.title.toLowerCase().indexOf(ch);
                    if (start >= 0) {
                        spannable.setSpan(new ForegroundColorSpan(SearchIndexManager.MARK_COLOR),
                            start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            } else {
                int start = mod.title.toLowerCase().indexOf(lowerQuery);
                if (start >= 0) {
                    spannable.setSpan(new ForegroundColorSpan(SearchIndexManager.MARK_COLOR),
                        start, start + lowerQuery.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            title.setText(spannable, TextView.BufferType.SPANNABLE);

            // 只有多层路径才显示
            boolean hasMultiLevel = mod.breadcrumbs != null && mod.breadcrumbs.contains("/");
            if (hasMultiLevel) {
                summary.setText(mod.breadcrumbs);
                summary.setVisibility(View.VISIBLE);
            } else {
                summary.setVisibility(View.GONE);
            }
        }
    }

    static class EmptyHolder extends RecyclerView.ViewHolder {
        EmptyHolder(View v) { super(v); }
    }
}
