package com.sevtinge.cemiuiler.module;

import android.os.Build;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.home.WidgetCornerRadius;
import com.sevtinge.cemiuiler.module.systemframework.*;
import com.sevtinge.cemiuiler.module.systemframework.PackagePermissions;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForR;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForS;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForSv2;
import com.sevtinge.cemiuiler.module.systemframework.corepatch.CorePatchForT;
import com.sevtinge.cemiuiler.module.systemui.statusbar.ClockShowSecond;
import com.sevtinge.cemiuiler.module.thememanager.ThemeCrack;
import com.sevtinge.cemiuiler.utils.LogUtils;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SystemFramework extends BaseModule {

    @Override
    public void handleLoadPackage() {

        //小窗
        initHook(new FreeFormCount(), mPrefsMap.getBoolean("system_framework_freeform_count"));
        initHook(new FreeformBubble(), mPrefsMap.getBoolean("system_framework_freeform_bubble"));
        initHook(new DisableFreeformBlackList(), mPrefsMap.getBoolean("system_framework_disable_freeform_blacklist"));
        initHook(new StickyFloatingWindows(), mPrefsMap.getBoolean("system_framework_freeform_sticky"));

        //音量
        initHook(new VolumeDefaultStream());
        initHook(new VolumeFirstPress(), mPrefsMap.getBoolean("system_framework_volume_first_press"));
        initHook(new VolumeSeparateControl(), mPrefsMap.getBoolean("system_framework_volume_separate_control"));
        initHook(new VolumeSteps(), mPrefsMap.getInt("system_framework_volume_steps", 0) > 0);
        initHook(new VolumeMediaSteps(), mPrefsMap.getInt("system_framework_volume_media_steps",15)>15);
        initHook(new VolumeDisableSafe(),mPrefsMap.getBoolean("system_framework_volume_disable_safe"));
        initHook(new ClockShowSecond(), mPrefsMap.getBoolean("system_ui_statusbar_clock_show_second"));

        //主题破解
        initHook(new ThemeProvider(), mPrefsMap.getBoolean("hidden_function") && mPrefsMap.getBoolean("various_theme_crack"));

        //核心破解
        /*switch (Build.VERSION.SDK_INT) {
            case Build.VERSION_CODES.R : // 30
                initHook(new CorePatchForR(), true);
                break;
            case Build.VERSION_CODES.S : // 31
                initHook(new CorePatchForS(), true);
                break;
            case Build.VERSION_CODES.S_V2 : // 32
                initHook(new CorePatchForSv2(), true);
                break;
            case Build.VERSION_CODES.TIRAMISU: // 33
                initHook(new CorePatchForT(), true);
                break;
            default:
                LogUtils.log(" Warning: Unsupported Version of Android " + Build.VERSION.SDK_INT);
                break;
        }*/


        //其他
        initHook(new ScreenRotation(), mPrefsMap.getBoolean("system_framework_screen_all_rotations"));
        initHook(new CleanShareMenu(), mPrefsMap.getBoolean("system_framework_clean_share_menu"));
        initHook(new CleanOpenMenu(), mPrefsMap.getBoolean("system_framework_clean_open_menu"));
        initHook(new AllowUntrustedTouch(), mPrefsMap.getBoolean("system_framework_allow_untrusted_touch"));
        initHook(new FlagSecure(), mPrefsMap.getBoolean("system_other_flag_secure"));

        //位置模拟
        initHook(new LocationSimulation(), true);

        //Other
        initHook(new PackagePermissions());
        initHook(new GlobalActions(), mLoadPackageParam.processName.equals("android"));
        initHook(new AppDisableService());
    }

    /*public static void handleLoad(LoadPackageParam lpparam) {
        mLoadPackageParam = lpparam;

        initHook(new GlobalActions(), lpparam.processName.equals("android"));


        initHook(new FreeformBubbleHook(), mPrefsMap.getBoolean("system_framework_bubble"));
        initHook(new FirstVolumePressHook(), mPrefsMap.getBoolean("system_framework_first_volume_press"));
        initHook(new NotificationVolumeServiceHook(), mPrefsMap.getBoolean("system_framework_separate_volume"));
        initHook(new DefaultVolumeStreamHook(), mPrefsMap.getInt("system_framework_default_volume_stream", 0) > 1);
        initHook(new AllRotationsHook(), mPrefsMap.getBoolean("system_framework_freeform_count"));
    }*/
}
