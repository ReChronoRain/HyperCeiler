package com.sevtinge.cemiuiler.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;
import android.util.SparseIntArray;

import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class ResourcesHook {
    private boolean hooksApplied = false;

    public enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    private final SparseIntArray fakes = new SparseIntArray();
    private final ConcurrentHashMap<String, Pair<ReplacementType, Object>> replacements = new ConcurrentHashMap<>();

    public static int getFakeResId(String resourceName) {
        return 0x7e00f000 | (resourceName.hashCode() & 0x00ffffff);
    }

    private final Helpers.MethodHook mReplaceHook = new Helpers.MethodHook() {
        @Override
        protected void before(MethodHookParam param) {
            Context mContext = Helpers.findContext();
            if (mContext == null) return;
            String method = param.method.getName();
            Object value = getFakeResource(mContext, method, param.args);
            if (value == null) {
                value = getResourceReplacement(mContext, (Resources) param.thisObject, method, param.args);
                if (value == null) return;
                if ("getDimensionPixelOffset".equals(method) || "getDimensionPixelSize".equals(method)) {
                    if (value instanceof Float) value = ((Float) value).intValue();
                }
            }
            param.setResult(value);
        }
    };

    public ResourcesHook() {
    }

    private void applyHooks() {
        if (hooksApplied) return;
        hooksApplied = true;
        Helpers.findAndHookMethod(Resources.class, "getInteger", int.class, mReplaceHook);
        Helpers.findAndHookMethod(Resources.class, "getLayout", int.class, mReplaceHook);
        Helpers.findAndHookMethod(Resources.class, "getFraction", int.class, int.class, int.class, mReplaceHook);
        Helpers.findAndHookMethod(Resources.class, "getBoolean", int.class, mReplaceHook);
        Helpers.findAndHookMethod(Resources.class, "getDimension", int.class, mReplaceHook);
        Helpers.findAndHookMethod(Resources.class, "getDimensionPixelOffset", int.class, mReplaceHook);
        Helpers.findAndHookMethod(Resources.class, "getDimensionPixelSize", int.class, mReplaceHook);
        Helpers.findAndHookMethod(Resources.class, "getText", int.class, mReplaceHook);
        Helpers.findAndHookMethod(Resources.class, "getString", int.class, mReplaceHook);
        Helpers.findAndHookMethod(Resources.class, "getDrawableForDensity", int.class, int.class, Resources.Theme.class, mReplaceHook);
        Helpers.findAndHookMethod(Resources.class, "getIntArray", int.class, mReplaceHook);
        Helpers.findAndHookMethod(Resources.class, "getStringArray", int.class, mReplaceHook);
        Helpers.findAndHookMethod(Resources.class, "getTextArray", int.class, mReplaceHook);
        Helpers.findAndHookMethod(Resources.class, "getAnimation", int.class, mReplaceHook);
    }

    public int addResource(String resName, int resId) {
        try {
            applyHooks();
            int fakeResId = getFakeResId(resName);
            fakes.put(fakeResId, resId);
            return fakeResId;
        } catch (Throwable t) {
            XposedBridge.log(t);
            return 0;
        }
    }

    private Object getFakeResource(Context context, String method, Object[] args) {
        try {
            if (context == null) return null;
            int modResId = fakes.get((int) args[0]);
            if (modResId == 0) return null;

            Object value;
            Resources modRes = Helpers.getModuleRes(context);
            if ("getDrawable".equals(method))
                value = XposedHelpers.callMethod(modRes, method, modResId, args[1]);
            else if ("getDrawableForDensity".equals(method) || "getFraction".equals(method))
                value = XposedHelpers.callMethod(modRes, method, modResId, args[1], args[2]);
            else
                value = XposedHelpers.callMethod(modRes, method, modResId);
            return value;
        } catch (Throwable t) {
            Helpers.log(t);
            return null;
        }
    }

    public void setResReplacement(String pkg, String type, String name, int replacementResId) {
        try {
            applyHooks();
            replacements.put(pkg + ":" + type + "/" + name, new Pair<>(ReplacementType.ID, replacementResId));
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public void setDensityReplacement(String pkg, String type, String name, float replacementResValue) {
        try {
            applyHooks();
            replacements.put(pkg + ":" + type + "/" + name, new Pair<>(ReplacementType.DENSITY, replacementResValue));
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public void setObjectReplacement(String pkg, String type, String name, Object replacementResValue) {
        try {
            applyHooks();
            replacements.put(pkg + ":" + type + "/" + name, new Pair<>(ReplacementType.OBJECT, replacementResValue));
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private Object getResourceReplacement(Context context, Resources res, String method, Object[] args) {
        if (context == null) return null;

        String pkgName = null;
        String resType = null;
        String resName = null;
        try {
            pkgName = res.getResourcePackageName((int) args[0]);
            resType = res.getResourceTypeName((int) args[0]);
            resName = res.getResourceEntryName((int) args[0]);
        } catch (Throwable ignore) {
        }
        if (pkgName == null || resType == null || resName == null) return null;

        try {
            Object value;
            String resFullName = pkgName + ":" + resType + "/" + resName;
            String resAnyPkgName = "*:" + resType + "/" + resName;

            Integer modResId = null;
            Pair<ReplacementType, Object> replacement = null;
            if (replacements.containsKey(resFullName))
                replacement = replacements.get(resFullName);
            else if (replacements.containsKey(resAnyPkgName))
                replacement = replacements.get(resAnyPkgName);

            if (replacement != null)
                if (replacement.first == ReplacementType.OBJECT) return replacement.second;
                else if (replacement.first == ReplacementType.DENSITY) {
                    return (Float) replacement.second * res.getDisplayMetrics().density;
                } else if (replacement.first == ReplacementType.ID) modResId = (Integer) replacement.second;
            if (modResId == null) return null;

            Resources modRes = Helpers.getModuleRes(context);
            if ("getDrawable".equals(method))
                value = XposedHelpers.callMethod(modRes, method, modResId, args[1]);
            else if ("getDrawableForDensity".equals(method) || "getFraction".equals(method))
                value = XposedHelpers.callMethod(modRes, method, modResId, args[1], args[2]);
            else
                value = XposedHelpers.callMethod(modRes, method, modResId);
            return value;
        } catch (Throwable t) {
            Helpers.log(t);
            return null;
        }
    }

}
