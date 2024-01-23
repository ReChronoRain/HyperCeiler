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

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.StringRes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SuppressLint("WrongConstant")
public class ToastHelper {
    @IntDef(value = {
        LENGTH_SHORT,
        LENGTH_LONG
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Duration {
    }

    private static Toast mToast;
    public static final int LENGTH_SHORT = 0;
    public static final int LENGTH_LONG = 1;

    public static void makeText(Context context, CharSequence text) {
        clearToast();
        mToast = Toast.makeText(context, text, LENGTH_SHORT);
        mToast.show();
    }

    public static void makeText(Context context, CharSequence text, boolean needClear) {
        if (needClear) clearToast();
        mToast = Toast.makeText(context, text, LENGTH_SHORT);
        mToast.show();
    }

    public static void makeText(Context context, CharSequence text, @Duration int time) {
        clearToast();
        mToast = Toast.makeText(context, text, time);
        mToast.show();
    }

    public static void makeText(Context context, @StringRes int resId) {
        clearToast();
        mToast = Toast.makeText(context, resId, LENGTH_SHORT);
        mToast.show();
    }

    public static void makeText(Context context, @StringRes int resId, @Duration int time) {
        clearToast();
        mToast = Toast.makeText(context, resId, time);
        mToast.show();
    }

    public static void clearToast() {
        if (mToast != null) {
            mToast.cancel();
            mToast = null;
        }
    }
}
