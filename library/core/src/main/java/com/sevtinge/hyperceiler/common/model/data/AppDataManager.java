package com.sevtinge.hyperceiler.common.model.data;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.util.Log;

import com.sevtinge.hyperceiler.common.utils.PackagesUtils;
import com.sevtinge.hyperceiler.sub.SubPickerActivity;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AppDataManager {

    private static final String TAG = "AppDataManager";
    private final HashMap<String, Integer> mPackageMap = new HashMap<>();

    public List<AppData> getAppInfo(int modeSelection) {
        try {
            return switch (modeSelection) {
                case SubPickerActivity.LAUNCHER_MODE, SubPickerActivity.CALLBACK_MODE,
                     SubPickerActivity.INPUT_MODE -> getLauncherApps();
                case SubPickerActivity.APP_OPEN_MODE -> getOpenWithApps();
                case SubPickerActivity.PROCESS_TEXT_MODE -> getProcessTextApps();
                default -> new ArrayList<>();
            };
        } catch (Exception e) {
            Log.e(TAG, "Error getting app info for mode: " + modeSelection, e);
            return new ArrayList<>();
        }
    }

    private List<AppData> getLauncherApps() {
        return PackagesUtils.getPackagesByCode(new PackagesUtils.IPackageCode() {
            @Override
            public List<Parcelable> getPackageCodeList(PackageManager pm) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);

                List<ResolveInfo> resolveInfosHaveNoLauncher =
                    pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN),
                        PackageManager.GET_ACTIVITIES | PackageManager.MATCH_DEFAULT_ONLY);

                mPackageMap.clear();
                List<ResolveInfo> resolveInfoList = new ArrayList<>();

                for (ResolveInfo resolveInfo : resolveInfosHaveNoLauncher) {
                    if (resolveInfo.activityInfo == null) continue;

                    String packageName = resolveInfo.activityInfo.applicationInfo.packageName;
                    if (!mPackageMap.containsKey(packageName)) {
                        mPackageMap.put(packageName, 1);
                        resolveInfoList.add(resolveInfo);
                    }
                }

                Collator collator = Collator.getInstance(Locale.getDefault());
                resolveInfoList.sort((r1, r2) -> {
                    CharSequence label1 = r1.loadLabel(pm);
                    CharSequence label2 = r2.loadLabel(pm);
                    return collator.compare(
                        label1.toString(),
                        label2.toString()
                    );
                });
                return new ArrayList<>(resolveInfoList);
            }
        });
    }

    private List<AppData> getOpenWithApps() {
        try {
            return PackagesUtils.getOpenWithApps();
        } catch (Exception e) {
            Log.e(TAG, "Error getting open with apps", e);
            return new ArrayList<>();
        }
    }

    private List<AppData> getProcessTextApps() {
        return PackagesUtils.getPackagesByCode(new PackagesUtils.IPackageCode() {
            @Override
            public List<Parcelable> getPackageCodeList(PackageManager pm) {
                Intent intent = new Intent()
                    .setAction(Intent.ACTION_PROCESS_TEXT)
                    .setType("text/plain");
                intent.putExtra("HyperCeiler", true);

                List<ResolveInfo> resolveInfos =
                    pm.queryIntentActivities(intent, PackageManager.GET_ACTIVITIES | PackageManager.MATCH_DEFAULT_ONLY);
                List<ResolveInfo> resolveInfoList = new ArrayList<>();

                mPackageMap.clear();
                for (ResolveInfo resolveInfo : resolveInfos) {
                    if (resolveInfo.activityInfo == null) continue;

                    String packageName = resolveInfo.activityInfo.applicationInfo.packageName;
                    if (!mPackageMap.containsKey(packageName)) {
                        mPackageMap.put(packageName, 1);
                        resolveInfoList.add(resolveInfo);
                    }
                }
                return new ArrayList<>(resolveInfoList);
            }
        });
    }
}
