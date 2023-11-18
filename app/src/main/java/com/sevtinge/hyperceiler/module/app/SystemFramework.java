package com.sevtinge.hyperceiler.module.app;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;
import static com.sevtinge.hyperceiler.utils.log.LogManager.logLevelDesc;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.hook.systemframework.*;
import com.sevtinge.hyperceiler.module.hook.systemframework.corepatch.*;
import com.sevtinge.hyperceiler.module.hook.systemframework.display.*;
import com.sevtinge.hyperceiler.module.hook.systemframework.freeform.*;
import com.sevtinge.hyperceiler.module.hook.systemframework.mipad.*;
import com.sevtinge.hyperceiler.module.hook.systemframework.network.*;
import com.sevtinge.hyperceiler.module.hook.various.NoAccessDeviceLogsRequest;

import de.robv.android.xposed.XposedBridge;


public class SystemFramework extends BaseModule {

    @Override
    public void handleLoadPackage() {
        XposedBridge.log("[HyperCeiler][I]: Log level is " + logLevelDesc());

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
        initHook(DisplayCutout.INSTANCE, mPrefsMap.getBoolean("system_ui_display_hide_cutout_enable"));
        initHook(new ToastTime(), mPrefsMap.getBoolean("system_ui_display_toast_times_enable"));
        initHook(new AllDarkMode(), mPrefsMap.getBoolean("system_framework_allow_all_dark_mode"));
        // initHook(new AutoBrightness(), mPrefsMap.getBoolean("system_control_center_auto_brightness"));

        // 位置模拟
        // initHook(new LocationSimulation(), false);

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
        initHook(new ThermalBrightness(), mPrefsMap.getBoolean("system_framework_other_thermal_brightness"));
        initHook(DisableCleaner.INSTANCE, mPrefsMap.getBoolean("system_framework_other_disable_cleaner"));
        initHook(new DisablePinVerifyPer72h(), mPrefsMap.getBoolean("system_framework_disable_72h_verify"));
        initHook(new DisableVerifyCanBeDisabled(), mPrefsMap.getBoolean("system_framework_disable_verify_can_ve_disabled"));
    }

}
