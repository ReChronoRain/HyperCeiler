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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.app;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getModuleRes;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.MiuixPreferenceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.github.lingqiqi5211.ezhooktool.core.java.Fields;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class AppDetails extends BaseHook {

    private static final String INFO_FRAGMENT_CLASS =
        "com.miui.appmanager.fragment.AMAppInformationFragment";

    private static final String KEY_ORIGINAL_BASIC_CATEGORY = "category_app_infomation";
    private static final String KEY_VERSION_CODE = "hyperceiler_app_details_version_code";
    private static final String KEY_APP_UID = "hyperceiler_app_details_app_uid";
    private static final String KEY_SDK_CATEGORY = "hyperceiler_app_details_sdk_category";
    private static final String KEY_MIN_SDK = "hyperceiler_app_details_min_sdk";
    private static final String KEY_TARGET_SDK = "hyperceiler_app_details_target_sdk";
    private static final String KEY_COMPILE_SDK = "hyperceiler_app_details_compile_sdk";
    private static final String KEY_FILE_CATEGORY = "hyperceiler_app_details_file_category";
    private static final String KEY_DATA_PATH = "hyperceiler_app_details_data_path";
    private static final String KEY_APK_PATH = "hyperceiler_app_details_apk_path";
    private static final String KEY_SUPPORTED_ABI = "hyperceiler_app_details_supported_abi";
    private static final String KEY_INSTALL_CATEGORY = "hyperceiler_app_details_install_category";
    private static final String KEY_ACTION_CATEGORY = "hyperceiler_app_details_action_category";
    private static final String KEY_OPEN_IN_MARKET = "hyperceiler_app_details_open_in_market";
    private static final String KEY_OPEN_IN_APP = "hyperceiler_app_details_open_in_app";

    private static final List<String> ORIGINAL_INSTALL_KEYS = Arrays.asList(
        "install_source",
        "install_time",
        "update_source",
        "update_time"
    );

    private static final List<String> CUSTOM_CATEGORY_KEYS = Arrays.asList(
        KEY_SDK_CATEGORY,
        KEY_FILE_CATEGORY,
        KEY_INSTALL_CATEGORY,
        KEY_ACTION_CATEGORY
    );

    private static final List<String> CUSTOM_BASIC_KEYS = Arrays.asList(
        KEY_VERSION_CODE,
        KEY_APP_UID
    );

    private static final List<String> ABI_ORDER = Arrays.asList(
        "arm64-v8a",
        "armeabi-v7a",
        "armeabi",
        "x86_64",
        "x86",
        "mips64",
        "mips"
    );

    private Class<?> mInfoFragmentCls;

    @Override
    public void init() {
        mInfoFragmentCls = findClassIfExists(INFO_FRAGMENT_CLASS);
        if (mInfoFragmentCls == null) {
            return;
        }

        hookCreatePreferences();
        hookLoadFinished();
        hookPreferenceClick();
    }

    private void hookCreatePreferences() {
        findAndHookMethod(mInfoFragmentCls, "onCreatePreferences", Bundle.class, String.class, new IMethodHook() {
            @Override
            public void after(@NonNull HookParam param) {
                rebuildPreferenceGroups(param.getThisObject());
            }
        });
    }

    private void hookLoadFinished() {
        hookAllMethods(mInfoFragmentCls, "onLoadFinished", new IMethodHook() {
            @Override
            public void after(@NonNull HookParam param) {
                rebuildPreferenceGroups(param.getThisObject());
            }
        });
    }

    private void hookPreferenceClick() {
        hookAllMethods(mInfoFragmentCls, "onPreferenceTreeClick", new IMethodHook() {
            @Override
            public void before(@NonNull HookParam param) {
                Object preference = param.getArgs()[0];
                String key = (String) callMethod(preference, "getKey");
                if (TextUtils.isEmpty(key) || !handlePreferenceClick(param.getThisObject(), preference, key)) {
                    return;
                }
                param.setResult(true);
            }
        });
    }

    private void rebuildPreferenceGroups(Object fragment) {
        Context context = getContext(fragment);
        Object screen = callMethod(fragment, "getPreferenceScreen");
        PackageInfo packageInfo = getPackageInfo(fragment);
        ApplicationInfo appInfo = getApplicationInfo(packageInfo);
        if (context == null || screen == null || packageInfo == null || appInfo == null) {
            return;
        }

        Resources modRes = getModuleRes(context);
        List<PreferenceItem> installItems = collectInstallItems(fragment);

        removeCustomPreferences(fragment, screen);
        configureBasicCategory(fragment, context, modRes, packageInfo, appInfo);
        addSdkCategory(context, screen, modRes, appInfo);
        addFileCategory(context, screen, modRes, appInfo);
        addInstallCategory(context, screen, modRes, installItems);
        addActionCategory(context, screen, modRes);
    }

    private Context getContext(Object fragment) {
        Object context = callMethod(fragment, "getContext");
        return context instanceof Context ? (Context) context : null;
    }

    private PackageInfo getPackageInfo(Object fragment) {
        try {
            Object value = Fields.find(fragment.getClass()).filterByType(PackageInfo.class).first().get(fragment);
            return value instanceof PackageInfo ? (PackageInfo) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private ApplicationInfo getApplicationInfo(PackageInfo packageInfo) {
        if (packageInfo == null) {
            return null;
        }

        Object value = getObjectFieldOrNull(packageInfo, "applicationInfo");
        return value instanceof ApplicationInfo ? (ApplicationInfo) value : null;
    }

    private void configureBasicCategory(
        Object fragment,
        Context context,
        Resources modRes,
        PackageInfo packageInfo,
        ApplicationInfo appInfo
    ) {
        Object category = MiuixPreferenceUtils.INSTANCE.findPreference(fragment, KEY_ORIGINAL_BASIC_CATEGORY);
        if (category == null) {
            return;
        }

        callMethod(category, "setTitle", modRes.getString(R.string.app_details_category_basic));
        movePackageBeforeVersion(fragment);
        addTextPreference(
            context,
            category,
            KEY_VERSION_CODE,
            modRes.getString(R.string.app_details_apk_version_code),
            String.valueOf(packageInfo.getLongVersionCode()),
            100,
            false
        );
        addTextPreference(
            context,
            category,
            KEY_APP_UID,
            modRes.getString(R.string.app_details_app_uid),
            String.valueOf(getIntFieldOrDefault(appInfo, "uid")),
            101,
            false
        );
    }

    private void addSdkCategory(
        Context context,
        Object screen,
        Resources modRes,
        ApplicationInfo appInfo
    ) {
        Object category = addCategory(
            context,
            screen,
            KEY_SDK_CATEGORY,
            modRes.getString(R.string.app_details_category_sdk),
            10
        );
        addTextPreference(
            context,
            category,
            KEY_MIN_SDK,
            modRes.getString(R.string.app_details_min_sdk),
            String.valueOf(getIntFieldOrDefault(appInfo, "minSdkVersion")),
            0,
            false
        );
        addTextPreference(
            context,
            category,
            KEY_TARGET_SDK,
            modRes.getString(R.string.app_details_sdk),
            String.valueOf(getIntFieldOrDefault(appInfo, "targetSdkVersion")),
            1,
            false
        );
        addTextPreference(
            context,
            category,
            KEY_COMPILE_SDK,
            modRes.getString(R.string.app_details_compile_sdk),
            getCompileSdkVersion(appInfo, modRes),
            2,
            false
        );
    }

    private void addFileCategory(
        Context context,
        Object screen,
        Resources modRes,
        ApplicationInfo appInfo
    ) {
        Object category = addCategory(
            context,
            screen,
            KEY_FILE_CATEGORY,
            modRes.getString(R.string.app_details_category_file),
            20
        );
        addTextPreference(
            context,
            category,
            KEY_SUPPORTED_ABI,
            modRes.getString(R.string.app_details_supported_abi),
            getSupportedAbi(appInfo, modRes),
            0,
            false
        );
        String dataDir = getStringFieldOrNull(appInfo, "dataDir");
        String sourceDir = getStringFieldOrNull(appInfo, "sourceDir");
        addTextPreference(
            context,
            category,
            KEY_DATA_PATH,
            modRes.getString(R.string.app_details_data_path),
            displayText(dataDir, modRes),
            1,
            !TextUtils.isEmpty(dataDir)
        );
        addTextPreference(
            context,
            category,
            KEY_APK_PATH,
            modRes.getString(R.string.app_details_apk_file),
            displayText(sourceDir, modRes),
            2,
            !TextUtils.isEmpty(sourceDir)
        );
    }

    private void addInstallCategory(
        Context context,
        Object screen,
        Resources modRes,
        List<PreferenceItem> installItems
    ) {
        if (installItems.isEmpty()) {
            return;
        }

        Object category = addCategory(
            context,
            screen,
            KEY_INSTALL_CATEGORY,
            modRes.getString(R.string.app_details_category_install),
            30
        );
        int order = 0;
        for (PreferenceItem item : installItems) {
            addTextPreference(context, category, item.key, item.title, item.text, order++, false);
        }
    }

    private void addActionCategory(Context context, Object screen, Resources modRes) {
        Object category = addCategory(
            context,
            screen,
            KEY_ACTION_CATEGORY,
            modRes.getString(R.string.app_details_category_action),
            40
        );
        addTextPreference(
            context,
            category,
            KEY_OPEN_IN_MARKET,
            modRes.getString(R.string.app_details_playstore),
            null,
            0,
            true
        );
        addTextPreference(
            context,
            category,
            KEY_OPEN_IN_APP,
            modRes.getString(R.string.app_details_launch),
            null,
            1,
            true
        );
    }

    private Object addCategory(
        Context context,
        Object screen,
        String key,
        String title,
        int order
    ) {
        Object category = MiuixPreferenceUtils.INSTANCE.createPreferenceCategory(context);
        MiuixPreferenceUtils.INSTANCE.configurePreferenceCategory(category, key, title, true, order);
        MiuixPreferenceUtils.INSTANCE.addPreference(screen, category);
        return category;
    }

    private void addTextPreference(
        Context context,
        Object category,
        String key,
        String title,
        String text,
        int order,
        boolean clickable
    ) {
        Object preference = MiuixPreferenceUtils.INSTANCE.createTextPreference(context);
        MiuixPreferenceUtils.INSTANCE.configureTextPreference(
            preference,
            key,
            title,
            text,
            true,
            order,
            clickable
        );
        MiuixPreferenceUtils.INSTANCE.addPreference(category, preference);
    }

    private List<PreferenceItem> collectInstallItems(Object fragment) {
        List<PreferenceItem> items = new ArrayList<>();
        for (String key : ORIGINAL_INSTALL_KEYS) {
            Object preference = MiuixPreferenceUtils.INSTANCE.findPreference(fragment, key);
            if (preference == null) {
                preference = MiuixPreferenceUtils.INSTANCE.findPreference(fragment, PreferenceItem.toCustomKey(key));
            }
            String title = getPreferenceText(preference, "getTitle");
            String text = getPreferenceText(preference, "getText");
            if (TextUtils.isEmpty(text)) {
                text = getPreferenceText(preference, "getSummary");
            }
            if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(text)) {
                items.add(new PreferenceItem(key, title, text));
            }
        }
        return items;
    }

    private String getPreferenceText(Object preference, String method) {
        if (preference == null) {
            return null;
        }
        Object value = callMethod(preference, method);
        return value instanceof CharSequence ? value.toString() : null;
    }

    private void removeCustomPreferences(Object fragment, Object screen) {
        for (String key : CUSTOM_CATEGORY_KEYS) {
            removePreferenceIfExists(fragment, screen, key);
        }

        for (String key : CUSTOM_BASIC_KEYS) {
            removePreferenceIfExists(fragment, null, key);
        }

        for (String key : ORIGINAL_INSTALL_KEYS) {
            removePreferenceIfExists(fragment, null, key);
        }
    }

    private void removePreferenceIfExists(Object fragment, Object fallbackParent, String key) {
        Object preference = MiuixPreferenceUtils.INSTANCE.findPreference(fragment, key);
        if (preference == null) {
            return;
        }

        Object parent = callMethod(preference, "getParent");
        if (parent == null) {
            parent = fallbackParent;
        }
        if (parent != null) {
            MiuixPreferenceUtils.INSTANCE.removePreference(parent, preference);
        }
    }

    private void movePackageBeforeVersion(Object fragment) {
        Object packagePreference = MiuixPreferenceUtils.INSTANCE.findPreference(fragment, "am_info_pkgname");
        Object versionPreference = MiuixPreferenceUtils.INSTANCE.findPreference(fragment, "am_info_version");
        if (packagePreference == null || versionPreference == null) {
            return;
        }

        Integer packageOrder = getPreferenceOrder(packagePreference);
        Integer versionOrder = getPreferenceOrder(versionPreference);
        if (packageOrder == null || versionOrder == null) {
            return;
        }

        int firstOrder = Math.min(packageOrder, versionOrder);
        int secondOrder = Math.max(packageOrder, versionOrder);
        callMethod(packagePreference, "setOrder", firstOrder);
        callMethod(versionPreference, "setOrder", secondOrder);
    }

    private Integer getPreferenceOrder(Object preference) {
        Object order = callMethod(preference, "getOrder");
        return order instanceof Integer ? (Integer) order : null;
    }

    private String getCompileSdkVersion(ApplicationInfo appInfo, Resources modRes) {
        Object version = getObjectFieldOrNull(appInfo, "compileSdkVersion");
        if (!(version instanceof Integer) || (Integer) version <= 0) {
            return modRes.getString(R.string.app_details_unavailable);
        }

        return String.valueOf(version);
    }

    private String getSupportedAbi(ApplicationInfo appInfo, Resources modRes) {
        Set<String> abis = new LinkedHashSet<>();
        addAbi(abis, getObjectFieldOrNull(appInfo, "primaryCpuAbi"));
        addAbi(abis, getObjectFieldOrNull(appInfo, "secondaryCpuAbi"));
        collectAbiFromApks(appInfo, abis);

        if (abis.isEmpty()) {
            return modRes.getString(R.string.app_details_no_native_libs);
        }

        return TextUtils.join(", ", sortAbis(abis));
    }

    private void addAbi(Set<String> abis, Object abi) {
        if (abi instanceof String && !TextUtils.isEmpty((String) abi)) {
            abis.add((String) abi);
        }
    }

    private void collectAbiFromApks(ApplicationInfo appInfo, Set<String> abis) {
        collectAbiFromApk(getStringFieldOrNull(appInfo, "sourceDir"), abis);
        String[] splitSourceDirs = getStringArrayFieldOrNull(appInfo);
        if (splitSourceDirs == null) {
            return;
        }

        for (String sourceDir : splitSourceDirs) {
            collectAbiFromApk(sourceDir, abis);
        }
    }

    private void collectAbiFromApk(String path, Set<String> abis) {
        if (TextUtils.isEmpty(path)) {
            return;
        }

        try (ZipFile zipFile = new ZipFile(path)) {
            zipFile.stream()
                .map(ZipEntry::getName)
                .filter(name -> name.startsWith("lib/") && name.endsWith(".so"))
                .map(this::getAbiFromLibEntry)
                .filter(abi -> !TextUtils.isEmpty(abi))
                .forEach(abis::add);
        } catch (Throwable ignored) {
        }
    }

    private String getAbiFromLibEntry(String entryName) {
        String[] parts = entryName.split("/");
        return parts.length >= 3 ? parts[1] : null;
    }

    private List<String> sortAbis(Set<String> abis) {
        return abis.stream()
            .sorted(Comparator.comparingInt(this::getAbiOrder))
            .collect(Collectors.toList());
    }

    private int getAbiOrder(String abi) {
        int index = ABI_ORDER.indexOf(abi);
        return index >= 0 ? index : ABI_ORDER.size();
    }

    private Object getObjectFieldOrNull(Object instance, String fieldName) {
        if (instance == null) {
            return null;
        }

        try {
            return Fields.getObjectField(instance, fieldName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String getPackageName(PackageInfo packageInfo) {
        return getStringFieldOrNull(packageInfo, "packageName");
    }

    private String getStringFieldOrNull(Object instance, String fieldName) {
        Object value = getObjectFieldOrNull(instance, fieldName);
        return value instanceof String ? (String) value : null;
    }

    private String[] getStringArrayFieldOrNull(Object instance) {
        Object value = getObjectFieldOrNull(instance, "splitSourceDirs");
        return value instanceof String[] ? (String[]) value : null;
    }

    private int getIntFieldOrDefault(Object instance, String fieldName) {
        Object value = getObjectFieldOrNull(instance, fieldName);
        return value instanceof Integer ? (Integer) value : 0;
    }

    private String displayText(String value, Resources modRes) {
        return TextUtils.isEmpty(value) ? modRes.getString(R.string.app_details_unavailable) : value;
    }

    private boolean handlePreferenceClick(Object fragment, Object preference, String key) {
        Activity activity = (Activity) callMethod(fragment, "getActivity");
        PackageInfo packageInfo = getPackageInfo(fragment);
        String packageName = getPackageName(packageInfo);
        if (activity == null || TextUtils.isEmpty(packageName)) {
            return false;
        }

        return switch (key) {
            case KEY_APK_PATH, KEY_DATA_PATH -> copyPreferenceText(activity, preference);
            case KEY_OPEN_IN_MARKET -> openInMarket(activity, packageName);
            case KEY_OPEN_IN_APP -> openInApp(activity, packageName);
            default -> false;
        };
    }

    private boolean copyPreferenceText(Activity activity, Object preference) {
        String title = getPreferenceText(preference, "getTitle");
        String text = getPreferenceText(preference, "getText");
        if (TextUtils.isEmpty(text)) {
            return false;
        }

        ClipboardManager clipboard =
            (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(title, text));
        Toast.makeText(
            activity,
            activity.getResources().getIdentifier(
                "app_manager_copy_pkg_to_clip",
                "string",
                activity.getPackageName()
            ),
            Toast.LENGTH_SHORT
        ).show();
        return true;
    }

    private boolean openInMarket(Activity activity, String packageName) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            activity.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)
            );
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            activity.startActivity(intent);
        }
        return true;
    }

    private boolean openInApp(Activity activity, String packageName) {
        Resources modRes = getModuleRes(activity);
        Intent launchIntent = activity.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent == null) {
            Toast.makeText(activity, modRes.getString(R.string.app_details_nolaunch), Toast.LENGTH_SHORT).show();
            return false;
        }

        int user = getUserIdFromIntent(activity);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if (user != 0) {
            startActivityAsUser(activity, launchIntent, user);
        } else {
            activity.startActivity(launchIntent);
        }
        return true;
    }

    private int getUserIdFromIntent(Activity activity) {
        try {
            int uid = activity.getIntent().getIntExtra("am_app_uid", -1);
            return (int) callStaticMethod(UserHandle.class, "getUserId", uid);
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "getUserIdFromIntent error", t);
            return 0;
        }
    }

    private void startActivityAsUser(Activity activity, Intent intent, int user) {
        try {
            callMethod(activity, "startActivityAsUser", intent, newInstance(UserHandle.class, user));
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "startActivityAsUser error", t);
        }
    }

    private record PreferenceItem(String key, String title, String text) {
        private PreferenceItem(String key, String title, String text) {
            this.key = toCustomKey(key);
            this.title = title;
            this.text = text;
        }

        static String toCustomKey(String key) {
            return "hyperceiler_app_details_" + key;
        }
    }
}
