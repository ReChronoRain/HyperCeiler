package com.sevtinge.hyperceiler.home.utils;

import android.content.Context;
import android.text.TextUtils;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.home.Header;
import com.sevtinge.hyperceiler.home.HomePageFragment;
import com.sevtinge.hyperceiler.utils.LanguageHelper;
import com.sevtinge.hyperceiler.utils.PackagesUtils;
import com.sevtinge.hyperceiler.utils.ScopeManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HeaderManager {

    public static final String PREF_REMOVE_LIST = "header_remove_list";
    public static final String PREF_REMOVE_LIST_SCOPE_SYNC = "header_remove_list_scope_sync";
    public static final String PREF_SCOPE_SYNC = "prefs_key_settings_scope_sync";
    public static final String PREF_HIDE_CANT_SEE_APPS_GUIDE = "prefs_key_help_cant_see_apps_switch";
    private static final String SYSTEM_SCOPE_PACKAGE = "system";

    public static List<Header> getDisplayHeaders(Context context, List<Header> allHeaders) {
        updateHeaderDisplayStates(context, allHeaders);

        List<Header> filtered = new ArrayList<>();
        if (allHeaders == null) {
            return filtered;
        }

        for (Header header : allHeaders) {
            if (header != null && header.displayStatus) {
                filtered.add(header);
            }
        }
        return filtered;
    }

    public static void updateHeaderDisplayStates(Context context, List<Header> headers) {
        if (headers == null) {
            return;
        }

        boolean scopeSyncEnabled = isScopeSyncEnabled();
        Set<String> removeList = getRemoveList(scopeSyncEnabled);
        Set<String> scopePackages = getCurrentScopePackages();
        boolean useScopeSync = scopeSyncEnabled && scopePackages != null;

        for (Header header : headers) {
            if (header == null) {
                continue;
            }
            header.displayStatus = shouldDisplayHeader(context, header, removeList, scopePackages, useScopeSync);
        }
    }

    public static List<Header> getCustomOrderHeaders(Context context, List<Header> allHeaders) {
        boolean scopeSyncEnabled = isScopeSyncEnabled();
        Set<String> removeList = getRemoveList(scopeSyncEnabled);
        Set<String> scopePackages = getCurrentScopePackages();
        boolean useScopeSync = scopeSyncEnabled && scopePackages != null;
        List<Header> customList = new ArrayList<>();

        if (allHeaders == null) {
            return customList;
        }

        for (Header header : allHeaders) {
            if (!isHeaderAvailable(context, header)) {
                continue;
            }

            Header copy = copyHeader(header);
            if (useScopeSync && isScopeManagedHeader(header)) {
                copy.displayStatus = containsScopePackage(scopePackages, getNormalizedPackageName(header));
            } else {
                copy.displayStatus = !removeList.contains(getPackageName(header));
            }
            customList.add(copy);
        }
        return customList;
    }

    public static void saveHeaderPreferences(List<Header> editedHeaders) {
        Set<String> removeList = new LinkedHashSet<>();
        if (editedHeaders != null) {
            for (Header header : editedHeaders) {
                if (header != null && !header.displayStatus) {
                    removeList.add(getPackageName(header));
                }
            }
        }
        saveRemoveList(isScopeSyncEnabled(), removeList);
    }

    public static void saveHeaderPreferencesFromScopeState(List<Header> editedHeaders, Collection<String> scopePackages) {
        Set<String> normalizedScope = ScopeManager.normalizeScopePackages(scopePackages);
        Set<String> removeList = new LinkedHashSet<>();

        if (editedHeaders != null) {
            for (Header header : editedHeaders) {
                if (header == null) {
                    continue;
                }
                if (isScopeManagedHeader(header)) {
                    if (!containsScopePackage(normalizedScope, getNormalizedPackageName(header))) {
                        removeList.add(getPackageName(header));
                    }
                } else if (!header.displayStatus) {
                    removeList.add(getPackageName(header));
                }
            }
        }

        saveRemoveList(true, removeList);
    }

    public static void applyScopeStateToHeaders(List<Header> headers, Collection<String> scopePackages) {
        if (headers == null) {
            return;
        }

        Set<String> normalizedScope = ScopeManager.normalizeScopePackages(scopePackages);
        for (Header header : headers) {
            if (header != null && isScopeManagedHeader(header)) {
                header.displayStatus = containsScopePackage(normalizedScope, getNormalizedPackageName(header));
            }
        }
    }

    public static Set<String> collectSelectedScopeManagedPackages(List<Header> headers) {
        Set<String> selectedPackages = new LinkedHashSet<>();
        if (headers == null) {
            return selectedPackages;
        }

        for (Header header : headers) {
            if (header != null && header.displayStatus && isScopeManagedHeader(header)) {
                selectedPackages.add(getNormalizedPackageName(header));
            }
        }
        return selectedPackages;
    }

    public static Set<String> getCurrentScopeManagedPackages(List<Header> headers) {
        Set<String> scopePackages = getCurrentScopePackages();
        if (scopePackages == null) {
            return collectSelectedScopeManagedPackages(headers);
        }

        Set<String> homePackages = new LinkedHashSet<>();
        if (headers != null) {
            for (Header header : headers) {
                if (header != null && isScopeManagedHeader(header)) {
                    homePackages.add(getNormalizedPackageName(header));
                }
            }
        }

        Set<String> currentPackages = new LinkedHashSet<>();
        for (String packageName : scopePackages) {
            if (homePackages.contains(packageName)) {
                currentPackages.add(packageName);
            }
        }
        return currentPackages;
    }

    public static Set<String> getCurrentScopeManagedPackages(Context context) {
        return getCurrentScopeManagedPackages(loadHomeHeaders(context));
    }

    public static Set<String> getStoredScopeManagedPackages(Context context) {
        List<Header> headers = loadHomeHeaders(context);
        Set<String> removeList = getRemoveList(true);
        Set<String> storedPackages = new LinkedHashSet<>();

        if (headers == null) {
            return storedPackages;
        }

        for (Header header : headers) {
            if (header == null || !isScopeManagedHeader(header)) {
                continue;
            }
            if (!removeList.contains(getPackageName(header))) {
                storedPackages.add(getNormalizedPackageName(header));
            }
        }
        return storedPackages;
    }

    public static List<String> getHomeManagedPackages(Context context) {
        return getHomeManagedPackages(loadHomeHeaders(context));
    }

    public static List<String> getHomeManagedPackages(List<Header> headers) {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        if (headers == null) {
            return new ArrayList<>();
        }

        for (Header header : headers) {
            if (header != null && isScopeManagedHeader(header)) {
                packages.add(getPackageName(header));
            }
        }
        return new ArrayList<>(packages);
    }

    public static boolean isScopeSyncEnabled() {
        return PrefsBridge.getBoolean(PREF_SCOPE_SYNC, false);
    }

    public static boolean shouldShowCantSeeAppsGuide(Context context) {
        return context != null && !PrefsBridge.getBoolean(PREF_HIDE_CANT_SEE_APPS_GUIDE, false);
    }

    public static HiddenReport buildHiddenReport(Context context) {
        return buildHiddenReport(context, loadHomeHeaders(context));
    }

    public static HiddenReport buildHiddenReport(Context context, List<Header> headers) {
        HiddenReport report = new HiddenReport();
        if (context == null || headers == null) {
            return report;
        }

        boolean scopeSyncEnabled = isScopeSyncEnabled();
        Set<String> removeList = getRemoveList(scopeSyncEnabled);
        Set<String> scopePackages = getCurrentScopePackages();
        boolean useScopeSync = scopeSyncEnabled && scopePackages != null;

        for (Header header : headers) {
            if (header == null) {
                continue;
            }

            String entry = buildEntry(context, header);
            if (TextUtils.isEmpty(entry)) {
                continue;
            }

            if (!isHeaderAvailable(context, header)) {
                report.unavailableApps.add(entry);
                continue;
            }

            boolean isHiddenByUser = removeList.contains(getPackageName(header))
                && (!useScopeSync || !isScopeManagedHeader(header));
            if (isHiddenByUser) {
                report.hiddenApps.add(entry);
                continue;
            }

            if (useScopeSync && isScopeManagedHeader(header)
                && !containsScopePackage(scopePackages, getNormalizedPackageName(header))) {
                report.noScopedApps.add(entry);
            }
        }

        return report;
    }

    public static String buildHiddenSummary(Context context, HiddenReport report) {
        if (context == null) {
            return "";
        }

        if (report == null) {
            report = new HiddenReport();
        }

        String summary = context.getString(
            report.hasEntries()
                ? com.sevtinge.hyperceiler.core.R.string.help_cant_see_apps_desc
                : com.sevtinge.hyperceiler.core.R.string.help_cant_see_apps_desc_no_hidden
        );

        if (!report.hiddenApps.isEmpty()) {
            summary = summary + "\n\n"
                + context.getString(com.sevtinge.hyperceiler.core.R.string.help_cant_see_apps_disable)
                + TextUtils.join("\n", report.hiddenApps);
        }

        if (!report.unavailableApps.isEmpty()) {
            summary = summary + "\n\n"
                + context.getString(com.sevtinge.hyperceiler.core.R.string.help_cant_see_apps_uninstall)
                + TextUtils.join("\n", report.unavailableApps);
        }

        if (!report.noScopedApps.isEmpty()) {
            summary = summary + "\n\n"
                + context.getString(com.sevtinge.hyperceiler.core.R.string.help_cant_see_apps_scope)
                + TextUtils.join("\n", report.noScopedApps);
        }

        return summary;
    }

    public static String computeHomeStateSignature(Context context) {
        List<Header> headers = loadHomeHeaders(context);
        List<Header> displayHeaders = getDisplayHeaders(context, headers);
        StringBuilder signature = new StringBuilder();

        signature.append("scopeSync=").append(isScopeSyncEnabled()).append(';');
        signature.append("showGuide=").append(shouldShowCantSeeAppsGuide(context)).append(';');
        if (context != null) {
            signature.append("locale=").append(LanguageHelper.getCurrentLocale(context).toLanguageTag()).append(';');
        }

        for (Header header : displayHeaders) {
            signature.append(getPackageName(header)).append(';');
        }

        return signature.toString();
    }

    public static void syncHeaderPreferencesToCurrentScope(Context context) {
        Set<String> scopePackages = getCurrentScopePackages();
        if (scopePackages == null) {
            return;
        }
        List<Header> headers = getCustomOrderHeaders(context, loadHomeHeaders(context));
        saveHeaderPreferencesFromScopeState(headers, scopePackages);
    }

    private static List<Header> loadHomeHeaders(Context context) {
        List<Header> headers = new ArrayList<>();
        if (context != null) {
            HeaderUtils.loadHeadersFromResource(context, HomePageFragment.getHomeHeadersResourceId(), headers);
        }
        return headers;
    }

    private static boolean shouldDisplayHeader(
        Context context,
        Header header,
        Set<String> removeList,
        Set<String> scopePackages,
        boolean useScopeSync
    ) {
        if (!isHeaderAvailable(context, header)) {
            return false;
        }

        if (useScopeSync && isScopeManagedHeader(header)) {
            return containsScopePackage(scopePackages, getNormalizedPackageName(header));
        }

        return !removeList.contains(getPackageName(header));
    }

    private static boolean isHeaderAvailable(Context context, Header header) {
        String packageName = getPackageName(header);
        if (TextUtils.isEmpty(packageName)) {
            return true;
        }

        String normalizedPackageName = ScopeManager.normalizeScopePackageName(packageName);
        if (ScopeManager.isSystemScopePackage(normalizedPackageName)) {
            return true;
        }

        if (!packageName.contains(".")) {
            return true;
        }

        return !PackagesUtils.checkAppStatus(context, packageName);
    }

    private static boolean isScopeManagedHeader(Header header) {
        return getNormalizedPackageName(header) != null;
    }

    private static String getNormalizedPackageName(Header header) {
        String packageName = getPackageName(header);
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }

        String normalized = ScopeManager.normalizeScopePackageName(packageName);
        if (normalized == null) {
            return null;
        }

        if (ScopeManager.isSystemScopePackage(normalized)) {
            return normalized;
        }

        return packageName.contains(".") ? normalized : null;
    }

    private static String getPackageName(Header header) {
        return header != null && header.summary != null ? header.summary.toString() : "";
    }

    private static String buildEntry(Context context, Header header) {
        CharSequence title = header.getTitle(context.getResources());
        String packageName = getPackageName(header);
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(packageName)) {
            return "";
        }

        String label = !TextUtils.isEmpty(title) ? title.toString() : packageName;
        return TextUtils.isEmpty(packageName)
            ? " - " + label
            : " - " + label + " (" + packageName + ")";
    }

    private static Set<String> getCurrentScopePackages() {
        return ScopeManager.peekNormalizedScopeSync();
    }

    private static boolean containsScopePackage(Collection<String> scopePackages, String packageName) {
        return ScopeManager.containsScopePackage(scopePackages, packageName);
    }

    private static Set<String> getRemoveList(boolean scopeSyncEnabled) {
        return PrefsBridge.getStringSet(getRemoveListKey(scopeSyncEnabled));
    }

    private static void saveRemoveList(boolean scopeSyncEnabled, Set<String> removeList) {
        PrefsBridge.putByApp(getRemoveListKey(scopeSyncEnabled), removeList);
    }

    private static String getRemoveListKey(boolean scopeSyncEnabled) {
        return scopeSyncEnabled ? PREF_REMOVE_LIST_SCOPE_SYNC : PREF_REMOVE_LIST;
    }

    private static Header copyHeader(Header header) {
        Header copy = new Header();
        copy.id = header.id;
        copy.groupId = header.groupId;
        copy.iconRes = header.iconRes;
        copy.titleRes = header.titleRes;
        copy.title = header.title;
        copy.summaryRes = header.summaryRes;
        copy.summary = header.summary;
        copy.breadCrumbTitleRes = header.breadCrumbTitleRes;
        copy.breadCrumbTitle = header.breadCrumbTitle;
        copy.breadCrumbShortTitleRes = header.breadCrumbShortTitleRes;
        copy.breadCrumbShortTitle = header.breadCrumbShortTitle;
        copy.key = header.key;
        copy.intent = header.intent;
        copy.extras = header.extras;
        copy.fragment = header.fragment;
        copy.fragmentArguments = header.fragmentArguments;
        copy.inflatedXml = header.inflatedXml;
        copy.displayStatus = header.displayStatus;
        return copy;
    }

    public static class HiddenReport {
        public final List<String> unavailableApps = new ArrayList<>();
        public final List<String> hiddenApps = new ArrayList<>();
        public final List<String> noScopedApps = new ArrayList<>();

        public boolean hasEntries() {
            return !unavailableApps.isEmpty()
                || !hiddenApps.isEmpty()
                || !noScopedApps.isEmpty();
        }
    }
}
