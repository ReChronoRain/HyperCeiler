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

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceCategory;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.prefs.PreferenceHeader;
import com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.main.banner.HomePageBannerHelper;
import com.sevtinge.hyperceiler.main.fragment.PagePreferenceFragment;
import com.sevtinge.hyperceiler.main.page.settings.helper.HomepageEntrance;
import com.sevtinge.hyperceiler.model.data.AppInfoCache;
import com.sevtinge.hyperceiler.utils.XmlResourceParserHelper;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import fan.preference.TextButtonPreference;

public class HomeFragment extends PagePreferenceFragment implements HomepageEntrance.EntranceState {

    private static final String TAG = "HomeFragment";
    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private TextButtonPreference mShowAppTips;
    private PreferenceCategory mHeadtipGround;
    private PreferenceCategory mAppsList;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_main;
    }

    @Override
    public void initPrefs() {
        mHeadtipGround = findPreference("prefs_key_headtip_ground");
        mShowAppTips = findPreference("prefs_key_help_cant_see_app");
        mAppsList = findPreference("prefs_key_apps_list");
        HomepageEntrance.setEntranceStateListen(this);
        setPreference();

        Thread thread = getThread();
        thread.start();

        boolean isHideTip = getSharedPreferences().getBoolean("prefs_key_help_cant_see_apps_switch", false);
        if (isHideTip && mShowAppTips != null) {
            mShowAppTips.setVisible(false);
        }
    }

    @NonNull
    private Thread getThread() {
        Thread thread = new Thread(() -> {
            try {
                HomePageBannerHelper.init(requireContext().getApplicationContext(), mHeadtipGround);
            } catch (Exception e) {
                AndroidLogUtils.logE(TAG, "HomePageBannerHelper.init failed on background thread, retrying on UI thread", e);
                requireActivity().runOnUiThread(() -> {
                    try {
                        HomePageBannerHelper.init(requireContext(), mHeadtipGround);
                    } catch (Exception ex) {
                        AndroidLogUtils.logE(TAG, "HomePageBannerHelper.init failed on UI thread", ex);
                    }
                });
            }
        });
        thread.setName("HomePageBannerInit");
        return thread;
    }

    private void setPreference() {
        try {
            final SharedPreferences sp = getSharedPreferences();
            XmlResourceParserHelper.processCachedXmlResource(getResources(), R.xml.prefs_set_homepage_entrance,
                (key, summary) -> processSwitchPreference(key, sp));
            XmlResourceParserHelper.processCachedXmlResource(getResources(), R.xml.prefs_main,
                (key, summary) -> processPreferenceHeader(key, summary, sp));
        } catch (XmlPullParserException | IOException e) {
            AndroidLogUtils.logE(TAG, "An error occurred when reading the XML:", e);
        }
    }

    private void processSwitchPreference(String key, SharedPreferences sp) {
        if (key == null) return;
        String checkKey = key.endsWith("_state") ? key.substring(0, key.length() - "_state".length()) : key;
        if (!sp.getBoolean(key, true)) {
            PreferenceHeader preferenceHeader = findPreference(checkKey);
            if (preferenceHeader != null && preferenceHeader.isVisible()) {
                preferenceHeader.setVisible(false);
            }
        }
    }

    private void processPreferenceHeader(String key, String summary, SharedPreferences sp) {
        if (key == null || summary == null) return;

        PreferenceHeader header = findPreference(key);
        if (header == null) return;

        setIconAndTitle(header, summary);
        String title = header.getTitle() != null ? header.getTitle().toString() : "";
        String summary1 = header.getSummary() != null ? header.getSummary().toString() : "";
        header.setVisible(LSPosedScopeHelper.isInSelectedScope(requireContext(), title, summary1, key, sp));
    }

    private void setIconAndTitle(PreferenceHeader header, String packageName) {
        if (header == null || packageName == null) return;
        PackageManager pm = requireContext().getPackageManager();
        ApplicationInfo appInfo = AppInfoCache.getInstance(getContext()).getAppInfo(packageName);
        if (appInfo == null || pm == null) return;

        mAppsList.setVisible(false);
        Runnable loadAndApply = () -> {
            Drawable icon = appInfo.loadIcon(pm);
            CharSequence name = appInfo.loadLabel(pm);

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                header.setIcon(icon);
                if (!"android".equals(packageName)) {
                    header.setTitle(name);
                }
                mAppsList.setVisible(true);
            });
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            new Thread(loadAndApply, "SetIconAndTitle").start();
        } else {
            loadAndApply.run();
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
