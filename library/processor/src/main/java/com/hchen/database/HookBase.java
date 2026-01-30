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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.hchen.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 焕晨HChen
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface HookBase {
    /**
     * 目标作用域
     */
    String targetPackage();

    /**
     * 最低 Android SDK 版本 (>=)，-1 表示不限制
     */
    int minSdk() default -1;

    /**
     * 最高 Android SDK 版本 (<=)，-1 表示不限制
     */
    int maxSdk() default -1;

    /**
     * 最低 OS 版本 (>=)，-1 表示不限制
     */
    float minOSVersion() default -1F;

    /**
     * 最高 OS 版本 (<=)，-1 表示不限制
     */
    float maxOSVersion() default -1F;

    /**
     * 设备类型: 0=ALL, 1=PAD_ONLY, 2=PHONE_ONLY
     */
    int deviceType() default 0;
}
