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
package com.sevtinge.hyperceiler.hook.utils.api.effect;

/**
 * 音效列表
 *
 * @author 焕晨HChen
 */
public class EffectItem {
    public static final String EFFECT_DOLBY = "dolby";
    public static final String EFFECT_DOLBY_CONTROL = "dolby_control";
    public static final String EFFECT_MISOUND = "misound";
    public static final String EFFECT_MISOUND_CONTROL = "misound_control";
    public static final String EFFECT_NONE = "none";
    public static final String EFFECT_SPATIAL_AUDIO = "spatial";
    public static final String EFFECT_SURROUND = "surround";
    public static final String[] mEffectArray = new String[]{
            EFFECT_DOLBY, EFFECT_MISOUND, EFFECT_NONE, EFFECT_SPATIAL_AUDIO, EFFECT_SURROUND
    };
}
