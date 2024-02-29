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

import static com.sevtinge.hyperceiler.module.base.tool.ResourcesTool.ReplacementType.DENSITY;
import static com.sevtinge.hyperceiler.module.base.tool.ResourcesTool.ReplacementType.ID;
import static com.sevtinge.hyperceiler.module.base.tool.ResourcesTool.ReplacementType.OBJECT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.util.Pair;

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.utils.ContextUtils;
import com.sevtinge.hyperceiler.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 重写资源钩子，希望本钩子能有更好的生命力。
 */
public class ResourcesTool {
    private static final String TAG = "ResourcesTool";
    private boolean hooksApplied = false;
    // private Context mContext = null;
    private WeakReference<Context> weakContext;

    protected enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    private final ConcurrentHashMap<String, Pair<ReplacementType, Object>> replacements = new ConcurrentHashMap<>();

    public ResourcesTool() {
    }

    private Context getWeakContext() {
        return weakContext != null ? weakContext.get() : null;
    }

    private void setWeakContext(Context context) {
        weakContext = new WeakReference<>(context);
    }

    /**
     * 返回一个模拟的 ID
     */
    public static int getFakeResId(String resourceName) {
        return 0x7e00f000 | (resourceName.hashCode() & 0x00ffffff);
    }

    /**
     * @noinspection JavaReflectionMemberAccess
     */
    private static int loadRes(Context context) {
        // String TAG = "addModuleRes";
        try {
            int result = (int) XposedHelpers.callMethod(context.getResources().getAssets(), "addAssetPath",
                XposedInit.mModulePath);
            // XposedLogUtils.logE(TAG, "Have Apk Assets:" + XposedInit.mModulePath);
            /*Object[] apk = (Object[]) XposedHelpers.callMethod(context.getResources().getAssets(), "getApkAssets");
            for (Object a : apk) {
                XposedLogUtils.logE(TAG, "Have Apk Assets:" + a);
            }*/
            return result;
        } catch (Throwable e) {
            XposedLogUtils.logE(TAG, "CallMethod addAssetPathInternal failed!" + e);
        }
        try {
            @SuppressLint({"SoonBlockedPrivateApi", "DiscouragedPrivateApi"})
            Method AssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            AssetPath.setAccessible(true);
            return Integer.parseInt(String.valueOf(AssetPath.invoke(context.getResources().getAssets(), XposedInit.mModulePath)));
        } catch (NoSuchMethodException e) {
            XposedLogUtils.logE(TAG, "Method addAssetPath is null: ", e);
        } catch (InvocationTargetException e) {
            XposedLogUtils.logE(TAG, "InvocationTargetException: ", e);
        } catch (IllegalAccessException e) {
            XposedLogUtils.logE(TAG, "IllegalAccessException: ", e);
        } catch (NumberFormatException e) {
            XposedLogUtils.logE(TAG, "NumberFormatException: ", e);
        }
        return 0;
    }

    /**
     * 获取添加后的 Res.
     * 一般不需要，除非上面 loadModuleRes 加载后依然无效。
     */
    public static Resources loadModuleRes(Context context) {
        if (context == null) {
            XposedLogUtils.logE(TAG, "context can't is null!!");
            return null;
        }
        if (loadRes(context) == 0) {
            // loopAttempt(context);
            XposedLogUtils.logE(TAG, "loadModuleRes return 0, It may have failed.");
        }
        return context.getResources();
    }

    private static void loopAttempt(Context context) {
        try {
            ExecutorService executorService = ThreadPoolManager.getInstance();
            executorService.submit(() -> {
                long time = System.currentTimeMillis();
                long timeout = 2000; // 2秒
                int result = 0;
                while (true) {
                    long nowTime = System.currentTimeMillis();
                    result = loadRes(context);
                    if (nowTime - time > timeout || result != 0) {
                        break;
                    }
                }
                if (result == 0)
                    XposedLogUtils.logE(TAG, "If the timeout still returns 0, it must have failed!");
            });
        } catch (Throwable e) {
            XposedLogUtils.logE(TAG, "Unknown!" + e);
        }
    }

