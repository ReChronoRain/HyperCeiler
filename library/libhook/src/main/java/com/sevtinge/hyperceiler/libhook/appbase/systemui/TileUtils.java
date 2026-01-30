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
package com.sevtinge.hyperceiler.libhook.appbase.systemui;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.widget.Switch;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * 磁贴工具基类
 * <p>
 * 提供创建和管理自定义磁贴的基础设施
 *
 * <h3>使用方式</h3>
 * <pre>
 * public class MyTile extends TileUtils {
 *
 *     {@literal @}Override
 *     protected TileConfig onCreateTileConfig() {
 *         return new TileConfig.Builder()
 *             .setTileClass(findClassIfExists("com.android.systemui.qs.tiles.XXXTile"))
 *             .setTileName("custom_my_tile")
 *             .setTileProvider("xxxTileProvider")
 *             .setLabelResId(R.string.my_tile_label)
 *             .setIcons(R.drawable.ic_on, R.drawable.ic_off)
 *             .build();
 *     }
 *
 *     {@literal @}Override
 *     protected void onTileClick(TileContext ctx) {
 *         toggleMyFeature();
 *         ctx.refreshState();
 *     }
 *
 *     {@literal @}Override
 *     protected TileState onUpdateState(TileContext ctx) {
 *         return new TileState(isMyFeatureEnabled());
 *     }
 * }
 * </pre>
 *
 * <h3>两种模式</h3>
 * <ul>
 *<li><b>自定义模式</b>: 设置 tileName，创建新磁贴</li>
 *<li><b>覆写模式</b>: 不设置 tileName，修改现有磁贴行为</li>
 * </ul>
 */
public abstract class TileUtils extends BaseHook {

    // ==================== 系统类名常量 ====================

    private static final String CLASS_QS_FACTORY = "com.android.systemui.qs.tileimpl.MiuiQSFactory";
    private static final String CLASS_RESOURCE_ICON = "com.android.systemui.qs.tileimpl.QSTileImpl$ResourceIcon";
    private static final String CLASS_EXPANDABLE = "com.android.systemui.animation.Expandable";
    private static final String CLASS_CONTROL_CENTER_UTILS = "com.android.systemui.controlcenter.utils.ControlCenterUtils";
    private static final String CLASS_SYSTEMUI_APP = "com.android.systemui.SystemUIApplication";

    private static final String FIELD_CUSTOM_NAME = "hc_customTileName";
    private static final String METHOD_CREATE_TILE = "createTile";

    // ==================== 缓存字段 ====================

    private TileConfig mConfig;
    private Class<?> mResourceIconClass;
    private Class<?> mExpandableClass;
    private volatile boolean mIsRegistered = false;

    // ==================== 抽象方法 ====================

    /**
     * 创建磁贴配置（必须实现）
     *
     * @return 磁贴配置，不能为 null
     */
    @NonNull
    protected abstract TileConfig onCreateTileConfig();

    // ==================== 可覆写的回调方法 ====================

    /**
     * 检查磁贴是否可用
     * <p>
     * 用于控制磁贴在控制中心是否显示
     *
     * @param ctx 磁贴上下文
     * @return true 表示可用，false 表示不可用
     */
    protected boolean onCheckAvailable(TileContext ctx) {
        return true;
    }

    /**
     * 处理磁贴点击事件
     * <p>
     * 在此方法中实现点击切换逻辑，完成后调用 {@code ctx.refreshState()} 刷新状态
     *
     * @param ctx 磁贴上下文
     */
    protected void onTileClick(TileContext ctx) {
        // 默认空实现
    }

    /**
     * 处理磁贴点击后的逻辑（在原逻辑之后执行）
     * <p>
     * 仅当 {@link #needClickAfter()} 返回 true 时才会调用
     *
     * @param ctx 磁贴上下文
     */
    protected void onTileClickAfter(TileContext ctx) {
        // 默认空实现
    }

