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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.systemui.plugin;

import android.content.Context;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.CCGridForHyperOS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;

import com.sevtinge.hyperceiler.module.hook.systemui.*;
import com.sevtinge.hyperceiler.module.hook.systemui.controlcenter.*;
import com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.v.*;

public class NewPluginHelper extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookConstructor("com.android.systemui.shared.plugins.PluginActionManager$PluginContextWrapper", Context.class, ClassLoader.class, new MethodHook(){
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ClassLoader classLoader = (ClassLoader) XposedHelpers.getObjectField(param.thisObject,"mClassLoader");
                onPluginLoaded(classLoader);
            }
        });
    }

    private void onPluginLoaded(ClassLoader classLoader) {
        List<String> mCardStyleTiles = null;
        try {
            mCardStyleTiles = getTileList();
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getBoolean("system_ui_plugin_enable_volume_blur")) {
                EnableVolumeBlur.initEnableVolumeBlur(classLoader);
            }
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getBoolean("system_cc_volume_showpct_title")) {
                NewShowVolumePct.initLoader(classLoader); // 音量百分比
            }
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getBoolean("system_ui_unlock_super_volume")) {
                NewSuperVolume.initSuperVolume(classLoader); // 超大音量
            }
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getBoolean("system_framework_volume_separate_control") &&
                    mPrefsMap.getBoolean("system_framework_volume_separate_slider")) {
                NotificationVolumeSeparateSlider.initHideDeviceControlEntry(classLoader);
            }
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getStringAsInt("system_ui_control_center_mi_play_entry", 0) != 0) {
                HideMiPlayEntry.initHideMiPlayEntry(classLoader);
            }
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getBoolean("systemui_plugin_card_tiles_enabled") &&
                    !mPrefsMap.getString("systemui_plugin_card_tiles", "").isEmpty()) {
                CustomCardTiles.initCustomCardTiles(classLoader, mCardStyleTiles);
            }
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getBoolean("system_ui_control_center_qs_open_color") ||
                    mPrefsMap.getBoolean("system_ui_control_center_qs_big_open_color")) {
                QSColor.pluginHook(classLoader);
            }
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getBoolean("system_ui_control_center_hide_edit_botton")) {
                HideEditButton.initHideEditButton(classLoader);
            }
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getBoolean("system_ui_control_center_rounded_rect")) {
                CCGridForHyperOS.initCCGridForHyperOS(classLoader); // 控制中心磁贴圆角
            }
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getStringAsInt("system_ui_control_center_mi_smart_hub_entry", 0) != 0) {
                HideMiSmartHubEntry.initHideMiSmartHubEntry(classLoader);
            }
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getStringAsInt("system_ui_control_center_device_ctrl_entry", 0) != 0) {
                HideDeviceControlEntry.initHideDeviceControlEntry(classLoader);
            }
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getStringAsInt("system_ui_control_center_cc_bluetooth_tile_style", 1) > 1) {
                BluetoothTileStyle.initHideDeviceControlEntry(classLoader);
            }
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getStringAsInt("system_ui_control_center_hide_operator", 0) == 3) {
                ShowDeviceName.initShowDeviceName(classLoader);
            }
        } catch (Exception ignored) {}
        try {
            if (mPrefsMap.getBoolean("system_ui_control_center_disable_device_managed")) {
                DisableDeviceManaged.initDisableDeviceManaged(classLoader);
            }
        } catch (Exception ignored) {}
    }

    private List<String> getTileList() {
        String cardTiles = mPrefsMap.getString("systemui_plugin_card_tiles", "").replace("List_", "");
        if (cardTiles.isEmpty()) {
            return new ArrayList<>();
        } else {
            return Arrays.asList(cardTiles.split("\\|"));
        }
    }

}
