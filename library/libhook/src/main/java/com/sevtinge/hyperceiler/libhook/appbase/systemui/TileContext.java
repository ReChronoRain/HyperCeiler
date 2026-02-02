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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

/**
 * 磁贴上下文类
 * <p>
 * 封装磁贴操作的常用方法，简化子类实现
 *
 * <pre>
 * protected void onTileClick(TileContext ctx) {*     // 获取上下文
 *     Context context = ctx.getContext();
 *
 *     // 刷新状态
 *     ctx.refreshState();
 *
 *     // 存取附加字段
 *     ctx.setAdditionalField("myKey", myValue);
 *     MyType value = ctx.getAdditionalField("myKey");
 *
 *     // 获取内部字段
 *     Object controller = ctx.getField("mController");
 * }
 * </pre>
 */
public final class TileContext {

    @NonNull
    private final Object tileInstance;
    @NonNull
    private final Object[] args;
    @Nullable
    private final ResultSetter resultSetter;

    //懒加载缓存
    private Context cachedContext;
    private Handler cachedMainHandler;

    /**
     * 结果设置器接口
     */
    @FunctionalInterface
    public interface ResultSetter {
        void setResult(@Nullable Object result);
    }

    // ==================== 构造方法 ====================

    /**
     * 从 BeforeHookParam 创建（可以设置返回值）
     *
     * @param param BeforeHookParam
     */
    public TileContext(@NonNull BeforeHookParam param) {
        this.tileInstance = param.getThisObject();
        this.args = param.getArgs() != null ? param.getArgs() : new Object[0];
        this.resultSetter = param::setResult;
    }

    /**
     * 从 AfterHookParam 创建（不能设置返回值）
     *
     * @param param AfterHookParam
     */
    public TileContext(@NonNull AfterHookParam param) {
        this.tileInstance = param.getThisObject();
        this.args = param.getArgs() != null ? param.getArgs() : new Object[0];
        this.resultSetter = null;  // after阶段不能设置结果
    }

    /**
     * 通用构造方法
     *
     * @param tileInstance 磁贴实例
     * @param args         方法参数
     * @param resultSetter 结果设置器，可为 null
     */
    public TileContext(@NonNull Object tileInstance, @Nullable Object[] args, @Nullable ResultSetter resultSetter) {
        this.tileInstance = tileInstance;
        this.args = args != null ? args : new Object[0];
        this.resultSetter = resultSetter;
    }

    // ==================== 基础访问 ====================

    /**
     * 获取磁贴实例
     */
    @NonNull
    public Object getTileInstance() {
        return tileInstance;
    }

    /**
     * 获取方法参数数组
     */
    @NonNull
    public Object[] getArgs() {
        return args;
    }

    /**
     * 获取指定位置的方法参数
     *
     * @param index 参数索引
     * @param <T>   返回类型
     * @return 参数值，索引无效时返回 null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getArg(int index) {
        if (index >= 0 && index < args.length) {
            return (T) args[index];
        }
        return null;
    }

    /**
     * 是否可以设置返回值
     * <p>
     * before 阶段可以设置，after 阶段不可以
     */
    public boolean canSetResult() {
        return resultSetter != null;
    }

    // ==================== Context 相关 ====================

    /**
     * 获取磁贴的Context
     *
     * @throws IllegalStateException 如果获取失败
     */
    @NonNull
    public Context getContext() {
        if (cachedContext == null) {
            cachedContext = getField("mContext");
            if (cachedContext == null) {
                throw new IllegalStateException("Failed to get mContext from tile");
            }
        }
        return cachedContext;
    }

    /**
     * 获取 ContentResolver
     */
    @NonNull
    public ContentResolver getContentResolver() {
        return getContext().getContentResolver();
    }

    /**
     * 获取模块资源
     */
    @NonNull
    public Resources getModuleRes() {
        return AppsTool.getModuleRes(getContext());
    }

    /**
     * 获取主线程 Handler
     */
    @NonNull
    public Handler getMainHandler() {
        if (cachedMainHandler == null) {
            cachedMainHandler = new Handler(getContext().getMainLooper());
        }
        return cachedMainHandler;
    }

    // ==================== 磁贴操作 ====================

