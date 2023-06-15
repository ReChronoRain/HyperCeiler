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
        parsePrefXml(context, R.xml.framework_freeform,
            R.string.system_framework,
            R.string.floating_window,
            "com.sevtinge.cemiuiler.ui.fragment.framework.FreeFormSettings");

        parsePrefXml(context, R.xml.framework_volume,
            R.string.system_framework,
            R.string.system_framework_volume_title,
            "com.sevtinge.cemiuiler.ui.fragment.framework.VolumeSettings");
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
