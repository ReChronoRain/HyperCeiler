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

import static com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper.mScope;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.utils.PackagesUtils;
import com.sevtinge.hyperceiler.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.hook.utils.ToastHelper;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.model.data.AppInfoCache;
import com.sevtinge.hyperceiler.utils.XmlResourceParserHelper;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import fan.preference.TextButtonPreference;

public class HomepageEntrance extends DashboardFragment implements Preference.OnPreferenceChangeListener {
    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private boolean isInit = false;
    private static final String TAG = "HomepageEntrance";
    private static EntranceState mEntranceState = null;

    private TextButtonPreference mShowAppTips;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_set_homepage_entrance;
    }

    public static void setEntranceStateListen(EntranceState entranceState) {
        mEntranceState = entranceState;
    }

    @Override
    public void initPrefs() {
        if (isInit) return;
        Resources resources = getResources();
        try {
            XmlResourceParserHelper.processCachedXmlResource(resources, R.xml.prefs_set_homepage_entrance, (key, summary) -> processSwitchPreference(key));
            XmlResourceParserHelper.processCachedXmlResource(resources, R.xml.prefs_set_homepage_entrance, this::processSwitchPreferenceHeader);
        } catch (XmlPullParserException | IOException e) {
            AndroidLogUtils.logE(TAG, "An error occurred when reading the XML:", e);
        }
        mShowAppTips = findPreference("prefs_key_help_cant_see_app");
        boolean isHideTip = getSharedPreferences().getBoolean("prefs_key_help_cant_see_apps_switch", false);
        if (isHideTip && mShowAppTips != null) {
            mShowAppTips.setVisible(false);
        }

        isInit = true;
    }

    private void processSwitchPreference(String key) {
        SwitchPreference switchPreference = findPreference(key);
        if (switchPreference != null) {
            String summary = String.valueOf(switchPreference.getSummary());
            if (!"android".equals(summary) && PackagesUtils.checkAppStatus(getContext(), summary)) {
                switchPreference.setVisible(false);
            }
            switchPreference.setOnPreferenceChangeListener(this);
        }
    }

    private void processSwitchPreferenceHeader(String key, String summary) {
        if (key == null || summary == null) return;
        SwitchPreference preferenceHeader = findPreference(key);
        if (preferenceHeader == null) return;
        if (!mScope.contains(summary)) {
            preferenceHeader.setVisible(false);
        } else {
            setIconAndTitle(preferenceHeader, summary);
        }
    }

    private void setIconAndTitle(SwitchPreference header, String packageName) {
        if (header == null || packageName == null) return;
        PackageManager pm = requireContext().getPackageManager();

        try {
            pm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }

        ApplicationInfo appInfo = AppInfoCache.getInstance(getContext()).getAppInfo(packageName);
        if (appInfo == null) return;

        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(() -> {
                final Drawable icon = appInfo.loadIcon(pm);
                final CharSequence name = appInfo.loadLabel(pm);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    header.setIcon(icon);
                    if (!"android".equals(packageName)) {
                        header.setTitle(name);
                    }
                });
            }, "SetIconAndTitle").start();
        } else {
            final Drawable icon = appInfo.loadIcon(pm);
            final CharSequence name = appInfo.loadLabel(pm);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                header.setIcon(icon);
                if (!"android".equals(packageName)) {
                    header.setTitle(name);
                }
            });
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (!isInit) {
            ToastHelper.makeText(getContext(), "Loading. Please wait.");
            return false;
        }
        if (mEntranceState != null) {
            mEntranceState.onEntranceStateChange(preference.getKey(), (boolean) o);
        }
        return true;
    }

    public interface EntranceState {
        void onEntranceStateChange(String key, boolean state);
    }
}