    /**
     * 刷新磁贴状态
     */
    public void refreshState() {
        EzxHelpUtils.callMethod(tileInstance, "refreshState");
    }

    /**
     *刷新磁贴状态（带参数）
     *
     * @param arg 刷新参数
     */
    public void refreshState(@Nullable Object arg) {
        EzxHelpUtils.callMethod(tileInstance, "refreshState", arg);
    }

    /**
     * 获取磁贴标签
     *
     * @return 标签文本，获取失败时返回 null
     */
    @Nullable
    public String getTileLabel() {
        Object label = EzxHelpUtils.callMethod(tileInstance, "getTileLabel");
        return label != null ? label.toString() : null;
    }

    /**
     * 获取磁贴规格名（TileSpec）
     *
     * @return 规格名，如 "wifi", "custom_5G" 等
     */
    @Nullable
    public String getTileSpec() {
        return getField("mTileSpec");
    }

    // ==================== 字段访问 ====================

    /**
     * 获取磁贴内部字段
     *
     * @param name 字段名
     * @param <T>  返回类型
     * @return 字段值
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getField(@NonNull String name) {
        return (T) EzxHelpUtils.getObjectField(tileInstance, name);
    }

    /**
     * 设置磁贴内部字段
     *
     * @param name  字段名
     * @param value 字段值
     */
    public void setField(@NonNull String name, @Nullable Object value) {
        EzxHelpUtils.setObjectField(tileInstance, name, value);
    }

    /**
     * 获取磁贴附加字段（Xposed 动态附加的字段）
     *
     * @param name 字段名
     * @param <T>  返回类型
     * @return 字段值
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getAdditionalField(@NonNull String name) {
        return (T) EzxHelpUtils.getAdditionalInstanceField(tileInstance, name);
    }

    /**
     * 设置磁贴附加字段（Xposed 动态附加的字段）
     *
     * @param name  字段名
     * @param value 字段值
     */
    public void setAdditionalField(@NonNull String name, @Nullable Object value) {
        EzxHelpUtils.setAdditionalInstanceField(tileInstance, name, value);
    }

    /**
     * 移除磁贴附加字段
     *
     * @param name 字段名
     */
    public void removeAdditionalField(@NonNull String name) {
        EzxHelpUtils.removeAdditionalInstanceField(tileInstance, name);
    }

    // ==================== 方法调用 ====================

    /**
     * 调用磁贴方法
     *
     * @param methodName 方法名
     * @param args 方法参数
     * @return 方法返回值
     */
    @Nullable
    public Object callMethod(@NonNull String methodName, Object... args) {
        return EzxHelpUtils.callMethod(tileInstance, methodName, args);
    }

    // ==================== 结果设置 ====================

    /**
     * 设置方法返回值
     * <p>
     * 注意：仅在 before 阶段有效，after 阶段调用无效果
     *
     * @param result 返回值
     */
    public void setResult(@Nullable Object result) {
        if (resultSetter != null) {
            resultSetter.setResult(result);
        }
    }

    /**
     * 设置方法返回 null（阻止原方法执行）
     */
    public void setResultNull() {
        setResult(null);
    }

    /**
     * 设置方法返回 true
     */
    public void setResultTrue() {
        setResult(Boolean.TRUE);
    }

    /**
     * 设置方法返回 false
     */
    public void setResultFalse() {
        setResult(Boolean.FALSE);
    }

    // ==================== Intent 工具方法 ====================

    /**
     * 为Intent 添加 NEW_TASK 标志
     *
     * @param intent 原Intent
     * @return 添加标志后的 Intent
     */
    @NonNull
    public static Intent newTaskIntent(@NonNull Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * 为 Intent 添加 NEW_TASK 和 CLEAR_TOP 标志
     *
     * @param intent 原 Intent
     * @return 添加标志后的 Intent
     */
    @NonNull
    public static Intent newTaskIntentClearTop(@NonNull Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    @NonNull
    @Override
    public String toString() {
        return "TileContext{" +
            "tile=" + tileInstance.getClass().getSimpleName() +
            ", tileSpec=" + getTileSpec() +
            ", argsCount=" + args.length +
            ", canSetResult=" + canSetResult() +
            '}';
    }
}
