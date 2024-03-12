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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcelable;

import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.data.AppData;
import com.sevtinge.hyperceiler.provider.SharedPrefsProvider;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Author by 焕晨HChen
 */
public class PackagesUtils {
    private static final String TAG = ITAG.TAG;

    /**
     * 通过 flag 获取系统内已经安装的软件。
     *
     * @param flag flag
     * @return 获取到的包
     */
    public static List<AppData> getInstalledPackagesByFlag(int flag) {
        return getPackages(flag);
    }

    /**
     * 通过意图的 flag 获取系统内已经安装的软件。
     *
     * @param flag flag
     * @return 获取到的包
     */
    public static List<AppData> getPackageByIntent(String flag) {
        return getPackagesByIntent(flag);
    }

    /**
     * 同下，只不过不需要上下文。
     */
    public static boolean checkAppStatus(String pkg) {
        return checkAppStatus(context(), pkg);
    }

    /**
     * 检查应用是否被卸载，禁用，隐藏。请注意满足其中任意一项就会返回 true。
     *
     * @param context 上下文
     * @param pkg     包名
     * @return 状态
     */
    public static boolean checkAppStatus(Context context, String pkg) {
        return isUninstall(context, pkg) || isDisable(context, pkg) || isHidden(context, pkg);
    }

    /**
     * 同下，只不过不用上下文。
     */
    public static boolean isUninstall(String pkg) {
        return isUninstall(context(), pkg);
    }

