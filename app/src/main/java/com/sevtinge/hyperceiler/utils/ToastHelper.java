package com.sevtinge.hyperceiler.utils;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.StringRes;

public class ToastHelper {
    private static Toast mToast;

    public static void makeText(Context context, CharSequence text) {
        clearToast();
        mToast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        mToast.show();
    }

    public static void makeText(Context context, @StringRes int resId) {
        clearToast();
        mToast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        mToast.show();
    }

    public static void clearToast() {
        if (mToast != null) {
            mToast.cancel();
            mToast = null;
        }
    }
}
