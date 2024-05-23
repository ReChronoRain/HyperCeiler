package com.sevtinge.hyperceiler.utils;

import android.content.Context;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import fan.appcompat.app.AlertDialog;

public class AppCrashHelper {

    public static void showErrorDialog(Context context) {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(R.string.tip)
                .setMessage(R.string.tip)
                .setHapticFeedbackEnabled(true)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    public static void showCrashReportDialog(Context context, String appCrash) {
        CharSequence msg = String.format(context.getString(R.string.safe_mode_later_desc), appCrash);
        new AlertDialog.Builder(context)
                .setTitle(R.string.safe_mode_later_title)
                .setMessage(msg)
                .setHapticFeedbackEnabled(true)
                .setCancelable(false)
                .setPositiveButton(R.string.safe_mode_cancel, (dialog, which) -> {
                    ShellInit.getShell().run("setprop persist.hyperceiler.crash.report \"\"").sync();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.safe_mode_ok, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
