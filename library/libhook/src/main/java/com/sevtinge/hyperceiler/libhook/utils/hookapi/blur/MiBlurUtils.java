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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.hookapi.blur;

import android.graphics.Color;
import android.view.View;

import com.sevtinge.hyperceiler.common.log.AndroidLog;

import io.github.lingqiqi5211.ezhooktool.core.java.Methods;

public class MiBlurUtils {

    public static void setContainerPassBlur(View view, int i, boolean z) {
        if (view == null) {
            AndroidLog.d("MiBlurUtils", "setPassBlur view is null");
            return;
        }
        try {
            boolean passWindowBlurEnabled = setPassWindowBlurEnabled(view, z);
            setMiBackgroundBlurMode(view, 1);
            setMiBackgroundBlurRadius(view, i);
            AndroidLog.i("MiBlurUtils", "setContainerPassBlur result :" + passWindowBlurEnabled + ",view : " + view);
        } catch (Exception e) {
            AndroidLog.e("MiBlurUtils", "setContainerPassBlur error , view :" + view);
            e.printStackTrace();
        }
    }

    public static boolean setPassWindowBlurEnabled(View view, boolean z) {
        return (Boolean) Methods.callMethod(view, "setPassWindowBlurEnabled", z);
    }

    public static void setMiBackgroundBlurMode(View view, int i) {
        Methods.callMethod(view, "setMiBackgroundBlurMode", i);
    }

    public static void setMiBackgroundBlurRadius(View view, int i /* max 500 */) {
        Methods.callMethod(view, "setMiBackgroundBlurRadius", i);
    }

    public static void addMiBackgroundBlendColor(View view, int i, int i2) {
        /*
        i2 =
        101 子view模糊
        103 当前view模糊
        105 当前view和子view都模糊
       */
        Methods.callMethod(view, "addMiBackgroundBlendColor", i, i2);
    }

    public static void clearMiBackgroundBlendColor(View view) {
        Methods.callMethod(view, "clearMiBackgroundBlendColor");
    }

    public static void setMiViewBlurMode(View view, int i) {
        Methods.callMethod(view, "setMiViewBlurMode", i);
    }

    public static void disableMiBackgroundContainBelow(View view, boolean z) {
        Methods.callMethod(view, "disableMiBackgroundContainBelow", z);
    }

    public static void clearContainerPassBlur(View view) {
        if (view == null) {
            AndroidLog.d("MiBlurUtils", "clearContainerMiBackgroundBlur view is null");
            return;
        }
        try {
            setMiBackgroundBlurMode(view, 0);
            boolean passWindowBlurEnabled = setPassWindowBlurEnabled(view, false);
            AndroidLog.d("MiBlurUtils", "clearContainerPassBlur result :" + passWindowBlurEnabled + ", view: " + view);
        } catch (Exception e) {
            AndroidLog.e("MiBlurUtils", "clearContainerMiBackgroundBlur error , view :" + view);
            e.printStackTrace();
        }
    }

    public static void setMemberBlendColor(View view, boolean z, int i) {
        setMemberBlendColor(view, z, i, 255);
    }

    public static void setMemberBlendColor(View view, boolean z, int i, int i2) {
        if (view == null) {
            AndroidLog.d("MiBlurUtils", "setMemberBlendColor view is null");
            return;
        }
        try {
            clearMiBackgroundBlendColor(view);
            setMiViewBlurMode(view, 3);
            int argb = Color.argb(i2, Color.red(i), Color.green(i), Color.blue(i));
            int argb2 = Color.argb(i2, 0, 0, 0);
            addMiBackgroundBlendColor(view, argb, 101);
            if (z) {
                addMiBackgroundBlendColor(view, argb2, 105);
            } else {
                addMiBackgroundBlendColor(view, argb2, 103);
            }
            AndroidLog.i("MiBlurUtils", "setMemberBlendColor: view:" + view + ",colorDark:" + z + ",color:" + Integer.toHexString(argb) + ",labColor:" + Integer.toHexString(argb2));
        } catch (Exception e) {
            AndroidLog.e("MiBlurUtils", "setMemberBlendColor error , view :" + view);
            e.printStackTrace();
        }
    }

    public static void clearMemberBlendColor(View view) {
        if (view == null) {
            AndroidLog.d("MiBlurUtils", "clearMemberBlendColor view is null");
            return;
        }
        try {
            setMiViewBlurMode(view, 0);
            clearMiBackgroundBlendColor(view);
            AndroidLog.d("MiBlurUtils", "clearMemberBlendColor view :" + view);
        } catch (Exception e) {
            AndroidLog.e("MiBlurUtils", "clearMemberBlendColor error , view :" + view);
            e.printStackTrace();
        }
    }
}
