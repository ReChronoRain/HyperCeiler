package com.fan.common.logviewer;

import android.graphics.Color;
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

import com.fan.common.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fan.internal.utils.AnimHelper;
import fan.recyclerview.card.CardGroupAdapter;

public class LogAdapter extends CardGroupAdapter<LogAdapter.LogViewHolder>
        implements Filterable {

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

    public LogAdapter(List<LogEntry> logEntries) {
        mOriginalLogEntries = new ArrayList<>(logEntries);
        mFilteredLogEntries = new ArrayList<>(logEntries);
        // 提取所有可用的模块和级别
        extractAvailableList();
    }

    private void extractAvailableList() {

        mLevelList.clear();
        mModuleList.clear();

        mLevelList.add("全部");
        mModuleList.add("全部");

        Set<String> levelSet = new HashSet<>();
        Set<String> moduleSet = new HashSet<>();
        for (LogEntry entry : mOriginalLogEntries) {
            if (entry != null) {
                if (entry.getLevel() != null) {
                    switch (entry.getLevel()) {
                        case "V" -> levelSet.add("Verbose");
                        case "D" -> levelSet.add("Debug");
                        case "I" -> levelSet.add("Info");
                        case "W" -> levelSet.add("Warn");
                        case "E" -> levelSet.add("Error");
                    }
                }
                if (entry.getModule() != null) {
                    moduleSet.add(entry.getModule());
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
    // 安全的更新数据方法
    public void updateData(List<LogEntry> newLogEntries) {
        if (newLogEntries == null) {
            return;
        }

        mOriginalLogEntries = new ArrayList<>(newLogEntries);
        extractAvailableList();
        performFiltering();
    }

    private void performFiltering() {
        List<LogEntry> filteredList = new ArrayList<>();

        String searchLower = mSearchKeyword.toLowerCase();

        for (LogEntry entry : mOriginalLogEntries) {
            // 级别过滤
            boolean levelMatch = "ALL".equals(mSelectedLevel) ||
                    mSelectedLevel.equals(entry.getLevel());

            // 模块过滤
            boolean moduleMatch = "ALL".equals(mSelectedModule) ||
                    mSelectedModule.equals(entry.getModule());

            // 搜索过滤
            boolean searchMatch = mSearchKeyword.isEmpty() ||
                    entry.getMessage().toLowerCase().contains(searchLower) ||
                    entry.getModule().toLowerCase().contains(searchLower);

            if (levelMatch && moduleMatch && searchMatch) {
                filteredList.add(entry);
            }
        }

        mFilteredLogEntries = filteredList;
        notifyDataSetChanged();

        // 通知过滤变化
        if (mFilterChangeListener != null) {
            mFilterChangeListener.onFilterChanged(
                    mFilteredLogEntries.size(),
                    mOriginalLogEntries.size()
            );
        }
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
            switch (level) {
                case "全部" -> mSelectedLevel = "ALL";
                case "Verbose" -> mSelectedLevel = "V";
                case "Debug" -> mSelectedLevel = "D";
                case "Info" -> mSelectedLevel = "I";
                case "Warn" -> mSelectedLevel = "W";
                case "Error" -> mSelectedLevel = "E";
                default -> mSelectedLevel = "ALL";
            }
        }
        performFiltering();
    }

    // 模块过滤
    public void setModuleFilter(String module) {
        if (module != null) {
            mSelectedModule = module.equals("全部") ? "ALL" : module;
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

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void setHasStableIds() {

    }

    @Override
    public int getItemViewGroup(int position) {
        return 0;
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        if (position >= 0 && position < mFilteredLogEntries.size()) {
            LogEntry logEntry = mFilteredLogEntries.get(position);
            holder.bind(logEntry, mSearchKeyword);
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

        private final View mLevelIndicator;

        private final TextView mTimeTextView;
        private final TextView mLevelTextView;
        private final TextView mModuleTextView;
        private final TextView mMessageTextView;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            mLevelIndicator = itemView.findViewById(R.id.level_indicator);

            mTimeTextView = itemView.findViewById(R.id.textTime);
            mLevelTextView = itemView.findViewById(R.id.textLevel);
            mModuleTextView = itemView.findViewById(R.id.textModule);
            mMessageTextView = itemView.findViewById(R.id.textMessage);
            itemView.setOnClickListener(AnimHelper::addItemPressEffect);
        }

        public void bind(LogEntry logEntry, String searchKeyword) {
            mLevelIndicator.setBackgroundColor(logEntry.getColor());

            // 设置时间
            mTimeTextView.setText(logEntry.getFormattedTime() + " | ");

            // 设置级别（带颜色）
            mLevelTextView.setText(logEntry.getLevel());
            //mLevelTextView.setTextColor(logEntry.getColor());

            // 设置模块
            mModuleTextView.setText(logEntry.getModule());

            // 设置消息（支持搜索高亮和换行）
            String message = logEntry.getMessage();
            if (!TextUtils.isEmpty(searchKeyword) && message.toLowerCase()
                    .contains(searchKeyword.toLowerCase())) {
                mMessageTextView.setText(highlightText(message, searchKeyword));
            } else {
                mMessageTextView.setText(message);
            }

            // 处理换行显示
            if (logEntry.isNewLine()) {
                mMessageTextView.setSingleLine(false);
                mMessageTextView.setMaxLines(10);
                mMessageTextView.setEllipsize(null);
            } else {
                mMessageTextView.setSingleLine(true);
                mMessageTextView.setEllipsize(TextUtils.TruncateAt.END);
            }
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
                        keywordIndex,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                startIndex = endIndex;
            }

            return spannable;
        }
    }

    // 过滤变化监听器接口
    public interface OnFilterChangeListener {
        void onFilterChanged(int filteredCount, int totalCount);
    }
}
