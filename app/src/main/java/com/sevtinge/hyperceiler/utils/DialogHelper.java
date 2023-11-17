package com.sevtinge.hyperceiler.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;

import com.sevtinge.hyperceiler.R;
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

}
