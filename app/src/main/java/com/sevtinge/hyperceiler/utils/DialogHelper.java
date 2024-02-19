package com.sevtinge.hyperceiler.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;
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

    public static void showCrashReportDialog(Activity activity, String pkg, View view) {
        new AlertDialog.Builder(activity)
            .setCancelable(false)
            .setTitle("警告")
            .setView(view)
            .setHapticFeedbackEnabled(true)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                ShellUtils.OpenShellExecWindow open = new ShellUtils.OpenShellExecWindow(
                    "setprop persist.hyperceiler.crash.report \"[]\"", true, true) {
                    @Override
                    public void readOutput(String out, String type) {
                        AndroidLogUtils.LogI(ITAG.TAG, "D O: " + out + " T: " + type);
                    }
                };
                open.append("settings put system hyperceiler_crash_report \"[]\"");
                open.getResult();
                open.close();
                activity.finish();
            })
            .show();
    }
}
