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
package com.sevtinge.hyperceiler.dashboard;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;
import androidx.core.view.MenuProvider;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.dashboard.DashboardFuncHintHelper.FuncHintRule;
import com.sevtinge.hyperceiler.dashboard.DashboardFuncHintHelper.VersionRange;
import com.sevtinge.hyperceiler.libhook.utils.pkg.CheckModifyUtils;
import com.sevtinge.hyperceiler.utils.DialogHelper;
import com.sevtinge.hyperceiler.utils.HotReloadDialogHelper;
import com.sevtinge.hyperceiler.utils.HotReloadManager;
import com.sevtinge.hyperceiler.utils.ThreadUtils;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fan.preference.PreferenceFragment;

public class DashboardFragment extends SettingsPreferenceFragment {

    private static final String TAG = "DashboardFragment";
    private static final String APP_NS = "http://schemas.android.com/apk/res-auto";
    private static final String WARNING_BANNER_KEY = "prefs_key_app_version_warning";
    protected static final int APP_HINT_UNSUPPORTED = 1;
    protected static final int APP_HINT_SUPPORTED = 2;
    protected static final int APP_MATCH_OUT_OF_RANGE = 1;
    protected static final int APP_MATCH_IN_RANGE = 2;

    // 静态缓存，避免每次进入子页面都重新解析 XML
    private static final Map<Integer, String> sQuickRestartCache = new ConcurrentHashMap<>();
    private static final Map<Integer, String> sHotReloadPreferredCache = new ConcurrentHashMap<>();

    private final DashboardPreferencePageLockHelper mPageLockHelper =
        new DashboardPreferencePageLockHelper(this, WARNING_BANNER_KEY);
    private final DashboardFuncHintHelper mFuncHintHelper =
        new DashboardFuncHintHelper(this, mPageLockHelper);
    private String mQuickRestartPackageName;
    private String mHotReloadPreferredPackageName;

    @Override
    public int getPreferenceScreenResId() {
        return mPreferenceResId != 0 ? mPreferenceResId : 0;
    }