    /**
     * 获取长按跳转的 Intent
     * <p>
     * 返回的 Intent 会被系统用于长按磁贴时的跳转
     *
     * @param ctx 磁贴上下文
     * @return 跳转 Intent，返回 null 则使用原有逻辑
     */
    @Nullable
    protected Intent onGetLongClickIntent(TileContext ctx) {
        return null;
    }

    /**
     * 处理长按事件（用于自定义长按行为）
     * <p>
     * 返回 Intent则执行跳转，返回 null 则不处理
     *
     * @param ctx 磁贴上下文
     * @return 跳转 Intent，返回 null 则不处理
     */
    @Nullable
    protected Intent onHandleLongClick(TileContext ctx) {
        return null;
    }

    /**
     * 监听状态变化
     *
     * @param ctx       磁贴上下文
     * @param listening true 表示开始监听，false 表示停止监听
     */
    protected void onListeningChanged(TileContext ctx, boolean listening) {
        // 默认空实现
    }

    /**
     * 更新磁贴状态
     * <p>
     * 返回 {@link TileState} 对象来更新磁贴的显示状态
     *
     * @param ctx 磁贴上下文
     * @return 磁贴状态，返回 null 则使用原有逻辑
     */
    @Nullable
    protected TileState onUpdateState(TileContext ctx) {
        return null;
    }

    /**
     * 是否需要在点击后执行逻辑
     * <p>
     * 返回 true 则会在原点击逻辑执行后调用 {@link #onTileClickAfter(TileContext)}
     *
     * @return 是否需要 after 回调
     */
    protected boolean needClickAfter() {
        return false;
    }

    // ==================== 初始化 ====================

    @Override
    @CallSuper
    public void init() {
        // 获取配置
        mConfig = onCreateTileConfig();
        if (mConfig == null) {
            XposedLog.e(TAG, "onCreateTileConfig() returned null");
            return;
        }
        if (mConfig.getTileClass() == null) {
            XposedLog.e(TAG, "TileConfig.tileClass is null");
            return;
        }

        // 缓存常用 Class
        mResourceIconClass = findClassIfExists(CLASS_RESOURCE_ICON);
        mExpandableClass = findClassIfExists(CLASS_EXPANDABLE);

        if (mResourceIconClass == null) {
            XposedLog.e(TAG, "ResourceIcon class not found, tile hooks may not work");return;
        }

        // 自定义磁贴模式：注册并 Hook 创建逻辑
        if (mConfig.isCustomTile()) {
            registerCustomTile();
            hookTileCreation();
        }

        // Hook 磁贴方法
        hookTileMethods();

        XposedLog.d(TAG, "Tile initialized: " + mConfig);
    }

    // ==================== 磁贴注册 ====================

