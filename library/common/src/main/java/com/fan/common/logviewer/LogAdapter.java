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
package com.fan.common.logviewer;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.core.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import fan.internal.utils.AnimHelper;
import fan.recyclerview.card.CardGroupAdapter;

public class LogAdapter extends CardGroupAdapter<LogAdapter.LogViewHolder>
        implements Filterable {
    private Context mContext;

    // 数据相关
    private List<LogEntry> mOriginalLogEntries;
    private List<LogEntry> mFilteredLogEntries;

    // 过滤条件
    private String mSearchKeyword = "";
    private String mSelectedLevel = "ALL";
    private String mSelectedModule = "ALL";
    private final List<String> mLevelList = new ArrayList<>();
    private final List<String> mModuleList = new ArrayList<>();

    // 颜色配置
    private static final int sSearchHighlightColor = Color.RED;
    private static final int sDefaultTextColor = Color.BLACK;

    // 监听器
    private OnFilterChangeListener mFilterChangeListener;
    private OnLogItemClickListener mLogItemClickListener;
    private Runnable mPendingUpdate;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mFilterExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean mIsFiltering = false;

    public interface OnDataUpdateListener {
        void onDataUpdated();
    }

    private OnDataUpdateListener mDataUpdateListener;

    public void setOnDataUpdateListener(OnDataUpdateListener listener) {
        mDataUpdateListener = listener;
    }

    public LogAdapter(Context context, List<LogEntry> logEntries) {
        mContext = context;
        mOriginalLogEntries = new ArrayList<>(logEntries);
        // 倒序显示，最新的日志在最上面
        Collections.reverse(mOriginalLogEntries);
        mFilteredLogEntries = new ArrayList<>(mOriginalLogEntries);
        // 提取所有可用的模块和级别
        extractAvailableList();
    }

    private void extractAvailableList() {
        mLevelList.clear();
        mModuleList.clear();

        mLevelList.add(mContext.getString(R.string.log_filter_all));
        mModuleList.add(mContext.getString(R.string.log_filter_all));

        Set<String> levelSet = new HashSet<>();
        Set<String> moduleSet = new HashSet<>();
        for (LogEntry entry : mOriginalLogEntries) {
            if (entry != null) {
                if (entry.getLevel() != null) {
                    switch (entry.getLevel()) {
                        case "C" -> levelSet.add(mContext.getString(R.string.log_level_crash));
                        case "D" -> levelSet.add(mContext.getString(R.string.log_level_debug));
                        case "I" -> levelSet.add(mContext.getString(R.string.log_level_info));
                        case "W" -> levelSet.add(mContext.getString(R.string.log_level_warn));
                        case "E" -> levelSet.add(mContext.getString(R.string.log_level_error));
                    }
                }
                if (entry.getTag() != null) {
                    moduleSet.add(entry.getTag());
                }
            }
        }

        mLevelList.addAll(levelSet);
        mModuleList.addAll(moduleSet);
    }


    // 获取模块列表（线程安全）
    public List<String> getModuleList() {
        return new ArrayList<>(mModuleList);
    }

    public List<String> getLevelList() {
        return new ArrayList<>(mLevelList);
    }

    // 更新数据
    public void updateData(List<LogEntry> newLogEntries) {
        if (newLogEntries == null) return;

        if (mPendingUpdate != null) {
            mMainHandler.removeCallbacks(mPendingUpdate);
        }

        mPendingUpdate = () -> {
            mOriginalLogEntries = new ArrayList<>(newLogEntries);
            Collections.reverse(mOriginalLogEntries);
            extractAvailableList();
            performFiltering();

            // 通知数据更新完成
            if (mDataUpdateListener != null) {
                mDataUpdateListener.onDataUpdated();
            }
        };

        mMainHandler.postDelayed(mPendingUpdate, 100);
    }

    private void performFiltering() {
        if (mIsFiltering) return;
        mIsFiltering = true;

        // 复制当前过滤条件，避免并发问题
        final String searchLower = mSearchKeyword.toLowerCase();
        final String selectedLevel = mSelectedLevel;
        final String selectedModule = mSelectedModule;
        final List<LogEntry> originalList = new ArrayList<>(mOriginalLogEntries);

        mFilterExecutor.execute(() -> {
            try {
                List<LogEntry> filteredList = new ArrayList<>();

                for (LogEntry entry : originalList) {
                    if (entry == null) continue;

                    // 级别过滤
                    boolean levelMatch = "ALL".equals(selectedLevel) ||selectedLevel.equals(entry.getLevel());
                    if (!levelMatch) continue;

                    // 模块过滤
                    boolean moduleMatch = "ALL".equals(selectedModule) ||
                        selectedModule.equals(entry.getTag());
                    if (!moduleMatch) continue;

                    // 搜索过滤
                    if (!searchLower.isEmpty()) {
                        String message = entry.getMessage();
                        String tag = entry.getTag();
                        if (message == null || tag == null) continue;

                        boolean searchMatch = message.toLowerCase().contains(searchLower) ||
                            tag.toLowerCase().contains(searchLower);
                        if (!searchMatch) continue;
                    }

                    filteredList.add(entry);
                }

                final List<LogEntry> result = filteredList;
                final int filteredSize = result.size();
                final int totalSize = originalList.size();

                mMainHandler.post(() -> {
                    mFilteredLogEntries = result;
                    notifyDataSetChanged();

                    if (mFilterChangeListener != null) {
                        mFilterChangeListener.onFilterChanged(filteredSize, totalSize);
                    }
                    mIsFiltering = false;
                });
            } catch (Exception e) {
                mIsFiltering = false;
            }
        });
    }

    // 搜索功能
    // 设置过滤条件
    public void setSearchKeyword(String keyword) {
        mSearchKeyword = keyword != null ? keyword : "";
        performFiltering();
    }

    // 级别过滤
    public void setLevelFilter(String level) {
        if (level != null) {
            String all = mContext.getString(R.string.log_filter_all);
            String crash = mContext.getString(R.string.log_level_crash);
            String debug = mContext.getString(R.string.log_level_debug);
            String info = mContext.getString(R.string.log_level_info);
            String warn = mContext.getString(R.string.log_level_warn);
            String error = mContext.getString(R.string.log_level_error);

            if (level.equals(all)) {
                mSelectedLevel = "ALL";
            } else if (level.equals(crash)) {
                mSelectedLevel = "C";
            } else if (level.equals(debug)) {
                mSelectedLevel = "D";
            } else if (level.equals(info)) {
                mSelectedLevel = "I";
            } else if (level.equals(warn)) {
                mSelectedLevel = "W";
            } else if (level.equals(error)) {
                mSelectedLevel = "E";
            } else {
                mSelectedLevel = "ALL";
            }
        }
        performFiltering();
    }


    // 模块过滤
    public void setModuleFilter(String module) {
        if (module != null) {
            String all = mContext.getString(R.string.log_filter_all);
            mSelectedModule = module.equals(all) ? "ALL" : module;
        }
        performFiltering();
    }

    // 清除所有过滤条件
    public void clearAllFilters() {
        mSearchKeyword = "";
        mSelectedLevel = "ALL";
        mSelectedModule = "ALL";
        performFiltering();
    }

    // 设置过滤变化监听器
    public void setOnFilterChangeListener(OnFilterChangeListener listener) {
        mFilterChangeListener = listener;
    }

    // 设置日志条目点击监听器
    public void setOnLogItemClickListener(OnLogItemClickListener listener) {
        mLogItemClickListener = listener;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(com.fan.common.R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void setHasStableIds() {

    }

    @Override
    public int getItemViewGroup(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        if (position >= 0 && position < mFilteredLogEntries.size()) {
            LogEntry logEntry = mFilteredLogEntries.get(position);
            holder.bind(logEntry, mSearchKeyword);
            holder.itemView.setOnClickListener(v -> {
                AnimHelper.addItemPressEffect(v);
                if (mLogItemClickListener != null) {
                    mLogItemClickListener.onLogItemClick(logEntry);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mFilteredLogEntries.size();
    }

    @Override
    public Filter getFilter() {
        // 返回一个空的Filter，因为我们使用performFiltering方法
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                results.values = mFilteredLogEntries;
                results.count = mFilteredLogEntries.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                // 空实现，因为我们手动处理
            }
        };
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {

        private final TextView mTimeTextView;
        private final TextView mLevelTextView;
        private final TextView mModuleTextView;
        private final TextView mMessageTextView;
        private final StringBuilder mTempBuilder = new StringBuilder();

        private static final String[] LEVEL_LABELS = {"C", "E", "W", "I", "D"};
        private static final String[] LEVEL_DISPLAY = {"CRASH", "ERROR", "WARN", "INFO", "DEBUG"};

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            mTimeTextView = itemView.findViewById(com.fan.common.R.id.textTime);
            mLevelTextView = itemView.findViewById(com.fan.common.R.id.textLevel);
            mModuleTextView = itemView.findViewById(com.fan.common.R.id.textModule);
            mMessageTextView = itemView.findViewById(com.fan.common.R.id.textMessage);
        }

        public void bind(LogEntry logEntry, String searchKeyword) {
            // 时间
            mTempBuilder.setLength(0);
            mTempBuilder.append(logEntry.getFormattedTime()).append(" | ");
            mTimeTextView.setText(mTempBuilder);

            // 标签
            mModuleTextView.setText(logEntry.getTag());

            // 级别徽标
            String level = logEntry.getLevel();
            String displayLevel = level;
            for (int i = 0; i < LEVEL_LABELS.length; i++) {
                if (LEVEL_LABELS[i].equals(level)) {
                    displayLevel = LEVEL_DISPLAY[i];
                    break;
                }
            }
            mLevelTextView.setText(displayLevel);
            mLevelTextView.getBackground().setTint(getLevelBadgeColor(level));
            mLevelTextView.setTextColor(getLevelTextColor(level));

            // 消息（最多3行）
            String message = logEntry.getMessage();
            if ("C".equals(level)) {
                mMessageTextView.setTextColor(0xFFD32F2F);
            } else {
                mMessageTextView.setTextColor(mMessageTextView.getContext()
                    .getColor(com.fan.common.R.color.item_view_title_color_light));
            }

            if (!TextUtils.isEmpty(searchKeyword) && message != null &&
                message.toLowerCase().contains(searchKeyword.toLowerCase())) {
                mMessageTextView.setText(highlightText(message, searchKeyword));
            } else {
                mMessageTextView.setText(message);
            }
        }

        private int getLevelBadgeColor(String level) {
            return switch (level) {
                case "C" -> 0xFFD32F2F;
                case "E" -> 0x40F44336;
                case "W" -> 0x40FFC107;
                case "I" -> 0x404CAF50;
                case "D" -> 0x402196F3;
                default -> 0x40909090;
            };
        }

        private int getLevelTextColor(String level) {
            return switch (level) {
                case "C" -> 0xFFFFFFFF;
                case "E" -> 0xFFF44336;
                case "W" -> 0xFFFF8F00;
                case "I" -> 0xFF388E3C;
                case "D" -> 0xFF1976D2;
                default -> 0xFF757575;
            };
        }

        private SpannableString highlightText(String text, String keyword) {
            SpannableString spannable = new SpannableString(text);
            String lowerText = text.toLowerCase();
            String lowerKeyword = keyword.toLowerCase();
            int startIndex = 0;
            int keywordIndex;
            while ((keywordIndex = lowerText.indexOf(lowerKeyword, startIndex)) != -1) {
                int endIndex = keywordIndex + keyword.length();
                spannable.setSpan(
                    new ForegroundColorSpan(sSearchHighlightColor),
                    keywordIndex, endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                startIndex = endIndex;
            }
            return spannable;
        }
    }

    // 过滤变化监听器接口
    public interface OnFilterChangeListener {
        void onFilterChanged(int filteredCount, int totalCount);
    }

    // 日志条目点击监听器接口
    public interface OnLogItemClickListener {
        void onLogItemClick(LogEntry logEntry);
    }
}
