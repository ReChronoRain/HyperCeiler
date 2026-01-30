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
import androidx.annotation.StringRes;

/**
 * 磁贴配置类
 *<p>
 * 使用 Builder 模式构建磁贴配置
 *
 * <pre>
 * TileConfig config = new TileConfig.Builder()
 *     .setTileClass(findClass("com.android.systemui.qs.tiles.XXXTile"))
 *     .setTileName("custom_my_tile")
 *     .setTileProvider("xxxTileProvider")
 *     .setLabelResId(R.string.my_tile_label)
 *     .setIcons(R.drawable.ic_on, R.drawable.ic_off)
 *     .build();
 * </pre>
 */
public final class TileConfig {

    @Nullable
    private final Class<?> tileClass;
    @NonNull
    private final String tileName;
    @NonNull
    private final String tileProvider;
    @StringRes
    private final int labelResId;
    @DrawableRes
    private final int iconOn;
    @DrawableRes
    private final int iconOff;

    private TileConfig(Builder builder) {
        this.tileClass = builder.tileClass;
        this.tileName = builder.tileName != null ? builder.tileName : "";
        this.tileProvider = builder.tileProvider != null ? builder.tileProvider : "";
        this.labelResId = builder.labelResId;
        this.iconOn = builder.iconOn;
        this.iconOff = builder.iconOff;
    }

    //==================== Getters ====================

    @Nullable
    public Class<?> getTileClass() {
        return tileClass;
    }

    @NonNull
    public String getTileName() {
        return tileName;
    }

    @NonNull
    public String getTileProvider() {
        return tileProvider;
    }

    @StringRes
    public int getLabelResId() {
        return labelResId;
    }

    @DrawableRes
    public int getIconOn() {
        return iconOn;
    }

    @DrawableRes
    public int getIconOff() {
        return iconOff;
    }

    /**
     * 是否是自定义新磁贴（而非覆写原有磁贴）
     * <p>
     * 如果设置了 tileName，则为自定义磁贴模式；
     * 否则为覆写模式，会修改原有磁贴的行为
     */
    public boolean isCustomTile() {
        return !tileName.isEmpty();
    }

    /**
     * 是否有有效的标签资源
     */
    public boolean hasLabel() {
        return labelResId != -1;
    }

    /**
     * 是否有有效的图标资源
     */
    public boolean hasIcons() {
        return iconOn != -1 && iconOff != -1;
    }

    /**
     * 根据状态获取图标资源 ID
     *
     * @param enabled 是否启用
     * @return 图标资源 ID
     */
    @DrawableRes
    public int getIconByState(boolean enabled) {
        return enabled ? iconOn : iconOff;
    }

    // ==================== Builder ====================

    public static final class Builder {
        private Class<?> tileClass;
        private String tileName;
        private String tileProvider;
        private int labelResId = -1;
        private int iconOn = -1;
        private int iconOff = -1;

        public Builder() {
        }

        /**
         * 设置磁贴类（必需）
         *
         * @param tileClass 磁贴类，通常通过 findClassIfExists 获取
         */
        public Builder setTileClass(@Nullable Class<?> tileClass) {
            this.tileClass = tileClass;
            return this;
        }

        /**
         * 设置自定义磁贴名称
         * <p>
         * 留空则为覆写模式，会修改原有磁贴的行为；
         * 设置后为自定义模式，会创建新磁贴
         *
         * @param tileName 磁贴名称，如"custom_5G"
         */
        public Builder setTileName(@Nullable String tileName) {
            this.tileName = tileName;
            return this;
        }

        /**
         * 设置磁贴 Provider 字段名
         * <p>
         * 用于从 MiuiQSFactory 中获取磁贴实例
         *
         * @param tileProvider Provider 字段名，如 "nfcTileProvider"
         */
        public Builder setTileProvider(@Nullable String tileProvider) {
            this.tileProvider = tileProvider;
            return this;
        }

        /**
         * 设置磁贴标签资源 ID
         *
         * @param labelResId 字符串资源 ID
         */
        public Builder setLabelResId(@StringRes int labelResId) {
            this.labelResId = labelResId;
            return this;
        }

        /**
         * 设置开启状态图标
         *
         * @param iconOn 图标资源 ID
         */
        public Builder setIconOn(@DrawableRes int iconOn) {
            this.iconOn = iconOn;
            return this;
        }

        /**
         * 设置关闭状态图标
         *
         * @param iconOff 图标资源 ID
         */
        public Builder setIconOff(@DrawableRes int iconOff) {
            this.iconOff = iconOff;
            return this;
        }

        /**
         * 同时设置开关状态图标
         *
         * @param iconOn  开启状态图标
         * @param iconOff 关闭状态图标
         */
        public Builder setIcons(@DrawableRes int iconOn, @DrawableRes int iconOff) {
            this.iconOn = iconOn;
            this.iconOff = iconOff;
            return this;
        }

        /**
         * 构建配置
         *
         * @return TileConfig 实例
         * @throws IllegalStateException 如果 tileClass 为 null
         */
        public TileConfig build() {
            if (tileClass == null) {
                throw new IllegalStateException("tileClass must not be null");
            }
            return new TileConfig(this);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "TileConfig{" +
            "tileClass=" + (tileClass != null ? tileClass.getSimpleName() : "null") +
            ", tileName='" + tileName + '\'' +
            ", tileProvider='" + tileProvider + '\'' +
            ", isCustomTile=" + isCustomTile() +
            ", hasLabel=" + hasLabel() +
            ", hasIcons=" + hasIcons() +
            '}';
    }
}
