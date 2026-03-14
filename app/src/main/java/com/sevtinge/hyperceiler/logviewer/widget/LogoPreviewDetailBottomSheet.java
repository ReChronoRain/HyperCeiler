package com.sevtinge.hyperceiler.logviewer.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.logviewer.LogAdapter;
import com.sevtinge.hyperceiler.logviewer.LogEntry;
import com.sevtinge.hyperceiler.logviewer.LogViewHolder;
import com.sevtinge.hyperceiler.logviewer.LogXposedParseHelper;

import fan.bottomsheet.BottomSheetBehavior;
import fan.bottomsheet.BottomSheetModal;
import fan.core.utils.HyperMaterialUtils;
import fan.core.utils.RomUtils;

public class LogoPreviewDetailBottomSheet extends BottomSheetModal {
    public LogoPreviewDetailBottomSheet(Activity activity) {
        super(activity);
        init(activity);
    }


    private final void init(Activity activity) {
        setDragHandleViewEnabled(true);
        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        behavior.setDraggable(false);
        behavior.setSkipHalfExpanded(true);
        behavior.setSkipCollapsed(false);
        behavior.setForceFullHeight(false);
        behavior.setPeekHeight(-1);

        setCanceledOnTouchOutside(false);
        setDragHandleViewEnabled(true);

        if (HyperMaterialUtils.isFeatureEnable(activity) && RomUtils.getHyperOsVersion() >= 2) {
            behavior.setModeConfig(0);
            applyBlur(true);
        }
    }

    public void initView(Context context, LogEntry logEntry, View.OnClickListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_log_detail, null);

        TextView titleView = view.findViewById(android.R.id.title);
        View cancelButton = view.findViewById(R.id.cancel_button);
        View confirmButton = view.findViewById(R.id.confirm_button);

        titleView.setText(R.string.log_detail_title);

        confirmButton.setVisibility(View.INVISIBLE);
        cancelButton.setOnClickListener(v -> dismiss());

        TextView timeView = view.findViewById(R.id.dialog_time);
        TextView tagView = view.findViewById(R.id.dialog_tag);
        TextView levelView = view.findViewById(R.id.dialog_level);
        TextView primaryView = view.findViewById(R.id.dialog_primary);
        TextView messageView = view.findViewById(R.id.dialog_message);

        // 时间
        timeView.setText(logEntry.getFormattedTime());

        // TAG 徽标
        tagView.setText(logEntry.getTag());
        tagView.getBackground().setTint(LogXposedParseHelper.getLevelBadgeColor("V"));
        tagView.setTextColor(LogXposedParseHelper.getLevelTextColor("V"));

        // 级别徽标
        String level = logEntry.getLevel();
        String[] labels = {"C", "E", "W", "I", "D"};
        String[] display = {"CRASH", "ERROR", "WARN", "INFO", "DEBUG"};
        String displayLevel = level;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(level)) { displayLevel = display[i]; break; }
        }
        levelView.setText(displayLevel);
        levelView.getBackground().setTint(LogXposedParseHelper.getLevelBadgeColor(level));
        levelView.setTextColor(LogXposedParseHelper.getLevelTextColor(level));

        // 消息内容
        String message = logEntry.getMessage();
        boolean isXposed = "Xposed".equals(logEntry.getModule());

        if (isXposed) {
            String[] parsed = LogXposedParseHelper.parseXposedDisplay(message, level);
            String primary = parsed[0];
            String secondary = parsed[1];

            if (primary != null && !primary.isEmpty()) {
                primaryView.setVisibility(View.VISIBLE);
                primaryView.setText(primary);
            } else {
                primaryView.setVisibility(View.GONE);
            }

            messageView.setText(secondary);
            if ("C".equals(level)) {
                primaryView.setVisibility(View.GONE);
                messageView.setTextColor(0xFFD32F2F);
                messageView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            }
        } else {
            primaryView.setVisibility(View.GONE);
            messageView.setText(message);
            if ("C".equals(level)) {
                messageView.setTextColor(0xFFD32F2F);
            }
        }

        View copyBtn = view.findViewById(R.id.log_copy);
        copyBtn.setOnClickListener(listener);
        setContentView(view);
    }
}
