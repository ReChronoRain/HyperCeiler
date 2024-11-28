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
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.ui.fragment.main.helper;

import static com.sevtinge.hyperceiler.prefs.PreferenceHeader.scope;
import static com.sevtinge.hyperceiler.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.prefs.PreferenceHeader;
import com.sevtinge.hyperceiler.ui.fragment.base.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.ui.fragment.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.PackagesUtils;
import com.sevtinge.hyperceiler.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.utils.ToastHelper;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class HomepageEntrance extends DashboardFragment implements Preference.OnPreferenceChangeListener {
    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private boolean isInit = false;
    private final String TAG = "HomepageEntrance";
    private static EntranceState entranceState = null;

    Preference mSecurityCenter;
    Preference mMiLink;
    Preference mAod;
    Preference mGuardProvider;

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
        ThreadPoolManager.getInstance().submit(() -> {
            try (XmlResourceParser xml = resources.getXml(R.xml.prefs_set_homepage_entrance)) {
                try {
                    int event = xml.getEventType();
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG) {
                            if (xml.getName().equals("SwitchPreference")) {
                                String key = xml.getAttributeValue(ANDROID_NS, "key");
                                SwitchPreference switchPreference = findPreference(key);
                                if (switchPreference != null) {
                                    String summary = (String) switchPreference.getSummary();
                                    if (summary != null && !summary.equals("android")) {
                                        if (PackagesUtils.checkAppStatus(getContext(), summary)) {
                                            switchPreference.setVisible(false);
                                        }
                                    }
                                    switchPreference.setOnPreferenceChangeListener(HomepageEntrance.this);
                                }
                            }
                        }
                        event = xml.next();
                    }
                    isInit = true;
                } catch (XmlPullParserException | IOException e) {
                    AndroidLogUtils.logE(TAG, "An error occurred when reading the XML:", e);
                }
            }
        });
        mSecurityCenter = findPreference("prefs_key_security_center_state");
        mMiLink = findPreference("prefs_key_milink_state");
        mAod = findPreference("prefs_key_aod_state");
        mGuardProvider = findPreference("prefs_key_guardprovider_state");
        if (isMoreHyperOSVersion(1f)) {
            mAod.setTitle(R.string.aod_hyperos);
            mMiLink.setTitle(R.string.milink_hyperos);
            mGuardProvider.setTitle(R.string.guard_provider_hyperos);
            mSecurityCenter.setTitle(R.string.security_center_hyperos);
        } else {
            mAod.setTitle(R.string.aod);
            mMiLink.setTitle(R.string.milink);
            mGuardProvider.setTitle(R.string.guard_provider);
            if (isPad()) {
                mSecurityCenter.setTitle(R.string.security_center_pad);
            } else {
                mSecurityCenter.setTitle(R.string.security_center);
            }
        }

        setPreference();
    }

    private void setPreference() {
        Resources resources = getResources();
        try (XmlResourceParser xml = resources.getXml(R.xml.prefs_set_homepage_entrance)) {
            int event = xml.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && xml.getName().equals("SwitchPreference")) {
                    String key = xml.getAttributeValue(ANDROID_NS, "key");
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
                event = xml.next();
            }
        } catch (XmlPullParserException | IOException e) {
            AndroidLogUtils.logE(TAG, "An error occurred when reading the XML:", e);
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
