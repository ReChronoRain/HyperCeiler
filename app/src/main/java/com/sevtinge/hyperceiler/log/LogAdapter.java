package com.sevtinge.hyperceiler.log;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.log.LogLevelFilter;
import com.sevtinge.hyperceiler.log.db.LogEntry;
import com.sevtinge.hyperceiler.logviewer.LogXposedParseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fan.recyclerview.card.CardGroupAdapter;

public class LogAdapter extends CardGroupAdapter<LogAdapter.LogViewHolder> {

    private List<LogEntry> mData = new ArrayList<>();
    private String mKeyword = "";
    private final Context mContext;

    public LogAdapter(Context context) {
        mContext = context;
    }

    public void setData(List<LogEntry> data) {
        mData.clear();
        mData.addAll(data);
    }

    public void updateData(List<LogEntry> newData, String keyword) {
        mKeyword = keyword;
        // 增量刷新核心逻辑
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return mData.size();
            }

            @Override
            public int getNewListSize() {
                return newData.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return mData.get(oldPos).getId() == newData.get(newPos).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return mData.get(oldPos).equals(newData.get(newPos));
            }
        });
        mData = new ArrayList<>(newData);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.item_log_view, parent, false);
        return new LogViewHolder(v);
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
        holder.onBind(mData.get(position), mKeyword);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvLevel, tvTag, tvMsg, tvTime;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLevel = itemView.findViewById(R.id.textLevel);
            tvTag = itemView.findViewById(R.id.textTag);
            tvMsg = itemView.findViewById(R.id.textMessage);
            tvTime = itemView.findViewById(R.id.textTime);
        }

        public void onBind(LogEntry entry, String keyword) {
            // 设置等级颜色
            tvLevel.setText(entry.getLevel());
            tvLevel.getBackground().setTint(getLogColor(entry.getLevel()));

            tvTime.setText(entry.getFormattedTime());

            // 高亮处理消息和 Tag
            tvTag.setText(HighLightUtils.getHighlightedText(entry.getTag(), keyword));
            tvMsg.setText(HighLightUtils.getHighlightedText(entry.getMessage(), keyword));
        }

        private int getLogColor(String level) {
            for (LogLevelFilter f : LogLevelFilter.values()) {
                if (f.getValue().equals(level)) return f.getColor();
            }
            return 0xFF757575;
        }
    }
}