    /**
     * 判断目标包名应用是否已经被卸载。
     *
     * @param context 上下文
     * @param pkg     包名
     * @return 状态
     */
    public static boolean isUninstall(Context context, String pkg) {
        if (context == null) return false;
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(pkg, PackageManager.MATCH_ALL);
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            AndroidLogUtils.logW(TAG, "Didn't find this app on the machine, it may have been uninstalled! " + pkg + " E: " + e);
            return true;
        }
    }

    /**
     * 同下，只不过不需要上下文。
     */
    public static boolean isDisable(String pkg) {
        return isDisable(context(), pkg);
    }

    /**
     * 获取包名应用是否被禁用。
     *
     * @param context 上下文
     * @param pkg     包名
     * @return 状态
     */
    public static boolean isDisable(Context context, String pkg) {
        if (context == null) return false;
        PackageManager packageManager = context.getPackageManager();
        try {
            int result = packageManager.getApplicationEnabledSetting(pkg);
            if (result == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return false;
    }

    /**
     * 同下，只不过不需要上下文。
     */
    public static boolean isHidden(String pkg) {
        return isHidden(context(), pkg);
    }

    /**
     * 获取包名应用是否被 Hidden，一般来说被隐藏视为未安装，可以使用 isUninstall() 来判断。
     *
     * @param context 上下文
     * @param pkg     包名
     * @return 状态
     */
    public static boolean isHidden(Context context, String pkg) {
        try {
            if (context == null) return false;
            PackageManager packageManager = context.getPackageManager();
            packageManager.getApplicationInfo(pkg, 0);
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }

    /**
     * 获取系统打开方式
     */
    public static List<AppData> getOpenWithApps() {
        Context context = context();
        List<AppData> appInfoList = new ArrayList<>();
        if (context == null) return appInfoList;
        PackageManager pm = context.getPackageManager();

        Intent mainIntent = new Intent();
        mainIntent.setAction(Intent.ACTION_VIEW);
        mainIntent.setDataAndType(Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/test/5"), "*/*");
        mainIntent.putExtra("HyperCeiler", true);
        List<ResolveInfo> packs = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL | PackageManager.MATCH_DISABLED_COMPONENTS);

        mainIntent = new Intent();
        mainIntent.setAction(Intent.ACTION_VIEW);
        mainIntent.setData(Uri.parse("https://home.miui.com/"));
        mainIntent.putExtra("HyperCeiler", true);
        List<ResolveInfo> packs2 = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL);

        mainIntent = new Intent();
        mainIntent.setAction(Intent.ACTION_VIEW);
        mainIntent.setData(Uri.parse("vnd.youtube:n9AcG0glVu4"));
        mainIntent.putExtra("HyperCeiler", true);
        List<ResolveInfo> packs3 = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL);

        mainIntent = new Intent();
        mainIntent.setAction(Intent.ACTION_SEND);
        mainIntent.putExtra(Intent.EXTRA_TEXT, "HyperCeiler is the best!");
        mainIntent.setType("*/*");
        List<ResolveInfo> packs4 = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL);

        packs.addAll(packs2);
        packs.addAll(packs3);
        packs.addAll(packs4);

        for (ResolveInfo pack : packs) {
            try {
                boolean exists = false;
                for (AppData openWithApp : appInfoList) {
                    if (openWithApp.packageName.equals(pack.activityInfo.applicationInfo.packageName)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    continue;
                }
                appInfoList.add(addAppData(pack, pm));
            } catch (Throwable e) {
                AndroidLogUtils.logE(TAG, "Failed to get open way!", e);
            }
        }
        return appInfoList;
    }

    /**
     * 通过自定义代码获取 Package 信息，
     * 支持 `PackageInfo ResolveInfo
     * PermissionGroupInfo ActivityInfo
     * ApplicationInfo ProviderInfo
     * PermissionInfo` 类型的返回值.
     * 返回使用 return new ArrayList<>(XX); 包裹。
     *
     * @param iPackageCode 需要执行的代码
     * @return ListAppData 包含各种应用详细信息
     * @see #addAppData(Parcelable, PackageManager)
     */
    public static List<AppData> getPackagesByCode(IPackageCode iPackageCode) {
        List<AppData> appDataList = new ArrayList<>();
        Context context = context();
        if (context == null) return appDataList;
        PackageManager packageManager = context.getPackageManager();
        Parcelable parcelable = iPackageCode.getPackageCode(packageManager);
        List<Parcelable> packageCodeList = iPackageCode.getPackageCodeList(packageManager);
        try {
            if (parcelable != null) {
                appDataList.add(addAppData(parcelable, packageManager));
            } else {
                if (packageCodeList != null) {
                    for (Parcelable get : packageCodeList) {
                        appDataList.add(addAppData(get, packageManager));
                    }
                }
            }
        } catch (Throwable e) {
            AndroidLogUtils.logE(TAG, "Failed to get package via code!", e);
        }
        return appDataList;
    }

    private static List<AppData> getPackages(int flag) {
        List<AppData> appDataList = new ArrayList<>();
        Context context = context();
        if (context == null) return appDataList;
        try {
            PackageManager packageManager = context.getPackageManager();
            List<PackageInfo> packageInfos = packageManager.getInstalledPackages(flag);
            for (PackageInfo packageInfo : packageInfos) {
                appDataList.add(addAppData(packageInfo, packageManager));
            }
            return appDataList;
        } catch (Throwable e) {
            AndroidLogUtils.logE(TAG, "Failed to get the list of installed apps via flag!", e);
        }
        return appDataList;
    }

    private static List<AppData> getPackagesByIntent(String flag) {
        Context context = context();
        List<AppData> appDataList = new ArrayList<>();
        if (context == null) return appDataList;
        try {
            PackageManager packageManager = context.getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN);
            mainIntent.addCategory(flag);
            List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(mainIntent, PackageManager.GET_SIGNING_CERTIFICATES);
            for (ResolveInfo resolveInfo : resolveInfos) {
                appDataList.add(addAppData(resolveInfo, packageManager));
            }
            return appDataList;
        } catch (Throwable e) {
            AndroidLogUtils.logE(TAG, "Failed to get app list via intent!", e);
        }
        return appDataList;
    }

    private static AppData addAppData(Parcelable parcelable, PackageManager pm) throws Throwable {
        AppData appData = new AppData();
        try {
            if (parcelable instanceof PackageInfo) {
                appData.icon = BitmapUtils.drawableToBitmap(((PackageInfo) parcelable).applicationInfo.loadIcon(pm));
                appData.label = ((PackageInfo) parcelable).applicationInfo.loadLabel(pm).toString();
                appData.packageName = ((PackageInfo) parcelable).applicationInfo.packageName;
                appData.versionName = ((PackageInfo) parcelable).versionName;
                appData.versionCode = Long.toString(((PackageInfo) parcelable).getLongVersionCode());
                appData.isSystemApp = isSystem(((PackageInfo) parcelable).applicationInfo);
                appData.enabled = ((PackageInfo) parcelable).applicationInfo.enabled;
                // AndroidLogUtils.LogE(TAG, "PackageInfo", null);
            } else if (parcelable instanceof ResolveInfo) {
                appData.icon = BitmapUtils.drawableToBitmap(((ResolveInfo) parcelable).activityInfo.applicationInfo.loadIcon(pm));
                appData.label = ((ResolveInfo) parcelable).activityInfo.applicationInfo.loadLabel(pm).toString();
                appData.packageName = ((ResolveInfo) parcelable).activityInfo.applicationInfo.packageName;
                appData.activityName = ((ResolveInfo) parcelable).activityInfo.name;
                appData.isSystemApp = isSystem(((ResolveInfo) parcelable).activityInfo.applicationInfo);
                appData.enabled = ((ResolveInfo) parcelable).activityInfo.applicationInfo.enabled;
                // AndroidLogUtils.LogE(TAG, "ResolveInfo", null);
            } else if (parcelable instanceof PermissionGroupInfo) {
                appData.icon = BitmapUtils.drawableToBitmap(((PermissionGroupInfo) parcelable).loadIcon(pm));
                appData.label = ((PermissionGroupInfo) parcelable).loadLabel(pm).toString();
                appData.packageName = ((PermissionGroupInfo) parcelable).packageName;
                // AndroidLogUtils.LogE(TAG, "PermissionGroupInfo", null);
            } else if (parcelable instanceof ActivityInfo) {
                appData.icon = BitmapUtils.drawableToBitmap(((ActivityInfo) parcelable).applicationInfo.loadIcon(pm));
                appData.label = ((ActivityInfo) parcelable).applicationInfo.loadLabel(pm).toString();
                appData.packageName = ((ActivityInfo) parcelable).applicationInfo.packageName;
                appData.isSystemApp = isSystem(((ActivityInfo) parcelable).applicationInfo);
                appData.activityName = ((ActivityInfo) parcelable).name;
                appData.enabled = ((ActivityInfo) parcelable).applicationInfo.enabled;
                // AndroidLogUtils.LogE(TAG, "ActivityInfo", null);
            } else if (parcelable instanceof ApplicationInfo) {
                appData.icon = BitmapUtils.drawableToBitmap(((ApplicationInfo) parcelable).loadIcon(pm));
                appData.label = ((ApplicationInfo) parcelable).loadLabel(pm).toString();
                appData.packageName = ((ApplicationInfo) parcelable).packageName;
                appData.isSystemApp = isSystem(((ApplicationInfo) parcelable));
                appData.enabled = ((ApplicationInfo) parcelable).enabled;
                // AndroidLogUtils.LogE(TAG, "ApplicationInfo", null);
            } else if (parcelable instanceof ProviderInfo) {
                appData.icon = BitmapUtils.drawableToBitmap(((ProviderInfo) parcelable).applicationInfo.loadIcon(pm));
                appData.label = ((ProviderInfo) parcelable).applicationInfo.loadLabel(pm).toString();
                appData.packageName = ((ProviderInfo) parcelable).applicationInfo.packageName;
                appData.isSystemApp = isSystem(((ProviderInfo) parcelable).applicationInfo);
                appData.enabled = ((ProviderInfo) parcelable).applicationInfo.enabled;
                // AndroidLogUtils.LogE(TAG, "ProviderInfo", null);
            } else if (parcelable instanceof PermissionInfo) {
                appData.icon = BitmapUtils.drawableToBitmap(((PermissionInfo) parcelable).loadIcon(pm));
                appData.label = ((PermissionInfo) parcelable).loadLabel(pm).toString();
                appData.packageName = ((PermissionInfo) parcelable).packageName;
                // AndroidLogUtils.LogE(TAG, "PermissionInfo", null);
            }
        } catch (Throwable e) {
            throw new Throwable("Error in obtaining application information: " + parcelable, e);
        }
        return appData;
    }

    /**
     * 可用于判断是否是系统应用。
     * 如果 app 为 null 则固定返回 false，请注意检查 app 是否为 null。
     *
     * @return 返回 true 代表是系统应用
     */
    public static boolean isSystem(ApplicationInfo app) {
        if (Objects.isNull(app)) {
            AndroidLogUtils.logE(TAG, "isSystem app is null, will return false");
            return false;
        }
        if (app.uid < 10000) {
            return true;
        }
        return (app.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    private static Context context() {
        try {
            return getContext();
        } catch (Throwable e) {
            AndroidLogUtils.logE(TAG, e);
            return null;
        }
    }

    private static Context getContext() throws Throwable {
        Context context = ContextUtils.getContext(ContextUtils.FLAG_CURRENT_APP);
        if (context == null) {
            context = ContextUtils.getContext(ContextUtils.FlAG_ONLY_ANDROID);
        }
        if (context == null) {
            throw new Throwable("context is null");
        }
        return context;
    }

    public interface IPackageCode {
        default Parcelable getPackageCode(PackageManager pm) {
            return null;
        }

        default List<Parcelable> getPackageCodeList(PackageManager pm) {
            return null;
        }
    }
}
