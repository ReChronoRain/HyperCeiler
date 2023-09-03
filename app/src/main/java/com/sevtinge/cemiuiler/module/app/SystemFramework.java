package com.sevtinge.cemiuiler.module.app;

import static com.sevtinge.cemiuiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.cemiuiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import com.sevtinge.cemiuiler.module.base.BaseModule;
import com.sevtinge.cemiuiler.module.hook.systemframework.AllowUntrustedTouch;
import com.sevtinge.cemiuiler.module.hook.systemframework.AppDisableService;
import com.sevtinge.cemiuiler.module.hook.systemframework.AppLinkVerify;
import com.sevtinge.cemiuiler.module.hook.systemframework.CleanOpenMenu;
import com.sevtinge.cemiuiler.module.hook.systemframework.CleanShareMenu;
import com.sevtinge.cemiuiler.module.hook.systemframework.DeleteOnPostNotification;
import com.sevtinge.cemiuiler.module.hook.systemframework.DisableCleaner;
import com.sevtinge.cemiuiler.module.hook.systemframework.DisableFreeformBlackList;
import com.sevtinge.cemiuiler.module.hook.systemframework.DisablePinVerifyPer72h;
import com.sevtinge.cemiuiler.module.hook.systemframework.FlagSecure;
import com.sevtinge.cemiuiler.module.hook.systemframework.FreeFormCount;
import com.sevtinge.cemiuiler.module.hook.systemframework.FreeformBubble;
import com.sevtinge.cemiuiler.module.hook.systemframework.IsDetailLog;
import com.sevtinge.cemiuiler.module.hook.systemframework.LocationSimulation;
import com.sevtinge.cemiuiler.module.hook.systemframework.MultiFreeFormSupported;
import com.sevtinge.cemiuiler.module.hook.systemframework.PackagePermissions;
import com.sevtinge.cemiuiler.module.hook.systemframework.RemoveSmallWindowRestrictions;
import com.sevtinge.cemiuiler.module.hook.systemframework.ScreenRotation;
import com.sevtinge.cemiuiler.module.hook.systemframework.SpeedInstall;
import com.sevtinge.cemiuiler.module.hook.systemframework.StickyFloatingWindows;
import com.sevtinge.cemiuiler.module.hook.systemframework.ThemeProvider;
import com.sevtinge.cemiuiler.module.hook.systemframework.UseOriginalAnimation;
import com.sevtinge.cemiuiler.module.hook.systemframework.VolumeDefaultStream;
import com.sevtinge.cemiuiler.module.hook.systemframework.VolumeDisableSafe;
import com.sevtinge.cemiuiler.module.hook.systemframework.VolumeFirstPress;
import com.sevtinge.cemiuiler.module.hook.systemframework.VolumeMediaSteps;
import com.sevtinge.cemiuiler.module.hook.systemframework.VolumeSeparateControl;
import com.sevtinge.cemiuiler.module.hook.systemframework.VolumeSteps;
import com.sevtinge.cemiuiler.module.hook.systemframework.corepatch.BypassSignCheckForT;
import com.sevtinge.cemiuiler.module.hook.systemframework.freeform.OpenAppInFreeForm;
import com.sevtinge.cemiuiler.module.hook.systemframework.mipad.IgnoreStylusKeyGesture;
import com.sevtinge.cemiuiler.module.hook.systemframework.mipad.NoMagicPointer;
import com.sevtinge.cemiuiler.module.hook.systemframework.mipad.RemoveStylusBluetoothRestriction;
import com.sevtinge.cemiuiler.module.hook.systemframework.mipad.RestoreEsc;
import com.sevtinge.cemiuiler.module.hook.systemframework.mipad.SetGestureNeedFingerNum;
import com.sevtinge.cemiuiler.module.hook.systemframework.network.DualNRSupport;
import com.sevtinge.cemiuiler.module.hook.systemframework.network.DualSASupport;
import com.sevtinge.cemiuiler.module.hook.systemframework.network.N1Band;
import com.sevtinge.cemiuiler.module.hook.systemframework.network.N28Band;
import com.sevtinge.cemiuiler.module.hook.systemframework.network.N5N8Band;
import com.sevtinge.cemiuiler.module.hook.systemframework.display.ToastTime;
import com.sevtinge.cemiuiler.module.hook.various.NoAccessDeviceLogsRequest;


public class SystemFramework extends BaseModule {