    @Override
    public void onCreatePreferencesAfter(Bundle bundle, String s) {
        super.onCreatePreferencesAfter(bundle, s);
        int xmlResId = getPreferenceScreenResId();
        // 先查缓存
        if (sQuickRestartCache.containsKey(xmlResId)) {
            mQuickRestartPackageName = sQuickRestartCache.get(xmlResId);
            String preferred = sHotReloadPreferredCache.get(xmlResId);
            mHotReloadPreferredPackageName = TextUtils.isEmpty(preferred)
                ? mQuickRestartPackageName : preferred;
        } else {
            // 缓存未命中，后台解析，不阻塞主线程
            Context context = getContext();
            if (context == null) return;
            ThreadUtils.postOnBackgroundThread(() -> {
                String pkg = getQuickRestartPackageName(context, xmlResId);
                String hotReloadPreferred = getHotReloadPreferredPackageName(context, xmlResId);
                sQuickRestartCache.put(xmlResId, pkg != null ? pkg : "");
                sHotReloadPreferredCache.put(xmlResId,
                    hotReloadPreferred != null ? hotReloadPreferred : "");
                ThreadUtils.postOnMainThread(() -> {
                    if (!isAdded()) return;
                    mQuickRestartPackageName = pkg;
                    mHotReloadPreferredPackageName = TextUtils.isEmpty(hotReloadPreferred)
                        ? pkg : hotReloadPreferred;
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.invalidateOptionsMenu();
                    }
                });
            });
        }
        checkHighlightedKeyVisibility();
    }

    /**
     * 检查搜索跳转的目标 Preference 是否可见/可用，不可用时提示用户
     */
    private void checkHighlightedKeyVisibility() {
        Bundle args = getArguments();
        if (args == null) return;
        String highlightKey = args.getString(":settings:fragment_args_key");
        if (highlightKey == null) return;
        Preference pref = findPreference(highlightKey);
        if (pref == null) return;

        boolean unavailable = !pref.isVisible() || !pref.isEnabled();
        if (!unavailable && pref.getDependency() != null) {
            Preference dep = findPreference(pref.getDependency());
            if (dep != null) {
                unavailable = dep.shouldDisableDependents();
            }
        }

        Context context = getContext();
        if (context != null) {
            if (unavailable) {
                Toast.makeText(context, getString(R.string.search_result_not_available), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                if (!TextUtils.isEmpty(mQuickRestartPackageName)) {
                    menuInflater.inflate(R.menu.settings_sub_menu, menu);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.quick_restart && !TextUtils.isEmpty(mQuickRestartPackageName)) {
                    Activity activity = getActivity();
                    if (activity == null) return true;
                    if (HotReloadManager.isHotReloadAvailable()) {
                        // API 102：只展示 framework scope 内应用，当前页面对应应用置顶并默认选中。
                        HotReloadDialogHelper.showScopedAppPicker(activity,
                            TextUtils.isEmpty(mHotReloadPreferredPackageName)
                                ? mQuickRestartPackageName : mHotReloadPreferredPackageName);
                    } else if ("system".equals(mQuickRestartPackageName)) {
                        // API 101 / 无 service：保留原本的重启设备降级路径。
                        DialogHelper.showRestartSystemDialog(activity);
                    } else {
                        // API 101 / 无 service：AppsTool 会按包前缀结束该应用的全部进程。
                        DialogHelper.showRestartDialog(activity, mQuickRestartPackageName);
                    }
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());
    }

    private String getQuickRestartPackageName(Context context, @XmlRes int xmlResId) {
        if (xmlResId == 0) return null;
        Resources res = context.getResources();
        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "PreferenceScreen".equals(xml.getName())) {
                    return xml.getAttributeValue(APP_NS, "quick_restart");
                }
                eventType = xml.next();
            }
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to access XML resource!", e);
        }
        return null;
    }

    @Nullable
    private String getHotReloadPreferredPackageName(Context context, @XmlRes int xmlResId) {
        if (xmlResId == 0) return null;
        Resources res = context.getResources();
        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "PreferenceScreen".equals(xml.getName())) {
                    return xml.getAttributeValue(APP_NS, "hot_reload_preferred");
                }
                eventType = xml.next();
            }
        } catch (Exception e) {
            AndroidLog.e(TAG, "Failed to access XML resource for hot reload target!", e);
        }
        return null;
    }

    protected void setOverlayMode() {
        try {
            Field declaredField = PreferenceFragment.class.getDeclaredField("mIsOverlayMode");
            declaredField.setAccessible(true);
            declaredField.set(this, false);
        } catch (Exception e) {
            AndroidLog.e(TAG, "setOverlayMode error", e);
        }
    }

    public void setFuncHint(Preference p, int value) {
        cleanKey(p.getKey());
        p.setEnabled(false);
        switch (value) {
            case 1 -> p.setSummary(R.string.unsupported_system_func);
            case 2 -> p.setSummary(R.string.supported_system_func);
            case 3 -> p.setSummary(R.string.feature_doing_func);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        }
    }

    public boolean setFuncHint(Preference p, int value, String pkgName, long minVersionCode, long maxVersionCode) {
        return mFuncHintHelper.setFuncHint(p, value, pkgName, minVersionCode, maxVersionCode);
    }

    public boolean setFuncHint(Preference p, int value, int matchMode, String pkgName, long minVersionCode, long maxVersionCode) {
        return mFuncHintHelper.setFuncHint(p, value, matchMode, pkgName, minVersionCode, maxVersionCode);
    }

    public boolean setFuncHint(Preference p, int value, String pkgName, long... ranges) {
        return mFuncHintHelper.setFuncHint(p, value, pkgName, ranges);
    }

    public boolean setFuncHint(Preference p, int value, int matchMode, String pkgName, long... ranges) {
        return mFuncHintHelper.setFuncHint(p, value, matchMode, pkgName, ranges);
    }

    public boolean setFuncHint(Preference p, int value, String pkgName, @Nullable VersionRange... ranges) {
        return mFuncHintHelper.setFuncHint(p, value, pkgName, ranges);
    }

    public boolean setFuncHint(Preference p, int value, int matchMode, String pkgName, @Nullable VersionRange... ranges) {
        return mFuncHintHelper.setFuncHint(p, value, matchMode, pkgName, ranges);
    }

    public void setFuncHints(String pkgName, @NonNull FuncHintRule... rules) {
        mFuncHintHelper.setFuncHints(pkgName, rules);
    }

    protected static FuncHintRule rule(@NonNull Preference preference, int value, @Nullable VersionRange... ranges) {
        return DashboardFuncHintHelper.rule(preference, value, ranges);
    }

    protected static FuncHintRule rule(@NonNull Preference preference, int value, int matchMode, @Nullable VersionRange... ranges) {
        return DashboardFuncHintHelper.rule(preference, value, matchMode, ranges);
    }

    protected static VersionRange range(long minVersionCode, long maxVersionCode) {
        return DashboardFuncHintHelper.range(minVersionCode, maxVersionCode);
    }

    protected static VersionRange atLeast(long minVersionCode) {
        return DashboardFuncHintHelper.atLeast(minVersionCode);
    }

    protected static VersionRange atMost(long maxVersionCode) {
        return DashboardFuncHintHelper.atMost(maxVersionCode);
    }

    public void setPreVisible(Preference p, boolean b) {
        if (!b) {
            cleanKey(p.getKey());
            p.setVisible(false);
        }
    }

    public void setAppModWarn(Preference p, String pkgName) {
        boolean check = CheckModifyUtils.INSTANCE.getCheckResult(getContext(), pkgName);
        boolean isDebugMode = getSharedPreferences().getBoolean("prefs_key_development_debug_mode", false);
        int debugVersionCode = getSharedPreferences().getInt("debug_choose_" + pkgName, 0);
        if (debugVersionCode <= 0) {
            debugVersionCode = getSharedPreferences().getInt("prefs_key_debug_choose_" + pkgName, 0);
        }
        boolean isDebugVersion = debugVersionCode == 0;
        boolean showWarning = check && !isDebugMode && isDebugVersion;
        p.setVisible(showWarning);
        if (showWarning) {
            mPageLockHelper.lock(p, null);
        }
    }
}
