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
package com.sevtinge.hyperceiler.hook.module.hook.systemsettings;

import static com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool.FlAG_ONLY_ANDROID;
import static com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool.findContext;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.provider.Settings;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LinkTurbo extends BaseHook {
    List<String> mPackage = new ArrayList<>();

    @Override
    public void init() {
        getPackage(findContext(FlAG_ONLY_ANDROID));
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
                if (Settings.System.getString(Objects.requireNonNull(findContext(FlAG_ONLY_ANDROID)).getContentResolver(), "cloud_sla_whitelist") == null) {
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
                Settings.System.putString(Objects.requireNonNull(findContext(FlAG_ONLY_ANDROID)).getContentResolver(),
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
