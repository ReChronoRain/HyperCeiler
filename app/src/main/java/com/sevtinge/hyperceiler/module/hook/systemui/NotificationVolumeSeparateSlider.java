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
package com.sevtinge.hyperceiler.module.hook.systemui;

import static de.robv.android.xposed.XposedHelpers.findClassIfExists;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.module.base.tool.HookTool;

import de.robv.android.xposed.XposedHelpers;

public class NotificationVolumeSeparateSlider {
    public static void initHideDeviceControlEntry(ClassLoader pluginLoader) {
        int notifVolumeOnResId;
        int notifVolumeOffResId;

        Class<?> mMiuiVolumeDialogImpl = findClassIfExists("com.android.systemui.miui.volume.MiuiVolumeDialogImpl", pluginLoader);

        notifVolumeOnResId = R.drawable.ic_miui_volume_notification;
        notifVolumeOffResId = R.drawable.ic_miui_volume_notification_mute;

        XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_content_width_expanded", R.dimen.miui_volume_content_width_expanded);
        XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_ringer_layout_width_expanded", R.dimen.miui_volume_ringer_layout_width_expanded);
        XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_column_width_expanded", R.dimen.miui_volume_column_width_expanded);
        XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_column_margin_horizontal_expanded", R.dimen.miui_volume_column_margin_horizontal_expanded);

        HookTool.hookAllMethods(mMiuiVolumeDialogImpl, "addColumn", new HookTool.MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                if (param.args.length != 4) return;
                int streamType = (int) param.args[0];
                if (streamType == 4) {
                    XposedHelpers.callMethod(param.thisObject, "addColumn", 5, notifVolumeOnResId, notifVolumeOffResId, true, false);
                }
            }
        });
    }
}
