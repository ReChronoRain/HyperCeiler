package com.sevtinge.cemiuiler.utils;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.StringRes;

public class ToastHelper {

    public static void makeText(Context context, CharSequence text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void makeText(Context context, @StringRes int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }
}
