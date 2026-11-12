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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.home.safemode;

import static com.sevtinge.hyperceiler.common.utils.api.ProjectApi.isDebug;
import static com.sevtinge.hyperceiler.libhook.utils.api.PropUtils.getProp;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.home.banner.HomePageBannerManager;
import com.sevtinge.hyperceiler.libhook.safecrash.CrashScope;
import com.sevtinge.hyperceiler.libhook.safecrash.SafeModeHandler;
import com.sevtinge.hyperceiler.utils.AppIconCache;
import com.sevtinge.hyperceiler.utils.DialogHelper;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SafeModeFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    public static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private String mPkgList;
    private SwitchPreference mHome;
    private SwitchPreference mSettings;
    private SwitchPreference mSystemUi;
    private SwitchPreference mSecurityCenter;
    private SwitchPreference mDemo;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_settings_safe_mode;
    }

    @Override
    public void initPrefs() {
        mPkgList = getProp(CrashScope.PROP_SAFE_MODE);
        mHome = findPreference("prefs_key_home_safe_mode_enable");
        mSettings = findPreference("prefs_key_system_settings_safe_mode_enable");
        mSystemUi = findPreference("prefs_key_system_ui_safe_mode_enable");
        mSecurityCenter = findPreference("prefs_key_security_center_safe_mode_enable");
        mDemo = findPreference("prefs_key_demo_safe_mode_enable");
        Objects.requireNonNull(mDemo).setVisible(isDebug());

        setCheckedState();

        mHome.setOnPreferenceChangeListener(this);
        mDemo.setOnPreferenceChangeListener(this);
        mSettings.setOnPreferenceChangeListener(this);
        mSystemUi.setOnPreferenceChangeListener(this);
        mSecurityCenter.setOnPreferenceChangeListener(this);

        setPreference();
    }

    private void setCheckedState() {
        Set<String> pkgSet = new HashSet<>(CrashScope.getCrashingAliases());
        setCheckedSilently(mSystemUi, isEnabled("systemui", "system_ui_safe_mode_enable", pkgSet));
        setCheckedSilently(mSettings, isEnabled("settings", "settings_safe_mode_enable", pkgSet));
        setCheckedSilently(mHome, isEnabled("home", "home_safe_mode_enable", pkgSet));
        setCheckedSilently(mSecurityCenter, isEnabled("center", "security_center_safe_mode_enable", pkgSet));
        setCheckedSilently(mDemo, isEnabled("demo", "demo_safe_mode_enable", pkgSet));
    }

    /**
     * 这里只是回显当前状态，不能把回显结果写回 prefs。
     * 否则只要打开过本页面，{@code prefs_key_*_safe_mode_enable} 就会被置为 true，
     * 而安全模式是“属性或 prefs 任一为 true 即生效”，于是解除属性后仍然一直处于安全模式。
     */
    private void setCheckedSilently(SwitchPreference preference, boolean checked) {
        if (preference == null) {
            return;
        }
        preference.setPersistent(false);
        preference.setChecked(checked);
        preference.setPersistent(true);
    }

    private boolean isEnabled(String alias, String configKey, Set<String> pkgSet) {
        return pkgSet.contains(alias) || PrefsBridge.getBoolean(configKey, false);
    }

    private void setPreference() {
        if (getContext() == null) {
            return;
        }
        int iconSize = getResources().getDimensionPixelSize(R.dimen.header_icon_size);
        Resources resources = getResources();
        try (XmlResourceParser xml = resources.getXml(R.xml.prefs_settings_safe_mode)) {
            int event = xml.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "SwitchPreference".equals(xml.getName())) {
                    String key = xml.getAttributeValue(ANDROID_NS, "key");
                    String summary = xml.getAttributeValue(ANDROID_NS, "summary");
                    if (key != null && summary != null) {
                        String name = getPackageName(summary);
                        SwitchPreference preferenceHeader = findPreference(key);
                        if (preferenceHeader != null) {
                            Drawable cachedIcon = AppIconCache.getCached(requireContext(), summary, iconSize);
                            if (cachedIcon != null) {
                                preferenceHeader.setIcon(cachedIcon);
                            } else {
                                AppIconCache.loadIconAsync(requireContext(), summary, iconSize, icon -> {
                                    if (icon != null && isAdded()) {
                                        preferenceHeader.setIcon(icon);
                                    }
                                });
                            }
                            if (!"android".equals(summary) && name != null) preferenceHeader.setTitle(name);
                        }
                    }
                }
                event = xml.next();
            }
        } catch (XmlPullParserException | IOException e) {
            AndroidLog.e("SafeModeFragment", "An error occurred when reading the XML:", e);
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

    private String getAlias(Preference preference) {
        if (preference == mHome) {
            return "home";
        } else if (preference == mSettings) {
            return "settings";
        } else if (preference == mSystemUi) {
            return "systemui";
        } else if (preference == mSecurityCenter) {
            return "center";
        } else if (preference == mDemo) {
            return "demo";
        }
        return null;
    }

    private String getConfigKey(String alias) {
        String pkgName = CrashScope.getPackageName(alias);
        return pkgName == null ? null : CrashScope.getSafeModeConfigKey(pkgName);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        String alias = getAlias(preference);
        if (alias == null) {
            return true;
        }

        boolean enable = (boolean) o;
        if (enable) {
            SafeModeHandler.updateCrashProp(alias);
        } else {
            SafeModeHandler.removeCrashProp(alias);
        }
        mPkgList = getProp(CrashScope.PROP_SAFE_MODE);

        // 写属性依赖 Root/Su，失败时既不会抛异常也不会有任何提示，
        // 界面却已经把开关拨过去了，看起来就像“安全模式关不掉”。
        // 这里回读属性确认结果，写入失败则保持开关原状并提示。
        boolean applied = CrashScope.getCrashingAliases().contains(alias) == enable;
        if (!applied) {
            DialogHelper.showNoRootPermissionDialog(requireContext());
            return false;
        }

        if (!enable) {
            // 关闭时同时清掉 prefs 里的开关，避免属性已清除但仍被判为安全模式
            String configKey = getConfigKey(alias);
            if (configKey != null) {
                PrefsBridge.removeByApp(configKey);
            }
        }

        if (HomePageBannerManager.updateSafeModeCache(requireContext())) {
            HomePageBannerManager.requestRefresh();
        } else {
            HomePageBannerManager.invalidateCache();
        }
        return true;
    }
}
