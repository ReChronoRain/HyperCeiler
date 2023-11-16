package com.sevtinge.hyperceiler.utils;

import static com.sevtinge.hyperceiler.utils.api.VoyagerApisKt.isPad;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.data.ModData;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SearchHelper {

    public static final int MARK_COLOR_VIBRANT = Color.rgb(255, 0, 0);
    public static final String NEW_MODS_SEARCH_QUERY = "\uD83C\uDD95";
    public static ArrayList<ModData> allModsList = new ArrayList<>();

    public static final HashSet<String> NEW_MODS = new HashSet<>(
        Set.of(
            "pref_key_launcher_nozoomanim"
        )
    );

    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    public static final String MIUIZER_NS = "http://schemas.android.com/apk/res-auto";

    public static void getAllMods(Context context, boolean force) {
        if (force) {
            allModsList.clear();
        } else if (allModsList.size() > 0) {
            return;
        }
        // 系统框架页面相关
        parsePrefXml(context, R.xml.framework_freeform,
            R.string.system_framework,
            R.string.floating_window,
            R.string.floating_window,
            R.string.floating_window,
            R.string.floating_window,
            "com.sevtinge.hyperceiler.ui.fragment.framework.FreeFormSettings");

        parsePrefXml(context, R.xml.framework_volume,
            R.string.system_framework,
            R.string.system_framework_volume_title,
            R.string.system_framework_volume_title,
            R.string.system_framework_volume_title,
            R.string.system_framework_volume_title,
            "com.sevtinge.hyperceiler.ui.fragment.framework.VolumeSettings");

        parsePrefXml(context, R.xml.framework_phone,
            R.string.system_framework,
            R.string.system_framework_phone_title,
            R.string.system_framework_phone_title,
            R.string.system_framework_phone_title,
            R.string.system_framework_phone_title,
            "com.sevtinge.hyperceiler.ui.fragment.framework.NetworkSettings");

        parsePrefXml(context, R.xml.framework_display,
            R.string.system_framework,
            R.string.system_framework_display_title,
            R.string.system_framework_display_title,
            R.string.system_framework_display_title,
            R.string.system_framework_display_title,
            "com.sevtinge.hyperceiler.ui.fragment.framework.DisplaySettings");

        parsePrefXml(context, R.xml.framework_other,
            R.string.system_framework,
            R.string.system_framework_other_title,
            R.string.system_framework_other_title,
            R.string.system_framework_other_title,
            R.string.system_framework_other_title,
            "com.sevtinge.hyperceiler.ui.fragment.framework.OtherSettings");

        // 系统界面页面相关
        parsePrefXml(context, R.xml.system_ui_lock_screen,
            R.string.system_ui,
            R.string.system_ui_lockscreen_title,
            R.string.system_ui_lockscreen_title,
            R.string.system_ui_lockscreen_title,
            R.string.system_ui_lockscreen_title,
            "com.sevtinge.hyperceiler.ui.fragment.systemui.LockScreenSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar,
            R.string.system_ui,
            R.string.system_ui_statusbar_title,
            R.string.system_ui_statusbar_title,
            R.string.system_ui_statusbar_title,
            R.string.system_ui_statusbar_title,
            "com.sevtinge.hyperceiler.ui.fragment.systemui.StatusBarSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar_icon_manage,
            R.string.system_ui,
            R.string.system_ui_statusbar_title,
            R.string.system_ui_statusbar_iconmanage_title,
            R.string.system_ui_statusbar_iconmanage_title,
            R.string.system_ui_statusbar_iconmanage_title,
            "com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar.IconManageSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar_mobile_network_type,
            R.string.system_ui,
            R.string.system_ui_statusbar_title,
            R.string.system_ui_statusbar_iconmanage_title,
            R.string.system_ui_status_bar_mobile_type_single_title,
            R.string.system_ui_status_bar_mobile_type_single_title,
            "com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar.MobileNetworkTypeSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar_doubleline_network,
            R.string.system_ui,
            R.string.system_ui_statusbar_title,
            R.string.system_ui_statusbar_iconmanage_title,
            R.string.system_ui_statusbar_iconmanage_mobile_network_title,
            R.string.system_ui_statusbar_iconmanage_mobile_network_title,
            "com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar.DoubleLineNetworkSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar_battery_styles,
            R.string.system_ui,
            R.string.system_ui_statusbar_title,
            R.string.system_ui_statusbar_iconmanage_title,
            R.string.system_ui_status_bar_battery_style_title,
            R.string.system_ui_status_bar_battery_style_title,
            "com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar.BatteryStyleSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar_network_speed_indicator,
            R.string.system_ui,
            R.string.system_ui_statusbar_title,
            R.string.system_ui_statusbar_network_speed_indicator_title,
            R.string.system_ui_statusbar_network_speed_indicator_title,
            R.string.system_ui_statusbar_network_speed_indicator_title,
            "com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar.NetworkSpeedIndicatorSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar_clock_indicator,
            R.string.system_ui,
            R.string.system_ui_statusbar_title,
            R.string.system_ui_statusbar_clock_title,
            R.string.system_ui_statusbar_clock_title,
            R.string.system_ui_statusbar_clock_title,
            "com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar.ClockIndicatorSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar_hardware_detail_indicator,
            R.string.system_ui,
            R.string.system_ui_statusbar_title,
            R.string.system_ui_statusbar_device_title,
            R.string.system_ui_statusbar_device_title,
            R.string.system_ui_statusbar_device_title,
            "com.sevtinge.hyperceiler.ui.fragment.systemui.statusbar.BatteryDetailIndicatorSettings");

        parsePrefXml(context, R.xml.system_ui_navigation,
            R.string.system_ui,
            R.string.system_ui_navigation_title,
            R.string.system_ui_navigation_title,
            R.string.system_ui_navigation_title,
            R.string.system_ui_navigation_title,
            "com.sevtinge.hyperceiler.ui.fragment.systemui.NavigationSettings");

        parsePrefXml(context, R.xml.system_ui_control_center,
            R.string.system_ui,
            R.string.system_ui_controlcenter_title,
            R.string.system_ui_controlcenter_title,
            R.string.system_ui_controlcenter_title,
            R.string.system_ui_controlcenter_title,
            "com.sevtinge.hyperceiler.ui.fragment.systemui.ControlCenterSettings");

        parsePrefXml(context, R.xml.system_ui_other,
            R.string.system_ui,
            R.string.system_ui_other_title,
            R.string.system_ui_other_title,
            R.string.system_ui_other_title,
            R.string.system_ui_other_title,
            "com.sevtinge.hyperceiler.ui.fragment.systemui.SystemUIOtherSettings");

        // 系统桌面相关
        parsePrefXml(context, R.xml.home_gesture,
            R.string.mihome,
            R.string.home_gesture,
            R.string.home_gesture,
            R.string.home_gesture,
            R.string.home_gesture,
            "com.sevtinge.hyperceiler.ui.fragment.home.HomeGestureSettings");

        parsePrefXml(context, R.xml.home_layout,
            R.string.mihome,
            R.string.home_layout,
            R.string.home_layout,
            R.string.home_layout,
            R.string.home_layout,
            "com.sevtinge.hyperceiler.ui.fragment.home.HomeLayoutSettings");

        parsePrefXml(context, R.xml.home_folder,
            R.string.mihome,
            R.string.home_folder,
            R.string.home_folder,
            R.string.home_folder,
            R.string.home_folder,
            "com.sevtinge.hyperceiler.ui.fragment.home.HomeFolderSettings");

        parsePrefXml(context, R.xml.home_drawer,
            R.string.mihome,
            R.string.home_drawer,
            R.string.home_drawer,
            R.string.home_drawer,
            R.string.home_drawer,
            "com.sevtinge.hyperceiler.ui.fragment.home.HomeDrawerSettings");

        parsePrefXml(context, R.xml.home_title,
            R.string.mihome,
            R.string.home_title,
            R.string.home_title,
            R.string.home_title,
            R.string.home_title,
            "com.sevtinge.hyperceiler.ui.fragment.home.HomeTitleSettings");

        parsePrefXml(context, R.xml.home_title_anim,
            R.string.mihome,
            R.string.home_title,
            R.string.home_title_custom_anim_param,
            R.string.home_title_custom_anim_param,
            R.string.home_title_custom_anim_param,
            "com.sevtinge.hyperceiler.ui.fragment.home.HomeTitleAnimSettings");

        parsePrefXml(context, R.xml.home_title_anim_1,
            R.string.mihome,
            R.string.home_title,
            R.string.home_title_custom_anim_param,
            R.string.home_title_custom_anim_param_1_title,
            R.string.home_title_custom_anim_param_1_title,
            "com.sevtinge.hyperceiler.ui.fragment.home.anim.HomeTitleAnim1Settings");

        parsePrefXml(context, R.xml.home_title_anim_2,
            R.string.mihome,
            R.string.home_title,
            R.string.home_title_custom_anim_param,
            R.string.home_title_custom_anim_param_2_title,
            R.string.home_title_custom_anim_param_2_title,
            "com.sevtinge.hyperceiler.ui.fragment.home.anim.HomeTitleAnim2Settings");

        parsePrefXml(context, R.xml.home_title_anim_3,
            R.string.mihome,
            R.string.home_title,
            R.string.home_title_custom_anim_param,
            R.string.home_title_custom_anim_param_3_title,
            R.string.home_title_custom_anim_param_3_title,
            "com.sevtinge.hyperceiler.ui.fragment.home.anim.HomeTitleAnim3Settings");

        parsePrefXml(context, R.xml.home_title_anim_4,
            R.string.mihome,
            R.string.home_title,
            R.string.home_title_custom_anim_param,
            R.string.home_title_custom_anim_param_4_title,
            R.string.home_title_custom_anim_param_4_title,
            "com.sevtinge.hyperceiler.ui.fragment.home.anim.HomeTitleAnim4Settings");

        parsePrefXml(context, R.xml.home_title_anim_5,
            R.string.mihome,
            R.string.home_title,
            R.string.home_title_custom_anim_param,
            R.string.home_title_custom_anim_param_5_title,
            R.string.home_title_custom_anim_param_5_title,
            "com.sevtinge.hyperceiler.ui.fragment.home.anim.HomeTitleAnim5Settings");

        parsePrefXml(context, R.xml.home_title_anim_6,
            R.string.mihome,
            R.string.home_title,
            R.string.home_title_custom_anim_param,
            R.string.home_title_custom_anim_param_6_title,
            R.string.home_title_custom_anim_param_6_title,
            "com.sevtinge.hyperceiler.ui.fragment.home.anim.HomeTitleAnimSettings");

        parsePrefXml(context, R.xml.home_title_anim_7,
            R.string.mihome,
            R.string.home_title,
            R.string.home_title_custom_anim_param,
            R.string.home_title_custom_anim_param_7_title,
            R.string.home_title_custom_anim_param_7_title,
            "com.sevtinge.hyperceiler.ui.fragment.home.anim.HomeTitleAnim7Settings");

        parsePrefXml(context, R.xml.home_title_anim_8,
            R.string.mihome,
            R.string.home_title,
            R.string.home_title_custom_anim_param,
            R.string.home_title_custom_anim_param_8_title,
            R.string.home_title_custom_anim_param_8_title,
            "com.sevtinge.hyperceiler.ui.fragment.home.anim.HomeTitleAnim8Settings");

        parsePrefXml(context, R.xml.home_recent,
            R.string.mihome,
            R.string.home_recent,
            R.string.home_recent,
            R.string.home_recent,
            R.string.home_recent,
            "com.sevtinge.hyperceiler.ui.fragment.home.HomeRecentSettings");

        parsePrefXml(context, R.xml.home_widget,
            R.string.mihome,
            R.string.home_widget,
            R.string.home_widget,
            R.string.home_widget,
            R.string.home_widget,
            "com.sevtinge.hyperceiler.ui.fragment.home.HomeWidgetSettings");

        parsePrefXml(context, R.xml.home_dock,
            R.string.mihome,
            R.string.home_dock,
            R.string.home_dock,
            R.string.home_dock,
            R.string.home_dock,
            "com.sevtinge.hyperceiler.ui.fragment.home.HomeDockSettings");

        parsePrefXml(context, R.xml.home_other,
            R.string.mihome,
            R.string.home_other,
            R.string.home_other,
            R.string.home_other,
            R.string.home_other,
            "com.sevtinge.hyperceiler.ui.fragment.home.HomeOtherSettings");

        // 设置相关
        parsePrefXml(context, R.xml.system_settings,
            R.string.system_settings,
            R.string.system_settings,
            R.string.system_settings,
            R.string.system_settings,
            R.string.system_settings,
            "com.sevtinge.hyperceiler.ui.fragment.SystemSettingsFragment");

        // 其他杂项
        parsePrefXml(context, R.xml.browser,
            R.string.browser,
            R.string.browser,
            R.string.browser,
            R.string.browser,
            R.string.browser,
            "com.sevtinge.hyperceiler.ui.fragment.BrowserFragment");

        parsePrefXml(context, R.xml.camera,
            R.string.camera,
            R.string.camera,
            R.string.camera,
            R.string.camera,
            R.string.camera,
            "com.sevtinge.hyperceiler.ui.fragment.CameraFragment");

        parsePrefXml(context, R.xml.clock,
            R.string.clock,
            R.string.clock,
            R.string.clock,
            R.string.clock,
            R.string.clock,
            "com.sevtinge.hyperceiler.ui.fragment.ClockFragment");

        parsePrefXml(context, R.xml.fileexplorer,
            R.string.fileexplorer,
            R.string.fileexplorer,
            R.string.fileexplorer,
            R.string.fileexplorer,
            R.string.fileexplorer,
            "com.sevtinge.hyperceiler.ui.fragment.FileExplorerFragment");

        parsePrefXml(context, R.xml.incallui,
            R.string.incallui,
            R.string.incallui,
            R.string.incallui,
            R.string.incallui,
            R.string.incallui,
            "com.sevtinge.hyperceiler.ui.fragment.InCallUiFragment");

        parsePrefXml(context, R.xml.mms,
            R.string.mms,
            R.string.mms,
            R.string.mms,
            R.string.mms,
            R.string.mms,
            "com.sevtinge.hyperceiler.ui.fragment.MmsFragment");

        parsePrefXml(context, R.xml.nfc,
            R.string.nfc,
            R.string.nfc,
            R.string.nfc,
            R.string.nfc,
            R.string.nfc,
            "com.sevtinge.hyperceiler.ui.fragment.NfcFragment");

        parsePrefXml(context, R.xml.phone,
            R.string.phone,
            R.string.phone,
            R.string.phone,
            R.string.phone,
            R.string.phone,
            "com.sevtinge.hyperceiler.ui.fragment.PhoneFragment");

        parsePrefXml(context, R.xml.downloads,
            R.string.downloads,
            R.string.downloads,
            R.string.downloads,
            R.string.downloads,
            R.string.downloads,
            "com.sevtinge.hyperceiler.ui.fragment.DownloadsFragment");

        parsePrefXml(context, R.xml.updater,
            R.string.updater,
            R.string.updater,
            R.string.updater,
            R.string.updater,
            R.string.updater,
            "com.sevtinge.hyperceiler.ui.fragment.UpdaterFragment");

        parsePrefXml(context, R.xml.lbe_security,
            R.string.lbe,
            R.string.lbe,
            R.string.lbe,
            R.string.lbe,
            R.string.lbe,
            "com.sevtinge.hyperceiler.ui.fragment.LbeFragment");

        parsePrefXml(context, R.xml.milink,
            R.string.milink,
            R.string.milink,
            R.string.milink,
            R.string.milink,
            R.string.milink,
            "com.sevtinge.hyperceiler.ui.fragment.MiLinkFragment");

        parsePrefXml(context, R.xml.aod,
            R.string.aod,
            R.string.aod,
            R.string.aod,
            R.string.aod,
            R.string.aod,
            "com.sevtinge.hyperceiler.ui.fragment.AodFragment");

        parsePrefXml(context, R.xml.content_extension,
            R.string.content_extension,
            R.string.content_extension,
            R.string.content_extension,
            R.string.content_extension,
            R.string.content_extension,
            "com.sevtinge.hyperceiler.ui.fragment.ContentExtensionFragment");

        parsePrefXml(context, R.xml.gallery,
            R.string.gallery,
            R.string.gallery,
            R.string.gallery,
            R.string.gallery,
            R.string.gallery,
            "com.sevtinge.hyperceiler.ui.fragment.GalleryFragment");

        parsePrefXml(context, R.xml.guard_provider,
            R.string.guard_provider,
            R.string.guard_provider,
            R.string.guard_provider,
            R.string.guard_provider,
            R.string.guard_provider,
            "com.sevtinge.hyperceiler.ui.fragment.GuardProviderFragment");

        parsePrefXml(context, R.xml.mediaeditor,
            R.string.mediaeditor,
            R.string.mediaeditor,
            R.string.mediaeditor,
            R.string.mediaeditor,
            R.string.mediaeditor,
            "com.sevtinge.hyperceiler.ui.fragment.MediaEditorFragment");

        parsePrefXml(context, R.xml.mishare,
            R.string.mishare,
            R.string.mishare,
            R.string.mishare,
            R.string.mishare,
            R.string.mishare,
            "com.sevtinge.hyperceiler.ui.fragment.MiShareFragment");

        parsePrefXml(context, R.xml.miwallpaper,
            R.string.miwallpaper,
            R.string.miwallpaper,
            R.string.miwallpaper,
            R.string.miwallpaper,
            R.string.miwallpaper,
            "com.sevtinge.hyperceiler.ui.fragment.MiWallpaperFragment");

        parsePrefXml(context, R.xml.package_installer,
            R.string.package_installer,
            R.string.package_installer,
            R.string.package_installer,
            R.string.package_installer,
            R.string.package_installer,
            "com.sevtinge.hyperceiler.ui.fragment.MiuiPackageInstallerFragment");

        parsePrefXml(context, R.xml.music,
            R.string.music,
            R.string.music,
            R.string.music,
            R.string.music,
            R.string.music,
            "com.sevtinge.hyperceiler.ui.fragment.MusicFragment");

        parsePrefXml(context, R.xml.powerkeeper,
            R.string.powerkeeper,
            R.string.powerkeeper,
            R.string.powerkeeper,
            R.string.powerkeeper,
            R.string.powerkeeper,
            "com.sevtinge.hyperceiler.ui.fragment.PowerKeeperFragment");

        parsePrefXml(context, R.xml.screenrecorder,
            R.string.screenrecorder,
            R.string.screenrecorder,
            R.string.screenrecorder,
            R.string.screenrecorder,
            R.string.screenrecorder,
            "com.sevtinge.hyperceiler.ui.fragment.ScreenRecorderFragment");

        parsePrefXml(context, R.xml.screenshot,
            R.string.screenshot,
            R.string.screenshot,
            R.string.screenshot,
            R.string.screenshot,
            R.string.screenshot,
            "com.sevtinge.hyperceiler.ui.fragment.ScreenShotFragment");

        parsePrefXml(context, R.xml.security_center,
            !isPad() ? R.string.security_center : R.string.security_center_pad,
            !isPad() ? R.string.security_center : R.string.security_center_pad,
            !isPad() ? R.string.security_center : R.string.security_center_pad,
            !isPad() ? R.string.security_center : R.string.security_center_pad,
            !isPad() ? R.string.security_center : R.string.security_center_pad,
            "com.sevtinge.hyperceiler.ui.fragment.SecurityCenterFragment");

        parsePrefXml(context, R.xml.tsmclient,
            R.string.tsmclient,
            R.string.tsmclient,
            R.string.tsmclient,
            R.string.tsmclient,
            R.string.tsmclient,
            "com.sevtinge.hyperceiler.ui.fragment.TsmClientFragment");

        parsePrefXml(context, R.xml.weather,
            R.string.weather,
            R.string.weather,
            R.string.weather,
            R.string.weather,
            R.string.weather,
            "com.sevtinge.hyperceiler.ui.fragment.WeatherFragment");

        parsePrefXml(context, R.xml.aiasst,
            R.string.aiasst,
            R.string.aiasst,
            R.string.aiasst,
            R.string.aiasst,
            R.string.aiasst,
            "com.sevtinge.hyperceiler.ui.fragment.AiAsstFragment");

        parsePrefXml(context, R.xml.tsmclient,
            R.string.tsmclient,
            R.string.tsmclient,
            R.string.tsmclient,
            R.string.tsmclient,
            R.string.tsmclient,
            "com.sevtinge.hyperceiler.ui.fragment.TsmClientFragment");

        parsePrefXml(context, R.xml.barrage,
            R.string.barrage,
            R.string.barrage,
            R.string.barrage,
            R.string.barrage,
            R.string.barrage,
            "com.sevtinge.hyperceiler.ui.fragment.BarrageFragment");

        parsePrefXml(context, R.xml.joyose,
            R.string.joyose,
            R.string.joyose,
            R.string.joyose,
            R.string.joyose,
            R.string.joyose,
            "com.sevtinge.hyperceiler.ui.fragment.JoyoseFragment");

        parsePrefXml(context, R.xml.market,
            R.string.market,
            R.string.market,
            R.string.market,
            R.string.market,
            R.string.market,
            "com.sevtinge.hyperceiler.ui.fragment.MarketFragment");

        parsePrefXml(context, R.xml.mirror,
            R.string.mirror,
            R.string.mirror,
            R.string.mirror,
            R.string.mirror,
            R.string.mirror,
            "com.sevtinge.hyperceiler.ui.fragment.MirrorFragment");

        parsePrefXml(context, R.xml.mtb,
            R.string.mtb,
            R.string.mtb,
            R.string.mtb,
            R.string.mtb,
            R.string.mtb,
            "com.sevtinge.hyperceiler.ui.fragment.MtbFragment");

        parsePrefXml(context, R.xml.scanner,
            R.string.scanner,
            R.string.scanner,
            R.string.scanner,
            R.string.scanner,
            R.string.scanner,
            "com.sevtinge.hyperceiler.ui.fragment.ScannerFragment");

        parsePrefXml(context, R.xml.creation,
            R.string.creation,
            R.string.creation,
            R.string.creation,
            R.string.creation,
            R.string.creation,
            "com.sevtinge.hyperceiler.ui.fragment.CreationFragment");

        parsePrefXml(context, R.xml.various,
            R.string.various,
            R.string.various,
            R.string.various,
            R.string.various,
            R.string.various,
            "com.sevtinge.hyperceiler.ui.fragment.VariousFragment");

        parsePrefXml(context, R.xml.various_aosp,
            R.string.various,
            R.string.various,
            R.string.various_open_aosp_something_title,
            R.string.various_open_aosp_something_title,
            R.string.various_open_aosp_something_title,
            "com.sevtinge.hyperceiler.ui.fragment.various.AOSPSettings");

        if (isPad()) {
            parsePrefXml(context, R.xml.various_mipad,
                R.string.various,
                R.string.various_mipad_title,
                R.string.various_mipad_title,
                R.string.various_mipad_title,
                R.string.various_mipad_title,
                "com.sevtinge.hyperceiler.ui.fragment.VariousFragment");
        }

        // 实验性
        parsePrefXml(context, R.xml.theme_manager,
            R.string.theme_manager,
            R.string.theme_manager,
            R.string.theme_manager,
            R.string.theme_manager,
            R.string.theme_manager,
            "com.sevtinge.hyperceiler.ui.fragment.ThemeManagerFragment");

        parsePrefXml(context, R.xml.personal_assistant,
            R.string.personal_assistant,
            R.string.personal_assistant,
            R.string.personal_assistant,
            R.string.personal_assistant,
            R.string.personal_assistant,
            "com.sevtinge.hyperceiler.ui.fragment.PersonalAssistantFragment");
    }

    private static void parsePrefXml(Context context, int xmlResId, int catResId, int catSub1ResId, int catSub2ResId, int catSub3ResId, int catSub4ResId, String catPrefsFragment) {
        Resources res = context.getResources();
        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int order = 0;
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && !xml.getName().equals("PreferenceCategory")) {
                    try {
                        ModData modData = new ModData();
                        modData.title = getModTitle(res, xml.getAttributeValue(ANDROID_NS, "title"));
                        if (!TextUtils.isEmpty(modData.title)) {
                            if (!res.getString(catSub3ResId).equals(res.getString(catSub4ResId))) {
                                modData.breadcrumbs = res.getString(catResId) + "/" + res.getString(catSub1ResId) + "/" + res.getString(catSub2ResId) + "/" + res.getString(catSub3ResId) + "/" + res.getString(catSub4ResId);
                            } else if (!res.getString(catSub2ResId).equals(res.getString(catSub3ResId))) {
                                modData.breadcrumbs = res.getString(catResId) + "/" + res.getString(catSub1ResId) + "/" + res.getString(catSub2ResId) + "/" + res.getString(catSub3ResId);
                            } else if (!res.getString(catSub1ResId).equals(res.getString(catSub2ResId))) {
                                modData.breadcrumbs = res.getString(catResId) + "/" + res.getString(catSub1ResId) + "/" + res.getString(catSub2ResId);
                            } else if (!res.getString(catResId).equals(res.getString(catSub1ResId))) {
                                modData.breadcrumbs = res.getString(catResId) + "/" + res.getString(catSub1ResId);
                            } else {
                                modData.breadcrumbs = res.getString(catResId);
                            }
                            modData.key = xml.getAttributeValue(ANDROID_NS, "key");
                            modData.order = order;
                            modData.catTitleResId = catSub4ResId;
                            modData.fragment = catPrefsFragment;
                            allModsList.add(modData);
                        }
                        order++;
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                eventType = xml.next();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static String getModTitle(Resources res, String title) {
        if (title == null) {
            return null;
        }
        int titleResId = Integer.parseInt(title.substring(1));
        if (titleResId <= 0) {
            return null;
        }
        return res.getString(titleResId);
    }
}
