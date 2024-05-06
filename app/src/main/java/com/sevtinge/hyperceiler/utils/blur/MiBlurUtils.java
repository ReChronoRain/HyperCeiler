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
package com.sevtinge.hyperceiler.utils.blur;

import android.graphics.Color;
import android.util.Log;
import android.view.View;

import com.sevtinge.hyperceiler.utils.InvokeUtils;

public class MiBlurUtils {

    public static void setContainerPassBlur(View view, int i) {
        if (view == null) {
            Log.d("MiBlurUtils", "setPassBlur view is null");
            return;
        }
        try {
            boolean passWindowBlurEnabled = setPassWindowBlurEnabled(view, false);
            setMiBackgroundBlurMode(view, 1);
            setMiBackgroundBlurRadius(view, i);
            Log.i("MiBlurUtils", "setContainerPassBlur result :" + passWindowBlurEnabled + ",view : " + view);
        } catch (Exception e) {
            Log.e("MiBlurUtils", "setContainerPassBlur error , view :" + view);
            e.printStackTrace();
        }
    }

    public static boolean setPassWindowBlurEnabled(View view, boolean z) {
        return InvokeUtils.callMethod(View.class, view, "setPassWindowBlurEnabled", new Class[]{boolean.class}, z);
    }

    public static void setMiBackgroundBlurMode(View view, int i) {
        InvokeUtils.callMethod(View.class, view, "setMiBackgroundBlurMode", new Class[]{int.class}, i);
    }

    public static void setMiBackgroundBlurRadius(View view, int i) {
        InvokeUtils.callMethod(View.class, view, "setMiBackgroundBlurRadius", new Class[]{int.class}, i);
    }

    public static void addMiBackgroundBlendColor(View view, int i, int i2) {
        /*
        i2 =
        101 子view模糊
        103 当前view模糊
        105 当前view和子view都模糊
       */
        InvokeUtils.callMethod(View.class, view, "addMiBackgroundBlendColor", new Class[]{int.class, int.class}, i, i2);
    }

    public static void clearMiBackgroundBlendColor(View view) {
        InvokeUtils.callMethod(View.class, view, "clearMiBackgroundBlendColor", new Class[]{});
    }

    public static void setMiViewBlurMode(View view, int i) {
        InvokeUtils.callMethod(View.class, view, "setMiViewBlurMode", new Class[]{int.class}, i);
    }

    public static void disableMiBackgroundContainBelow(View view, boolean z) {
        InvokeUtils.callMethod(View.class, view, "disableMiBackgroundContainBelow", new Class[]{boolean.class}, z);
    }

    public static void clearContainerPassBlur(View view) {
        if (view == null) {
            Log.d("MiBlurUtils", "clearContainerMiBackgroundBlur view is null");
            return;
        }
        try {
            setMiBackgroundBlurMode(view, 0);
            boolean passWindowBlurEnabled = setPassWindowBlurEnabled(view, false);
            Log.d("MiBlurUtils", "clearContainerPassBlur result :" + passWindowBlurEnabled + ", view: " + view);
        } catch (Exception e) {
            Log.e("MiBlurUtils", "clearContainerMiBackgroundBlur error , view :" + view);
            e.printStackTrace();
        }
    }

    public static void setMemberBlendColor(View view, boolean z, int i) {
        setMemberBlendColor(view, z, i, 255);
    }

    public static void setMemberBlendColor(View view, boolean z, int i, int i2) {
        if (view == null) {
            Log.d("MiBlurUtils", "setMemberBlendColor view is null");
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
            Log.i("MiBlurUtils", "setMemberBlendColor: view:" + view + ",colorDark:" + z + ",color:" + Integer.toHexString(argb) + ",labColor:" + Integer.toHexString(argb2));
        } catch (Exception e) {
            Log.e("MiBlurUtils", "setMemberBlendColor error , view :" + view);
            e.printStackTrace();
        }
    }

    public static void clearMemberBlendColor(View view) {
        if (view == null) {
            Log.d("MiBlurUtils", "clearMemberBlendColor view is null");
            return;
        }
        try {
            setMiViewBlurMode(view, 0);
            clearMiBackgroundBlendColor(view);
            Log.d("MiBlurUtils", "clearMemberBlendColor view :" + view);
        } catch (Exception e) {
            Log.e("MiBlurUtils", "clearMemberBlendColor error , view :" + view);
            e.printStackTrace();
        }
    }
}
