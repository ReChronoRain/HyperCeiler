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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.hook.module.base.tool;

import static com.sevtinge.hyperceiler.hook.module.base.tool.ResourcesTool.ReplacementType.DENSITY;
import static com.sevtinge.hyperceiler.hook.module.base.tool.ResourcesTool.ReplacementType.ID;
import static com.sevtinge.hyperceiler.hook.module.base.tool.ResourcesTool.ReplacementType.OBJECT;
import static com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.hook.utils.log.XposedLogUtils.logW;

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

import com.sevtinge.hyperceiler.hook.utils.ContextUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * 重写资源钩子，希望本钩子能有更好的生命力。
 *
 * @author 焕晨HChen
 * @co-author Ling Qiqi
 */
public class ResourcesTool {
    private static final String TAG = "ResourcesTool";
    private static volatile ResourcesTool sInstance = null;

    private final String mModulePath;
    private volatile boolean hooksApplied = false;
    private volatile boolean isInit = false;
    private Handler mHandler = null;
    private volatile ResourcesLoader resourcesLoader;

    // 使用线程安全的 CopyOnWriteArrayList
    private final CopyOnWriteArrayList<Resources> resourcesArrayList = new CopyOnWriteArrayList<>();
    private final java.util.Set<Integer> resMap = ConcurrentHashMap.newKeySet();
    private final CopyOnWriteArrayList<XC_MethodHook.Unhook> unhooks = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<ResKey, Pair<ReplacementType, Object>> replacements = new ConcurrentHashMap<>();

    /**
     * 结构化键，用于替换 Map 中的字符串拼接
     */
    private record ResKey(String pkg, String type, String name) {
        // 使用 record 自动生成 equals/hashCode/toString，避免手动实现导致不一致
    }

    protected enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    // 构造函数私有化
    private ResourcesTool(String modulePath) {
        this.mModulePath = modulePath;
        resourcesArrayList.clear();
        resMap.clear();
        unhooks.clear();
        applyHooks();
        isInit = true;
    }

    /**
     * 获取单例实例，首次调用时必须提供模块路径
     */
    public static ResourcesTool getInstance(String modulePath) {
        if (sInstance == null) {
            synchronized (ResourcesTool.class) {
                if (sInstance == null) {
                    sInstance = new ResourcesTool(modulePath);
                }
            }
        }
        return sInstance;
    }

