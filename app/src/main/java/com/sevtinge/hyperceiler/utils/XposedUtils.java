package com.sevtinge.hyperceiler.utils;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import java.lang.ref.WeakReference;

import de.robv.android.xposed.XposedHelpers;

public class XposedUtils extends XposedLogUtils {

    // public static TextView mPct = null;
    public static WeakReference<TextView> mPct;

    // public  Context mModuleContext = null;

    public static void setTextView(TextView textView) {
        mPct = new WeakReference<>(textView);
    }

    public static TextView getTextView() {
        return mPct != null ? mPct.get() : null;
    }

    public static synchronized Context getModuleContext(Context context) throws Throwable {
        return getModuleContext(context, null);
    }

    public static synchronized Context getModuleContext(Context context, Configuration config) throws Throwable {
        Context mModuleContext;
        mModuleContext = context.createPackageContext(Helpers.mAppModulePkg, Context.CONTEXT_IGNORE_SECURITY).createDeviceProtectedStorageContext();
        return config == null ? mModuleContext : mModuleContext.createConfigurationContext(config);
    }

    public static synchronized Resources getModuleRes(Context context) throws Throwable {
        Configuration config = context.getResources().getConfiguration();
        Context moduleContext = getModuleContext(context);
        return (config == null ? moduleContext.getResources() : moduleContext.createConfigurationContext(config).getResources());
    }

    public static Context findContext() {
        Context context;
        try {
            context = (Application) XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null), "currentApplication");
            if (context == null) {
                Object currentActivityThread = XposedHelpers.callStaticMethod(XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread");
                if (currentActivityThread != null)
                    context = (Context) XposedHelpers.callMethod(currentActivityThread, "getSystemContext");
            }
            return context;
        } catch (Throwable ignore) {
        }
        return null;
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
                getTextView().setTextColor(modRes.getColor(R.color.color_on_surface_variant, context.getTheme()));
                getTextView().setBackground(ResourcesCompat.getDrawable(modRes, R.drawable.input_background, context.getTheme()));
            } catch (Throwable err) {
                XposedLogUtils.logE("ShowVolumePct", err);
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
