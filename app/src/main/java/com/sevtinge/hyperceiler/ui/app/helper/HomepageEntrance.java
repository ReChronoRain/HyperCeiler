/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.ui.app.helper;

import static com.sevtinge.hyperceiler.prefs.PreferenceHeader.scope;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.ui.hooker.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.PackagesUtils;
import com.sevtinge.hyperceiler.utils.ToastHelper;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.function.BiConsumer;

public class HomepageEntrance extends DashboardFragment implements Preference.OnPreferenceChangeListener {
    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private boolean isInit = false;
    private static final String TAG = "HomepageEntrance";
    private static EntranceState entranceState = null;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_set_homepage_entrance;
    }

    public static void setEntranceStateListen(EntranceState entranceState) {
        HomepageEntrance.entranceState = entranceState;
    }

    @Override
    public void initPrefs() {
        super.initPrefs();
        if (isInit) return;
        Resources resources = getResources();
        parseXmlResource(resources, R.xml.prefs_set_homepage_entrance, this::processSwitchPreference);
        parseXmlResource(resources, R.xml.prefs_set_homepage_entrance, this::processSwitchPreferenceHeader);

        isInit = true;
    }

    private void parseXmlResource(Resources resources, int xmlResId, BiConsumer<XmlResourceParser, String> processor) {
        try (XmlResourceParser xml = resources.getXml(xmlResId)) {
            int event = xml.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "SwitchPreference".equals(xml.getName())) {
                    String key = xml.getAttributeValue(ANDROID_NS, "key");
                    processor.accept(xml, key);
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
            String summary = (String) switchPreference.getSummary();
            if (summary != null && !summary.equals("android") && PackagesUtils.checkAppStatus(getContext(), summary)) {
                switchPreference.setVisible(false);
            }
            switchPreference.setOnPreferenceChangeListener(HomepageEntrance.this);
        }
    }

    private void processSwitchPreferenceHeader(XmlResourceParser xml, String key) {
        String summary = xml.getAttributeValue(ANDROID_NS, "summary");
        if (key != null && summary != null) {
            SwitchPreference preferenceHeader = findPreference(key);
            if (!scope.contains(summary)) {
                if (preferenceHeader != null) {
                    preferenceHeader.setVisible(false);
                }
            } else {
                Drawable icon = getPackageIcon(summary);
                String name = getPackageName(summary);
                if (preferenceHeader != null) {
                    preferenceHeader.setIcon(icon);
                    if (!summary.equals("android")) preferenceHeader.setTitle(name);
                }
            }
        }
    }


    private Drawable getPackageIcon(String packageName) {
        try {
            return requireContext().getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getPackageName(String packageName) {
        try {
            return (String) requireContext().getPackageManager().getApplicationLabel(requireContext().getPackageManager().getApplicationInfo(packageName, 0));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null; // 如果包名找不到则返回 null
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (!isInit) {
            ToastHelper.makeText(getContext(), "Loading. Please wait.");
            return false;
        }
        entranceState.onEntranceStateChange(preference.getKey(), (boolean) o);
        return true;
    }

    public interface EntranceState {
        void onEntranceStateChange(String key, boolean state);
    }
}
