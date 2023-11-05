package com.sevtinge.hyperceiler.module.hook.systemui;

import static de.robv.android.xposed.XposedHelpers.findClassIfExists;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.XposedInit;
import com.sevtinge.hyperceiler.utils.hook.HookUtils;

import de.robv.android.xposed.XposedHelpers;

public class NotificationVolumeSeparateSlider {
    public static void initHideDeviceControlEntry(ClassLoader pluginLoader) {
        int notifVolumeOnResId;
        int notifVolumeOffResId;

        Class<?> mMiuiVolumeDialogImpl = findClassIfExists("com.android.systemui.miui.volume.MiuiVolumeDialogImpl", pluginLoader);

        notifVolumeOnResId = XposedInit.mResHook.addResource("ic_miui_volume_notification", R.drawable.ic_miui_volume_notification);
        notifVolumeOffResId = XposedInit.mResHook.addResource("ic_miui_volume_notification_mute", R.drawable.ic_miui_volume_notification_mute);

        XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_content_width_expanded", R.dimen.miui_volume_content_width_expanded);
        XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_ringer_layout_width_expanded", R.dimen.miui_volume_ringer_layout_width_expanded);
        XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_column_width_expanded", R.dimen.miui_volume_column_width_expanded);
        XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_column_margin_horizontal_expanded", R.dimen.miui_volume_column_margin_horizontal_expanded);

        HookUtils.hookAllMethods(mMiuiVolumeDialogImpl, "addColumn", new HookUtils.MethodHook() {
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