    /**
     * 获取已初始化的单例实例
     */
    public static synchronized ResourcesTool getInstance() {
        if (sInstance == null) {
            // 避免空指针，但提示需要先初始化
            logE(TAG, "ResourcesTool not initialized. Call getInstance(String modulePath) first.");
        }
        return sInstance;
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

    public Resources loadModuleRes(Resources resources, boolean doOnMainLooper) {
        if (resources == null) {
            logW(TAG, "Context can't be null!");
            return null;
        }
        boolean loaded = loadResAboveApi30(resources, doOnMainLooper);
        if (loaded) {
            if (!resourcesArrayList.contains(resources)) {
                resourcesArrayList.add(resources);
            }
        } else {
            logW(TAG, "loadModuleRes: failed to load resources: " + resources);
        }
        return resources;
    }

    public Resources loadModuleRes(Resources resources) {
        return loadModuleRes(resources, false);
    }

    public Resources loadModuleRes(Context context, boolean doOnMainLooper) {
        return loadModuleRes(context.getResources(), doOnMainLooper);
    }

    public Resources loadModuleRes(Context context) {
        return loadModuleRes(context, false);
    }

    /**
     * 来自 QA 的方法
     */
    private boolean loadResAboveApi30(Resources resources, boolean doOnMainLooper) {
        if (resourcesLoader == null) {
            synchronized (this) {
                if (resourcesLoader == null) {
                    try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(mModulePath),
                        ParcelFileDescriptor.MODE_READ_ONLY)) {
                        ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
                        ResourcesLoader loader = new ResourcesLoader();
                        loader.addProvider(provider);
                        resourcesLoader = loader;
                    } catch (IOException e) {
                        logE(TAG, "Failed to add resource! debug: above api 30.", e);
                        return false;
                    }
                }
            }
        }
        if (doOnMainLooper) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                return addLoaders(resources);
            } else {
                synchronized (this) {
                    if (mHandler == null) {
                        mHandler = new Handler(Looper.getMainLooper());
                    }
                }
                mHandler.post(() -> addLoaders(resources));
                return true;
            }
        } else {
            return addLoaders(resources);
        }
    }

    private boolean addLoaders(Resources resources) {
        try {
            resources.addLoaders(resourcesLoader);
        } catch (IllegalArgumentException e) {
            String expected1 = "Cannot modify resource loaders of ResourcesImpl not registered with ResourcesManager";
            if (expected1.equals(e.getMessage())) {
                // fallback to below API 30
                return loadResBelowApi30(resources);
            } else {
                logE(TAG, "Failed to add loaders!", e);
                return false;
            }
        }
        return true;
    }

    @SuppressLint("DiscouragedPrivateApi")
    private boolean loadResBelowApi30(Resources resources) {
        try {
            AssetManager assets = resources.getAssets();
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            Integer cookie = (Integer) addAssetPath.invoke(assets, mModulePath);
            if (cookie == null || cookie == 0) {
                logW(TAG, "Method 'addAssetPath' result 0, maybe load res failed!");
                return false;
            }
        } catch (Throwable e) {
            logE(TAG, "Failed to add resource! debug: below api 30.", e);
            return false;
        }
        return true;
    }

    private synchronized void applyHooks() {
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
        hooksApplied = false; // 允许重新应用
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
                Context context = ContextUtils.getContextNoError(ContextUtils.FLAG_CURRENT_APP);
                if (context != null) {
                    Resources resources = loadModuleRes(context);
                    if (resources != null) {
                        resourcesArrayList.add(resources); // 重新加载 res
                    }
                }
            }
            int reqId = (int) param.args[0];
            if (resMap.contains(reqId)) {
                return;
            }
            for (Resources resources : resourcesArrayList) {
                if (resources == null) continue;
                String method = param.method.getName();
                Object value;
                try {
                    value = getResourceReplacement(resources, (Resources) param.thisObject, method, param.args);
                } catch (Resources.NotFoundException e) {
                    continue;
                }
                if (value != null) {
                    Object finalResult = null;

                    switch (method) {
                        case "getInteger":
                        case "getColor":
                        case "getDimensionPixelOffset":
                        case "getDimensionPixelSize":
                            if (value instanceof Number) {
                                // 对于像素尺寸，使用四舍五入更精确
                                finalResult = Math.round(((Number) value).floatValue());
                            }
                            break;

                        case "getDimension":
                        case "getFloat":
                            if (value instanceof Number) {
                                finalResult = ((Number) value).floatValue();
                            }
                            break;

                        case "getText":
                            if (value instanceof CharSequence) {
                                finalResult = value;
                            }
                            break;

                        case "getBoolean":
                            if (value instanceof Boolean) {
                                finalResult = value;
                            }
                            break;

                        default:
                            finalResult = value;
                            break;
                    }

                    if (finalResult != null) {
                        param.setResult(finalResult);
                    } else {
                        // 如果类型转换失败，记录日志并放弃本次替换，避免崩溃
                        logW(TAG, "Mismatched replacement type for method " + method + ". Got " + value.getClass().getName());
                    }
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
            replacements.put(new ResKey(pkg, type, name), new Pair<>(ID, replacementResId));
        } catch (Throwable t) {
            logE(TAG, "setResReplacement failed", t);
        }
    }

    /**
     * 设置密度类型的资源
     */
    public void setDensityReplacement(String pkg, String type, String name, float replacementResValue) {
        try {
            applyHooks();
            replacements.put(new ResKey(pkg, type, name), new Pair<>(DENSITY, replacementResValue));
        } catch (Throwable t) {
            logE(TAG, "setDensityReplacement failed", t);
        }
    }

    /**
     * 设置 Object 类型的资源
     */
    public void setObjectReplacement(String pkg, String type, String name, Object replacementResValue) {
        try {
            applyHooks();
            replacements.put(new ResKey(pkg, type, name), new Pair<>(OBJECT, replacementResValue));
        } catch (Throwable t) {
            logE(TAG, "setObjectReplacement failed", t);
        }
    }

    private Object getResourceReplacement(Resources resources, Resources res, String method, Object[] args) throws Resources.NotFoundException {
        if (resources == null) return null;
        String pkgName;
        String resType;
        String resName;
        try {
            int resId = (int) args[0];
            // 避免 ID 为 0 时进行无效查询
            if (resId == 0) return null;
            pkgName = res.getResourcePackageName(resId);
            resType = res.getResourceTypeName(resId);
            resName = res.getResourceEntryName(resId);
        } catch (Throwable ignore) {
            return null;
        }

        if (pkgName == null || resType == null || resName == null) return null;

        // 使用 ResKey 进行查找
        ResKey resFullNameKey = new ResKey(pkgName, resType, resName);
        ResKey resAnyPkgNameKey = new ResKey("*", resType, resName);

        Pair<ReplacementType, Object> replacement = replacements.get(resFullNameKey);
        if (replacement == null) {
            replacement = replacements.get(resAnyPkgNameKey);
        }

        if (replacement != null) {
            switch (replacement.first) {
                case OBJECT:
                    if ("getText".equals(method) && !(replacement.second instanceof CharSequence)) {
                        logW(TAG, "Mismatched type: OBJECT replacement is not a CharSequence for getText method.");
                        return null;
                    }
                    return replacement.second;
                case DENSITY: {
                    if ("getText".equals(method)) {
                        logW(TAG, "Mismatched type: DENSITY replacement cannot be used for getText method.");
                        return null;
                    }
                    Object repl = replacement.second;
                    if (repl instanceof Number) {
                        return ((Number) repl).floatValue() * res.getDisplayMetrics().density;
                    } else if (repl instanceof String) {
                        try {
                            return Float.parseFloat((String) repl) * res.getDisplayMetrics().density;
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    logW(TAG, "Invalid DENSITY replacement type: " + repl);
                    return null;
                }
                case ID: {
                    if (!(replacement.second instanceof Number)) return null;
                    Integer modResId = ((Number) replacement.second).intValue();
                    if (modResId == 0) return null;
                    try {
                        // 验证资源是否存在
                        resources.getResourceName(modResId);
                    } catch (Resources.NotFoundException n) {
                        throw n;
                    }
                    if (method == null) return null;
                    // 标记正在处理，避免重入
                    resMap.add(modResId);
                    Object value;
                    try {
                        if ("getDrawable".equals(method) && args.length >= 2) {
                            value = XposedHelpers.callMethod(resources, method, modResId, args[1]);
                        } else if (("getDrawableForDensity".equals(method) || "getFraction".equals(method)) && args.length >= 3) {
                            value = XposedHelpers.callMethod(resources, method, modResId, args[1], args[2]);
                        } else {
                            value = XposedHelpers.callMethod(resources, method, modResId);
                        }
                    } finally {
                        resMap.remove(modResId);
                    }
                    return value;
                }
            }
        }
        return null;
    }

    private Object getTypedArrayReplacement(Resources resources, int id) {
        if (id == 0) return null;
        String pkgName;
        String resType;
        String resName;
        try {
            pkgName = resources.getResourcePackageName(id);
            resType = resources.getResourceTypeName(id);
            resName = resources.getResourceEntryName(id);
        } catch (Throwable ignore) {
            return null;
        }
        if (pkgName == null || resType == null || resName == null) return null;

        try {
            // 使用 ResKey 进行查找
            ResKey resFullNameKey = new ResKey(pkgName, resType, resName);
            ResKey resAnyPkgNameKey = new ResKey("*", resType, resName);

            Pair<ReplacementType, Object> replacement = replacements.get(resFullNameKey);
            if (replacement == null) {
                replacement = replacements.get(resAnyPkgNameKey);
            }

            if (replacement != null && replacement.first == OBJECT) {
                return replacement.second;
            }
        } catch (Throwable e) {
            logE(TAG, "getTypedArrayReplacement failed", e);
        }
        return null;
    }
}
