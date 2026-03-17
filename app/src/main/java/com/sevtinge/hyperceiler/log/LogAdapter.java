package com.sevtinge.hyperceiler.log;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.log.db.LogEntry;

import java.util.ArrayList;
import java.util.List;

import fan.recyclerview.card.CardGroupAdapter;

public class LogAdapter extends CardGroupAdapter<LogAdapter.LogViewHolder> {

    private List<LogEntry> mData = new ArrayList<>();
    private String mKeyword = "";
    private final Context mContext;
    private final OnLogClickListener mOnLogClickListener;

    public LogAdapter(Context context, OnLogClickListener onLogClickListener) {
        mContext = context;
        mOnLogClickListener = onLogClickListener;
    }

    public void updateData(List<LogEntry> newData, String keyword) {
        mKeyword = keyword;
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
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_log_view, parent, false);
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
        LogEntry entry = mData.get(position);
        holder.onBind(entry, mKeyword);
        holder.itemView.setOnClickListener(v -> {
            if (mOnLogClickListener != null) {
                mOnLogClickListener.onLogClick(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvLevel;
        private final TextView tvTag;
        private final TextView tvMsg;
        private final TextView tvTime;
        private final TextView tvModule;
        private final int mDefaultMessageColor;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLevel = itemView.findViewById(R.id.textLevel);
            tvTag = itemView.findViewById(R.id.textTag);
            tvMsg = itemView.findViewById(R.id.textMessage);
            tvTime = itemView.findViewById(R.id.textTime);
            tvModule = itemView.findViewById(R.id.textModule);
            mDefaultMessageColor = tvMsg.getCurrentTextColor();
        }

        void onBind(LogEntry entry, String keyword) {
            String level = entry.getLevel();
            String module = entry.getModule();
            String message = entry.getMessage();
            String title = LogDisplayHelper.getListTitle(module, entry.getTag(), message, level);
            String subtitle = LogDisplayHelper.getListSubtitle(module, entry.getTag(), message);
            String body = LogDisplayHelper.getListMessage(module, message, level);
            int badgeColor = ContextCompat.getColor(itemView.getContext(), LogDisplayHelper.getLevelBadgeColorRes(level));
            int badgeTextColor = ContextCompat.getColor(itemView.getContext(), LogDisplayHelper.getLevelTextColorRes(level));

            tvLevel.setText(level == null ? "" : level);
            tvLevel.getBackground().mutate().setTint(badgeColor);
            tvLevel.setTextColor(badgeTextColor);

            tvTag.setText(HighLightUtils.getHighlightedText(title, keyword));
            tvTime.setText(entry.getFormattedTime());
            if (subtitle == null || subtitle.isEmpty()) {
                tvModule.setVisibility(View.GONE);
            } else {
                tvModule.setVisibility(View.VISIBLE);
                tvModule.setText(HighLightUtils.getHighlightedText(subtitle, keyword));
            }
            tvMsg.setText(HighLightUtils.getHighlightedText(body, keyword));
            tvMsg.setTextColor("C".equals(level) ? badgeColor : mDefaultMessageColor);
        }
    }

    public interface OnLogClickListener {
        void onLogClick(LogEntry entry);
    }
}
