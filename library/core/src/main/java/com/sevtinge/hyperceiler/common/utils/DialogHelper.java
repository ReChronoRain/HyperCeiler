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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.common.utils;

import static com.sevtinge.hyperceiler.common.utils.CtaUtils.setCtaValue;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils.dp2px;
import static com.sevtinge.hyperceiler.hook.utils.log.LogManager.LOGGER_CHECKER_ERR_CODE;
import static com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.checkRootPermission;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.common.view.RestartAlertDialog;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.hook.utils.shell.ShellExec;
import com.sevtinge.hyperceiler.hook.utils.shell.ShellInit;

import fan.androidbase.widget.LinkMovementMethod;
import fan.appcompat.app.AlertDialog;

public class DialogHelper {

    public static void showDialog(Activity activity, String title, String message) {
        showDialog(activity, title, message, null);
    }

    public static void showDialog(Activity activity, String title, String message, DialogInterface.OnClickListener onClickListener) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void showDialog(Activity activity, int title, int message, DialogInterface.OnClickListener onClickListener) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void showDialog(Activity activity, int title, int message, DialogInterface.OnClickListener onClickListener, DialogInterface.OnClickListener onClickListener2) {
        new AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, onClickListener)
            .setNegativeButton(android.R.string.cancel, onClickListener2)
            .show();
    }


    public static void showPositiveButtonDialog(Activity activity, String title, String message, DialogInterface.OnClickListener onClickListener) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, onClickListener)
                .show();
    }

    public static void showXposedActivateDialog(Context context) {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(R.string.tip)
                .setMessage(R.string.hook_failed)
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(R.string.exit, (dialogInterface, i) -> System.exit(0))
                .setNegativeButton(R.string.ignore, null)
                .show();
    }

    public static void showUserAgreeDialog(Context context) {
        int textColor = ContextCompat.getColor(context, R.color.textview_black);
        int linkColor = ContextCompat.getColor(context, R.color.textview_blue);

        CharSequence raw = context.getText(R.string.new_cta_app_all_purpose_title);
        SpannableString ss = new SpannableString(raw);

        // 全文颜色
        // 也可用 msgView.setTextColor(textColor)
        ss.setSpan(new ForegroundColorSpan(textColor), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        Annotation[] anns = ss.getSpans(0, ss.length(), Annotation.class);
        for (Annotation an : anns) {
            int start = ss.getSpanStart(an);
            int end = ss.getSpanEnd(an);
            String key = an.getValue(); // "protocol" or "privacy"
            ss.removeSpan(an);

            ClickableSpan span;
            if ("protocol".equals(key)) {
                span = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://hyperceiler.sevtinge.com/Protocol")));
                    }
                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        ds.setColor(linkColor);
                        ds.setUnderlineText(true);
                    }
                };
            } else if ("privacy".equals(key)) {
                span = new ClickableSpan() {
                    @Override public void onClick(@NonNull View widget) {
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://hyperceiler.sevtinge.com/Privacy")));
                    }
                    @Override public void updateDrawState(@NonNull TextPaint ds) {
                        ds.setColor(linkColor);
                        ds.setUnderlineText(true);
                    }
                };
            } else {
                continue;
            }
            ss.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        TextView msgView = new TextView(context);
        msgView.setText(ss);
        msgView.setMovementMethod(LinkMovementMethod.getInstance());
        msgView.setPadding(dp2px(context, 24), dp2px(context, 12), dp2px(context, 24), dp2px(context, 24));
        msgView.setTextSize(16);

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(R.string.new_cta_app_all_purpose_welcome)
            .setView(msgView)
            .setPositiveButton(R.string.new_cta_app_all_purpose_agree, (d, w) -> setCtaValue(context, true))
            .setNegativeButton(R.string.new_cta_app_all_purpose_reject, (d, w) -> System.exit(0))
            .create();
        dialog.show();
    }

    public static void showCrashReportDialog(Activity activity, View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(true);
        builder.setTitle(R.string.warn);
        builder.setView(view);
        builder.setHapticFeedbackEnabled(true);
        builder.setPositiveButton(R.string.safe_mode_cancel, (dialog, which) -> {
                    ShellExec shellExec = ShellInit.getShell();
                    shellExec.run("setprop persist.service.hyperceiler.crash.report \"\"").sync();
                    activity.finish();
                });
        builder.setNegativeButton(R.string.safe_mode_ok, (dialog, which) -> activity.finish());

        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(dialogInterface -> activity.finish());
        dialog.show();
    }

    public static void showCrashMsgDialog(Context context, String throwClassName, String throwFileName,
                                          int throwLineNumber, String throwMethodName, String longMsg, String stackTrace) {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(R.string.safe_mode_recorder_title)
                .setMessage(
                        "[" + context.getString(R.string.safe_mode_recorder_file) + "]: " + throwFileName +
                        "\n[" + context.getString(R.string.safe_mode_recorder_class) + "]: " + throwClassName +
                        "\n[" + context.getString(R.string.safe_mode_recorder_method) + "]: " + throwMethodName +
                        "\n[" + context.getString(R.string.safe_mode_recorder_line) + "]: " + throwLineNumber +
                        "\n[" + context.getString(R.string.safe_mode_recorder_msg) + "]: " + longMsg +
                        "\n[" + context.getString(R.string.safe_mode_recorder_st) + "]: " + stackTrace)
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }


    public static void showLogServiceWarnDialog(Context context) {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(R.string.warn)
                .setMessage(context.getResources().getString(R.string.headtip_notice_dead_logger_errcode, LOGGER_CHECKER_ERR_CODE))
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public static void showSafeModeDialog(Context context, String msg) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.safe_mode_later_title)
                .setMessage(msg)
                .setHapticFeedbackEnabled(true)
                .setCancelable(false)
                .setPositiveButton(R.string.safe_mode_cancel, (dialog, which) -> {
                    ShellInit.getShell().run("setprop persist.service.hyperceiler.crash.report \"\"").sync();
                    PrefsUtils.mSharedPreferences.edit().remove("prefs_key_system_ui_safe_mode_enable").apply();
                    PrefsUtils.mSharedPreferences.edit().remove("prefs_key_home_safe_mode_enable").apply();
                    PrefsUtils.mSharedPreferences.edit().remove("prefs_key_system_settings_safe_mode_enable").apply();
                    PrefsUtils.mSharedPreferences.edit().remove("prefs_key_security_center_safe_mode_enable").apply();
                    PrefsUtils.mSharedPreferences.edit().remove("prefs_key_demo_safe_mode_enable").apply();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.safe_mode_ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static void showNoRootPermissionDialog(Context context) {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(R.string.tip)
                .setMessage(R.string.root)
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public static void showRestartDialog(Context context) {
        new RestartAlertDialog(context).show();
    }

    public static void showRestartDialog(Context context, String packageName) {
        String appLabel = PackagesUtils.getAppLabel(context, packageName);
        showRestartDialog(context, false, appLabel, new String[]{packageName});
    }

    public static void showRestartDialog(Context context, String appLabel, String packageName) {
        showRestartDialog(context, false, appLabel, new String[]{packageName});
    }

    public static void showRestartDialog(Context context, String appLabel, String[] packageName) {
        showRestartDialog(context, false, appLabel, packageName);
    }

    public static void showRestartSystemDialog(Context context) {
        showRestartDialog(context, true, "", new String[]{""});
    }

    public static void showRestartDialog(Context context, boolean isRestartSystem, String appLabel, String[] packageName) {
        String isSystem = context.getResources().getString(R.string.restart_app_desc, appLabel);
        String isOther = context.getResources().getString(R.string.restart_app_desc, " " + appLabel + " ");

        isSystem = isSystem.replace("  ", " ");
        isOther = isOther.replace("  ", " ");

        isSystem = isSystem.replaceAll("^\\s+|\\s+$", "");
        isOther = isOther.replaceAll("^\\s+|\\s+$", "");

        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(context.getResources().getString(R.string.soft_reboot) + " " + appLabel)
                .setMessage(isRestartSystem ? isSystem : isOther)
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                        doRestart(context, packageName, isRestartSystem)
                )
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void doRestart(Context context, String[] packageName, boolean isRestartSystem) {
        boolean result;
        boolean pid = true;

        if (isRestartSystem) {
            result = ShellInit.getShell().run("reboot").sync().isResult();
        } else {
            if (checkRootPermission() != 0) {
                result = false;
            } else {
                result = AppsTool.killApps(packageName);
                pid = result;
            }
        }

        if (!result) {
            showAlertDialog(context, isRestartSystem, pid);
        }
    }

    public static void showAlertDialog(Context context, boolean isRestartSystem, boolean pid) {
        new AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(R.string.tip)
            .setMessage(getErrorMessage(isRestartSystem, pid))
            .setHapticFeedbackEnabled(true)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private static int getErrorMessage(boolean isRestartSystem, boolean pid) {
        if (isRestartSystem) {
            return R.string.reboot_failed;
        } else {
            return pid ? R.string.kill_failed : R.string.pid_failed;
        }
    }
}
