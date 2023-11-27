package com.sevtinge.hyperceiler.module.hook.systemsettings;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.provider.Settings;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LinkTurbo extends BaseHook {
    List<String> mPackage = new ArrayList<>();

    @Override
    public void init() {
        getPackage(findContext());
        if (mPackage.isEmpty()) {
            logE(TAG, "mPackage is null");
            return;
        }
        // logE(TAG, "im get: " + mPackage);
        // logE(TAG, "settings :");
        // 一层保险
        try {
            findAndHookMethod("com.android.settings.wifi.linkturbo.LinkTurboClient",
                "getLinkTurboDefaultPn",
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        super.before(param);
                        param.setResult(mPackage);
                    }
                }
            );
        } catch (Throwable throwable) {
            try {
                // 二层保险
                StringBuilder packageAll = null;
                if (Settings.System.getString(Objects.requireNonNull(findContext()).getContentResolver(), "cloud_sla_whitelist") == null) {
                    logE(TAG, "cloud_sla_whitelist is null");
                    return;
                }
                for (int i = 0; i < mPackage.size(); i++) {
                    if (i < mPackage.size() - 1) {
                        if (i == 0) packageAll = new StringBuilder(mPackage.get(0) + ",");
                        packageAll.append(mPackage.get(i)).append(",");
                    } else {
                        packageAll = (packageAll == null ? new StringBuilder() : packageAll).append(mPackage.get(i));
                    }
                }
                Settings.System.putString(Objects.requireNonNull(findContext()).getContentResolver(),
                    "cloud_sla_whitelist", packageAll == null ? null : packageAll.toString());
            } catch (Throwable throwable1) {
                logE(TAG, "error: " + throwable1);
            }
        }
    }

    public void getPackage(Context mContext) {
        List<String> getInfo = new ArrayList<>();
        if (mContext == null) return;
        List<PackageInfo> packageInfos = mContext.getPackageManager().getInstalledPackages(0);
        for (PackageInfo packageInfo : packageInfos) {
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                getInfo.add(packageInfo.packageName);
            }
        }
        if (getInfo.isEmpty()) return;
        mPackage = getInfo;
    }
}
