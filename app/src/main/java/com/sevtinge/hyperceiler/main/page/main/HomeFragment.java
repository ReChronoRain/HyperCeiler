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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.main.page.main;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;

import androidx.preference.PreferenceCategory;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.prefs.PreferenceHeader;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.main.banner.HomePageBannerHelper;
import com.sevtinge.hyperceiler.main.fragment.PagePreferenceFragment;
import com.sevtinge.hyperceiler.main.page.settings.helper.HomepageEntrance;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.function.BiConsumer;

import fan.preference.TextButtonPreference;

public class HomeFragment extends PagePreferenceFragment implements HomepageEntrance.EntranceState {

    private static final String TAG = "HomeFragment";
    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private TextButtonPreference mShowAppTips;
    private PreferenceCategory mHeadtipGround;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_main;
    }

    @Override
    public void initPrefs() {
        HomepageEntrance.setEntranceStateListen(this);
        setPreference();
        mHeadtipGround = findPreference("prefs_key_headtip_ground");
        mShowAppTips = findPreference("prefs_key_help_cant_see_app");
        HomePageBannerHelper.init(requireContext(), mHeadtipGround);

        boolean isHideTip = getSharedPreferences().getBoolean("prefs_key_help_cant_see_apps_switch", false);
        if (isHideTip && mShowAppTips != null) {
            mShowAppTips.setVisible(false);
        }
    }

    private void setPreference() {
        try {
            processXmlResource(R.xml.prefs_set_homepage_entrance, (key, summary) -> processSwitchPreference(key));
            processXmlResource(R.xml.prefs_main, this::processPreferenceHeader);
        } catch (XmlPullParserException | IOException e) {
            AndroidLogUtils.logE(TAG, "An error occurred when reading the XML:", e);
        }
    }

    private void processXmlResource(int xmlResId, BiConsumer<String, String> processor) throws XmlPullParserException, IOException {
        try (XmlResourceParser xml = getResources().getXml(xmlResId)) {
            int event = xml.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    String name = xml.getName();
                    if ("SwitchPreference".equals(name)) {
                        String key = xml.getAttributeValue(ANDROID_NS, "key");
                        processor.accept(key, null);
                    } else if (isPreferenceHeaderTag(xml)) {
                        String key = xml.getAttributeValue(ANDROID_NS, "key");
                        String summary = xml.getAttributeValue(ANDROID_NS, "summary");
                        processor.accept(key, summary);
                    }
                }
                event = xml.next();
            }
        }
    }

    private boolean isPreferenceHeaderTag(XmlResourceParser xml) {
        return "com.sevtinge.hyperceiler.common.prefs.PreferenceHeader".equals(xml.getName());
    }

    private void processSwitchPreference(String key) {
        if (key == null) return;
        String checkKey = key.replace("_state", "");
        boolean state = getSharedPreferences().getBoolean(key, true);
        if (!state) {
            PreferenceHeader preferenceHeader = findPreference(checkKey);
            if (preferenceHeader != null && preferenceHeader.isVisible()) {
                preferenceHeader.setVisible(false);
            }
        }
    }

    private void processPreferenceHeader(String key, String summary) {
        if (key == null || summary == null) return;
        PreferenceHeader header = findPreference(key);
        if (header != null) {
            setIconAndTitle(header, summary);
        }
    }

    private void setIconAndTitle(PreferenceHeader header, String packageName) {
        if (header == null || packageName == null) return;
        try {
            PackageManager pm = requireContext().getPackageManager();
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);
            Drawable icon = applicationInfo.loadIcon(pm);
            CharSequence name = applicationInfo.loadLabel(pm);
            header.setIcon(icon);
            if (!"android".equals(packageName)) {
                header.setTitle(name);
            }
        } catch (PackageManager.NameNotFoundException e) {
            AndroidLogUtils.logE(TAG, "Package not found: " + packageName, e);
        }
    }

    @Override
    public void onEntranceStateChange(String key, boolean state) {
        if (key == null) return;
        String mainKey = key.replace("_state", "");
        PreferenceHeader preferenceHeader = findPreference(mainKey);
        if (preferenceHeader != null) {
            boolean last = preferenceHeader.isVisible();
            if (!last || state) return;
            preferenceHeader.setVisible(false);
        }
    }
}
