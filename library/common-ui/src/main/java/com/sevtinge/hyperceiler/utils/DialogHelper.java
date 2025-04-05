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
package com.sevtinge.hyperceiler.utils;

import static com.sevtinge.hyperceiler.utils.log.LogManager.LOGGER_CHECKER_ERR_CODE;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.module.base.tool.AppsTool;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.utils.shell.ShellExec;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;
import com.sevtinge.hyperceiler.view.RestartAlertDialog;

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

    public static void showCrashReportDialog(Activity activity, View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(true);
        builder.setTitle(R.string.warn);
        builder.setView(view);
        builder.setHapticFeedbackEnabled(true);
        builder.setPositiveButton(R.string.safe_mode_cancel, (dialog, which) -> {
                    ShellExec shellExec = ShellInit.getShell();
                    shellExec.run("setprop persist.hyperceiler.crash.report \"\"").sync();
                    activity.finish();
                });
        builder.setNegativeButton(R.string.safe_mode_ok, (dialog, which) -> activity.finish());

        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(dialogInterface -> {
            activity.finish();
        });
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
                    ShellInit.getShell().run("setprop persist.hyperceiler.crash.report \"\"").sync();
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
            result = AppsTool.handlePackages(packageName);
            pid = result;
        }

        if (!result) {
            showAlertDialog(context, isRestartSystem, pid);
        }
    }

    private static void showAlertDialog(Context context, boolean isRestartSystem, boolean pid) {
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
