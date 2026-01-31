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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.effect;

/**
 * 音效相关常量定义
 *
 * @author 焕晨HChen
 */
public final class EffectItem {

    private EffectItem() {
        // 防止实例化
    }

    // ==================== 音效类型 ====================
    public static final String EFFECT_DOLBY = "dolby";
    public static final String EFFECT_DOLBY_CONTROL = "dolby_control";
    public static final String EFFECT_MISOUND = "misound";
    public static final String EFFECT_MISOUND_CONTROL = "misound_control";
    public static final String EFFECT_NONE = "none";
    public static final String EFFECT_SPATIAL_AUDIO = "spatial";
    public static final String EFFECT_SURROUND = "surround";
    public static final String EFFECT_KEEP = "keep";

    public static final String[] EFFECT_ARRAY = new String[]{
        EFFECT_DOLBY, EFFECT_MISOUND, EFFECT_NONE, EFFECT_SPATIAL_AUDIO, EFFECT_SURROUND
    };

    // ==================== MiSound 参数 ====================
    public static final int MISOUND_PARAM_ENABLE = 25;
    public static final int MISOUND_PARAM_3D_SURROUND = 20;

    // ==================== Dolby 参数 ====================
    public static final int DOLBY_PARAM_DAP_ON = 0;
    public static final int DOLBY_SET_PARAM_ID = 5;

    // ==================== Settings Key ====================
    public static final String SETTINGS_KEY_EARPHONE_STATE = "auto_effect_switch_earphone_state";
    public static final String SETTINGS_KEY_RESTORE_STATE = "auto_effect_switch_restore_earphone_state";
    public static final String SETTINGS_KEY_EFFECT_IMPLEMENTER = "effect_implementer";

    public static final String SETTINGS_KEY_CONFIG_LOCK_SELECTION = "misound_bluetooth_effect_lock_selection";
    public static final String SETTINGS_KEY_CONFIG_REMEMBER_DEVICE = "misound_bluetooth_effect_remember_device";
    public static final String SETTINGS_KEY_CONFIG_DEFAULT_EFFECT = "misound_bluetooth_effect_default_effect";


    // ==================== Prefs Key ====================
    public static final String PREFS_KEY_LOCK_SELECTION = "misound_bluetooth_lock_selection";
    public static final String PREFS_KEY_REMEMBER_DEVICE = "misound_bluetooth_remember_device";
    public static final String PREFS_KEY_DEFAULT_EFFECT = "misound_bluetooth_default_effect";

    // ==================== Binder Key ====================
    public static final String BINDER_KEY_EFFECT_INFO = "effect_info";

    // ==================== 返回值 ====================
    public static final int RESULT_SUCCESS = 0;

    // ==================== Handler Message ====================
    public static final int MSG_STATE_CHANGE = 0;
    public static final int MSG_DUMP = 1;
    public static final long DELAY_STATE_CHANGE = 1000L;
    public static final long DELAY_DUMP = 1000L;

    // ==================== 设备类型 ====================
    public static final String DEVICE_TYPE_WIRED = "wired";
    public static final String DEVICE_TYPE_BLUETOOTH_A2DP = "bt_a2dp";
    public static final String DEVICE_TYPE_BLUETOOTH_LE = "bt_le";
    public static final String DEVICE_TYPE_USB = "usb";
    public static final String DEVICE_TYPE_SPEAKER = "speaker";
}

