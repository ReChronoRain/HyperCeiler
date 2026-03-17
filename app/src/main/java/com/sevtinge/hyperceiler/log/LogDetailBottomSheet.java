/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.log;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.log.db.LogEntry;

import fan.bottomsheet.BottomSheetBehavior;
import fan.bottomsheet.BottomSheetModal;
import fan.core.utils.HyperMaterialUtils;
import fan.core.utils.RomUtils;

public class LogDetailBottomSheet extends BottomSheetModal {

    private final Activity mActivity;

    public LogDetailBottomSheet(Activity activity) {
        super(activity);
        mActivity = activity;
        initBehavior(activity);
    }

    private void initBehavior(Activity activity) {
        setDragHandleViewEnabled(true);
        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        behavior.setDraggable(false);
        behavior.setSkipHalfExpanded(true);
        behavior.setSkipCollapsed(false);
        behavior.setForceFullHeight(false);
        behavior.setPeekHeight(-1);

        setCanceledOnTouchOutside(false);
        if (HyperMaterialUtils.isFeatureEnable(activity) && RomUtils.getHyperOsVersion() >= 2) {
            behavior.setModeConfig(0);
            applyBlur(true);
        }
    }

    public void showRecord(LogEntry entry) {
        View view = LayoutInflater.from(mActivity).inflate(R.layout.dialog_log_detail, null);

        TextView titleView = view.findViewById(android.R.id.title);
        View cancelButton = view.findViewById(R.id.cancel_button);
        View confirmButton = view.findViewById(R.id.confirm_button);
        TextView timeView = view.findViewById(R.id.dialog_time);
        TextView processView = view.findViewById(R.id.dialog_process);
        TextView tagView = view.findViewById(R.id.dialog_tag);
        TextView levelView = view.findViewById(R.id.dialog_level);
        TextView primaryView = view.findViewById(R.id.dialog_primary);
        TextView messageView = view.findViewById(R.id.dialog_message);
        View copyButton = view.findViewById(R.id.log_copy);

        titleView.setText(R.string.log_detail_title);
        confirmButton.setVisibility(View.INVISIBLE);
        cancelButton.setOnClickListener(v -> dismiss());

        timeView.setText(entry.getFormattedTime());
        tagView.setText(entry.getTag());
        tagView.getBackground().mutate().setTint(ContextCompat.getColor(mActivity, LogDisplayHelper.getLevelBadgeColorRes("V")));
        tagView.setTextColor(ContextCompat.getColor(mActivity, LogDisplayHelper.getLevelTextColorRes("V")));

        String level = entry.getLevel();
        int levelBadgeColor = ContextCompat.getColor(mActivity, LogDisplayHelper.getLevelBadgeColorRes(level));
        int levelTextColor = ContextCompat.getColor(mActivity, LogDisplayHelper.getLevelTextColorRes(level));
        levelView.setText(LogDisplayHelper.getDisplayLevel(level));
        levelView.getBackground().mutate().setTint(levelBadgeColor);
        levelView.setTextColor(levelTextColor);

        String message = entry.getMessage();
        boolean isXposed = "Xposed".equals(entry.getModule());
        String uidPid = isXposed ? LogDisplayHelper.extractUidPid(message) : "";
        if (uidPid.isEmpty()) {
            processView.setVisibility(View.GONE);
        } else {
            processView.setVisibility(View.VISIBLE);
            processView.setText(uidPid);
        }
        if (isXposed) {
            String[] parsed = LogDisplayHelper.parseXposedDisplay(message, level);
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
                messageView.setTextColor(levelBadgeColor);
                messageView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            } else {
                messageView.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            }
        } else {
            primaryView.setVisibility(View.GONE);
            messageView.setText(message);
            if ("C".equals(level)) {
                messageView.setTextColor(levelBadgeColor);
                messageView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            } else {
                messageView.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            }
        }

        copyButton.setOnClickListener(v -> copyLogEntry(entry));

        setContentView(view);
        show();
    }

    private void copyLogEntry(LogEntry entry) {
        ClipboardManager clipboardManager = (ClipboardManager) mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }

        ClipData clipData = ClipData.newPlainText(
            mActivity.getString(R.string.log_detail_title),
            LogManager.formatLogEntryForCopy(entry)
        );
        clipboardManager.setPrimaryClip(clipData);
        Toast.makeText(mActivity, R.string.log_copy_success, Toast.LENGTH_SHORT).show();
    }
}
