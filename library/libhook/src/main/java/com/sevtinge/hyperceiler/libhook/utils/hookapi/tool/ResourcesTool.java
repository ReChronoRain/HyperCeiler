/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.utils.hookapi.tool;

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

import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;
import io.github.libxposed.api.XposedInterface;

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

    private final CopyOnWriteArrayList<Resources> resourcesArrayList = new CopyOnWriteArrayList<>();
    private final java.util.Set<Integer> resMap = ConcurrentHashMap.newKeySet();
    private final CopyOnWriteArrayList<XposedInterface.MethodUnhooker<?>> unhooks = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<ResKey, Pair<ReplacementType, Object>> replacements = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, ResKey> resIdCache = new ConcurrentHashMap<>();
    private static final ThreadLocal<Integer> currentResId = ThreadLocal.withInitial(() -> 0);
    private record ResKey(String pkg, String type, String name) {}

    /**
     * 资源替换类型
     */
    protected enum ReplacementType {
        ID,
        DENSITY,
        OBJECT
    }

    private ResourcesTool(String modulePath) {
        this.mModulePath = modulePath;
        resourcesArrayList.clear();
        resMap.clear();
        resIdCache.clear();
        unhooks.clear();
        applyHooks();
        isInit = true;
    }

    /**
     * 获取单例实例（需要先调用getInstance(String)初始化）
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
     * 获取单例实例
     */
    public static synchronized ResourcesTool getInstance() {
        if (sInstance == null) {
            XposedLog.w(TAG, "ResourcesTool not initialized. Call getInstance(String modulePath) first.");
        }
        return sInstance;
    }

    /**
     * 检查是否已初始化
     */
    public boolean isInit() {
        return isInit;
    }

    // ==================== 资源加载 ====================

    /**
     * 生成虚拟资源 ID
     */
    public static int getFakeResId(String resourceName) {
        return 0x7e00f000 | (resourceName.hashCode() & 0x00ffffff);
    }

    /**
     * 加载模块资源到指定 Resources 对象
     *
     * @param resources 目标 Resources 对象
     * @param doOnMainLooper 是否在主线程执行
     * @return 加载后的Resources对象，失败返回 null
     */
    public Resources loadModuleRes(Resources resources, boolean doOnMainLooper) {
        if (resources == null) {
            XposedLog.w(TAG, "Resources can't be null!");
            return null;
        }
        boolean loaded = loadResAboveApi30(resources, doOnMainLooper);
        if (loaded) {
            if (!resourcesArrayList.contains(resources)) {
                resourcesArrayList.add(resources);
            }
        } else {
            XposedLog.w(TAG, "loadModuleRes: failed to load resources: " + resources);
        }
        return resources;
    }

    /**
     * 加载模块资源到指定Resources对象（默认不在主线程执行）
     */
    public Resources loadModuleRes(Resources resources) {
        return loadModuleRes(resources, false);
    }

    /**
     * 加载模块资源到指定 Context 的 Resources
     */
    public Resources loadModuleRes(Context context, boolean doOnMainLooper) {
        return loadModuleRes(context.getResources(), doOnMainLooper);
    }

    /**
     * 加载模块资源到指定 Context 的 Resources（默认不在主线程执行）
     */
    public Resources loadModuleRes(Context context) {
        return loadModuleRes(context, false);
    }

    /**
     * API 30+ 资源加载实现
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
                    } catch (IOException ex) {
                        XposedLog.e(TAG, "Failed to add resource! debug: above api 30.", ex);
                        return false;
                    }
                }}
        }
        if (doOnMainLooper) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                return addLoaders(resources);
            } else {
                synchronized (this) {
                    if (mHandler == null) {
                        mHandler = new Handler(Looper.getMainLooper());
                    }
                }mHandler.post(() -> addLoaders(resources));
                return true;
            }
        } else {
            return addLoaders(resources);
        }}

    /**
     * 添加 ResourcesLoader 到 Resources 对象
     */
    private boolean addLoaders(Resources resources) {
        try {
            resources.addLoaders(resourcesLoader);
        } catch (IllegalArgumentException ex) {
            String expected1 = "Cannot modify resource loaders of ResourcesImpl not registered with ResourcesManager";
            if (expected1.equals(ex.getMessage())) {
                return loadResBelowApi30(resources);
            } else {
                XposedLog.e(TAG, "Failed to add loaders!", ex);
                return false;
            }
        }
        return true;
    }

    /**
     * API 30 以下的资源加载实现
     */
    @SuppressLint("DiscouragedPrivateApi")
    private boolean loadResBelowApi30(Resources resources) {
        try {
            AssetManager assets = resources.getAssets();
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            Integer cookie = (Integer) addAssetPath.invoke(assets, mModulePath);
            if (cookie == null || cookie == 0) {
                XposedLog.w(TAG, "Method 'addAssetPath' result 0, maybe load res failed!");
                return false;
            }} catch (Throwable ex) {
            XposedLog.e(TAG, "Failed to add resource! debug: below api 30.", ex);
            return false;
        }
        return true;
    }

    /**
     * 应用所有资源相关的 Hook
     */
    private synchronized void applyHooks() {
        if (hooksApplied) return;
        hooksApplied = true;

        XposedInterface xposed = BaseLoad.getXposed();
        if (xposed == null) {
            XposedLog.e(TAG, "XposedInterface not initialized!");
            return;
        }

        hookResourcesMethods();
        hookTypedArrayMethods();
    }

    /**
     * Hook Resources 类的方法
     */
    private void hookResourcesMethods() {
        Method[] resMethods = Resources.class.getDeclaredMethods();
        for (Method method : resMethods) {
            String name = method.getName();
            Class<?>[] paramTypes = method.getParameterTypes();

            if (shouldHookResourcesMethod(name, paramTypes)) {
                try {
                    XposedInterface.MethodUnhooker<?> unhook = EzxHelpUtils.hookMethod(method, ResHooker);
                    unhooks.add(unhook);
                } catch (Throwable t) {
                    XposedLog.e(TAG, "Failed to hook Resources." + name, t);
                }
            }
        }
    }

    /**
     * 判断是否需要 Hook 该 Resources 方法
     */
    private boolean shouldHookResourcesMethod(String name, Class<?>[] paramTypes) {
        return switch (name) {
            case "getInteger", "getLayout", "getBoolean", "getDimension", "getDimensionPixelOffset",
                 "getDimensionPixelSize", "getText", "getFloat", "getIntArray", "getStringArray",
                 "getTextArray", "getAnimation" ->
                paramTypes.length == 1 && paramTypes[0].equals(int.class);
            case "getColor" -> paramTypes.length == 2 && paramTypes[0].equals(int.class);
            case "getFraction", "getDrawableForDensity" ->
                paramTypes.length == 3 && paramTypes[0].equals(int.class);
            default -> false;
        };
    }

    /**
     * Hook TypedArray 类的方法
     */
    private void hookTypedArrayMethods() {
        Method[] typedMethods = TypedArray.class.getDeclaredMethods();
        for (Method method : typedMethods) {
            if (method.getName().equals("getColor")) {
                Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 2 && paramTypes[0].equals(int.class) && paramTypes[1].equals(int.class)) {
                    try {
                        XposedInterface.MethodUnhooker<?> unhook = EzxHelpUtils.hookMethod(method, TypedArrayHooker);
                        unhooks.add(unhook);
                    } catch (Throwable t) {
                        XposedLog.e(TAG, "Failed to hook TypedArray.getColor", t);
                    }
                }
            }
        }
    }

    public void unHookRes() {
        if (unhooks.isEmpty()) {
            isInit = false;
            return;
        }
        for (XposedInterface.MethodUnhooker<?> unhook : unhooks) {
            unhook.unhook();
        }
        unhooks.clear();
        resIdCache.clear();
        currentResId.remove();
        isInit = false;
        hooksApplied = false;
    }

    /**
     * TypedArray.getColor() Hook 回调
     */
    private final IMethodHook TypedArrayHooker = new IMethodHook() {
        @Override
        public void before(BeforeHookParam callback) {
            Object[] args = callback.getArgs();
            if (args == null || args.length < 1) return;

            int index = (int) args[0];
            Object thisObject = callback.getThisObject();

            int[] mData = (int[]) EzxHelpUtils.getObjectField(thisObject, "mData");
            if (mData == null || index + 3 >= mData.length) return;

            int type = mData[index];
            int id = mData[index + 3];

            if (id != 0 && (type != TypedValue.TYPE_NULL)) {
                Resources mResources = (Resources) EzxHelpUtils.getObjectField(thisObject, "mResources");
                Object value = getTypedArrayReplacement(mResources, id);
                if (value != null) {
                    callback.setResult(value);
                }
            }
        }
    };

    /**
     * Resources 方法 Hook 回调
     */
    private final IMethodHook ResHooker = new IMethodHook() {
        @Override
        public void before(BeforeHookParam callback) {
            // 确保已加载模块资源
            if (resourcesArrayList.isEmpty()) {
                Context context = ContextUtils.getContextNoError(ContextUtils.FLAG_CURRENT_APP);
                if (context != null) {
                    Resources resources = loadModuleRes(context);
                    if (resources != null) {
                        resourcesArrayList.add(resources);
                    }
                }
            }

            Object[] args = callback.getArgs();
            if (args == null || args.length < 1) return;

            int reqId = (int) args[0];
            int previousId = currentResId.get();
            if (previousId == reqId) {
                return;
            }

            currentResId.set(reqId);
            try {
                Resources thisRes = (Resources) callback.getThisObject();
                String methodName = callback.getMember().getName();

                // 遍历所有加载的模块资源
                for (Resources resources : resourcesArrayList) {
                    if (resources == null) continue;

                    Object value;
                    try {
                        value = getResourceReplacement(resources, thisRes, methodName, args);
                    } catch (Resources.NotFoundException ex) {
                        continue;
                    }

                    if (value != null) {
                        Object finalResult = convertResultType(methodName, value);
                        if (finalResult != null) {
                            callback.setResult(finalResult);
                        } else {
                            XposedLog.w(TAG, "Mismatched replacement type for method " + methodName +". Got " + value.getClass().getName());
                        }
                        break;
                    }
                }
            } finally {
                currentResId.set(previousId);
            }
        }
    };

    /**
     * 根据方法名转换返回值类型
     */
    private Object convertResultType(String methodName, Object value) {
        switch (methodName) {
            case "getInteger", "getColor", "getDimensionPixelOffset", "getDimensionPixelSize":
                if (value instanceof Number) {
                    return Math.round(((Number) value).floatValue());
                }
                break;

            case "getDimension", "getFloat":
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
                }
                break;

            case "getText":
                if (value instanceof CharSequence) {
                    return value;
                }
                break;

            case "getBoolean":
                if (value instanceof Boolean) {
                    return value;
                }
                break;

            default:
                return value;
        }
        return null;
    }

    /**
     * 设置资源 ID 替换
     *
     * @param pkg 包名（支持通配符 "*"）
     * @param type 资源类型（如 "drawable"、"color"）
     * @param name 资源名称
     * @param replacementResId 替换的资源 ID
     */
    public void setResReplacement(String pkg, String type, String name, int replacementResId) {
        try {
            applyHooks();
            replacements.put(new ResKey(pkg, type, name), new Pair<>(ReplacementType.ID, replacementResId));
        } catch (Throwable t) {
            XposedLog.e(TAG, "setResReplacement failed", t);
        }
    }

    /**
     * 设置密度相关的数值替换
     *
     * @param pkg 包名（支持通配符"*"）
     * @param type 资源类型
     * @param name 资源名称
     * @param replacementResValue 替换的数值（会乘以屏幕密度）
     */
    public void setDensityReplacement(String pkg, String type, String name, float replacementResValue) {
        try {
            applyHooks();
            replacements.put(new ResKey(pkg, type, name), new Pair<>(ReplacementType.DENSITY, replacementResValue));
        } catch (Throwable t) {
            XposedLog.e(TAG, "setDensityReplacement failed", t);
        }
    }

    /**
     * 设置对象替换
     *
     * @param pkg 包名（支持通配符 "*"）
     * @param type 资源类型
     * @param name 资源名称
     * @param replacementResValue 替换的对象
     */
    public void setObjectReplacement(String pkg, String type, String name, Object replacementResValue) {
        try {
            applyHooks();
            replacements.put(new ResKey(pkg, type, name), new Pair<>(ReplacementType.OBJECT, replacementResValue));
        } catch (Throwable t) {
            XposedLog.e(TAG, "setObjectReplacement failed", t);
        }
    }

    /**
     * 获取资源替换值
     *
     * @param resources 模块 Resources 对象
     * @param res 目标 Resources 对象
     * @param method 调用的方法名
     * @param args 方法参数
     * @return 替换值，如果没有替换则返回 null
     */
    private Object getResourceReplacement(Resources resources, Resources res, String method, Object[] args)
        throws Resources.NotFoundException {
        if (resources == null) return null;

        int resId = (int) args[0];
        if (resId == 0) return null;

        ResKey resKey = resIdCache.computeIfAbsent(resId, id -> {
            try {
                String pkgName = res.getResourcePackageName(id);
                String resType = res.getResourceTypeName(id);
                String resName = res.getResourceEntryName(id);

                if (pkgName == null || resType == null || resName == null) {
                    return null;
                }
                return new ResKey(pkgName, resType, resName);
            } catch (Throwable ignore) {
                return null;
            }
        });

        if (resKey == null) return null;

        // 查询替换规则
        Pair<ReplacementType, Object> replacement = replacements.get(resKey);
        if (replacement == null && !resKey.pkg.equals("*")) {
            replacement = replacements.get(new ResKey("*", resKey.type, resKey.name));
        }

        if (replacement != null) {
            return handleReplacement(replacement, resources, res, method, args);
        }
        return null;
    }

    /**
     * 处理资源替换
     */
    private Object handleReplacement(Pair<ReplacementType, Object> replacement, Resources resources,Resources res, String method, Object[] args)
        throws Resources.NotFoundException {
        return switch (replacement.first) {
            case OBJECT -> handleObjectReplacement(replacement.second, method);
            case DENSITY -> handleDensityReplacement(replacement.second, res, method);
            case ID -> handleIdReplacement(replacement.second, resources, method, args);
        };
    }

    /**
     * 处理对象类型替换
     */
    private Object handleObjectReplacement(Object value, String method) {
        if ("getText".equals(method) && !(value instanceof CharSequence)) {
            XposedLog.w(TAG, "Mismatched type: OBJECT replacement is not a CharSequence for getText method.");
            return null;
        }
        return value;
    }

    /**
     * 处理密度类型替换
     */
    private Object handleDensityReplacement(Object value, Resources res, String method) {
        if ("getText".equals(method)) {
            XposedLog.w(TAG, "Mismatched type: DENSITY replacement cannot be used for getText method.");
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).floatValue() * res.getDisplayMetrics().density;
        } else if (value instanceof String) {
            try {
                return Float.parseFloat((String) value) * res.getDisplayMetrics().density;
            } catch (NumberFormatException ignored) {
            }
        }

        XposedLog.w(TAG, "Invalid DENSITY replacement type: " + value);
        return null;
    }

    /**
     * 处理 ID 类型替换
     */
    private Object handleIdReplacement(Object value, Resources resources, String method, Object[] args)
        throws Resources.NotFoundException {
        if (!(value instanceof Number)) return null;

        int modResId = ((Number) value).intValue();
        if (modResId == 0) return null;

        try {
            resources.getResourceName(modResId);
        } catch (Resources.NotFoundException n) {
            throw n;
        }

        if (method == null) return null;

        resMap.add(modResId);
        try {
            return callResourceMethod(resources, method, modResId, args);
        } finally {
            resMap.remove(modResId);
        }
    }

    /**
     * 调用 Resources 方法
     */
    private Object callResourceMethod(Resources resources, String method, int modResId, Object[] args) {
        if ("getDrawable".equals(method) && args.length >= 2) {
            return EzxHelpUtils.callMethod(resources, method, modResId, args[1]);
        } else if (("getDrawableForDensity".equals(method) || "getFraction".equals(method)) && args.length >= 3) {
            return EzxHelpUtils.callMethod(resources, method, modResId, args[1], args[2]);
        } else {
            return EzxHelpUtils.callMethod(resources, method, modResId);
        }
    }

    /**
     * 获取 TypedArray 资源替换值
     *
     * @param resources 模块 Resources 对象
     * @param id 资源 ID
     * @return 替换值，如果没有替换则返回 null
     */
    private Object getTypedArrayReplacement(Resources resources, int id) {
        if (id == 0) return null;

        ResKey resKey = resIdCache.computeIfAbsent(id, resId -> {
            try {
                String pkgName = resources.getResourcePackageName(resId);
                String resType = resources.getResourceTypeName(resId);
                String resName = resources.getResourceEntryName(resId);

                if (pkgName == null || resType == null || resName == null) {
                    return null;
                }
                return new ResKey(pkgName, resType, resName);
            } catch (Throwable ignore) {
                return null;
            }
        });

        if (resKey == null) return null;

        try {
            Pair<ReplacementType, Object> replacement = replacements.get(resKey);
            if (replacement == null && !resKey.pkg.equals("*")) {
                replacement = replacements.get(new ResKey("*", resKey.type, resKey.name));
            }

            if (replacement != null && replacement.first == ReplacementType.OBJECT) {
                return replacement.second;
            }
        } catch (Throwable ex) {
            XposedLog.e(TAG, "getTypedArrayReplacement failed", ex);
        }
        return null;
    }

    public void clearResIdCache() {
        resIdCache.clear();
        XposedLog.d(TAG, "Resource ID cache cleared");
    }

    public int getCacheSize() {
        return resIdCache.size();
    }
}

