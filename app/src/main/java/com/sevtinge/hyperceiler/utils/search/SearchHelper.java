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
package com.sevtinge.hyperceiler.utils.search;

import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.data.ModData;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchHelper {

    public static final int MARK_COLOR_VIBRANT = Color.rgb(255, 0, 0);
    public static final String NEW_MODS_SEARCH_QUERY = "\uD83C\uDD95";
    public static List<ModData> allModsList = new ArrayList<>();

    public static String TAG = "SearchHelper";

    public static final HashSet<String> NEW_MODS = new HashSet<>(
            Set.of(
                    "pref_key_launcher_nozoomanim"
            )
    );

    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    public static final String APP_NS = "http://schemas.android.com/apk/res-auto";

    public static void getAllMods(Context context, boolean force) {
        if (force) {
            allModsList.clear();
        } else if (!allModsList.isEmpty()) {
            return;
        }
        // 系统框架页面相关
        parsePrefXml(context, R.xml.framework_freeform,
                "com.sevtinge.hyperceiler.ui.fragment.app.framework.FreeFormSettings",
                R.string.system_framework
        );

        parsePrefXml(context, R.xml.framework_volume,
                "com.sevtinge.hyperceiler.ui.fragment.app.framework.VolumeSettings",
                R.string.system_framework);

        parsePrefXml(context, R.xml.framework_phone,
                "com.sevtinge.hyperceiler.ui.fragment.app.framework.NetworkSettings",
                R.string.system_framework);

        parsePrefXml(context, R.xml.framework_display,
                "com.sevtinge.hyperceiler.ui.fragment.app.framework.DisplaySettings",
                R.string.system_framework);

        parsePrefXml(context, R.xml.framework_other,
                "com.sevtinge.hyperceiler.ui.fragment.app.framework.OtherSettings",
                R.string.system_framework);

        // 系统界面页面相关
        parsePrefXml(context, R.xml.system_ui_lock_screen,
                "com.sevtinge.hyperceiler.ui.fragment.app.systemui.LockScreenSettings",
                R.string.system_ui);

        parsePrefXml(context, R.xml.system_ui_status_bar,
                "com.sevtinge.hyperceiler.ui.fragment.app.systemui.StatusBarSettings",
                R.string.system_ui);

        parsePrefXml(context, !isMoreHyperOSVersion(1f) ? R.xml.system_ui_status_bar_icon_manage : R.xml.system_ui_status_bar_icon_manage_new,
                !isMoreHyperOSVersion(1f) ? "com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.IconManageSettings" : "com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.IconManageNewSettings",
                R.string.system_ui,
                R.string.system_ui_statusbar_title);

        parsePrefXml(context, R.xml.system_ui_status_bar_mobile_network_type,
                "com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.MobileNetworkTypeSettings",
                R.string.system_ui,
                R.string.system_ui_statusbar_title,
                R.string.system_ui_statusbar_iconmanage_title);

        parsePrefXml(context, R.xml.system_ui_status_bar_doubleline_network,
                "com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.DoubleLineNetworkSettings",
                R.string.system_ui,
                R.string.system_ui_statusbar_title,
                R.string.system_ui_statusbar_iconmanage_title);

        parsePrefXml(context, R.xml.system_ui_status_bar_battery_styles,
                "com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.BatteryStyleSettings",
                R.string.system_ui,
                R.string.system_ui_statusbar_title,
                R.string.system_ui_statusbar_iconmanage_title);

        parsePrefXml(context, R.xml.system_ui_status_bar_network_speed_indicator,
                "com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.NetworkSpeedIndicatorSettings",
                R.string.system_ui,
                R.string.system_ui_statusbar_title);

        parsePrefXml(context, !isMoreHyperOSVersion(1f) ? R.xml.system_ui_status_bar_clock_indicator : R.xml.system_ui_status_bar_new_clock_indicator,
                !isMoreHyperOSVersion(1f) ? "com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.ClockIndicatorSettings" : "com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.NewClockIndicatorSettings",
                R.string.system_ui,
                R.string.system_ui_statusbar_title);

        // 这里
        parsePrefXml(context, R.xml.system_ui_status_bar_hardware_detail_indicator,
                "com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.BatteryDetailIndicatorSettings",
                R.string.system_ui,
                R.string.system_ui_statusbar_title);

        parsePrefXml(context, R.xml.system_ui_status_bar_strong_toast,
                "com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.StrongToastSettings",
                R.string.system_ui,
                R.string.system_ui_statusbar_title);

        parsePrefXml(context, R.xml.system_ui_navigation,
                "com.sevtinge.hyperceiler.ui.fragment.app.systemui.NavigationSettings",
                R.string.system_ui);

        parsePrefXml(context, R.xml.system_ui_control_center,
                "com.sevtinge.hyperceiler.ui.fragment.app.systemui.ControlCenterSettings",
                R.string.system_ui);

        parsePrefXml(context, R.xml.system_ui_other,
                "com.sevtinge.hyperceiler.ui.fragment.app.systemui.SystemUIOtherSettings",
                R.string.system_ui);

        // 系统桌面相关
        parsePrefXml(context, R.xml.home_gesture,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.HomeGestureSettings",
                R.string.mihome);

        parsePrefXml(context, R.xml.home_layout,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.HomeLayoutSettings",
                R.string.mihome);

        parsePrefXml(context, R.xml.home_folder,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.HomeFolderSettings",
                R.string.mihome);

        parsePrefXml(context, R.xml.home_drawer,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.HomeDrawerSettings",
                R.string.mihome);

        parsePrefXml(context, R.xml.home_title,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.HomeTitleSettings",
                R.string.mihome);

        parsePrefXml(context, R.xml.home_title_anim,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.HomeTitleAnimSettings",
                R.string.mihome,
                R.string.home_title);

        parsePrefXml(context, R.xml.home_title_anim_1,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim1Settings",
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param);

        parsePrefXml(context, R.xml.home_title_anim_2,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim2Settings",
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param);

        parsePrefXml(context, R.xml.home_title_anim_3,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim3Settings",
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param);

        parsePrefXml(context, R.xml.home_title_anim_4,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim4Settings",
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param);

        parsePrefXml(context, R.xml.home_title_anim_5,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim5Settings",
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param);

        parsePrefXml(context, R.xml.home_title_anim_6,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnimSettings",
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param);

        parsePrefXml(context, R.xml.home_title_anim_7,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim7Settings",
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param);

        parsePrefXml(context, R.xml.home_title_anim_8,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim8Settings",
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param);

        parsePrefXml(context, R.xml.home_recent,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.HomeRecentSettings",
                R.string.mihome);

        parsePrefXml(context, R.xml.home_widget,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.HomeWidgetSettings",
                R.string.mihome);

        parsePrefXml(context, R.xml.home_dock,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.HomeDockSettings",
                R.string.mihome);

        parsePrefXml(context, R.xml.home_other,
                "com.sevtinge.hyperceiler.ui.fragment.app.home.HomeOtherSettings",
                R.string.mihome);

        // 设置相关
        parsePrefXml(context, R.xml.system_settings,
                "com.sevtinge.hyperceiler.ui.fragment.app.SystemSettingsFragment");

        // 其他杂项
        parsePrefXml(context, R.xml.analytics,
                "com.sevtinge.hyperceiler.ui.fragment.app.AnalyticsFragment");

        parsePrefXml(context, R.xml.browser,
                "com.sevtinge.hyperceiler.ui.fragment.app.BrowserFragment");

        parsePrefXml(context, !isMoreHyperOSVersion(1f) ? R.xml.camera : R.xml.camera_new,
                !isMoreHyperOSVersion(1f) ? "com.sevtinge.hyperceiler.ui.fragment.app.CameraFragment" : "com.sevtinge.hyperceiler.ui.fragment.app.CameraNewFragment");

        parsePrefXml(context, R.xml.fileexplorer,
                "com.sevtinge.hyperceiler.ui.fragment.app.FileExplorerFragment");

        parsePrefXml(context, R.xml.incallui,
                "com.sevtinge.hyperceiler.ui.fragment.app.InCallUiFragment");

        parsePrefXml(context, R.xml.mms,
                "com.sevtinge.hyperceiler.ui.fragment.app.MmsFragment");

        parsePrefXml(context, R.xml.remotecontroller,
                "com.sevtinge.hyperceiler.ui.fragment.app.RemoteControllerFragment");

        parsePrefXml(context, R.xml.nfc,
                "com.sevtinge.hyperceiler.ui.fragment.app.NfcFragment");

        parsePrefXml(context, R.xml.phone,
                "com.sevtinge.hyperceiler.ui.fragment.app.PhoneFragment");

        parsePrefXml(context, R.xml.downloads,
                "com.sevtinge.hyperceiler.ui.fragment.app.DownloadsFragment");

        parsePrefXml(context, R.xml.downloads_ui,
                "com.sevtinge.hyperceiler.ui.fragment.app.DownloadsUIFragment");

        parsePrefXml(context, R.xml.updater,
                "com.sevtinge.hyperceiler.ui.fragment.app.UpdaterFragment");

        parsePrefXml(context, R.xml.lbe_security,
                "com.sevtinge.hyperceiler.ui.fragment.app.LbeFragment");

        parsePrefXml(context, R.xml.lpa,
                "com.sevtinge.hyperceiler.ui.fragment.app.LpaFragment");

        parsePrefXml(context, R.xml.milink,
                "com.sevtinge.hyperceiler.ui.fragment.app.MiLinkFragment");

        parsePrefXml(context, R.xml.aod,
                "com.sevtinge.hyperceiler.ui.fragment.app.AodFragment");

        parsePrefXml(context, R.xml.content_extension,
                "com.sevtinge.hyperceiler.ui.fragment.app.ContentExtensionFragment");

        parsePrefXml(context, R.xml.gallery,
                "com.sevtinge.hyperceiler.ui.fragment.app.GalleryFragment");

        parsePrefXml(context, R.xml.guard_provider,
                "com.sevtinge.hyperceiler.ui.fragment.app.GuardProviderFragment");

        parsePrefXml(context, R.xml.mediaeditor,
                "com.sevtinge.hyperceiler.ui.fragment.app.MediaEditorFragment");

        parsePrefXml(context, R.xml.mishare,
                "com.sevtinge.hyperceiler.ui.fragment.app.MiShareFragment");

        parsePrefXml(context, R.xml.miwallpaper,
                "com.sevtinge.hyperceiler.ui.fragment.app.MiWallpaperFragment");

        parsePrefXml(context, R.xml.package_installer,
                "com.sevtinge.hyperceiler.ui.fragment.app.MiuiPackageInstallerFragment");

        parsePrefXml(context, R.xml.powerkeeper,
                "com.sevtinge.hyperceiler.ui.fragment.app.PowerKeeperFragment");

        parsePrefXml(context, R.xml.screenrecorder,
                "com.sevtinge.hyperceiler.ui.fragment.app.ScreenRecorderFragment");

        parsePrefXml(context, R.xml.screenshot,
                "com.sevtinge.hyperceiler.ui.fragment.app.ScreenShotFragment");

        parsePrefXml(context, R.xml.security_center_app,
                "com.sevtinge.hyperceiler.ui.fragment.app.securitycenter.ApplicationsSettings",
                R.string.security_center);

        parsePrefXml(context, R.xml.security_center_battery,
                "com.sevtinge.hyperceiler.ui.fragment.app.securitycenter.BatterySettings",
                R.string.security_center);

        parsePrefXml(context, R.xml.security_center_privacy_safety,
                "com.sevtinge.hyperceiler.ui.fragment.app.securitycenter.PrivacySafetySettings",
                R.string.security_center);

        parsePrefXml(context, R.xml.security_center_sidebar,
                "com.sevtinge.hyperceiler.ui.fragment.app.securitycenter.SidebarSettings",
                R.string.security_center);

        parsePrefXml(context, R.xml.security_center_other,
                "com.sevtinge.hyperceiler.ui.fragment.app.securitycenter.OtherSettings",
                R.string.security_center);

        parsePrefXml(context, R.xml.tsmclient,
                "com.sevtinge.hyperceiler.ui.fragment.app.TsmClientFragment");

        parsePrefXml(context, R.xml.html_viewer,
                "com.sevtinge.hyperceiler.ui.fragment.app.HtmlViewerFragment");

        parsePrefXml(context, R.xml.weather,
                "com.sevtinge.hyperceiler.ui.fragment.app.WeatherFragment");

        parsePrefXml(context, R.xml.aiasst,
                "com.sevtinge.hyperceiler.ui.fragment.app.AiAsstFragment");

        parsePrefXml(context, R.xml.voicetrigger,
                "com.sevtinge.hyperceiler.ui.fragment.app.VoiceTriggerFragment");

        parsePrefXml(context, R.xml.telecom,
                "com.sevtinge.hyperceiler.ui.fragment.app.TelecomFragment");

        parsePrefXml(context, R.xml.tsmclient,
                "com.sevtinge.hyperceiler.ui.fragment.app.TsmClientFragment");

        parsePrefXml(context, R.xml.barrage,
                "com.sevtinge.hyperceiler.ui.fragment.app.BarrageFragment");

        parsePrefXml(context, R.xml.joyose,
                "com.sevtinge.hyperceiler.ui.fragment.app.JoyoseFragment");

        parsePrefXml(context, R.xml.getapps,
                "com.sevtinge.hyperceiler.ui.fragment.app.MarketFragment");

        parsePrefXml(context, R.xml.notes,
                "com.sevtinge.hyperceiler.ui.fragment.app.NotesFragment");

        parsePrefXml(context, R.xml.mtb,
                "com.sevtinge.hyperceiler.ui.fragment.app.MtbFragment");

        parsePrefXml(context, R.xml.scanner,
                "com.sevtinge.hyperceiler.ui.fragment.app.ScannerFragment");

        parsePrefXml(context, R.xml.micloud_service,
                "com.sevtinge.hyperceiler.ui.fragment.app.MiCloudServiceFragment");

        parsePrefXml(context, R.xml.creation,
                "com.sevtinge.hyperceiler.ui.fragment.app.CreationFragment");

        parsePrefXml(context, R.xml.huanji,
                "com.sevtinge.hyperceiler.ui.fragment.app.HuanjiFragment");

        parsePrefXml(context, R.xml.misound,
                "com.sevtinge.hyperceiler.ui.fragment.app.MiSoundFragment");

        parsePrefXml(context, R.xml.trustservice,
                "com.sevtinge.hyperceiler.ui.fragment.app.TrustServiceFragment");

        parsePrefXml(context, R.xml.calendar,
                "com.sevtinge.hyperceiler.ui.fragment.app.CalendarFragment");

        parsePrefXml(context, R.xml.securityadd,
                "com.sevtinge.hyperceiler.ui.fragment.app.SecurityAddFragment");

        parsePrefXml(context, R.xml.various,
                "com.sevtinge.hyperceiler.ui.fragment.app.VariousFragment");

        parsePrefXml(context, R.xml.various_aosp,
                "com.sevtinge.hyperceiler.ui.fragment.app.various.AOSPSettings",
                R.string.various);

        parsePrefXml(context, R.xml.community,
                "com.sevtinge.hyperceiler.ui.fragment.app.CommunityFragment");

        if (isPad()) {
            parsePrefXml(context, R.xml.various_mipad,
                    "com.sevtinge.hyperceiler.ui.fragment.app.VariousFragment",
                    R.string.various);
        }

        // 实验性
        parsePrefXml(context, R.xml.theme_manager,
                "com.sevtinge.hyperceiler.ui.fragment.app.ThemeManagerFragment");

        parsePrefXml(context, R.xml.personal_assistant,
                "com.sevtinge.hyperceiler.ui.fragment.app.PersonalAssistantFragment");
    }

    private static void parsePrefXml(Context context, int xmlResId, String catPrefsFragment, int... internalId) {
        Resources res = context.getResources();
        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int order = 0;
            String location = null;
            String locationHyper = null;
            String locationPad = null;
            int locationId = 0;
            int locationHyperId = 0;
            int locationPadId = 0;
            boolean isPad = isPad();
            StringBuilder internalName = null;
            int eventType = xml.getEventType();
            if (internalId.length != 0) {
                for (int id : internalId) {
                    String getString = res.getString(id);
                    if (internalName == null) {
                        internalName = new StringBuilder(getString);
                    } else {
                        internalName.append("/").append(getString);
                    }
                }
            }
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && !xml.getName().equals("PreferenceCategory")) {
                    try {
                        ModData modData = new ModData();
                        modData.title = getModTitle(res, xml.getAttributeValue(ANDROID_NS, "title"));
                        boolean isPreferenceVisible = Boolean.parseBoolean(xml.getAttributeValue(APP_NS, "isPreferenceVisible"));
                        if (locationHyper == null) {
                            locationHyper = getModTitle(res, xml.getAttributeValue(APP_NS, "myLocationHyper"));
                            locationHyperId = getModId(xml.getAttributeValue(APP_NS, "myLocationHyper"));
                        }
                        if (locationPad == null) {
                            locationPad = getModTitle(res, xml.getAttributeValue(APP_NS, "myLocationPad"));
                            locationPadId = getModId(xml.getAttributeValue(APP_NS, "myLocationPad"));
                        }
                        if (location == null) {
                            location = getModTitle(res, xml.getAttributeValue(APP_NS, "myLocation"));
                            locationId = getModId(xml.getAttributeValue(APP_NS, "myLocation"));
                        }
                        if (!TextUtils.isEmpty(modData.title) && !isPreferenceVisible) {
                            String internalHyper = internalName == null ? locationHyper : internalName + "/" + locationHyper;
                            String internalPad = internalName == null ? locationPad : internalName + "/" + locationPad;
                            String internalMiui = internalName == null ? location : internalName + "/" + location;
                            if (locationHyper == null || location == null || (isPad && locationPad == null)) {
                                if (location != null) {
                                    modData.breadcrumbs = internalMiui;
                                    modData.catTitleResId = locationId;
                                } else if (locationHyper != null) {
                                    modData.breadcrumbs = internalHyper;
                                    modData.catTitleResId = locationHyperId;
                                } else if (locationPad != null) {
                                    modData.breadcrumbs = internalPad;
                                    modData.catTitleResId = locationPadId;
                                }
                            } else {
                                if (!isPad) {
                                    if (isMoreHyperOSVersion(1f)) {
                                        modData.breadcrumbs = internalHyper;
                                        modData.catTitleResId = locationHyperId;
                                    } else {
                                        modData.breadcrumbs = internalMiui;
                                        modData.catTitleResId = locationId;
                                    }
                                } else {
                                    modData.breadcrumbs = internalPad;
                                    modData.catTitleResId = locationPadId;
                                }
                            }
                            modData.key = xml.getAttributeValue(ANDROID_NS, "key");
                            modData.order = order;

                            modData.fragment = catPrefsFragment;
                            allModsList.add(modData);
                        }
                        order++;
                    } catch (Throwable t) {
                        AndroidLogUtils.logE(TAG, "Failed to get xml keyword object!", t);
                    }
                }
                eventType = xml.next();
            }
        } catch (Throwable t) {
            AndroidLogUtils.logE(TAG, "Failed to access XML resource!", t);
        }
    }

    private static int getModId(String title) {
        if (title == null) {
            return -1;
        }
        int titleResId = Integer.parseInt(title.substring(1));
        if (titleResId <= 0) {
            return -1;
        }
        return titleResId;
    }

    private static String getModTitle(Resources res, String title) {
        int id = getModId(title);
        if (id == -1) return null;
        return res.getString(id);
    }
}
