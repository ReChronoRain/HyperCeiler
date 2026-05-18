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

/**
 * Java 兼容入口，全部实现委托给 {@link MiBlurUtilsKt}（基于反射缓存）。
 *
 * <p>历史上这里用 EzHookTool 的 {@code Methods.callMethod} 做反射，但每次调用都要重新查方法，
 * 还要手动写 parameterTypes 数组，重复且容易出错。
 * 现在统一走 {@link MiBlurUtilsKt}：方法对象 lazy 缓存，签名维护在一处。</p>
 */
public class MiBlurUtils {

    private static final String TAG = "MiBlurUtils";

    private static final MiBlurUtilsKt KT = MiBlurUtilsKt.INSTANCE;

    private MiBlurUtils() {
    }

    public static void setContainerPassBlur(View view, int radius, boolean passWindowBlur) {
        if (view == null) {
            AndroidLog.d(TAG, "setPassBlur view is null");
            return;
        }
        try {
            setPassWindowBlurEnabled(view, passWindowBlur);
            setMiBackgroundBlurMode(view, 1);
            setMiBackgroundBlurRadius(view, radius);
            AndroidLog.i(TAG, "setContainerPassBlur view: " + view);
        } catch (Throwable e) {
            AndroidLog.e(TAG, "setContainerPassBlur error , view :" + view);
            e.printStackTrace();
        }
    }

    public static boolean setPassWindowBlurEnabled(View view, boolean enabled) {
        if (view == null) return false;
        try {
            KT.setPassWindowBlurEnabled(view, enabled);
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void setMiBackgroundBlurMode(View view, int mode) {
        if (view == null) return;
        try {
            KT.setMiBackgroundBlurMode(view, mode);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void setMiBackgroundBlurRadius(View view, int radius /* max 500 */) {
        if (view == null) return;
        try {
            KT.setMiBackgroundBlurRadius(view, radius);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void addMiBackgroundBlendColor(View view, int color, int mode) {
        /*
         * mode:
         * 101 子 view 模糊
         * 103 当前 view 模糊
         * 105 当前 view 和子 view 都模糊
         */
        if (view == null) return;
        try {
            KT.addMiBackgroundBlendColor(view, color, mode);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void clearMiBackgroundBlendColor(View view) {
        if (view == null) return;
        try {
            KT.clearMiBackgroundBlendColor(view);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void setMiViewBlurMode(View view, int mode) {
        if (view == null) return;
        try {
            KT.setMiViewBlurMode(view, mode);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void disableMiBackgroundContainBelow(View view, boolean disabled) {
        if (view == null) return;
        try {
            KT.disableMiBackgroundContainBelow(view, disabled);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void clearContainerPassBlur(View view) {
        if (view == null) {
            AndroidLog.d(TAG, "clearContainerMiBackgroundBlur view is null");
            return;
        }
        try {
            setMiBackgroundBlurMode(view, 0);
            setPassWindowBlurEnabled(view, false);
            AndroidLog.d(TAG, "clearContainerPassBlur view: " + view);
        } catch (Throwable e) {
            AndroidLog.e(TAG, "clearContainerMiBackgroundBlur error , view :" + view);
            e.printStackTrace();
        }
    }

    public static void setMemberBlendColor(View view, boolean colorDark, int color) {
        setMemberBlendColor(view, colorDark, color, 255);
    }

    public static void setMemberBlendColor(View view, boolean colorDark, int color, int alpha) {
        if (view == null) {
            AndroidLog.d(TAG, "setMemberBlendColor view is null");
            return;
        }
        try {
            clearMiBackgroundBlendColor(view);
            setMiViewBlurMode(view, 3);
            int argb = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
            int labColor = Color.argb(alpha, 0, 0, 0);
            addMiBackgroundBlendColor(view, argb, 101);
            addMiBackgroundBlendColor(view, labColor, colorDark ? 105 : 103);
            AndroidLog.i(TAG, "setMemberBlendColor: view:" + view
                    + ",colorDark:" + colorDark
                    + ",color:" + Integer.toHexString(argb)
                    + ",labColor:" + Integer.toHexString(labColor));
        } catch (Throwable e) {
            AndroidLog.e(TAG, "setMemberBlendColor error , view :" + view);
            e.printStackTrace();
        }
    }

    public static void clearMemberBlendColor(View view) {
        if (view == null) {
            AndroidLog.d(TAG, "clearMemberBlendColor view is null");
            return;
        }
        try {
            setMiViewBlurMode(view, 0);
            clearMiBackgroundBlendColor(view);
            AndroidLog.d(TAG, "clearMemberBlendColor view :" + view);
        } catch (Throwable e) {
            AndroidLog.e(TAG, "clearMemberBlendColor error , view :" + view);
            e.printStackTrace();
        }
    }
}
