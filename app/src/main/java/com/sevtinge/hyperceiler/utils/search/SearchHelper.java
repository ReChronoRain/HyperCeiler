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
import com.sevtinge.hyperceiler.ui.fragment.app.AodFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.CameraFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.CameraNewFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.ContentExtensionFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.MiCloudServiceFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.MiLinkFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.MiShareFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.NfcFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.PersonalAssistantFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.PhoneFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.SecurityAddFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.SystemSettingsFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.UpdaterFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.VariousFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.WeatherFragment;
import com.sevtinge.hyperceiler.ui.fragment.app.framework.CorePatchSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.framework.DisplaySettings;
import com.sevtinge.hyperceiler.ui.fragment.app.framework.FreeFormSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.framework.NetworkSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.framework.OtherSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.framework.VolumeSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.HomeDockSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.HomeDrawerSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.HomeFolderSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.HomeGestureSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.HomeLayoutSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.HomeOtherSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.HomeRecentSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.HomeTitleAnimSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.HomeTitleSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.HomeWidgetSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim1Settings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim2Settings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim3Settings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim4Settings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim5Settings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim7Settings;
import com.sevtinge.hyperceiler.ui.fragment.app.home.anim.HomeTitleAnim8Settings;
import com.sevtinge.hyperceiler.ui.fragment.app.securitycenter.ApplicationsSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.securitycenter.PrivacySafetySettings;
import com.sevtinge.hyperceiler.ui.fragment.app.securitycenter.SidebarSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.ControlCenterSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.LockScreenSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.NavigationSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.StatusBarSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.SystemUIOtherSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.BatteryDetailIndicatorSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.BatteryStyleSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.ClockIndicatorSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.DoubleLineNetworkSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.IconManageNewSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.IconManageSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.MobileNetworkTypeSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.NetworkSpeedIndicatorSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.NewClockIndicatorSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.systemui.statusbar.StrongToastSettings;
import com.sevtinge.hyperceiler.ui.fragment.app.various.AOSPSettings;
import com.sevtinge.hyperceiler.ui.fragment.dashboard.DashboardFragment;
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
        parsePrefXmlForFramework(context, FreeFormSettings.class, R.xml.framework_freeform);
        parsePrefXmlForFramework(context, VolumeSettings.class, R.xml.framework_volume);
        parsePrefXmlForFramework(context, NetworkSettings.class, R.xml.framework_phone);
        parsePrefXmlForFramework(context, DisplaySettings.class, R.xml.framework_display);
        parsePrefXmlForFramework(context, OtherSettings.class, R.xml.framework_other);
        parsePrefXmlForFramework(context, CorePatchSettings.class, R.xml.framework_core_patch);

        // 系统界面页面相关
        parsePrefXml(context, LockScreenSettings.class, R.xml.system_ui_lock_screen, R.string.system_ui);

        parsePrefXml(context, StatusBarSettings.class, R.xml.system_ui_status_bar, R.string.system_ui);

        parsePrefXml(context,
                !isMoreHyperOSVersion(1f) ? IconManageSettings.class : IconManageNewSettings.class,
                !isMoreHyperOSVersion(1f) ? R.xml.system_ui_status_bar_icon_manage : R.xml.system_ui_status_bar_icon_manage_new,
                R.string.system_ui,
                R.string.system_ui_statusbar_title
        );

        parsePrefXml(context, MobileNetworkTypeSettings.class, R.xml.system_ui_status_bar_mobile_network_type,
                R.string.system_ui,
                R.string.system_ui_statusbar_title,
                R.string.system_ui_statusbar_iconmanage_title
        );

        parsePrefXml(context, DoubleLineNetworkSettings.class, R.xml.system_ui_status_bar_doubleline_network,
                R.string.system_ui,
                R.string.system_ui_statusbar_title,
                R.string.system_ui_statusbar_iconmanage_title
        );

        parsePrefXml(context, BatteryStyleSettings.class, R.xml.system_ui_status_bar_battery_styles,
                R.string.system_ui,
                R.string.system_ui_statusbar_title,
                R.string.system_ui_statusbar_iconmanage_title
        );

        parsePrefXml(context, NetworkSpeedIndicatorSettings.class, R.xml.system_ui_status_bar_network_speed_indicator,
                R.string.system_ui,
                R.string.system_ui_statusbar_title
        );

        parsePrefXml(context,
                !isMoreHyperOSVersion(1f) ? ClockIndicatorSettings.class : NewClockIndicatorSettings.class,
                !isMoreHyperOSVersion(1f) ? R.xml.system_ui_status_bar_clock_indicator : R.xml.system_ui_status_bar_new_clock_indicator,
                R.string.system_ui,
                R.string.system_ui_statusbar_title
        );

        // 这里
        parsePrefXml(context, BatteryDetailIndicatorSettings.class,
                R.xml.system_ui_status_bar_hardware_detail_indicator,
                R.string.system_ui,
                R.string.system_ui_statusbar_title
        );

        parsePrefXml(context, StrongToastSettings.class,
                R.xml.system_ui_status_bar_strong_toast,
                R.string.system_ui,
                R.string.system_ui_statusbar_title
        );

        parsePrefXml(context,
                NavigationSettings.class,
                R.xml.system_ui_navigation,
                R.string.system_ui);

        parsePrefXml(context,
                ControlCenterSettings.class,
                R.xml.system_ui_control_center,
                R.string.system_ui
        );

        parsePrefXml(context, SystemUIOtherSettings.class,
                R.xml.system_ui_other,
                R.string.system_ui);

        // 系统桌面相关
        parsePrefXmlForHome(context, HomeGestureSettings.class, R.xml.home_gesture);
        parsePrefXml(context, HomeLayoutSettings.class, R.xml.home_layout);
        parsePrefXml(context, HomeFolderSettings.class, R.xml.home_folder);
        parsePrefXml(context, HomeDrawerSettings.class, R.xml.home_drawer);
        parsePrefXml(context, HomeTitleSettings.class, R.xml.home_title);

        parsePrefXml(context, HomeTitleAnimSettings.class, R.xml.home_title_anim, R.string.mihome, R.string.home_title);

        parsePrefXml(context, HomeTitleAnim1Settings.class,
                R.xml.home_title_anim_1,
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param
        );

        parsePrefXml(context, HomeTitleAnim2Settings.class,
                R.xml.home_title_anim_2,
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param
        );

        parsePrefXml(context, HomeTitleAnim3Settings.class,
                R.xml.home_title_anim_3,
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param
        );

        parsePrefXml(context,
                HomeTitleAnim4Settings.class,
                R.xml.home_title_anim_4,
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param);

        parsePrefXml(context,
                HomeTitleAnim5Settings.class,
                R.xml.home_title_anim_5,
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param);

        parsePrefXml(context,
                HomeTitleAnimSettings.class,
                R.xml.home_title_anim_6,
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param);

        parsePrefXml(context,
                HomeTitleAnim7Settings.class,
                R.xml.home_title_anim_7,
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param);

        parsePrefXml(context,
                HomeTitleAnim8Settings.class,
                R.xml.home_title_anim_8,
                R.string.mihome,
                R.string.home_title,
                R.string.home_title_custom_anim_param
        );

        parsePrefXmlForHome(context, HomeRecentSettings.class, R.xml.home_recent);
        parsePrefXmlForHome(context, HomeWidgetSettings.class, R.xml.home_widget);
        parsePrefXmlForHome(context, HomeDockSettings.class, R.xml.home_dock);
        parsePrefXmlForHome(context, HomeOtherSettings.class, R.xml.home_other);

        // 设置相关
        parsePrefXml(context, SystemSettingsFragment.class, R.xml.system_settings);

        // 其他杂项
        parsePrefXmlForDashboardFragment(context, R.xml.analytics);
        parsePrefXmlForDashboardFragment(context, R.xml.browser);
        parsePrefXml(
                context,
                !isMoreHyperOSVersion(1f) ? CameraFragment.class : CameraNewFragment.class,
                !isMoreHyperOSVersion(1f) ? R.xml.camera : R.xml.camera_new
        );
        parsePrefXmlForDashboardFragment(context, R.xml.fileexplorer);
        parsePrefXmlForDashboardFragment(context, R.xml.incallui);
        parsePrefXmlForDashboardFragment(context, R.xml.mms);
        parsePrefXmlForDashboardFragment(context, R.xml.remotecontroller);

        parsePrefXml(context, NfcFragment.class, R.xml.nfc);
        parsePrefXml(context, PhoneFragment.class, R.xml.phone);

        parsePrefXmlForDashboardFragment(context, R.xml.downloads);
        parsePrefXmlForDashboardFragment(context, R.xml.downloads_ui);

        parsePrefXml(context, UpdaterFragment.class, R.xml.updater);
        parsePrefXmlForDashboardFragment(context, R.xml.lbe_security);
        parsePrefXmlForDashboardFragment(context, R.xml.lpa);
        parsePrefXml(context, MiLinkFragment.class, R.xml.milink);
        parsePrefXml(context, AodFragment.class, R.xml.aod);
        parsePrefXml(context, ContentExtensionFragment.class, R.xml.content_extension);
        parsePrefXmlForDashboardFragment(context, R.xml.gallery);
        parsePrefXmlForDashboardFragment(context, R.xml.guard_provider);
        parsePrefXmlForDashboardFragment(context, R.xml.mediaeditor);
        parsePrefXml(context, MiShareFragment.class, R.xml.mishare);
        parsePrefXmlForDashboardFragment(context, R.xml.miwallpaper);
        parsePrefXmlForDashboardFragment(context, R.xml.package_installer);
        parsePrefXmlForDashboardFragment(context, R.xml.powerkeeper);
        parsePrefXmlForDashboardFragment(context, R.xml.screenrecorder);
        parsePrefXmlForDashboardFragment(context, R.xml.screenshot);

        parsePrefXmlForSecurityCenter(context, ApplicationsSettings.class, R.xml.security_center_app);
        parsePrefXmlForDashboardFragment(context, R.xml.security_center_battery);
        parsePrefXmlForSecurityCenter(context, PrivacySafetySettings.class, R.xml.security_center_privacy_safety);
        parsePrefXmlForSecurityCenter(context, SidebarSettings.class, R.xml.security_center_sidebar);
        parsePrefXmlForSecurityCenter(context, com.sevtinge.hyperceiler.ui.fragment.app.securitycenter.OtherSettings.class, R.xml.security_center_other);

        parsePrefXmlForDashboardFragment(context, R.xml.tsmclient);
        parsePrefXmlForDashboardFragment(context, R.xml.soundrecorder);
        parsePrefXmlForDashboardFragment(context, R.xml.html_viewer);
        parsePrefXml(context, WeatherFragment.class, R.xml.weather);
        parsePrefXmlForDashboardFragment(context, R.xml.aiasst);
        parsePrefXmlForDashboardFragment(context, R.xml.voicetrigger);
        parsePrefXmlForDashboardFragment(context, R.xml.telecom);
        parsePrefXmlForDashboardFragment(context, R.xml.tsmclient);
        parsePrefXmlForDashboardFragment(context, R.xml.barrage);
        parsePrefXmlForDashboardFragment(context, R.xml.joyose);
        parsePrefXmlForDashboardFragment(context, R.xml.getapps);
        parsePrefXmlForDashboardFragment(context, R.xml.notes);
        parsePrefXmlForDashboardFragment(context, R.xml.mtb);
        parsePrefXmlForDashboardFragment(context, R.xml.scanner);
        parsePrefXml(context, MiCloudServiceFragment.class, R.xml.micloud_service);
        parsePrefXmlForDashboardFragment(context, R.xml.creation);
        parsePrefXmlForDashboardFragment(context, R.xml.huanji);
        parsePrefXmlForDashboardFragment(context, R.xml.misound);
        parsePrefXmlForDashboardFragment(context, R.xml.trustservice);
        parsePrefXmlForDashboardFragment(context, R.xml.calendar);
        parsePrefXml(context, SecurityAddFragment.class, R.xml.securityadd);

        parsePrefXmlForDashboardFragment(context, R.xml.community);

        parsePrefXml(context, AOSPSettings.class, R.xml.various_aosp, R.string.various);
        parsePrefXmlForVarious(context, R.xml.various);
        if (isPad()) {
            parsePrefXmlForVarious(context, R.xml.various_mipad);
        }

        // 实验性
        parsePrefXmlForDashboardFragment(context, R.xml.theme_manager);
        parsePrefXml(context, PersonalAssistantFragment.class, R.xml.personal_assistant);
    }

    private static void parsePrefXmlForDashboardFragment(Context context, int xmlResId, int... internalId) {
        parsePrefXml(context, DashboardFragment.class, xmlResId, internalId);
    }

    private static void parsePrefXmlForFramework(Context context, Class<?> catPrefsFragment, int xmlResId) {
        parsePrefXml(context, catPrefsFragment, xmlResId, R.string.system_framework);
    }

    private static void parsePrefXmlForHome(Context context, Class<?> catPrefsFragment, int xmlResId) {
        parsePrefXml(context, catPrefsFragment, xmlResId, R.string.mihome);
    }

    private static void parsePrefXmlForSecurityCenter(Context context, Class<?> catPrefsFragment, int xmlResId) {
        parsePrefXml(context, catPrefsFragment, xmlResId, R.string.security_center);
    }

    private static void parsePrefXmlForVarious(Context context, int xmlResId) {
        parsePrefXml(context, VariousFragment.class, xmlResId, R.string.various);
    }

    private static void parsePrefXml(Context context, Class<?> catPrefsFragment, int xmlResId, int... internalId) {
        parsePrefXml(context, catPrefsFragment.getName(), xmlResId, internalId);
    }

    private static void parsePrefXml(Context context, String catPrefsFragment, int xmlResId, int... internalId) {
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
