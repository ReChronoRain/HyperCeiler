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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.data.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.data.ModData;
import com.sevtinge.hyperceiler.utils.search.SearchHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import fan.internal.utils.AnimHelper;
import fan.recyclerview.card.CardGroupAdapter;

public class ModSearchAdapter extends CardGroupAdapter {

    private Context mContext;
    private String filterString = "";
    private ItemFilter mFilter;
    private boolean isChina;
    private final static String TAG = "ModSearchAdapter";
    private onItemClickListener mItemClickListener;// item点击监听
    private final CopyOnWriteArrayList<ModData> modsList = new CopyOnWriteArrayList<>();

    public void setOnItemClickListener(onItemClickListener onItemClick) {
        mItemClickListener = onItemClick;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_search_result, parent, false);
        // 创建一个VIewHolder
        return new ModSearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        ModData ad = modsList.get(position);
        ((ModSearchViewHolder) holder).bind(ad, isChina, filterString, v -> mItemClickListener.onItemClick(v, ad));
    }

    @Override
    public int getItemCount() {
        return modsList.size();
    }

    public Filter getFilter(Context context) {
        // 如果ItemFilter对象为空，那么重写创建一个
        if (mFilter == null) {
            mFilter = new ItemFilter(context);
        }
        return mFilter;
    }

    @Override
    public int getItemViewGroup(int i) {
        return Integer.MIN_VALUE;
    }

    @Override
    public void setHasStableIds() {
        setHasStableIds(true);
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

        public void bind(ModData ad, boolean isChina, String filterString, View.OnClickListener onClickListener) {
            Spannable spannable = new SpannableString(ad.title);
            if (isChina) {
                for (int i = 0; i < filterString.length(); i++) {
                    char ch = filterString.charAt(i);
                    String str = String.valueOf(ch);
                    int start = ad.title.toLowerCase().indexOf(str);
                    if (start >= 0) {
                        spannable.setSpan(new ForegroundColorSpan(SearchHelper.MARK_COLOR_VIBRANT), start, start + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                mName.setText(spannable, TextView.BufferType.SPANNABLE);
            } else {
                int start = ad.title.toLowerCase().indexOf(filterString);
                if (start >= 0) {
                    spannable.setSpan(new ForegroundColorSpan(SearchHelper.MARK_COLOR_VIBRANT), start, start + filterString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    mName.setText(spannable, TextView.BufferType.SPANNABLE);
                } else {
                    mName.setText(ad.title);
                }
            }
            mPackageName.setText(ad.breadcrumbs);
            // 设置item点击监听事件
            itemView.setOnClickListener(onClickListener);
        }
    }


    public interface onItemClickListener {
        void onItemClick(View view, ModData ad);
    }


    private class ItemFilter extends Filter {
        private final Context context;

        ItemFilter(Context context) {
            this.context = context;
        }

        private final HashMap<String, Integer> modMap = new HashMap<>();

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            filterString = constraint.toString().toLowerCase();
            ArrayList<ModData> nlist = new ArrayList<>();
            modMap.clear();
            isChina = isChina(context);
            if (isChina) {
                for (int i = 0; i < filterString.length(); i++) {
                    char ch = filterString.charAt(i);
                    String str = String.valueOf(ch);
                    findList(constraint, str, nlist);
                }
            } else findList(constraint, filterString, nlist);
            FilterResults results = new FilterResults();
            results.values = nlist;
            results.count = nlist.size();
            return results;
        }

        private void findList(CharSequence constraint, String str, ArrayList<ModData> nlist) {
            for (ModData filterableData : SearchHelper.allModsList) {
                if (constraint.toString().equals(SearchHelper.NEW_MODS_SEARCH_QUERY)) {
                    if (SearchHelper.NEW_MODS.contains(filterableData.key)) {
                        if (check(filterableData.key)) {
                            nlist.add(filterableData);
                            modMap.put(filterableData.key, 0);
                        }
                    }
                } else if (filterableData.title.toLowerCase().contains(str)) {
                    if (check(filterableData.key)) {
                        nlist.add(filterableData);
                        modMap.put(filterableData.key, 0);
                    }
                }
            }
        }

        private boolean check(String key) {
            return modMap.get(key) == null;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            modsList.clear();
            if (results.count > 0 && results.values != null) {
                modsList.addAll((ArrayList<ModData>) results.values);
            }
            if (isChina) modsList.sort(new ModDataComparator(filterString));
            else sortList();
            notifyDataSetChanged();
        }
    }

    public boolean isChina(Context context) {
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        return locale.getLanguage().contains(new Locale("zh").getLanguage());
    }

    public void sortList() {
        modsList.sort(new Comparator<>() {
            public int compare(ModData app1, ModData app2) {
                int breadcrumbs = app1.breadcrumbs.compareToIgnoreCase(app2.breadcrumbs);
                if (breadcrumbs == 0)
                    return app1.title.compareToIgnoreCase(app2.title);
                else
                    return breadcrumbs;
            }
        });
    }

    public static class ModDataComparator implements Comparator<ModData> {
        private final String searchTerm;

        public ModDataComparator(String searchTerm) {
            this.searchTerm = searchTerm;
        }

        @Override
        public int compare(ModData app1, ModData app2) {
            int frequency1 = calculateFrequency(app1.title);
            int frequency2 = calculateFrequency(app2.title);

            return Integer.compare(frequency2, frequency1); // 频率高的排在前面
        }

        private int calculateFrequency(String str) {
            int frequency = 0;
            for (int i = 0; i < searchTerm.length(); i++) {
                char ch = searchTerm.charAt(i);
                String chStr = String.valueOf(ch);
                if (str.contains(chStr)) {
                    frequency = frequency + 1;
                }
            }
            return frequency;
        }
    }
}