    @Override
    public void handleLoadPackage() {

        initHook(new IsDetailLog());

        // 小窗
        initHook(new FreeFormCount(), mPrefsMap.getBoolean("system_framework_freeform_count"));
        initHook(new FreeformBubble(), mPrefsMap.getBoolean("system_framework_freeform_bubble"));
        initHook(new DisableFreeformBlackList(), mPrefsMap.getBoolean("system_framework_disable_freeform_blacklist"));
        initHook(RemoveSmallWindowRestrictions.INSTANCE, mPrefsMap.getBoolean("system_framework_disable_freeform_blacklist"));
        initHook(new StickyFloatingWindows(), mPrefsMap.getBoolean("system_framework_freeform_sticky"));
        initHook(MultiFreeFormSupported.INSTANCE, mPrefsMap.getBoolean("system_framework_freeform_recents_to_small_freeform"));
        initHook(new OpenAppInFreeForm(), mPrefsMap.getBoolean("system_framework_freeform_jump"));
        // initHook(new OpenAppInFreeForm(), mPrefsMap.getBoolean("system_framework_freeform_jump"));

        // 音量
        initHook(new VolumeDefaultStream());
        initHook(new VolumeFirstPress(), mPrefsMap.getBoolean("system_framework_volume_first_press"));
        initHook(new VolumeSeparateControl(), mPrefsMap.getBoolean("system_framework_volume_separate_control"));
        initHook(new VolumeSteps(), mPrefsMap.getInt("system_framework_volume_steps", 0) > 0);
        initHook(new VolumeMediaSteps(), mPrefsMap.getBoolean("system_framework_volume_media_steps_enable"));
        initHook(new VolumeDisableSafe(), mPrefsMap.getBoolean("system_framework_volume_disable_safe"));
        // initHook(new ClockShowSecond(), mPrefsMap.getBoolean("system_ui_statusbar_clock_show_second"));

        initHook(new ThemeProvider(), mPrefsMap.getBoolean("hidden_function") && mPrefsMap.getBoolean("various_theme_crack"));

        // 核心破解
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


        // 其他
        initHook(new ScreenRotation(), mPrefsMap.getBoolean("system_framework_screen_all_rotations"));
        initHook(new CleanShareMenu(), mPrefsMap.getBoolean("system_framework_clean_share_menu"));
        initHook(new CleanOpenMenu(), mPrefsMap.getBoolean("system_framework_clean_open_menu"));
        initHook(new AllowUntrustedTouch(), mPrefsMap.getBoolean("system_framework_allow_untrusted_touch"));
        initHook(new FlagSecure(), mPrefsMap.getBoolean("system_other_flag_secure"));
        initHook(new AppLinkVerify(), mPrefsMap.getBoolean("system_framework_disable_app_link_verify"));
        initHook(new UseOriginalAnimation(), mPrefsMap.getBoolean("system_framework_other_use_original_animation"));
        initHook(new SpeedInstall(), mPrefsMap.getBoolean("system_framework_other_speed_install"));
        initHook(DeleteOnPostNotification.INSTANCE, mPrefsMap.getBoolean("system_other_delete_on_post_notification"));
        initHook(NoAccessDeviceLogsRequest.INSTANCE, mPrefsMap.getBoolean("various_disable_access_device_logs"));

        // 显示
        initHook(new ToastTime(), mPrefsMap.getBoolean("system_ui_display_toast_times_enable"));
        // initHook(new AutoBrightness(), mPrefsMap.getBoolean("system_control_center_auto_brightness"));

        // 位置模拟
        initHook(new LocationSimulation(), false);

        // 小米/红米平板设置相关
        if (isPad()) {
            initHook(IgnoreStylusKeyGesture.INSTANCE, mPrefsMap.getBoolean("mipad_input_ingore_gesture"));
            initHook(NoMagicPointer.INSTANCE, mPrefsMap.getBoolean("mipad_input_close_magic"));
            initHook(RemoveStylusBluetoothRestriction.INSTANCE, mPrefsMap.getBoolean("mipad_input_disable_bluetooth"));
            initHook(RestoreEsc.INSTANCE, mPrefsMap.getBoolean("mipad_input_restore_esc"));
            initHook(SetGestureNeedFingerNum.INSTANCE, mPrefsMap.getBoolean("mipad_input_need_finger_num"));
        }

        // 核心破解
        if (isMoreAndroidVersion(33)) {
            initHook(BypassSignCheckForT.INSTANCE, mPrefsMap.getBoolean("system_framework_core_patch_auth_creak") ||
                mPrefsMap.getBoolean("system_framework_core_patch_disable_integrity"));
        }

        // 网络
        initHook(DualNRSupport.INSTANCE, mPrefsMap.getBoolean("phone_double_5g_nr"));
        initHook(DualSASupport.INSTANCE, mPrefsMap.getBoolean("phone_double_5g_sa"));
        initHook(N1Band.INSTANCE, mPrefsMap.getBoolean("phone_n1"));
        initHook(N5N8Band.INSTANCE, mPrefsMap.getBoolean("phone_n5_n8"));
        initHook(N28Band.INSTANCE, mPrefsMap.getBoolean("phone_n28"));

        // Other
        initHook(new PackagePermissions());
        initHook(new GlobalActions(), mLoadPackageParam.processName.equals("android"));
        initHook(new AppDisableService());
        initHook(DisableCleaner.INSTANCE, mPrefsMap.getBoolean("system_framework_other_disable_cleaner"));
        initHook(new DisablePinVerifyPer72h(), mPrefsMap.getBoolean("system_framework_disable_72h_verify"));
    }

}
