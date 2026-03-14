package com.sevtinge.hyperceiler.logviewer;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;

public class LogViewHolder extends RecyclerView.ViewHolder {

    private final TextView mTimeTextView;
    private final TextView mLevelTextView;
    private final TextView mModuleTextView;
    private final TextView mMessageTextView;
    private final TextView mPrimaryTextView;
    private final TextView mSecondaryTextView;
    private final StringBuilder mTempBuilder = new StringBuilder();

    private final int mDefaultMessageColor;
    private final int mDefaultSecondaryColor;

    // 颜色配置
    private static final int sDefaultTextColor = Color.BLACK;
    private static final int sSearchHighlightColor = Color.RED;

    private static final String[] LEVEL_LABELS = {"C", "E", "W", "I", "D"};
    private static final String[] LEVEL_DISPLAY = {"CRASH", "ERROR", "WARN", "INFO", "DEBUG"};

    public LogViewHolder(@NonNull View itemView) {
        super(itemView);
        mTimeTextView = itemView.findViewById(R.id.textTime);
        mLevelTextView = itemView.findViewById(R.id.textLevel);
        mModuleTextView = itemView.findViewById(R.id.textModule);
        mMessageTextView = itemView.findViewById(R.id.textMessage);
        mPrimaryTextView = itemView.findViewById(R.id.textPrimary);
        mSecondaryTextView = itemView.findViewById(R.id.textSecondary);
        mDefaultMessageColor = mMessageTextView.getCurrentTextColor();
        mDefaultSecondaryColor = mSecondaryTextView.getCurrentTextColor();
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
        mLevelTextView.getBackground().setTint(LogXposedParseHelper.getLevelBadgeColor(level));
        mLevelTextView.setTextColor(LogXposedParseHelper.getLevelTextColor(level));

        // 消息内容
        String message = logEntry.getMessage();
        boolean isXposed = "Xposed".equals(logEntry.getModule());

        if (isXposed) {
            mMessageTextView.setVisibility(View.GONE);
            String[] parsed = LogXposedParseHelper.parseXposedDisplay(message, level);
            String primary = parsed[0];
            String secondary = parsed[1];

            // 主要内容
            if (primary != null && !primary.isEmpty()) {
                mPrimaryTextView.setVisibility(View.VISIBLE);
                mPrimaryTextView.setTextColor(mDefaultMessageColor);
                if (!TextUtils.isEmpty(searchKeyword) &&
                    primary.toLowerCase().contains(searchKeyword.toLowerCase())) {
                    mPrimaryTextView.setText(highlightText(primary, searchKeyword));
                } else {
                    mPrimaryTextView.setText(primary);
                }
            } else {
                mPrimaryTextView.setVisibility(View.GONE);
            }

            // 次要内容
            if (secondary != null && !secondary.isEmpty()) {
                mSecondaryTextView.setVisibility(View.VISIBLE);
                if ("C".equals(level)) {
                    mSecondaryTextView.setTextColor(0xFFD32F2F);
                    mSecondaryTextView.setTypeface(android.graphics.Typeface.MONOSPACE,
                        android.graphics.Typeface.BOLD);
                    mSecondaryTextView.setMaxLines(4);
                } else {
                    mSecondaryTextView.setTextColor(mDefaultSecondaryColor);
                    mSecondaryTextView.setTypeface(android.graphics.Typeface.MONOSPACE,
                        android.graphics.Typeface.NORMAL);
                    mSecondaryTextView.setMaxLines(3);
                }
                if (!TextUtils.isEmpty(searchKeyword) &&
                    secondary.toLowerCase().contains(searchKeyword.toLowerCase())) {
                    mSecondaryTextView.setText(highlightText(secondary, searchKeyword));
                } else {
                    mSecondaryTextView.setText(secondary);
                }
            } else {
                mSecondaryTextView.setVisibility(View.GONE);
            }
        } else {
            // App 日志：原始行为
            mPrimaryTextView.setVisibility(View.GONE);
            mSecondaryTextView.setVisibility(View.GONE);
            mMessageTextView.setVisibility(View.VISIBLE);

            if ("C".equals(level)) {
                mMessageTextView.setTextColor(0xFFD32F2F);
            } else {
                mMessageTextView.setTextColor(mDefaultMessageColor);
            }

            if (!TextUtils.isEmpty(searchKeyword) && message != null &&
                message.toLowerCase().contains(searchKeyword.toLowerCase())) {
                mMessageTextView.setText(highlightText(message, searchKeyword));
            } else {
                mMessageTextView.setText(message);
            }
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
                keywordIndex, endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            startIndex = endIndex;
        }
        return spannable;
    }
}
