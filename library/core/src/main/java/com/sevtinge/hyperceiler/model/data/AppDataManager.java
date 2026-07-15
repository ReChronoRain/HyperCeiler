package com.sevtinge.hyperceiler.model.data;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.sub.SubPickerActivity;
import com.sevtinge.hyperceiler.utils.PackagesUtils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

public class AppDataManager {

    private static final String TAG = "AppDataManager";

    public List<AppData> getAppInfo(int modeSelection) {
        try {
            return switch (modeSelection) {
                case SubPickerActivity.LAUNCHER_MODE,
                     SubPickerActivity.LAUNCHER_PICK_MODE,
                     SubPickerActivity.INPUT_MODE -> getLauncherApps();
                case SubPickerActivity.APP_OPEN_MODE -> getOpenWithApps();
                case SubPickerActivity.PROCESS_TEXT_MODE -> getProcessTextApps();
                case SubPickerActivity.IME_MODE -> getInputMethodApps();
                case SubPickerActivity.ALL_APPS_MODE,
                     SubPickerActivity.SCOPE_MODE,
                     SubPickerActivity.CALLBACK_MODE -> getAllApps();
                default -> new ArrayList<>();
            };
        } catch (Exception e) {
            AndroidLog.e(TAG, "getAppInfo failed for mode " + modeSelection, e);
            return new ArrayList<>();
        }
    }

    private List<AppData> getLauncherApps() {
        return PackagesUtils.getPackagesByCode(new PackagesUtils.IPackageCode() {
            @Override
            public List<Parcelable> getPackageCodeList(PackageManager packageManager) {
                Intent intent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(
                    intent,
                    PackageManager.MATCH_ALL
                );
                List<ResolveInfo> apps = distinctByPackage(
                    resolveInfos,
                    AppDataManager::getActivityPackageName
                );
                sortByLabel(apps, resolveInfo -> resolveInfo.loadLabel(packageManager));
                return new ArrayList<>(apps);
            }
        });
    }

    private List<AppData> getOpenWithApps() {
        try {
            return PackagesUtils.getOpenWithApps();
        } catch (Exception e) {
            AndroidLog.e(TAG, "getOpenWithApps failed", e);
            return new ArrayList<>();
        }
    }

    private List<AppData> getProcessTextApps() {
        return PackagesUtils.getPackagesByCode(new PackagesUtils.IPackageCode() {
            @Override
            public List<Parcelable> getPackageCodeList(PackageManager packageManager) {
                Intent intent = new Intent(Intent.ACTION_PROCESS_TEXT)
                    .setType("text/plain")
                    .putExtra("HyperCeiler", true);
                List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(
                    intent,
                    PackageManager.GET_ACTIVITIES | PackageManager.MATCH_DEFAULT_ONLY
                );
                return new ArrayList<>(distinctByPackage(
                    resolveInfos,
                    AppDataManager::getActivityPackageName
                ));
            }
        });
    }

    private List<AppData> getAllApps() {
        return PackagesUtils.getPackagesByCode(new PackagesUtils.IPackageCode() {
            @Override
            public List<Parcelable> getPackageCodeList(PackageManager packageManager) {
                List<ApplicationInfo> apps = new ArrayList<>(packageManager.getInstalledApplications(0));
                sortByLabel(apps, appInfo -> appInfo.loadLabel(packageManager));
                return new ArrayList<>(apps);
            }
        });
    }

    private List<AppData> getInputMethodApps() {
        return PackagesUtils.getPackagesByCode(new PackagesUtils.IPackageCode() {
            @Override
            public List<Parcelable> getPackageCodeList(PackageManager packageManager) {
                Intent intent = new Intent("android.view.InputMethod");
                List<ResolveInfo> resolveInfos = packageManager.queryIntentServices(
                    intent,
                    PackageManager.MATCH_ALL | PackageManager.MATCH_DISABLED_COMPONENTS
                );

                List<ApplicationInfo> applicationInfos = new ArrayList<>();
                for (ResolveInfo resolveInfo : resolveInfos) {
                    if (resolveInfo.serviceInfo != null && resolveInfo.serviceInfo.applicationInfo != null) {
                        applicationInfos.add(resolveInfo.serviceInfo.applicationInfo);
                    }
                }

                List<ApplicationInfo> apps = distinctByPackage(
                    applicationInfos,
                    appInfo -> appInfo.packageName
                );
                sortByLabel(apps, appInfo -> appInfo.loadLabel(packageManager));
                return new ArrayList<>(apps);
            }
        });
    }

    private static String getActivityPackageName(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo == null || resolveInfo.activityInfo.applicationInfo == null) {
            return null;
        }
        return resolveInfo.activityInfo.applicationInfo.packageName;
    }

    private static <T> List<T> distinctByPackage(
        List<T> items,
        Function<T, String> packageNameProvider
    ) {
        Set<String> packageNames = new HashSet<>();
        List<T> distinctItems = new ArrayList<>();
        for (T item : items) {
            String packageName = packageNameProvider.apply(item);
            if (packageName != null && packageNames.add(packageName)) {
                distinctItems.add(item);
            }
        }
        return distinctItems;
    }

    private static <T> void sortByLabel(List<T> items, Function<T, CharSequence> labelProvider) {
        Collator collator = Collator.getInstance(Locale.getDefault());
        items.sort((left, right) -> collator.compare(
            labelProvider.apply(left).toString(),
            labelProvider.apply(right).toString()
        ));
    }
}
