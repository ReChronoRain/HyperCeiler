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
package com.sevtinge.hyperceiler.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.shell.ShellExec;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;
import com.sevtinge.hyperceiler.view.RestartAlertDialog;

import moralnorm.appcompat.app.AlertDialog;

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

    public static void showRestartDialog(Context context) {
        new RestartAlertDialog(context).show();
    }

    public static void showCrashReportDialog(Activity activity, View view) {
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(R.string.safe_mode_title)
                .setView(view)
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(R.string.safe_mode_cancel, (dialog, which) -> {
                    ShellExec shellExec = ShellInit.getShell();
                    shellExec.run("setprop persist.hyperceiler.crash.report \"\"").sync();
                    activity.finish();
                })
                .setNegativeButton(R.string.safe_mode_ok, (dialog, which) -> activity.finish())
                .show();
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
}
