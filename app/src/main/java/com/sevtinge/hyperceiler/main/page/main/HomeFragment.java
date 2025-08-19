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
import com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.main.banner.HomePageBannerHelper;
import com.sevtinge.hyperceiler.main.fragment.PagePreferenceFragment;
import com.sevtinge.hyperceiler.main.page.settings.helper.HomepageEntrance;
import com.sevtinge.hyperceiler.model.data.AppInfoCache;
import com.sevtinge.hyperceiler.utils.XmlResourceParserHelper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        mHeadtipGround = findPreference("prefs_key_headtip_ground");
        mShowAppTips = findPreference("prefs_key_help_cant_see_app");
        HomepageEntrance.setEntranceStateListen(this);
        setPreference();
        HomePageBannerHelper.init(requireContext(), mHeadtipGround);

        boolean isHideTip = getSharedPreferences().getBoolean("prefs_key_help_cant_see_apps_switch", false);
        if (isHideTip && mShowAppTips != null) {
            mShowAppTips.setVisible(false);
        }
    }

    private void setPreference() {
        try {
            XmlResourceParserHelper.processCachedXmlResource(getResources(), R.xml.prefs_set_homepage_entrance, (key, summary) -> processSwitchPreference(key));
            XmlResourceParserHelper.processCachedXmlResource(getResources(), R.xml.prefs_main, this::processPreferenceHeader);
        } catch (XmlPullParserException | IOException e) {
            AndroidLogUtils.logE(TAG, "An error occurred when reading the XML:", e);
        }
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
            header.setVisible(LSPosedScopeHelper.isInSelectedScope(
                requireContext(),
                (String) header.getTitle(),
                (String) header.getSummary()))
            ;
        }
    }

    private void setIconAndTitle(PreferenceHeader header, String packageName) {
        if (header == null || packageName == null) return;
        // 根据包名获取
        PackageManager pm = requireContext().getPackageManager();
        ApplicationInfo appInfo = AppInfoCache.getInstance(getContext()).getAppInfo(packageName);
        if (appInfo != null) {
            Drawable icon = appInfo.loadIcon(pm);
            CharSequence name = appInfo.loadLabel(pm);
            header.setIcon(icon);
            if (!"android".equals(packageName)) {
                header.setTitle(name);
            }
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
