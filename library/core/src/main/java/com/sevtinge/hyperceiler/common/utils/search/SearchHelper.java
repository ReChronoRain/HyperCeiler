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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.common.utils.search;


import static com.sevtinge.hyperceiler.common.utils.LanguageHelper.appLanguages;
import static com.sevtinge.hyperceiler.common.utils.LanguageHelper.localeFromAppLanguage;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.common.model.data.ModData;
import com.sevtinge.hyperceiler.common.utils.LanguageHelper;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.hooker.AodFragment;
import com.sevtinge.hyperceiler.hooker.CameraNewFragment;
import com.sevtinge.hyperceiler.hooker.ContentExtensionFragment;
import com.sevtinge.hyperceiler.hooker.MiCloudServiceFragment;
import com.sevtinge.hyperceiler.hooker.MiLinkFragment;
import com.sevtinge.hyperceiler.hooker.MiShareFragment;
import com.sevtinge.hyperceiler.hooker.NfcFragment;
import com.sevtinge.hyperceiler.hooker.PersonalAssistantFragment;
import com.sevtinge.hyperceiler.hooker.PhoneFragment;
import com.sevtinge.hyperceiler.hooker.SecurityAddFragment;
import com.sevtinge.hyperceiler.hooker.SystemSettingsFragment;
import com.sevtinge.hyperceiler.hooker.UpdaterFragment;
import com.sevtinge.hyperceiler.hooker.VariousFragment;
import com.sevtinge.hyperceiler.hooker.WeatherFragment;
import com.sevtinge.hyperceiler.hooker.framework.CorePatchSettings;
import com.sevtinge.hyperceiler.hooker.framework.DisplaySettings;
import com.sevtinge.hyperceiler.hooker.framework.FreeFormSettings;
import com.sevtinge.hyperceiler.hooker.framework.MiPadSettings;
import com.sevtinge.hyperceiler.hooker.framework.VolumeSettings;
import com.sevtinge.hyperceiler.hooker.home.HomeDockSettings;
import com.sevtinge.hyperceiler.hooker.home.HomeDrawerSettings;
import com.sevtinge.hyperceiler.hooker.home.HomeFolderSettings;
import com.sevtinge.hyperceiler.hooker.home.HomeGestureSettings;
import com.sevtinge.hyperceiler.hooker.home.HomeLayoutSettings;
import com.sevtinge.hyperceiler.hooker.home.HomeOtherSettings;
import com.sevtinge.hyperceiler.hooker.home.HomeRecentSettings;
import com.sevtinge.hyperceiler.hooker.home.HomeTitleSettings;
import com.sevtinge.hyperceiler.hooker.home.HomeWidgetSettings;
import com.sevtinge.hyperceiler.hooker.securitycenter.ApplicationsSettings;
import com.sevtinge.hyperceiler.hooker.securitycenter.OtherSettings;
import com.sevtinge.hyperceiler.hooker.securitycenter.PrivacySafetySettings;
import com.sevtinge.hyperceiler.hooker.securitycenter.SidebarSettings;
import com.sevtinge.hyperceiler.hooker.systemui.ControlCenterSettings;
import com.sevtinge.hyperceiler.hooker.systemui.LockScreenSettings;
import com.sevtinge.hyperceiler.hooker.systemui.MediaCardSettings;
import com.sevtinge.hyperceiler.hooker.systemui.NavigationSettings;
import com.sevtinge.hyperceiler.hooker.systemui.StatusBarSettings;
import com.sevtinge.hyperceiler.hooker.systemui.TileSettings;
import com.sevtinge.hyperceiler.hooker.systemui.statusbar.BatteryStyleSettings;
import com.sevtinge.hyperceiler.hooker.systemui.statusbar.DoubleLineNetworkSettings;
import com.sevtinge.hyperceiler.hooker.systemui.statusbar.IconManageNewSettings;
import com.sevtinge.hyperceiler.hooker.systemui.statusbar.MobileNetworkTypeSettings;
import com.sevtinge.hyperceiler.hooker.systemui.statusbar.NetworkSpeedIndicatorSettings;
import com.sevtinge.hyperceiler.hooker.systemui.statusbar.NewClockIndicatorSettings;
import com.sevtinge.hyperceiler.hooker.systemui.statusbar.StrongToastSettings;
import com.sevtinge.hyperceiler.hooker.various.AOSPSettings;
import com.sevtinge.hyperceiler.libhook.utils.api.ThreadPoolManager;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SearchHelper {

    public static final int MARK_COLOR_VIBRANT = Color.rgb(255, 0, 0);
    public static final String NEW_MODS_SEARCH_QUERY = "\uD83C\uDD95";
    public static final List<ModData> allModsList = new ArrayList<>();

    public static String TAG = "SearchHelper";

    public static final HashSet<String> NEW_MODS = new HashSet<>(Set.of("pref_key_launcher_nozoomanim"));

    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    public static final String APP_NS = "http://schemas.android.com/apk/res-auto";

    public static void init(Context context, boolean force) {
        ThreadPoolManager.getInstance().submit(() -> SearchHelper.getAllMods(context, force));
    }

    public static void getAllMods(Context context, boolean force) {
        if (force) {
            allModsList.clear();
        } else if (!allModsList.isEmpty()) {
            return;
        }

        // 系统框架页面相关
        parsePrefXmlForFramework(context, FreeFormSettings.class, R.xml.framework_freeform);
        parsePrefXmlForFramework(context, VolumeSettings.class, R.xml.framework_volume);
        parsePrefXmlForFramework(context, MiPadSettings.class, R.xml.various_mipad);
        parsePrefXmlForFramework(context, DisplaySettings.class, R.xml.framework_display);
        parsePrefXmlForFramework(context, OtherSettings.class, R.xml.framework_other);
        parsePrefXmlForFramework(context, CorePatchSettings.class, R.xml.framework_core_patch);

        // 系统界面页面相关
        parsePrefXml(context, LockScreenSettings.class, R.xml.system_ui_lock_screen, R.string.system_ui);

        parsePrefXml(context, StatusBarSettings.class, R.xml.system_ui_status_bar, R.string.system_ui);

        parsePrefXml(context,
                IconManageNewSettings.class,
                R.xml.system_ui_status_bar_icon_manage_new,
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
                NewClockIndicatorSettings.class,
                R.xml.system_ui_status_bar_new_clock_indicator,
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
            TileSettings.class,
            R.xml.system_ui_control_center_tiles,
            R.string.system_ui,
            R.string.system_ui_controlcenter_title
        );

        parsePrefXml(context,
            MediaCardSettings.class,
            R.xml.system_ui_control_center_media_cards,
            R.string.system_ui,
            R.string.system_ui_controlcenter_title
        );

        parsePrefXml(context,
                ControlCenterSettings.class,
                R.xml.system_ui_control_center,
                R.string.system_ui
        );

        parsePrefXml(context, com.sevtinge.hyperceiler.hooker.systemui.OtherSettings.class,
                R.xml.system_ui_other,
                R.string.system_ui);

        // 系统桌面相关
        parsePrefXmlForHome(context, HomeGestureSettings.class, R.xml.home_gesture);
        parsePrefXml(context, HomeLayoutSettings.class, R.xml.home_layout);
        parsePrefXml(context, HomeFolderSettings.class, R.xml.home_folder);
        parsePrefXml(context, HomeDrawerSettings.class, R.xml.home_drawer);
        parsePrefXml(context, HomeTitleSettings.class, R.xml.home_title);

        parsePrefXmlForHome(context, HomeRecentSettings.class, R.xml.home_recent);
        parsePrefXmlForHome(context, HomeWidgetSettings.class, R.xml.home_widget);
        parsePrefXmlForHome(context, HomeDockSettings.class, R.xml.home_dock);
        parsePrefXmlForHome(context, HomeOtherSettings.class, R.xml.home_other);

        // 设置相关
        parsePrefXml(context, SystemSettingsFragment.class, R.xml.system_settings);

        // 其他杂项
        parsePrefXmlForDashboardFragment(context, R.xml.analytics);
        parsePrefXmlForDashboardFragment(context, R.xml.browser);
        parsePrefXml(context, CameraNewFragment.class, R.xml.camera_new);
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
        parsePrefXmlForSecurityCenter(context, OtherSettings.class, R.xml.security_center_other);

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
        parsePrefXmlForDashboardFragment(context, R.xml.mtb);
        parsePrefXmlForDashboardFragment(context, R.xml.scanner);
        parsePrefXml(context, MiCloudServiceFragment.class, R.xml.micloud_service);
        parsePrefXmlForDashboardFragment(context, R.xml.creation);
        parsePrefXmlForDashboardFragment(context, R.xml.huanji);
        parsePrefXmlForDashboardFragment(context, R.xml.misound);
        parsePrefXmlForDashboardFragment(context, R.xml.trustservice);
        parsePrefXmlForDashboardFragment(context, R.xml.calendar);
        parsePrefXmlForDashboardFragment(context, R.xml.simactivate);
        parsePrefXmlForDashboardFragment(context, R.xml.contacts);
        parsePrefXmlForDashboardFragment(context, R.xml.health);
        parsePrefXml(context, SecurityAddFragment.class, R.xml.securityadd);

        parsePrefXmlForDashboardFragment(context, R.xml.community);
        parsePrefXmlForDashboardFragment(context, R.xml.phrase);

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
        parsePrefXml(context, catPrefsFragment, xmlResId, R.string.security_center_hyperos);
    }

    private static void parsePrefXmlForVarious(Context context, int xmlResId) {
        parsePrefXml(context, VariousFragment.class, xmlResId, R.string.various);
    }

    private static void parsePrefXml(Context context, Class<?> catPrefsFragment, int xmlResId, int... internalId) {
        parsePrefXml(context, catPrefsFragment.getName(), xmlResId, internalId);
    }

    private static Context createLocaleContext(Context base, Locale locale) {
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        return base.createConfigurationContext(config);
    }

    private static void parsePrefXml(Context context, String catPrefsFragment, int xmlResId, int... internalId) {
        ThreadPoolManager.getInstance().submit(() -> {
            int selectedLang = Integer.parseInt(PrefsUtils.getSharedStringPrefs(context, "prefs_key_settings_app_language", "0"));
            if (selectedLang < 0 || selectedLang >= appLanguages.length) selectedLang = 0;
            Locale locale = localeFromAppLanguage(appLanguages[selectedLang]);
            Context localeContext = createLocaleContext(context, locale);
            Resources res = localeContext.getResources();
            try (XmlResourceParser xml = res.getXml(xmlResId)) {
                int order = 0;
                String location = null, locationPad = null;
                int locationId = 0, locationPadId = 0;
                boolean isPadDevice = isPad();
                StringBuilder internalName = null;
                int eventType = xml.getEventType();

                if (internalId.length != 0) {
                    internalName = new StringBuilder();
                    for (int id : internalId) {
                        if (internalName.length() > 0) {
                            internalName.append("/");
                        }

                        internalName.append(res.getString(id));
                    }
                }

                List<ModData> localList = new ArrayList<>(32);

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && !"PreferenceCategory".equals(xml.getName())) {
                        try {
                            String titleAttr = xml.getAttributeValue(ANDROID_NS, "title");
                            String keyAttr = xml.getAttributeValue(ANDROID_NS, "key");
                            String isPrefVisibleAttr = xml.getAttributeValue(APP_NS, "isPreferenceVisible");
                            String myLocationPadAttr = xml.getAttributeValue(APP_NS, "myLocationPad");
                            String myLocationAttr = xml.getAttributeValue(APP_NS, "myLocation");

                            String modTitle = getModTitle(res, titleAttr);
                            boolean isPreferenceVisible = Boolean.parseBoolean(isPrefVisibleAttr);

                            if (locationPad == null && myLocationPadAttr != null) {
                                locationPad = getModTitle(res, myLocationPadAttr);
                                locationPadId = getModId(myLocationPadAttr);
                            }
                            if (location == null && myLocationAttr != null) {
                                location = getModTitle(res, myLocationAttr);
                                locationId = getModId(myLocationAttr);
                            }

                            if (!TextUtils.isEmpty(modTitle) && !isPreferenceVisible) {
                                String internalPad = internalName == null ? locationPad : internalName + "/" + locationPad;
                                String internal = internalName == null ? location : internalName + "/" + location;

                                ModData modData = new ModData();
                                modData.title = modTitle;
                                if (location != null && (!isPadDevice || locationPad == null)) {
                                    modData.breadcrumbs = internal;
                                    modData.catTitleResId = locationId;
                                } else if (locationPad != null) {
                                    modData.breadcrumbs = internalPad;
                                    modData.catTitleResId = locationPadId;
                                }
                                modData.xml = xmlResId;
                                modData.key = keyAttr;
                                modData.order = order;
                                modData.fragment = catPrefsFragment;
                                localList.add(modData);
                            }
                            order++;
                        } catch (Throwable t) {
                            AndroidLog.e(TAG, "Failed to get xml keyword object!", t);
                        }
                    }
                    eventType = xml.next();
                }

                if (!localList.isEmpty()) {
                    synchronized (allModsList) {
                        allModsList.addAll(localList);
                    }
                }

            } catch (Throwable t) {
                AndroidLog.e(TAG, "Failed to access XML resource!", t);
            }
        });
    }

    private static int getModId(String title) {
        if (title == null) {
            return -1;
        }
        try {
            int titleResId = Integer.parseInt(title.substring(1));
            if (titleResId <= 0) {
                return -1;
            }
            return titleResId;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String getModTitle(Resources res, String title) {
        int id = getModId(title);
        if (id == -1) return null;
        return res.getString(id);
    }
}
