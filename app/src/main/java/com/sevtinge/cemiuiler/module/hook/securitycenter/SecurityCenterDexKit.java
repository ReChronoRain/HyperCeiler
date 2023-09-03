package com.sevtinge.cemiuiler.module.hook.securitycenter;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexClassDescriptor;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

public class SecurityCenterDexKit extends BaseHook {

    public static Map<String, List<DexMethodDescriptor>> mSecurityCenterResultMap;
    public static Map<String, List<DexClassDescriptor>> mSecurityCenterResultClassMap;

    @Override
    public void init() {
        System.loadLibrary("dexkit");
        String apkPath = lpparam.appInfo.sourceDir;
        DexKitBridge bridge = DexKitBridge.create(apkPath);
        try {
            if (bridge == null) {
                return;
            }
            mSecurityCenterResultMap =
                bridge.batchFindMethodsUsingStrings(
                    BatchFindArgs.builder()
                        .addQuery("BeautyFace", Set.of("taoyao", "IN", "persist.vendor.vcb.ability"))
                        .addQuery("BeautyPc", Set.of("persist.vendor.camera.facetracker.support"))
                        .addQuery("BeautyLightAuto", Set.of("taoyao"))
                        .addQuery("ScoreManager", Set.of("getMinusPredictScore------------------------------------------------ "))
                        .addQuery("rootCheck", Set.of("key_check_item_root"))
                        .addQuery("SuperWirelessCharge", Set.of("persist.vendor.tx.speed.control"))
                        .addQuery("SuperWirelessChargeTip", Set.of("key_is_connected_super_wls_tx"))
                        .addQuery("Macro2", Set.of("pref_gb_unsupport_macro_apps"))
                        .addQuery("IsShowReport", Set.of("android.intent.action.VIEW", "com.xiaomi.market"))
                        .addQuery("FuckRiskPkg", Set.of("riskPkgList", "key_virus_pkg_list", "show_virus_notification"))
                        .addQuery("RemoveScreenHoldOn", Set.of("remove_screen_off_hold_on"))
                        .addQuery("AisSupport", Set.of("debug.config.media.video.ais.support"))
                        .addQuery("PowerRankHelperHolderSdkHelper", Set.of("ishtar", "nuwa", "fuxi"))
                        .matchType(MatchType.CONTAINS)
                        .build()
                );
            mSecurityCenterResultClassMap =
                bridge.batchFindClassesUsingStrings(
                    BatchFindArgs.builder()
                        .addQuery("Macro", Set.of("pref_gb_unsupport_macro_apps", "gb_game_gunsight", "com.tencent.tmgp.sgame"))
                        .addQuery("Macro1", Set.of("key_macro_toast", "content://com.xiaomi.macro.MacroStatusProvider/game_macro_change"))
                        .addQuery("LabUtils", Set.of("mi_lab_ai_clipboard_enable", "mi_lab_blur_location_enable"))
                        .addQuery("BeautyLight", Set.of("pref_support_front_light", "pref_privacy_support_devices"))
                        .addQuery("PowerRankHelperHolder", Set.of("PowerRankHelperHolder", "not support screenPowerSplit"))
                        .addQuery("GbGameCollimator",Set.of("gb_game_collimator_status"))
                        .addQuery("FrcSupport", Set.of("ro.vendor.media.video.frc.support"))
                        .matchType(MatchType.CONTAINS)
                        .build()
                );
        } catch (Throwable e) {
            e.printStackTrace();
        }
        bridge.close();
    }
}
