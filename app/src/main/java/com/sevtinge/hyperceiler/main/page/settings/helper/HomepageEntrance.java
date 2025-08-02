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
package com.sevtinge.hyperceiler.main.page.settings.helper;

import static com.sevtinge.hyperceiler.common.prefs.PreferenceHeader.scope;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.common.utils.PackagesUtils;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.hook.utils.ToastHelper;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.function.BiConsumer;

import fan.preference.TextButtonPreference;

public class HomepageEntrance extends DashboardFragment implements Preference.OnPreferenceChangeListener {
    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private boolean isInit = false;
    private static final String TAG = "HomepageEntrance";
    private static EntranceState entranceState = null;

    private TextButtonPreference mShowAppTips;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_set_homepage_entrance;
    }

    public static void setEntranceStateListen(EntranceState entranceState) {
        HomepageEntrance.entranceState = entranceState;
    }

    @Override
    public void initPrefs() {
        if (isInit) return;
        super.initPrefs();
        Resources resources = getResources();
        parseXmlResource(resources, R.xml.prefs_set_homepage_entrance, this::processSwitchPreference);
        parseXmlResource(resources, R.xml.prefs_set_homepage_entrance, this::processSwitchPreferenceHeader);

        mShowAppTips = findPreference("prefs_key_help_cant_see_app");
        boolean isHideTip = getSharedPreferences().getBoolean("prefs_key_help_cant_see_apps_switch", false);
        if (isHideTip && mShowAppTips != null) {
            mShowAppTips.setVisible(false);
        }

        isInit = true;
    }

    private void parseXmlResource(Resources resources, int xmlResId, BiConsumer<XmlResourceParser, String> processor) {
        try (XmlResourceParser xml = resources.getXml(xmlResId)) {
            int event = xml.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "SwitchPreference".equals(xml.getName())) {
                    String key = xml.getAttributeValue(ANDROID_NS, "key");
                    if (key != null) {
                        processor.accept(xml, key);
                    }
                }
                event = xml.next();
            }
        } catch (XmlPullParserException | IOException e) {
            AndroidLogUtils.logE(TAG, "An error occurred when reading the XML:", e);
        }
    }

    private void processSwitchPreference(XmlResourceParser xml, String key) {
        SwitchPreference switchPreference = findPreference(key);
        if (switchPreference != null) {
            String summary = String.valueOf(switchPreference.getSummary());
            if (!"android".equals(summary) && PackagesUtils.checkAppStatus(getContext(), summary)) {
                switchPreference.setVisible(false);
            }
            switchPreference.setOnPreferenceChangeListener(this);
        }
    }

    private void processSwitchPreferenceHeader(XmlResourceParser xml, String key) {
        String summary = xml.getAttributeValue(ANDROID_NS, "summary");
        if (key == null || summary == null) return;
        SwitchPreference preferenceHeader = findPreference(key);
        if (preferenceHeader == null) return;
        if (!scope.contains(summary)) {
            preferenceHeader.setVisible(false);
        } else {
            Drawable icon = getPackageIcon(summary);
            String name = getPackageName(summary);
            if (icon != null) preferenceHeader.setIcon(icon);
            if (!"android".equals(summary) && name != null) preferenceHeader.setTitle(name);
        }
    }

    private Drawable getPackageIcon(String packageName) {
        try {
            return requireContext().getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            AndroidLogUtils.logE(TAG, "Package icon not found: " + packageName, e);
            return null;
        }
    }

    private String getPackageName(String packageName) {
        try {
            return String.valueOf(requireContext().getPackageManager()
                    .getApplicationLabel(
                            requireContext().getPackageManager()
                                    .getApplicationInfo(packageName, 0)));
        } catch (PackageManager.NameNotFoundException e) {
            AndroidLogUtils.logE(TAG, "Package name not found: " + packageName, e);
            return null;
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (!isInit) {
            ToastHelper.makeText(getContext(), "Loading. Please wait.");
            return false;
        }
        if (entranceState != null) {
            entranceState.onEntranceStateChange(preference.getKey(), (boolean) o);
        }
        return true;
    }

    public interface EntranceState {
        void onEntranceStateChange(String key, boolean state);
    }
}
