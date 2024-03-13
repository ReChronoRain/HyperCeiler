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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.icon.all;

import static com.sevtinge.hyperceiler.utils.api.NekoQiqiApisKt.isNewNetworkStyle;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.List;

import de.robv.android.xposed.XposedHelpers;

public class StatusBarIcon extends BaseHook {

    @Override
    public void init() {
        if (!isMoreHyperOSVersion(1f)) {
            try {
                if (!isNewNetworkStyle()) {
                    findClass("com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl").getDeclaredMethod(
                            "setIconVisibility", String.class, boolean.class);
                    findAndHookMethod("com.android.systemui.statusbar.phone.MiuiDripLeftStatusBarIconControllerImpl",
                            "setIconVisibility", String.class, boolean.class,
                            new MethodHook() {
                                @Override
                                protected void before(MethodHookParam param) {
                                    switch (checkSlot((String) param.args[0])) {
                                        case 1 -> param.args[1] = true;
                                        case 2 -> param.args[1] = false;
                                        default -> {
                                        }
                                    }
                                }
                            }
                    );
                }
            } catch (Throwable ignored) {
            }

            findAndHookMethod("com.android.systemui.statusbar.phone.StatusBarIconControllerImpl",
                "setIconVisibility", String.class, boolean.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        switch (checkSlot((String) param.args[0])) {
                            case 1 -> param.args[1] = true;
                            case 2 -> param.args[1] = false;
                            default -> {
                            }
                        }
                    }
                }
            );
        } else {
            // from XiaomiHelper with GPL3
            Class<?> mMiuiIconManagerUtils = findClassIfExists("com.android.systemui.statusbar.phone.MiuiIconManagerUtils");
            List<String> statusBarList = (List<String>) XposedHelpers.getStaticObjectField(mMiuiIconManagerUtils, "RIGHT_BLOCK_LIST");
            List<String> ctrlCenterList = (List<String>) XposedHelpers.getStaticObjectField(mMiuiIconManagerUtils, "CONTROL_CENTER_BLOCK_LIST");

            hyperIconShowManager(statusBarList, ctrlCenterList, mMiuiIconManagerUtils);
        }
    }

    private void hyperIconShowManager(List<String> statusBarList, List<String> ctrlCenterList, Class<?> mMiuiIconManagerUtils) {
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_vpn", 0), "vpn", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_alarm_clock", 0), "alarm_clock", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_nfc", 0), "nfc", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_zen", 0), "zen", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_volume", 0), "volume", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi", 0), "wifi", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi", 0), "demo_wifi", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_airplane", 0), "airplane", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_location", 0), "location", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_mic", 0), "speakerphone", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_hotspot", 0), "hotspot", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_headset", 0), "headset", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_bluetooth", 0), "bluetooth", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_bluetooth_battery", 0), "bluetooth_handsfree_battery", statusBarList, ctrlCenterList);
        // setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_network_indicator", 0), "network_speed", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_mobile_network_signal_no_card", 0), "no_sim", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_new_hd", 0), "hd", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_car", 0), "car", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_pad", 0), "pad", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_pc", 0), "pc", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_phone", 0), "phone", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_soundbox", 0), "sound_box", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_soundbox_screen", 0), "sound_box_screen", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_soundbox_group", 0), "sound_box_group", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_soundbox_stereo", 0), "stereo", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_tv", 0), "tv", statusBarList, ctrlCenterList);
        setIcon(mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wireless_headset", 0), "wireless_headset", statusBarList, ctrlCenterList);

        XposedHelpers.setStaticObjectField(mMiuiIconManagerUtils, "RIGHT_BLOCK_LIST", statusBarList);
        XposedHelpers.setStaticObjectField(mMiuiIconManagerUtils, "CONTROL_CENTER_BLOCK_LIST", ctrlCenterList);
    }

    private void setIcon(int value, String name, List<String> statusBarList, List<String> controlList){
        // from XiaomiHelper with GPL3
        switch (value) {
            case 1 -> {
                if (statusBarList.contains(name)) statusBarList.remove(name);
                if (controlList.contains(name)) controlList.remove(name);
            }
            case 12 -> {
                if (statusBarList.contains(name)) statusBarList.remove(name);
                if (!controlList.contains(name)) controlList.add(name);
            }
            case 11 -> {
                if (!statusBarList.contains(name)) statusBarList.add(name);
                if (controlList.contains(name)) controlList.remove(name);
            }
            case 2 -> {
                if (!statusBarList.contains(name)) statusBarList.add(name);
                if (!controlList.contains(name)) controlList.add(name);
            }
            default -> {
            }
        }
    }

    private int checkSlot(String slotName) {
        int no_sim = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_mobile_network_signal_no_card", 0);
        int vpn = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_vpn", 0);
        int alarmClock = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_alarm_clock", 0);
        int nfc = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_nfc", 0);
        int zen = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_zen", 0);
        int volume = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_volume", 0);
        int wifi = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi", 0);
        int wifi_slave = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_wifi_slave", 0);
        int airplane = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_airplane", 0);
        int location = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_location", 0);
        int hotspot = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_hotspot", 0);
        int headset = mPrefsMap.getStringAsInt("system_ui_status_bar_icon_headset", 0);
        switch (slotName) {
            case "vpn" -> {
                return vpn;
            }
            case "alarm_clock" -> {
                return alarmClock;
            }
            case "nfc" -> {
                return nfc;
            }
            case "zen" -> {
                return zen;
            }
            case "volume" -> {
                return volume;
            }
            case "no_sim" -> {
                return no_sim;
            }
            case "wifi" -> {
                return wifi;
            }
            case "wifi_slave" -> {
                return wifi_slave;
            }
            case "airplane" -> {
                return airplane;
            }
            case "location" -> {
                return location;
            }
            case "hotspot" -> {
                return hotspot;
            }
            case "headset" -> {
                return headset;
            }
            default -> {
                return -1;
            }
        }
    }

}
