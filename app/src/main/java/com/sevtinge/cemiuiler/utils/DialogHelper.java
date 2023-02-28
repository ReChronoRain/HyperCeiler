package com.sevtinge.cemiuiler.utils;

import android.app.Activity;
import android.content.DialogInterface;

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
}