    private void applyHooks() {
        if (hooksApplied) return;
        hooksApplied = true;
        Method[] resMethods = Resources.class.getDeclaredMethods();
        for (Method method : resMethods) {
            switch (method.getName()) {
                case "getInteger", "getLayout", "getBoolean",
                    "getDimension", "getDimensionPixelOffset",
                    "getDimensionPixelSize", "getText",
                    "getString", "getIntArray", "getStringArray",
                    "getTextArray", "getAnimation" -> {
                    if (method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0].equals(int.class)) {
                        hookResMethod(method.getName(), int.class, hookBefore);
                    }
                }
                case "getFraction" -> {
                    if (method.getParameterTypes().length == 3) {
                        hookResMethod(method.getName(), int.class, int.class, int.class, hookBefore);
                    }
                }
                case "getDrawableForDensity" -> {
                    if (method.getParameterTypes().length == 3) {
                        hookResMethod(method.getName(), int.class, int.class, Resources.Theme.class, hookBefore);
                    }
                }
            }
        }
    }

    private void hookResMethod(String name, Object... args) {
        HookTool.findAndHookMethod(Resources.class, name, args);
    }

    private final HookTool.MethodHook hookBefore = new HookTool.MethodHook() {
        @Override
        protected void before(MethodHookParam param) {
            Context context;
            if ((context = getWeakContext()) == null) {
                context = XposedTool.findContext(ContextUtils.FLAG_ALL);
            }
            if (context == null) return;
            String method = param.method.getName();
            Object value = getResourceReplacement(context, (Resources) param.thisObject, method, param.args);
            if (value == null) return;
            if ("getDimensionPixelOffset".equals(method) || "getDimensionPixelSize".equals(method)) {
                if (value instanceof Float) value = ((Float) value).intValue();
            }
            param.setResult(value);
        }
    };

    /**
     * 设置资源 ID 类型的替换
     */
    public void setResReplacement(String pkg, String type, String name, int replacementResId) {
        try {
            applyHooks();
            replacements.put(pkg + ":" + type + "/" + name, new Pair<>(ID, replacementResId));
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /**
     * 设置资源 ID 类型的替换
     * 请注意无论何时设置 Context 都是最正确的
     */
    public void setResReplacement(Context context, String pkg, String type, String name, int replacementResId) {
        try {
            setWeakContext(context);
            replacements.put(pkg + ":" + type + "/" + name, new Pair<>(ID, replacementResId));
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /**
     * 设置密度类型的资源
     */
    public void setDensityReplacement(String pkg, String type, String name, float replacementResValue) {
        try {
            applyHooks();
            replacements.put(pkg + ":" + type + "/" + name, new Pair<>(DENSITY, replacementResValue));
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /**
     * 设置密度类型的资源
     * 请注意无论何时使用 Context 总是最好的
     */
    public void setDensityReplacement(Context context, String pkg, String type, String name, float replacementResValue) {
        try {
            setWeakContext(context);
            applyHooks();
            replacements.put(pkg + ":" + type + "/" + name, new Pair<>(DENSITY, replacementResValue));
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /**
     * 设置 Object 类型的资源
     */
    public void setObjectReplacement(String pkg, String type, String name, Object replacementResValue) {
        try {
            applyHooks();
            replacements.put(pkg + ":" + type + "/" + name, new Pair<>(OBJECT, replacementResValue));
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    /**
     * 设置 Object 类型的资源
     * 请注意无论何时使用 Context 总是最好的
     */
    public void setObjectReplacement(Context context, String pkg, String type, String name, Object replacementResValue) {
        try {
            setWeakContext(context);
            applyHooks();
            replacements.put(pkg + ":" + type + "/" + name, new Pair<>(OBJECT, replacementResValue));
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    private Object getResourceReplacement(Context context, Resources res, String method, Object[] args) {
        if (context == null) return null;
        loadModuleRes(context);
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
            String resFullName = pkgName + ":" + resType + "/" + resName;
            String resAnyPkgName = "*:" + resType + "/" + resName;

            Object value;
            Integer modResId;
            Pair<ReplacementType, Object> replacement = null;
            if (replacements.containsKey(resFullName))
                replacement = replacements.get(resFullName);
            else if (replacements.containsKey(resAnyPkgName))
                replacement = replacements.get(resAnyPkgName);
            if (replacement != null) {
                switch (replacement.first) {
                    case OBJECT -> {
                        return replacement.second;
                    }
                    case DENSITY -> {
                        return (Float) replacement.second * res.getDisplayMetrics().density;
                    }
                    case ID -> {
                        modResId = (Integer) replacement.second;
                        if (modResId == 0) return null;

                        Resources modRes = loadModuleRes(context);
                        if ("getDrawable".equals(method))
                            value = XposedHelpers.callMethod(modRes, method, modResId, args[1]);
                        else if ("getDrawableForDensity".equals(method) || "getFraction".equals(method))
                            value = XposedHelpers.callMethod(modRes, method, modResId, args[1], args[2]);
                        else
                            value = XposedHelpers.callMethod(modRes, method, modResId);
                        return value;
                    }
                }
            }
        } catch (Throwable t) {
            XposedLogUtils.logE("getResourceReplacement", t);
        }
        return null;
    }
}
