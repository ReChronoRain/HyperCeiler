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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileConfig;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileContext;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileState;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileUtils;
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellUtils;

public class SnowLeopardModeTile extends TileUtils {
    private static boolean isInSnowLeopardMode = false;

    @NonNull
    @Override
    protected TileConfig onCreateTileConfig() {
        return new TileConfig.Builder()
            .setTileClass(findClassIfExists("com.android.systemui.qs.tiles.QuietModeTile"))
            .setTileName("custom_SnowLeopardMode")
            .setTileProvider("quietModeTileProvider")
            .setLabelResId(R.string.tiles_snow_leopard_mode)
            .setIcons(
                R.drawable.ic_control_center_snow_leopard_mode_on,
                R.drawable.ic_control_center_snow_leopard_mode_off
            )
            .build();
    }

    @Override
    protected boolean onCheckAvailable(TileContext ctx) {
        String result = String.valueOf(ShellUtils.execCommand("ls /dev/snd/pcm*", false));
        return !result.isEmpty();
    }

    @Override
    protected void onTileClick(TileContext ctx) {
        if (isInSnowLeopardMode) {
            ShellUtils.rootExecCmd("kill $(pidof android.hardware.audio.service_64) && chmod 660 /dev/snd/pcm*");
            isInSnowLeopardMode = false;
        } else {
            ShellUtils.rootExecCmd("kill $(pidof android.hardware.audio.service_64) && chmod 000 /dev/snd/pcm*");
            isInSnowLeopardMode = true;
        }

        ctx.refreshState();
    }

    @Nullable
    @Override
    protected TileState onUpdateState(TileContext ctx) {
        return TileState.of(isInSnowLeopardMode);
    }
}
