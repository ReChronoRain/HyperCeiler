package com.sevtinge.cemiuiler.module.systemui;

import android.content.pm.ApplicationInfo;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.XposedInit;
import com.sevtinge.cemiuiler.module.base.SystemUIHook;

import de.robv.android.xposed.XposedHelpers;
import moralnorm.os.SdkVersion;

public class NotificationVolumeSeparateSlider extends SystemUIHook {

    boolean isHooked = false;
    ClassLoader pluginLoader = null;

    Class<?> mMiuiVolumeDialogImpl;

    int notifVolumeOnResId;
    int notifVolumeOffResId;

    @Override
    public void init() {
        initRes();

        hookAllMethods(mPluginLoaderClass, "getClassLoader", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ApplicationInfo appInfo = (ApplicationInfo) param.args[0];
                if ("miui.systemui.plugin".equals(appInfo.packageName) && !isHooked) {
                    isHooked = true;
                    if (pluginLoader == null) pluginLoader = (ClassLoader) param.getResult();

                    mMiuiVolumeDialogImpl = findClassIfExists("com.android.systemui.miui.volume.MiuiVolumeDialogImpl", pluginLoader);

                    hookAllMethods(mMiuiVolumeDialogImpl, "addColumn", new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) throws Throwable {
                            if (param.args.length != 4) return;
                            int streamType = (int) param.args[0];
                            if (streamType == 4) {
                                XposedHelpers.callMethod(param.thisObject, "addColumn", 5, notifVolumeOnResId, notifVolumeOffResId, true, false);
                            }
                        }
                    });
                }
            }
        });
    }

    public void initRes() {

        notifVolumeOnResId = XposedInit.mResHook.addResource("ic_miui_volume_notification", R.drawable.ic_miui_volume_notification);
        notifVolumeOffResId = XposedInit.mResHook.addResource("ic_miui_volume_notification_mute", R.drawable.ic_miui_volume_notification_mute);

        XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_content_width_expanded", R.dimen.miui_volume_content_width_expanded);
        XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_ringer_layout_width_expanded", R.dimen.miui_volume_ringer_layout_width_expanded);
        XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_column_width_expanded", R.dimen.miui_volume_column_width_expanded);
        XposedInit.mResHook.setResReplacement("miui.systemui.plugin", "dimen", "miui_volume_column_margin_horizontal_expanded", R.dimen.miui_volume_column_margin_horizontal_expanded);

    }
}
