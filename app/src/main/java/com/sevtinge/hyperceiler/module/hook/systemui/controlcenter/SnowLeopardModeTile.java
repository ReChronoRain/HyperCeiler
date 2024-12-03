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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.shell.ShellUtils.safeExecCommandWithRoot;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.ArrayMap;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.TileUtils;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedHelpers;

public class SnowLeopardModeTile extends TileUtils {
    private static boolean isInSnowLeopardMode = false;

    @Override
    public void init() {
        super.init();
    }

    @Override
    public Class<?> customClass() {
        return findClassIfExists("com.android.systemui.qs.tiles.QuietModeTile");
    }

    @Override
    public String setTileProvider() {
        return "quietModeTileProvider";
    }

    @Override
    public String customName() {
        return "custom_SnowLeopardMode";
    }

    @Override
    public int customRes() {
        return R.string.tiles_snow_leopard_mode;
    }

    @Override
    public void tileCheck(MethodHookParam param, String tileName) {
        param.setResult(!Objects.equals(safeExecCommandWithRoot("ls /dev/snd/pcm*"), ""));
    }

    @Override
    public void tileClick(MethodHookParam param, String tileName) {
        if (isInSnowLeopardMode) {
            safeExecCommandWithRoot("kill $(pidof android.hardware.audio.service_64) && chmod 660 /dev/snd/pcm*");
            isInSnowLeopardMode = false;
        } else {
            safeExecCommandWithRoot("kill $(pidof android.hardware.audio.service_64) && chmod 000 /dev/snd/pcm*");
            isInSnowLeopardMode = true;
        }
        XposedHelpers.callMethod(param.thisObject, "refreshState");
    }

    @Override
    public ArrayMap<String, Integer> tileUpdateState(MethodHookParam param, Class<?> mResourceIcon, String tileName) {
        boolean isEnable;
        isEnable = isInSnowLeopardMode;
        ArrayMap<String, Integer> tileResMap = new ArrayMap<>();
        tileResMap.put("custom_SnowLeopardMode_Enable", isEnable ? 1 : 0);
        tileResMap.put("custom_SnowLeopardMode_ON", R.drawable.ic_control_center_snow_leopard_mode_on);
        tileResMap.put("custom_SnowLeopardMode_OFF", R.drawable.ic_control_center_snow_leopard_mode_off);
        return tileResMap;
    }
}
