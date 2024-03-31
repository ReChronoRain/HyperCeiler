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
import static com.sevtinge.hyperceiler.utils.api.ProjectApi.mAppModulePkg;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Pair;

import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.utils.ContextUtils;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * 重写资源钩子，希望本钩子能有更好的生命力。
 *
 * @rewrite 焕晨HChen
 */
public class ResourcesTool {
    private static final String TAG = "ResourcesTool";
    private boolean hooksApplied = false;
    private boolean isInit = false;
    private boolean useModuleRes = true;
    private final String mModulePath;
    private Handler mHandler = null;
    private ResourcesLoader resourcesLoader = null;
    private final ConcurrentHashMap<Integer, Boolean> resMap = new ConcurrentHashMap<>();
    private final ArrayList<XC_MethodHook.Unhook> unhooks = new ArrayList<>();

    protected enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    private final ConcurrentHashMap<String, Pair<ReplacementType, Object>> replacements = new ConcurrentHashMap<>();

    public ResourcesTool(String modulePath) {
        mModulePath = modulePath;
        applyHooks();
        isInit = true;
    }

    public boolean isInit() {
        return isInit;
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
    private boolean loadResBelowApi30(Context context) {
        // String TAG = "addModuleRes";
        try {
            return (int) XposedHelpers.callMethod(context.getResources().getAssets(), "addAssetPath",
                    XposedInit.mModulePath) != 0;
        } catch (Throwable e) {
            XposedLogUtils.logE(TAG, "CallMethod addAssetPathInternal failed!" + e);
        }
        try {
            @SuppressLint({"SoonBlockedPrivateApi", "DiscouragedPrivateApi"})
            Method AssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            AssetPath.setAccessible(true);
            return Integer.parseInt(String.valueOf(AssetPath.invoke(context.getResources().getAssets(), XposedInit.mModulePath))) != 0;
        } catch (NoSuchMethodException e) {
            XposedLogUtils.logE(TAG, "Method addAssetPath is null: ", e);
        } catch (InvocationTargetException e) {
            XposedLogUtils.logE(TAG, "InvocationTargetException: ", e);
        } catch (IllegalAccessException e) {
            XposedLogUtils.logE(TAG, "IllegalAccessException: ", e);
        } catch (NumberFormatException e) {
            XposedLogUtils.logE(TAG, "NumberFormatException: ", e);
        }
        return false;
    }

    /**
     * 来自 QA 的方法
     */
    private boolean loadResAboveApi30(Context context) {
        if (resourcesLoader == null) {
            try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(mModulePath),
                    ParcelFileDescriptor.MODE_READ_ONLY)) {
                ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
                ResourcesLoader loader = new ResourcesLoader();
                loader.addProvider(provider);
                resourcesLoader = loader;
            } catch (IOException e) {
                XposedLogUtils.logE(TAG, "Failed to add resource!: " + e);
                return false;
            }
        }
        // if (Looper.myLooper() == Looper.getMainLooper()) {
        context.getResources().addLoaders(resourcesLoader);
        // } else {
        //     if (mHandler != null) {
        //         mHandler.post(() -> context.getResources().addLoaders(resourcesLoader));
        //     } else {
        //         return false;
        //     }
        // }
        return true;
    }

    /**
     * 获取添加后的 Res.
     * 一般不需要，除非上面 loadModuleRes 加载后依然无效。
     *
     * @noinspection UnusedReturnValue
     */
    public Resources loadModuleRes(Context context) {
        boolean load;
        if (context == null) {
            XposedLogUtils.logE(TAG, "context can't is null!!");
            return null;
        }
        if (Build.VERSION.SDK_INT >= 30) {
            load = loadResAboveApi30(context);
        } else {
            load = loadResBelowApi30(context);
        }
        if (!load) {
            XposedLogUtils.logW(TAG, "loadModuleRes return 0, It may have failed. Try the second method ...");
            try {
                Resources resources = getModuleRes(context);
                useModuleRes = true;
                return resources;
            } catch (PackageManager.NameNotFoundException e) {
                XposedLogUtils.logE(TAG, "Failed to load resource!Critical error!!Scope may crash!!\n" + e);
            }
        }
        return context.getResources();
    }

    public void putHandler(Handler handler) {
        mHandler = handler;
    }

    public static Context getModuleContext(Context context)
            throws PackageManager.NameNotFoundException {
        return getModuleContext(context, null);
    }

    public static Context getModuleContext(Context context, Configuration config)
            throws PackageManager.NameNotFoundException {
        Context mModuleContext;
        mModuleContext = context.createPackageContext(mAppModulePkg, Context.CONTEXT_IGNORE_SECURITY).createDeviceProtectedStorageContext();
        return config == null ? mModuleContext : mModuleContext.createConfigurationContext(config);
    }

    public static Resources getModuleRes(Context context)
            throws PackageManager.NameNotFoundException {
        Configuration config = context.getResources().getConfiguration();
        Context moduleContext = getModuleContext(context);
        return (config == null ? moduleContext.getResources() : moduleContext.createConfigurationContext(config).getResources());
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
                        "getIntArray", "getStringArray",
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
        XC_MethodHook.Unhook unhook = HookTool.findAndHookMethod(Resources.class, name, args);
        unhooks.add(unhook);
    }

    public void unHookRes() {
        if (unhooks.isEmpty()) {
            isInit = false;
            return;
        }
        for (XC_MethodHook.Unhook unhook : unhooks) {
            unhook.unhook();
        }
        unhooks.clear();
        isInit = false;
    }

    private final HookTool.MethodHook hookBefore = new HookTool.MethodHook() {
        @Override
        protected void before(MethodHookParam param) {
            Context context;
            context = XposedTool.findContext(ContextUtils.FLAG_ALL);
            if (context == null) return;
            String method = param.method.getName();
            Object value;
            value = getResourceReplacement(context, (Resources) param.thisObject, method, param.args);
            if (value != null) {
                if ("getDimensionPixelOffset".equals(method) || "getDimensionPixelSize".equals(method)) {
                    if (value instanceof Float) value = ((Float) value).intValue();
                }
                param.setResult(value);
            } else {
                if (useModuleRes) {
                    try {
                        if (Boolean.TRUE.equals(resMap.get((int) param.args[0]))) {
                            return;
                        }
                        context.getResources().getResourceName((int) param.args[0]);
                    } catch (Resources.NotFoundException e) {
                        value = findRes(context, method, param.args);
                    }
                    if (Boolean.TRUE.equals(resMap.get((int) param.args[0]))) {
                        resMap.remove((int) param.args[0]);
                    }
                    if (value == null) return;
                    param.setResult(value);
                }
            }
        }
    };

    private Object findRes(Context context, String method, Object[] args) {
        try {
            int modResId = (int) args[0];
            if (modResId == 0) return null;
            resMap.put(modResId, true);
            Object value;
            Resources modRes = getModuleRes(context);
            if ("getDrawable".equals(method))
                value = XposedHelpers.callMethod(modRes, method, modResId, args[1]);
            else if ("getDrawableForDensity".equals(method) || "getFraction".equals(method))
                value = XposedHelpers.callMethod(modRes, method, modResId, args[1], args[2]);
            else
                value = XposedHelpers.callMethod(modRes, method, modResId);
            return value;
        } catch (Throwable t) {
            // XposedLogUtils.logE(TAG, "Failed to get module resources and may not work! " + t);
            return null;
        }
    }

    /**
     * 设置资源 ID 类型的替换
     */
    public void setResReplacement(String pkg, String type, String name, int replacementResId) {
        try {
            applyHooks();
            replacements.put(pkg + ":" + type + "/" + name, new Pair<>(ID, replacementResId));
        } catch (Throwable t) {
            XposedLogUtils.logE(TAG, "setResReplacement: " + t);
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
            XposedLogUtils.logE(TAG, "setDensityReplacement: " + t);
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
            XposedLogUtils.logE(TAG, "setObjectReplacement: " + t);
        }
    }

    private Object getResourceReplacement(Context context, Resources res, String method, Object[] args) {
        if (context == null) return null;
        // loadModuleRes(context);
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
            if (replacements.containsKey(resFullName)) {
                replacement = replacements.get(resFullName);
                // replacements.remove(resFullName);
            } else if (replacements.containsKey(resAnyPkgName)) {
                replacement = replacements.get(resAnyPkgName);
                // replacements.remove(resAnyPkgName);
            }
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

                        Resources modRes;
                        if (useModuleRes) {
                            modRes = getModuleRes(context);
                        } else modRes = context.getResources();
                        if (method == null) return null;
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
