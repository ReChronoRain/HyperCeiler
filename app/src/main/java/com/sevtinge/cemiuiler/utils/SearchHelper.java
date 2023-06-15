package com.sevtinge.cemiuiler.utils;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.text.TextUtils;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.data.ModData;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class SearchHelper {

    public static final int markColorVibrant = Color.rgb(255, 0, 0);
    public static final String NEW_MODS_SEARCH_QUERY = "\uD83C\uDD95";
    public static ArrayList<ModData> allModsList = new ArrayList<ModData>();

    public static final HashSet<String> newMods = new HashSet<>(
        Arrays.asList(
            "pref_key_launcher_nozoomanim"
        )
    );

    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    public static final String MIUIZER_NS = "http://schemas.android.com/apk/res-auto";

    public static void getAllMods(Context context, boolean force) {
        if (force) allModsList.clear(); else if (allModsList.size() > 0) return;
        // 系统框架页面相关
        parsePrefXml(context, R.xml.framework_freeform,
            R.string.system_framework,
            R.string.floating_window,
            "com.sevtinge.cemiuiler.ui.fragment.framework.FreeFormSettings");

        parsePrefXml(context, R.xml.framework_volume,
            R.string.system_framework,
            R.string.system_framework_volume_title,
            "com.sevtinge.cemiuiler.ui.fragment.framework.VolumeSettings");

        parsePrefXml(context, R.xml.framework_phone,
            R.string.system_framework,
            R.string.system_framework_phone_title,
            "com.sevtinge.cemiuiler.ui.fragment.framework.NetworkSettings");

        parsePrefXml(context, R.xml.framework_other,
            R.string.system_framework,
            R.string.system_framework_other_title,
            "com.sevtinge.cemiuiler.ui.fragment.framework.OtherSettings");

        // 系统界面页面相关
        parsePrefXml(context, R.xml.system_ui_lock_screen,
            R.string.system_ui,
            R.string.system_ui_lockscreen_title,
            "com.sevtinge.cemiuiler.ui.fragment.systemui.LockScreenSettings");

        parsePrefXml(context, R.xml.system_ui_display,
            R.string.system_ui,
            R.string.system_ui_display_title,
            "com.sevtinge.cemiuiler.ui.fragment.systemui.DisplaySettings");

        parsePrefXml(context, R.xml.system_ui_status_bar,
            R.string.system_ui,
            R.string.system_ui_statusbar_title,
            "com.sevtinge.cemiuiler.ui.fragment.systemui.StatusBarSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar_icon_manage,
            R.string.system_ui,
            R.string.system_ui_statusbar_iconmanage_title,
            "com.sevtinge.cemiuiler.ui.fragment.systemui.statusbar.IconManageSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar_mobile_network_type,
            R.string.system_ui_statusbar_iconmanage_title,
            R.string.system_ui_status_bar_mobile_type_single_title,
            "com.sevtinge.cemiuiler.ui.fragment.systemui.statusbar.MobileNetworkTypeSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar_doubleline_network,
            R.string.system_ui_statusbar_iconmanage_title,
            R.string.system_ui_statusbar_iconmanage_mobile_network_title,
            "com.sevtinge.cemiuiler.ui.fragment.systemui.statusbar.DoubleLineNetworkSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar_network_speed_indicator,
            R.string.system_ui,
            R.string.system_ui_statusbar_network_speed_indicator_title,
            "com.sevtinge.cemiuiler.ui.fragment.systemui.statusbar.NetworkSpeedIndicatorSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar_clock_indicator,
            R.string.system_ui,
            R.string.system_ui_statusbar_clock_title,
            "com.sevtinge.cemiuiler.ui.fragment.systemui.statusbar.ClockIndicatorSettings");

        parsePrefXml(context, R.xml.system_ui_status_bar_hardware_detail_indicator,
            R.string.system_ui,
            R.string.system_ui_statusbar_device_title,
            "com.sevtinge.cemiuiler.ui.fragment.systemui.statusbar.BatteryDetailIndicatorSettings");

        parsePrefXml(context, R.xml.system_ui_navigation,
            R.string.system_ui,
            R.string.system_ui_navigation_title,
            "com.sevtinge.cemiuiler.ui.fragment.systemui.NavigationSettings");

        parsePrefXml(context, R.xml.system_ui_control_center,
            R.string.system_ui,
            R.string.system_ui_controlcenter_title,
            "com.sevtinge.cemiuiler.ui.fragment.systemui.ControlCenterSettings");

        parsePrefXml(context, R.xml.system_ui_other,
            R.string.system_ui,
            R.string.system_ui_other_title,
            "com.sevtinge.cemiuiler.ui.fragment.systemui.SystemUIOtherSettings");
    }

    private static void parsePrefXml(Context context, int xmlResId, int catResId, int catSubResId, String catPrefsFragment) {
        Resources res = context.getResources();
        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int order = 0;
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT ) {
                if (eventType == XmlPullParser.START_TAG && !xml.getName().equals("PreferenceCategory")) {
                    try {
                        ModData modData = new ModData();
                        modData.title = getModTitle(res, xml.getAttributeValue(ANDROID_NS, "title"));
                        if (!TextUtils.isEmpty(modData.title)) {
                            modData.breadcrumbs = res.getString(catResId) + "/" + res.getString(catSubResId);
                            modData.key = xml.getAttributeValue(ANDROID_NS, "key");
                            modData.order = order;
                            modData.catTitleResId = catSubResId;
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
        if (title == null) return null;
        int titleResId = Integer.parseInt(title.substring(1));
        if (titleResId <= 0) return null;
        return res.getString(titleResId);
    }
}
