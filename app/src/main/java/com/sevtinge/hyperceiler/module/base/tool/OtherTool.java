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
package com.sevtinge.hyperceiler.module.base.tool;

import static com.sevtinge.hyperceiler.module.base.tool.HookTool.mPrefsMap;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.isDarkMode;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.ContextUtils;
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtils;

import java.io.File;
import java.lang.ref.WeakReference;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class OtherTool {
    // 尝试全部
    public static final int FLAG_ALL = ContextUtils.FLAG_ALL;
    // 仅获取当前应用
    public static final int FLAG_CURRENT_APP = ContextUtils.FLAG_CURRENT_APP;
    // 获取 Android 系统
    public static final int FlAG_ONLY_ANDROID = ContextUtils.FlAG_ONLY_ANDROID;

    // public static TextView mPct = null;
    public static WeakReference<TextView> mPct;

    // public  Context mModuleContext = null;

    public static void setTextView(TextView textView) {
        mPct = new WeakReference<>(textView);
    }

    public static TextView getTextView() {
        return mPct != null ? mPct.get() : null;
    }

    public static Resources getModuleRes(Context context)
            throws PackageManager.NameNotFoundException {
        return ResourcesTool.loadModuleRes(context);
    }

    public static Context findContext(@ContextUtils.Duration int flag) {
        Context context = null;
        try {
            switch (flag) {
                case FLAG_ALL -> {
                    if ((context = currentApplication()) == null)
                        context = getSystemContext();
                }
                case FLAG_CURRENT_APP -> context = currentApplication();
                case FlAG_ONLY_ANDROID -> context = getSystemContext();
                default -> {
                }
            }
            return context;
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static Context currentApplication() {
        return (Application) XposedHelpers.callStaticMethod(XposedHelpers.findClass(
                        "android.app.ActivityThread", null),
                "currentApplication");
    }

    private static Context getSystemContext() {
        Context context = null;
        Object currentActivityThread = XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread",
                        null),
                "currentActivityThread");
        if (currentActivityThread != null)
            context = (Context) XposedHelpers.callMethod(currentActivityThread,
                    "getSystemContext");
        if (context == null)
            context = (Context) XposedHelpers.callMethod(currentActivityThread,
                    "getSystemUiContext");
        return context;
    }

    public static String getProp(String key, String defaultValue) {
        try {
            return (String) XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties",
                            null),
                    "get", key, defaultValue);
        } catch (Throwable throwable) {
            logE("getProp", "key get e: " + key + " will return default: " + defaultValue + " e:" + throwable);
            return defaultValue;
        }
    }

    public static void setProp(String key, String val) {
        try {
            XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.os.SystemProperties",
                            null),
                    "set", key, val);
        } catch (Throwable throwable) {
            logE("setProp", "set key e: " + key + " e:" + throwable);
        }
    }

    public static String getPackageVersionName(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> parserCls = XposedHelpers.findClass("android.content.pm.PackageParser", lpparam.classLoader);
            Object parser = parserCls.newInstance();
            File apkPath = new File(lpparam.appInfo.sourceDir);
            Object pkg = XposedHelpers.callMethod(parser, "parsePackage", apkPath, 0);
            return (String) XposedHelpers.getObjectField(pkg, "mVersionName");
        } catch (Throwable e) {
            return "";
        }
    }

    public static int getPackageVersionCode(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> parserCls = XposedHelpers.findClass("android.content.pm.PackageParser", lpparam.classLoader);
            Object parser = parserCls.newInstance();
            File apkPath = new File(lpparam.appInfo.sourceDir);
            Object pkg = XposedHelpers.callMethod(parser, "parsePackage", apkPath, 0);
            return XposedHelpers.getIntField(pkg, "mVersionCode");
        } catch (Throwable e) {
            return -1;
        }
    }

    public static void initPct(ViewGroup container, int source, Context context) {
        Resources res = context.getResources();
        if (getTextView() == null) {
            setTextView(new TextView(container.getContext()));
            getTextView().setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
            getTextView().setGravity(Gravity.CENTER);
            float density = res.getDisplayMetrics().density;
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Math.round(mPrefsMap.getInt("system_ui_others_showpct_top", 54) * density);
            // lp.topMargin = Math.round(54 * density);
            lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            getTextView().setPadding(Math.round(20 * density), Math.round(10 * density), Math.round(18 * density), Math.round(12 * density));
            getTextView().setLayoutParams(lp);
            try {
                Resources modRes = getModuleRes(context);
                getTextView().setTextColor(ColorStateList.valueOf(Color.parseColor("#FFFFFFFF")));
                getTextView().setBackground(modRes.getDrawable(R.drawable.input_background, context.getTheme()));
            } catch (Throwable err) {
                logE("ShowPct", err);
            }
            if (mPrefsMap.getBoolean("system_showpct_use_blur")) {
                try {
                    int i;
                    if (isDarkMode()) i = 180;
                    else i = 200;
                    int a;
                    if (isDarkMode()) a = 120;
                    else a = 140;
                    MiBlurUtils.setContainerPassBlur(getTextView(), i, true);
                    MiBlurUtils.setMiViewBlurMode(getTextView(), 1);
                    MiBlurUtils.clearMiBackgroundBlendColor(getTextView());
                    MiBlurUtils.addMiBackgroundBlendColor(getTextView(), Color.argb(a, 0, 0, 0), 101);
                } catch (Throwable e) {
                    logE("ShowPct", e);
                }
            }
            container.addView(getTextView());
        }
        getTextView().setTag(source);
        getTextView().setVisibility(View.GONE);
    }

    public static void removePct(TextView mPctText) {
        if (mPctText != null) {
            mPctText.setVisibility(View.GONE);
            ViewGroup p = (ViewGroup) mPctText.getParent();
            p.removeView(mPctText);
            mPct = null;
        }
    }
}
