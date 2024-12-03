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
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logW;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Pair;
import android.util.TypedValue;

import com.sevtinge.hyperceiler.utils.ContextUtils;
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * 重写资源钩子，希望本钩子能有更好的生命力。
 *
 * @author 焕晨HChen
 */
public class ResourcesTool {
    private static final String TAG = "ResourcesTool";
    private static boolean hooksApplied = false;
    private static boolean isInit = false;
    private static String mModulePath;
    private static Handler mHandler = null;
    private static final ArrayList<Resources> resourcesArrayList = new ArrayList<>();
    private static final ConcurrentHashMap<Integer, Boolean> resMap = new ConcurrentHashMap<>();
    private static final ArrayList<XC_MethodHook.Unhook> unhooks = new ArrayList<>();

    protected enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    private final ConcurrentHashMap<String, Pair<ReplacementType, Object>> replacements = new ConcurrentHashMap<>();

    public ResourcesTool(String modulePath) {
        mModulePath = modulePath;
        resourcesArrayList.clear();
        resMap.clear();
        unhooks.clear();
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

    public static Resources loadModuleRes(Resources resources, boolean doOnMainLooper) {
        if (resources == null) {
            logW(TAG, "Context can't is null!");
            return null;
        }
        loadResAboveApi30(resources, doOnMainLooper);
        if (!resourcesArrayList.contains(resources))
            resourcesArrayList.add(resources);
        return resources;
    }

    public static Resources loadModuleRes(Resources resources) {
        return loadModuleRes(resources, false);
    }

    public static Resources loadModuleRes(Context context, boolean doOnMainLooper) {
        return loadModuleRes(context.getResources(), doOnMainLooper);
    }

    public static Resources loadModuleRes(Context context) {
        return loadModuleRes(context, false);
    }

    private static ResourcesLoader resourcesLoader;

    /**
     * 来自 QA 的方法
     * <p>
     * from QA
     */
    private static boolean loadResAboveApi30(Resources resources, boolean doOnMainLooper) {
        if (resourcesLoader == null) {
            try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(mModulePath),
                    ParcelFileDescriptor.MODE_READ_ONLY)) {
                ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
                ResourcesLoader loader = new ResourcesLoader();
                loader.addProvider(provider);
                resourcesLoader = loader;
            } catch (IOException e) {
                XposedLogUtils.logE(TAG, "Failed to add resource! debug: above api 30.", e);
                return false;
            }
        }
        if (doOnMainLooper)
            if (Looper.myLooper() == Looper.getMainLooper()) {
                return addLoaders(resources);
            } else {
                if (mHandler == null) {
                    mHandler = new Handler(Looper.getMainLooper());
                }
                mHandler.post(() -> addLoaders(resources));
                return true; // 此状态下保持返回 true，请观察日志是否有报错来判断是否成功。
            }
        else
            return addLoaders(resources);
    }

    private static boolean addLoaders(Resources resources) {
        try {
            resources.addLoaders(resourcesLoader);
        } catch (IllegalArgumentException e) {
            String expected1 = "Cannot modify resource loaders of ResourcesImpl not registered with ResourcesManager";
            if (expected1.equals(e.getMessage())) {
                // fallback to below API 30
                return loadResBelowApi30(resources);
            } else {
                XposedLogUtils.logE(TAG, "Failed to add loaders!", e);
                return false;
            }
        }
        return true;
    }

    /**
     * @noinspection JavaReflectionMemberAccess
     */
    @SuppressLint("DiscouragedPrivateApi")
    private static boolean loadResBelowApi30(Resources resources) {
        try {
            AssetManager assets = resources.getAssets();
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            Integer cookie = (Integer) addAssetPath.invoke(assets, mModulePath);
            if (cookie == null || cookie == 0) {
                XposedLogUtils.logW(TAG, "Method 'addAssetPath' result 0, maybe load res failed!");
                return false;
            }
        } catch (Throwable e) {
            XposedLogUtils.logE(TAG, "Failed to add resource! debug: below api 30.", e);
            return false;
        }
        return true;
    }

    private void applyHooks() {
        if (hooksApplied) return;
        hooksApplied = true;
        Method[] resMethods = Resources.class.getDeclaredMethods();
        for (Method method : resMethods) {
            String name = method.getName();
            switch (name) {
                case "getInteger", "getLayout", "getBoolean", "getDimension",
                     "getDimensionPixelOffset", "getDimensionPixelSize", "getText", "getFloat",
                     "getIntArray", "getStringArray", "getTextArray", "getAnimation" -> {
                    if (method.getParameterTypes().length == 1
                            && method.getParameterTypes()[0].equals(int.class)) {
                        hookResMethod(method.getName(), int.class, hookResBefore);
                    }
                }
                case "getColor" -> {
                    if (method.getParameterTypes().length == 2) {
                        hookResMethod(method.getName(), int.class, Resources.Theme.class, hookResBefore);
                    }
                }
                case "getFraction" -> {
                    if (method.getParameterTypes().length == 3) {
                        hookResMethod(method.getName(), int.class, int.class, int.class, hookResBefore);
                    }
                }
                case "getDrawableForDensity" -> {
                    if (method.getParameterTypes().length == 3) {
                        hookResMethod(method.getName(), int.class, int.class, Resources.Theme.class, hookResBefore);
                    }
                }
            }
        }

        Method[] typedMethod = TypedArray.class.getDeclaredMethods();
        for (Method method : typedMethod) {
            if (method.getName().equals("getColor")) {
                hookTypedMethod(method.getName(), int.class, int.class, hookTypedBefore);
            }
        }
    }

    private void hookResMethod(String name, Object... args) {
        XC_MethodHook.Unhook unhook = HookTool.findAndHookMethod(Resources.class, name, args);
        unhooks.add(unhook);
    }

    private void hookTypedMethod(String name, Object... args) {
        XC_MethodHook.Unhook unhook = HookTool.findAndHookMethod(TypedArray.class, name, args);
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

    private final HookTool.MethodHook hookTypedBefore = new HookTool.MethodHook() {
        @Override
        protected void before(MethodHookParam param) {
            int index = (int) param.args[0];
            int[] mData = (int[]) XposedHelpers.getObjectField(param.thisObject, "mData");
            int type = mData[index];
            int id = mData[index + 3];

            if (id != 0 && (type != TypedValue.TYPE_NULL)) {
                Resources mResources = (Resources) XposedHelpers.getObjectField(param.thisObject, "mResources");
                Object value = getTypedArrayReplacement(mResources, id);
                if (value != null) {
                    param.setResult(value);
                }
            }
        }
    };

    private final HookTool.MethodHook hookResBefore = new HookTool.MethodHook() {
        @Override
        protected void before(MethodHookParam param) {
            if (resourcesArrayList.isEmpty()) {
                Resources resources = loadModuleRes(ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP));
                resourcesArrayList.add(resources); // 重新加载 res
            }
            if (Boolean.TRUE.equals(resMap.get((int) param.args[0]))) {
                return;
            }
            for (Resources resources : resourcesArrayList) {
                if (resources == null) return;
                String method = param.method.getName();
                Object value;
                try {
                    value = getResourceReplacement(resources, (Resources) param.thisObject, method, param.args);
                } catch (Resources.NotFoundException e) {
                    continue;
                }
                if (value != null) {
                    if ("getDimensionPixelOffset".equals(method) || "getDimensionPixelSize".equals(method)) {
                        if (value instanceof Float) value = ((Float) value).intValue();
                    }
                    param.setResult(value);
                    break;
                }
            }
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

    private Object getResourceReplacement(Resources resources, Resources res, String method, Object[] args) throws Resources.NotFoundException {
        if (resources == null) return null;
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

        String resFullName = pkgName + ":" + resType + "/" + resName;
        String resAnyPkgName = "*:" + resType + "/" + resName;

        Object value;
        Integer modResId;
        Pair<ReplacementType, Object> replacement = null;
        if (replacements.containsKey(resFullName)) {
            replacement = replacements.get(resFullName);
        } else if (replacements.containsKey(resAnyPkgName)) {
            replacement = replacements.get(resAnyPkgName);
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
                    try {
                        resources.getResourceName(modResId);
                    } catch (Resources.NotFoundException n) {
                        throw n;
                    }
                    if (method == null) return null;
                    resMap.put(modResId, true);
                    if ("getDrawable".equals(method))
                        value = XposedHelpers.callMethod(resources, method, modResId, args[1]);
                    else if ("getDrawableForDensity".equals(method) || "getFraction".equals(method))
                        value = XposedHelpers.callMethod(resources, method, modResId, args[1], args[2]);
                    else
                        value = XposedHelpers.callMethod(resources, method, modResId);
                    resMap.remove(modResId);
                    return value;
                }
            }
        }
        return null;
    }

    private Object getTypedArrayReplacement(Resources resources, int id) {
        if (id != 0) {
            String pkgName = null;
            String resType = null;
            String resName = null;
            try {
                pkgName = resources.getResourcePackageName(id);
                resType = resources.getResourceTypeName(id);
                resName = resources.getResourceEntryName(id);
            } catch (Throwable ignore) {
            }
            if (pkgName == null || resType == null || resName == null) return null;

            try {
                String resFullName = pkgName + ":" + resType + "/" + resName;
                String resAnyPkgName = "*:" + resType + "/" + resName;

                Pair<ReplacementType, Object> replacement = null;
                if (replacements.containsKey(resFullName)) {
                    replacement = replacements.get(resFullName);
                } else if (replacements.containsKey(resAnyPkgName)) {
                    replacement = replacements.get(resAnyPkgName);
                }
                if (replacement != null && (Objects.requireNonNull(replacement.first) == ReplacementType.OBJECT)) {
                    return replacement.second;
                }
            } catch (Throwable e) {
                XposedLogUtils.logE("getTypedArrayReplacement", e);
            }
        }
        return null;
    }
}
