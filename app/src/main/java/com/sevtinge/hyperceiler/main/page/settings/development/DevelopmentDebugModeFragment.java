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

package com.sevtinge.hyperceiler.main.page.settings.development;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isPad;
import static com.sevtinge.hyperceiler.main.page.settings.helper.HomepageEntrance.ANDROID_NS;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;

import androidx.preference.EditTextPreference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.hook.utils.pkg.DebugModeUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import fan.preference.DropDownPreference;

public class DevelopmentDebugModeFragment extends SettingsPreferenceFragment {

    SwitchPreference mDebugMode;

    DropDownPreference mHomeVersion;
    DropDownPreference mSecurityVersion;
    DropDownPreference mCameraVersion;
    EditTextPreference mEditHomeVersion;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_development_debug_mode;
    }

    @Override
    public void initPrefs() {
        mDebugMode = findPreference("prefs_key_development_debug_mode");
        mHomeVersion = findPreference("prefs_key_debug_mode_home");
        mSecurityVersion = findPreference("prefs_key_debug_mode_security");
        mCameraVersion = findPreference("prefs_key_debug_mode_camera");
        mEditHomeVersion = findPreference("prefs_key_debug_mode_home_edit");

        mDebugMode.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean isDebug = (boolean) newValue;
            if (isDebug) {
                DialogHelper.showDialog(getActivity(), R.string.debug_mode, R.string.open_debug_mode_tips, (dialog, which) -> {
                    /*Toast.makeText(getActivity(), R.string.feature_doing_func, Toast.LENGTH_LONG).show();
                    mDebugMode.setChecked(false);*/
                    dialog.dismiss();
                }, (dialog, which) -> {
                    mDebugMode.setChecked(false);
                    dialog.dismiss();
                });
            }
            return true;
        });

        int currentHomeValue = parseIntSafe(getSharedPreferences().getString("prefs_key_debug_mode_home", "0"));

        if (isPad()) {
            mHomeVersion.setEntries(com.sevtinge.hyperceiler.core.R.array.debug_mode_home_pad);
            mHomeVersion.setEntryValues(com.sevtinge.hyperceiler.core.R.array.debug_mode_home_pad_value);
        }

        mEditHomeVersion.setVisible(currentHomeValue == 1);

        mHomeVersion.setOnPreferenceChangeListener((preference, newValue) -> {
            int newVal = parseIntSafe(newValue);
            mEditHomeVersion.setVisible(newVal == 1);
            if (newVal != 1) {
                DebugModeUtils.INSTANCE.setChooseResult("com.miui.home", newVal);
            }
            return true;
        });

        setSimpleDropDownListener(mSecurityVersion, "com.miui.securitycenter");
        setSimpleDropDownListener(mCameraVersion, "com.android.camera");

        mEditHomeVersion.setOnPreferenceChangeListener((preference, newValue) -> {
            int newVal = parseIntSafe(newValue);
            DebugModeUtils.INSTANCE.setChooseResult("com.miui.home", newVal);
            return true;
        });

        setPreference();
    }

    private void setSimpleDropDownListener(DropDownPreference pref, String packageName) {
        if (pref == null) return;
        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            int newVal = parseIntSafe(newValue);
            DebugModeUtils.INSTANCE.setChooseResult(packageName, newVal);
            return true;
        });
    }

    private int parseIntSafe(Object value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setPreference() {
        Resources resources = getResources();
        try (XmlResourceParser xml = resources.getXml(R.xml.prefs_development_debug_mode)) {
            int event = xml.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "fan.preference.DropDownPreference".equals(xml.getName())) {
                    String key = xml.getAttributeValue(ANDROID_NS, "key");
                    String summary = xml.getAttributeValue(ANDROID_NS, "summary");
                    if (key != null && summary != null) {
                        Drawable icon = getPackageIcon(summary);
                        String name = getPackageName(summary);
                        DropDownPreference preferenceHeader = findPreference(key);
                        if (preferenceHeader != null) {
                            if (icon != null) preferenceHeader.setIcon(icon);
                            if (!"android".equals(summary) && name != null) preferenceHeader.setTitle(name);
                        }
                    }
                }
                event = xml.next();
            }
        } catch (XmlPullParserException | IOException e) {
            AndroidLogUtils.logE("DevelopmentDebugModeFragment", "An error occurred when reading the XML:", e);
        }
    }

    private Drawable getPackageIcon(String packageName) {
        try {
            return requireContext().getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private String getPackageName(String packageName) {
        try {
            return (String) requireContext().getPackageManager()
                .getApplicationLabel(requireContext().getPackageManager().getApplicationInfo(packageName, 0));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