    /**
     * 注册自定义磁贴到系统
     */
    private void registerCustomTile() {
        findAndHookMethod(CLASS_SYSTEMUI_APP, "onCreate", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                if (mIsRegistered) return;
                mIsRegistered = true;

                Context context = (Context) EzxHelpUtils.callMethod(
                    param.getThisObject(), "getApplicationContext");
                registerTileToStock(context);
            }
        });
    }

    @SuppressLint("DiscouragedApi")
    private void registerTileToStock(Context context) {
        String tileName = mConfig.getTileName();
        String packageName = getPackageName();

        int stockTilesResId = context.getResources().getIdentifier(
            "miui_quick_settings_tiles_stock", "string", packageName);

        if (stockTilesResId == 0) {
            XposedLog.e(TAG, "Failed to find stock tiles resource");
            return;
        }

        String stockTiles = context.getString(stockTilesResId) + "," + tileName;

        // 根据设备类型选择资源名
        String[] resNames = isPad()
            ? new String[]{"miui_quick_settings_tiles_stock_pad", "quick_settings_tiles_stock"}
            : new String[]{"miui_quick_settings_tiles_stock", "quick_settings_tiles_stock"};

        for (String resName : resNames) {
            setObjectReplacement(packageName, "string", resName, stockTiles);setObjectReplacement("miui.systemui.plugin", "string", resName, stockTiles);
        }

        XposedLog.d(TAG, "Registered tile: " + tileName);
    }

    /**
     * Hook 磁贴创建逻辑
     */
    private void hookTileCreation() {
        Class<?> qsFactory = findClassIfExists(CLASS_QS_FACTORY);
        if (qsFactory == null) {
            XposedLog.e(TAG, "QSFactory class not found");
            return;
        }

        findAndHookMethod(qsFactory, METHOD_CREATE_TILE, String.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                String requestedTile = (String) param.getArgs()[0];
                if (!mConfig.getTileName().equals(requestedTile)) return;

                try {
                    Object tile = createTileInstance(param.getThisObject());
                    if (tile != null) {
                        param.setResult(tile);
                    }
                } catch (Throwable t) {
                    XposedLog.e(TAG, "Failed to create tile: " + mConfig.getTileName(), t);
                }
            }
        });
    }

    /**
     * 创建磁贴实例
     */
    private Object createTileInstance(Object factory) {
        String provider = mConfig.getTileProvider();
        if (provider.isEmpty()) {
            XposedLog.e(TAG, "TileProvider is not set");
            return null;
        }

        Object providerObj = EzxHelpUtils.getObjectField(factory, provider);
        if (providerObj == null) {
            XposedLog.e(TAG, "Provider not found: " + provider);
            return null;
        }

        Object tile = EzxHelpUtils.callMethod(providerObj, "get");
        if (tile == null) {
            XposedLog.e(TAG, "Failed to get tile from provider: " + provider);
            return null;
        }

        // 标记为自定义磁贴
        EzxHelpUtils.setAdditionalInstanceField(tile, FIELD_CUSTOM_NAME, mConfig.getTileName());

        // 初始化磁贴
        initializeTile(tile);

        return tile;
    }

    /**
     * 初始化磁贴
     */
    private void initializeTile(Object tile) {
        Object handler = EzxHelpUtils.getObjectField(tile, "mHandler");
        if (handler != null) {
            EzxHelpUtils.callMethod(handler, "sendEmptyMessage", 12);
            EzxHelpUtils.callMethod(handler, "sendEmptyMessage", 11);
        }
        EzxHelpUtils.callMethod(tile, "setTileSpec", mConfig.getTileName());
    }

    //==================== Hook 磁贴方法 ====================

    private void hookTileMethods() {
        Class<?> tileClass = mConfig.getTileClass();

        hookIsAvailable(tileClass);
        hookGetTileLabel(tileClass);
        hookHandleSetListening(tileClass);
        hookGetLongClickIntent(tileClass);
        hookHandleLongClick(tileClass);
        hookHandleClick(tileClass);
        hookHandleUpdateState(tileClass);
        hookHandleShowStateMessage(tileClass);
    }

    // ==================== 方法覆写检查 ====================

    private boolean isMethodOverridden(String methodName, Class<?>... parameterTypes) {
        try {
            java.lang.reflect.Method method = this.getClass().getDeclaredMethod(methodName, parameterTypes);
            return !method.getDeclaringClass().equals(TileUtils.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    // ==================== 各个 Hook 方法 ====================

    private void hookIsAvailable(Class<?> tileClass) {
        if (!mConfig.isCustomTile() && !isMethodOverridden("onCheckAvailable", TileContext.class)) {
            return;
        }

        safeHookMethod(tileClass, "isAvailable", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                if (!shouldHandle(param)) return;

                TileContext ctx = new TileContext(param);
                boolean available = onCheckAvailable(ctx);
                param.setResult(available);
            }
        });
    }

    private void hookGetTileLabel(Class<?> tileClass) {
        if (!mConfig.isCustomTile() || !mConfig.hasLabel()) {
            return;
        }

        safeHookMethod(tileClass, "getTileLabel", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                if (!shouldHandle(param)) return;

                TileContext ctx = new TileContext(param);
                Resources modRes = AppsTool.getModuleRes(ctx.getContext());
                param.setResult(modRes.getString(mConfig.getLabelResId()));
            }
        });
    }

    private void hookHandleSetListening(Class<?> tileClass) {
        if (!mConfig.isCustomTile() && !isMethodOverridden("onListeningChanged", TileContext.class, boolean.class)) {
            return;
        }

        safeHookMethod(tileClass, "handleSetListening", boolean.class, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                if (!shouldHandle(param)) return;

                TileContext ctx = new TileContext(param);
                boolean listening = (boolean) param.getArgs()[0];
                onListeningChanged(ctx, listening);
                // 不设置 result，让原方法继续执行
            }
        });
    }

    private void hookGetLongClickIntent(Class<?> tileClass) {
        if (!mConfig.isCustomTile() && !isMethodOverridden("onGetLongClickIntent", TileContext.class)) {
            return;
        }

        safeHookMethod(tileClass, "getLongClickIntent", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                if (!shouldHandle(param)) return;

                TileContext ctx = new TileContext(param);
                Intent intent = onGetLongClickIntent(ctx);
                if (intent != null) {
                    param.setResult(intent);
                }
            }
        });
    }

    private void hookHandleLongClick(Class<?> tileClass) {
        if (mExpandableClass == null) return;

        if (!mConfig.isCustomTile() && !isMethodOverridden("onHandleLongClick", TileContext.class)) {
            return;
        }

        safeHookMethod(tileClass, "handleLongClick", mExpandableClass, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                if (!shouldHandle(param)) return;

                TileContext ctx = new TileContext(param);
                Intent intent = onHandleLongClick(ctx);
                if (intent != null) {
                    launchIntent(ctx, intent);param.setResult(null);
                }
            }
        });
    }

    private void hookHandleClick(Class<?> tileClass) {
        if (mExpandableClass == null) return;

        boolean needHook = mConfig.isCustomTile() ||
            isMethodOverridden("onTileClick", TileContext.class) ||
            mConfig.hasIcons();

        if (!needHook) return;

        safeHookMethod(tileClass, "handleClick", mExpandableClass, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                if (!isMethodOverridden("onTileClick", TileContext.class)) {
                    return;
                }

                if (!shouldHandle(param)) return;

                TileContext ctx = new TileContext(param);
                try {
                    onTileClick(ctx);
                    param.setResult(null);
                } catch (Throwable t) {
                    XposedLog.e(TAG, "Error in onTileClick", t);
                    param.setResult(null);
                }
            }

            @Override
            public void after(AfterHookParam param) {
                if (!shouldHandle(param)) return;

                // 如果配置了图标，延迟刷新状态确保系统状态已更新
                if (mConfig.hasIcons() && !isMethodOverridden("onTileClick", TileContext.class)) {
                    Object tile = param.getThisObject();
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.postDelayed(() -> {
                        try {
                            EzxHelpUtils.callMethod(tile, "refreshState");
                        } catch (Throwable t) {
                            XposedLog.e(TAG, "Failed to delayed refresh state", t);
                        }
                    }, 50);
                }

                if (!needClickAfter()) return;

                TileContext ctx = new TileContext(param);
                try {
                    onTileClickAfter(ctx);
                } catch (Throwable t) {
                    XposedLog.e(TAG, "Error in onTileClickAfter", t);
                }
            }
        });
    }


    private void hookHandleUpdateState(Class<?> tileClass) {
        if (!mConfig.isCustomTile() && !isMethodOverridden("onUpdateState", TileContext.class)
            && !mConfig.hasIcons()) {
            return;
        }

        hookAllMethods(tileClass, "handleUpdateState", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                if (!shouldHandle(param)) return;

                TileContext ctx = new TileContext(param);
                TileState state = onUpdateState(ctx);

                if (state != null) {
                    // 自定义磁贴：完全设置状态
                    // 覆写模式：覆盖部分状态
                    if (mConfig.isCustomTile()) {
                        applyTileState(ctx, state);
                    } else {
                        applyTileStateOverlay(ctx, state);
                    }
                } else if (mConfig.hasIcons()) {
                    // 没有自定义状态，但有配置图标，只替换图标
                    applyConfigIcons(ctx);
                }
            }
        });
    }

    private void hookHandleShowStateMessage(Class<?> tileClass) {
        if (!mConfig.isCustomTile()) {
            return;
        }

        safeHookMethod(tileClass, "handleShowStateMessage", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                if (!shouldHandle(param)) return;

                TileContext ctx = new TileContext(param);
                showStateMessage(ctx);
                param.setResult(null);
            }
        });
    }

    private void applyConfigIcons(TileContext ctx) {
        Object booleanState = ctx.getArg(0);
        if (booleanState == null) {
            XposedLog.e(TAG, "applyConfigIcons: booleanState is null");
            return;
        }

        try {
            // state: 0=UNAVAILABLE, 1=INACTIVE, 2=ACTIVE
            int state = (int) EzxHelpUtils.getObjectField(booleanState, "state");
            boolean enabled = (state == TileState.STATE_ACTIVE);

            int iconResId = mConfig.getIconByState(enabled);

            if (mResourceIconClass != null && iconResId != -1) {
                Object icon = EzxHelpUtils.callStaticMethod(mResourceIconClass, "get", iconResId);
                EzxHelpUtils.setObjectField(booleanState, "icon", icon);
            }
        } catch (Throwable t) {
            XposedLog.e(TAG, "applyConfigIcons error", t);
        }
    }
    private void applyTileStateOverlay(TileContext ctx, TileState state) {
        Object booleanState = ctx.getArg(0);
        if (booleanState == null) return;

        try {
            int stateValue = (int) EzxHelpUtils.getObjectField(booleanState, "state");
            boolean enabled = (stateValue == TileState.STATE_ACTIVE);

            // 覆盖标签
            String label = state.getLabel();
            if (label != null) {
                EzxHelpUtils.setObjectField(booleanState, "label", label);
                EzxHelpUtils.setObjectField(booleanState, "contentDescription", label);
            }

            // 覆盖图标
            int iconResId = state.getIconResId();
            if (iconResId == -1 && mConfig.hasIcons()) {
                iconResId = mConfig.getIconByState(enabled);
            }
            if (iconResId != -1 && mResourceIconClass != null) {
                Object icon = EzxHelpUtils.callStaticMethod(mResourceIconClass, "get", iconResId);
                EzxHelpUtils.setObjectField(booleanState, "icon", icon);
            }
        } catch (Throwable t) {
            XposedLog.e(TAG, "applyTileStateOverlay error", t);
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 判断是否应该处理该磁贴 (BeforeHookParam 版本)
     */
    private boolean shouldHandle(BeforeHookParam param) {
        if (!mConfig.isCustomTile()) {
            return true; // 覆写模式，始终处理
        }

        String tileName = (String) EzxHelpUtils.getAdditionalInstanceField(
            param.getThisObject(), FIELD_CUSTOM_NAME);
        return mConfig.getTileName().equals(tileName);
    }

    /**
     * 判断是否应该处理该磁贴 (AfterHookParam 版本)
     */
    private boolean shouldHandle(AfterHookParam param) {
        if (!mConfig.isCustomTile()) {
            return true;
        }

        String tileName = (String) EzxHelpUtils.getAdditionalInstanceField(
            param.getThisObject(), FIELD_CUSTOM_NAME);
        return mConfig.getTileName().equals(tileName);
    }

    /**
     * 应用磁贴状态
     */
    private void applyTileState(TileContext ctx, TileState state) {
        Object booleanState = ctx.getArg(0);
        if (booleanState == null) return;

        // 设置基础状态
        EzxHelpUtils.setObjectField(booleanState, "value", state.isEnabled());
        EzxHelpUtils.setObjectField(booleanState, "state", state.getStateValue());

        // 设置标签
        String label = state.getLabel();
        if (label == null) {
            label = ctx.getTileLabel();
        }
        EzxHelpUtils.setObjectField(booleanState, "label", label);
        EzxHelpUtils.setObjectField(booleanState, "contentDescription", label);
        EzxHelpUtils.setObjectField(booleanState, "expandedAccessibilityClassName", Switch.class.getName());

        // 设置图标
        int iconResId = state.getIconResId();
        if (iconResId == -1 && mConfig.hasIcons()) {
            iconResId = mConfig.getIconByState(state.isEnabled());
        }
        if (iconResId != -1 && mResourceIconClass != null) {
            Object icon = EzxHelpUtils.callStaticMethod(mResourceIconClass, "get", iconResId);
            EzxHelpUtils.setObjectField(booleanState, "icon", icon);
        }
    }

    /**
     * 启动 Intent
     */
    private void launchIntent(TileContext ctx, Intent intent) {
        try {
            Class<?> utilsClass = findClassIfExists(CLASS_CONTROL_CENTER_UTILS);
            if (utilsClass == null) {
                XposedLog.e(TAG, "ControlCenterUtils class not found");
                return;
            }

            Object splitIntent = EzxHelpUtils.callStaticMethod(
                utilsClass, "getSettingsSplitIntent", ctx.getContext(), intent);

            Object activityStarter = ctx.getField("mActivityStarter");
            EzxHelpUtils.callMethod(activityStarter,
                "postStartActivityDismissingKeyguard", splitIntent,0, null);
        } catch (Throwable t) {
            XposedLog.e(TAG, "Failed to launch intent", t);
        }
    }

    /**
     * 显示状态消息
     */
    private void showStateMessage(TileContext ctx) {
        try {
            Object message = ctx.callMethod("getStateMessage");
            ctx.callMethod("showStateMessage", message);
        } catch (Throwable t) {
            // Fallback：手动构建消息
            showStateMessageFallback(ctx);
        }
    }

    private void showStateMessageFallback(TileContext ctx) {
        try {
            Object state = ctx.getField("mState");
            if (state == null) return;

            int stateValue = (int) EzxHelpUtils.getObjectField(state, "state");
            String message = getMessage(ctx, stateValue);

            ctx.callMethod("showStateMessage", message);
        } catch (Throwable t) {
            XposedLog.e(TAG, "Failed to show state message", t);
        }
    }

    @Nullable
    private static String getMessage(TileContext ctx, int stateValue) {
        Context context = ctx.getContext();
        String label = ctx.getTileLabel();

        String message;
        if (stateValue == TileState.STATE_ACTIVE) {
            message = context.getString(R.string.quick_settings_state_change_message_on_my, label);
        } else if (stateValue == TileState.STATE_INACTIVE) {
            message = context.getString(R.string.quick_settings_state_change_message_off_my, label);
        } else {
            message = null;
        }
        return message;
    }

    /**
     * 安全 Hook 方法（忽略方法不存在的情况）
     */
    private void safeHookMethod(Class<?> clazz, String methodName, Object... paramsAndCallback) {
        try {
            // 提取参数类型（排除最后一个回调）
            int paramCount = paramsAndCallback.length - 1;
            Class<?>[] paramTypes = new Class<?>[paramCount];

            for (int i = 0; i < paramCount; i++) {
                Object param = paramsAndCallback[i];
                if (param instanceof Class) {
                    paramTypes[i] = (Class<?>) param;
                } else {
                    paramTypes[i] = param.getClass();
                }
            }

            // 检查方法是否存在
            clazz.getDeclaredMethod(methodName, paramTypes);

            // 存在则 Hook
            findAndHookMethod(clazz, methodName, paramsAndCallback);
        } catch (NoSuchMethodException e) {
            XposedLog.d(TAG, "Method not found (skipping): " + clazz.getSimpleName() + "." + methodName);
        } catch (Throwable t) {
            XposedLog.e(TAG, "Failed to hook method: " + methodName, t);
        }
    }

    // ==================== Getter ====================

    /**
     * 获取当前磁贴配置
     */
    @NonNull
    protected TileConfig getConfig() {
        return mConfig;
    }

    /**
     * 获取 ResourceIcon 类
     */
    @Nullable
    protected Class<?> getResourceIconClass() {
        return mResourceIconClass;
    }

    /**
     * 获取 Expandable 类
     */
    @Nullable
    protected Class<?> getExpandableClass() {
        return mExpandableClass;
    }
}
