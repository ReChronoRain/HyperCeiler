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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 磁贴状态类
 * <p>
 * 用于描述磁贴的当前状态，包括开关状态、图标和标签
 *
 * <pre>
 * // 简单用法：只设置开关状态
 * return new TileState(isEnabled);
 *
 * // 自定义图标
 * return new TileState(isEnabled, R.drawable.my_icon);
 *
 * // 完整配置
 * return new TileState(isEnabled, R.drawable.my_icon, "自定义标签");
 *
 * // 使用静态工厂方法
 * return TileState.active();
 * return TileState.inactive();
 * return TileState.unavailable();
 * </pre>
 */
public final class TileState {

    /**
     * 磁贴状态常量：不可用
     */
    public static final int STATE_UNAVAILABLE = 0;

    /**
     * 磁贴状态常量：未激活（关闭）
     */
    public static final int STATE_INACTIVE = 1;

    /**
     * 磁贴状态常量：已激活（开启）
     */
    public static final int STATE_ACTIVE = 2;

    private final boolean enabled;
    @DrawableRes
    private final int iconResId;
    @Nullable
    private final String label;
    private final int stateValue;

    /**
     * 创建简单的开关状态
     *
     * @param enabled 是否启用
     */
    public TileState(boolean enabled) {
        this(enabled, -1, null, enabled ? STATE_ACTIVE : STATE_INACTIVE);
    }

    /**
     * 创建带自定义图标的状态
     *
     * @param enabled   是否启用
     * @param iconResId 图标资源ID
     */
    public TileState(boolean enabled, @DrawableRes int iconResId) {
        this(enabled, iconResId, null, enabled ? STATE_ACTIVE : STATE_INACTIVE);
    }

    /**
     * 创建完整配置的状态
     *
     * @param enabled   是否启用
     * @param iconResId 图标资源ID
     * @param label     自定义标签
     */
    public TileState(boolean enabled, @DrawableRes int iconResId, @Nullable String label) {
        this(enabled, iconResId, label, enabled ? STATE_ACTIVE : STATE_INACTIVE);
    }

    /**
     * 创建完整配置的状态（包含自定义状态值）
     *
     * @param enabled    是否启用
     * @param iconResId  图标资源ID
     * @param label      自定义标签
     * @param stateValue 状态值 (STATE_UNAVAILABLE, STATE_INACTIVE, STATE_ACTIVE)
     */
    public TileState(boolean enabled, @DrawableRes int iconResId, @Nullable String label, int stateValue) {
        this.enabled = enabled;
        this.iconResId = iconResId;
        this.label = label;
        this.stateValue = stateValue;
    }

    // ==================== Getters ====================

    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取图标资源ID
     *
     * @return 图标资源ID，-1 表示未设置
     */
    @DrawableRes
    public int getIconResId() {
        return iconResId;
    }

    /**
     * 是否有自定义图标
     */
    public boolean hasCustomIcon() {
        return iconResId != -1;
    }

    /**
     * 获取自定义标签
     *
     * @return 标签，null 表示使用默认标签
     */
    @Nullable
    public String getLabel() {
        return label;
    }

    /**
     * 是否有自定义标签
     */
    public boolean hasCustomLabel() {
        return label != null;
    }

    /**
     * 获取状态值
     * <p>
     * 0=不可用，1 = 未激活，2 = 已激活
     */
    public int getStateValue() {
        return stateValue;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建激活状态
     */
    @NonNull
    public static TileState active() {
        return new TileState(true);
    }

    /**
     * 创建激活状态（带图标）
     *
     * @param iconResId 图标资源ID
     */
    @NonNull
    public static TileState active(@DrawableRes int iconResId) {
        return new TileState(true, iconResId);
    }

    /**
     * 创建激活状态（带图标和标签）
     *
     * @param iconResId 图标资源ID
     * @param label     标签
     */
    @NonNull
    public static TileState active(@DrawableRes int iconResId, @Nullable String label) {
        return new TileState(true, iconResId, label);
    }

    /**
     * 创建未激活状态
     */
    @NonNull
    public static TileState inactive() {
        return new TileState(false);
    }

    /**
     * 创建未激活状态（带图标）
     *
     * @param iconResId 图标资源ID
     */
    @NonNull
    public static TileState inactive(@DrawableRes int iconResId) {
        return new TileState(false, iconResId);
    }

    /**
     * 创建未激活状态（带图标和标签）
     *
     * @param iconResId 图标资源ID
     * @param label     标签
     */
    @NonNull
    public static TileState inactive(@DrawableRes int iconResId, @Nullable String label) {
        return new TileState(false, iconResId, label);
    }

    /**
     * 创建不可用状态
     */
    @NonNull
    public static TileState unavailable() {
        return new TileState(false, -1, null, STATE_UNAVAILABLE);
    }

    /**
     * 创建不可用状态（带图标）
     *
     * @param iconResId 图标资源ID
     */
    @NonNull
    public static TileState unavailable(@DrawableRes int iconResId) {
        return new TileState(false, iconResId, null, STATE_UNAVAILABLE);
    }

    /**
     * 根据条件创建状态
     *
     * @param enabled 是否启用
     */
    @NonNull
    public static TileState of(boolean enabled) {
        return enabled ? active() : inactive();
    }

    /**
     * 根据条件创建状态（带图标）
     *
     * @param enabled   是否启用
     * @param iconResId 图标资源ID
     */
    @NonNull
    public static TileState of(boolean enabled, @DrawableRes int iconResId) {
        return enabled ? active(iconResId) : inactive(iconResId);
    }

    @NonNull
    @Override
    public String toString() {
        return "TileState{" +
            "enabled=" + enabled +
            ", stateValue=" + stateValue +
            ", hasIcon=" + hasCustomIcon() +
            ", hasLabel=" + hasCustomLabel() +
            '}';
    }
}
