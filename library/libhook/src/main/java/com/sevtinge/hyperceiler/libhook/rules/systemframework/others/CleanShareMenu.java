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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.others;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.BadParcelableException;
import android.os.Handler;
import android.view.View;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsChangeObserver;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class CleanShareMenu extends BaseHook {

    Class<?> mPackageManagerService;

    @Override
    public void init() {

        mPackageManagerService = findClassIfExists("com.android.server.pm.PackageManagerService");

        findAndHookMethod(mPackageManagerService, "systemReady", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                Handler mHandler = (Handler) getObjectField(param.getThisObject(), "mHandler");

                new PrefsChangeObserver(mContext, mHandler, true, "prefs_key_system_framework_clean_share_apps");
            }
        });

        IMethodHook hook = new IMethodHook() {
            @Override
            @SuppressWarnings("unchecked")
            public void after(AfterHookParam param) {
                try {
                    if (param.getArgs()[0] == null) return;
                    Intent origIntent = (Intent) param.getArgs()[0];
                    String action = origIntent.getAction();
                    if (action == null) return;
                    if (!action.equals(Intent.ACTION_SEND) && !action.equals(Intent.ACTION_SENDTO) && !action.equals(Intent.ACTION_SEND_MULTIPLE))
                        return;
                    Intent intent = (Intent) origIntent.clone();
                    if (intent.getDataString() != null && intent.getDataString().contains(":"))
                        return;
                    if (intent.hasExtra("HyperCeiler") && intent.getBooleanExtra("HyperCeiler", false))
                        return;
                    Set<String> selectedApps = PrefsBridge.getStringSet("system_framework_clean_share_apps");
                    List<ResolveInfo> resolved = (List<ResolveInfo>) param.getResult();
                    ResolveInfo resolveInfo;
                    Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                    PackageManager pm = mContext.getPackageManager();
                    Iterator<ResolveInfo> itr = resolved.iterator();
                    while (itr.hasNext()) {
                        resolveInfo = itr.next();
                        boolean removeOriginal = selectedApps.contains(resolveInfo.activityInfo.packageName) || selectedApps.contains(resolveInfo.activityInfo.packageName + "|0");
                        boolean removeDual = selectedApps.contains(resolveInfo.activityInfo.packageName + "|999");
                        boolean hasDual = false;
                        try {
                            hasDual = callMethod(pm, "getPackageInfoAsUser", resolveInfo.activityInfo.packageName, 0, 999) != null;
                        } catch (Throwable ignore) {
                        }
                        if ((removeOriginal && !hasDual) || removeOriginal && hasDual && removeDual)
                            itr.remove();
                    }
                    param.setResult(resolved);
                } catch (Throwable t) {
                    if (!(t instanceof BadParcelableException))
                        XposedLog.e(TAG, getPackageName(), t);
                }
            }
        };

        hookAllMethods("com.android.server.pm.ComputerEngine", "queryIntentActivitiesInternal", hook);


        hookAllMethods("miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner", "run", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Intent mOriginalIntent = (Intent) getObjectField(param.getThisObject(), "mOriginalIntent");
                if (mOriginalIntent == null) return;
                String action = mOriginalIntent.getAction();
                if (action == null) return;
                if (!action.equals(Intent.ACTION_SEND) && !action.equals(Intent.ACTION_SENDTO) && !action.equals(Intent.ACTION_SEND_MULTIPLE))
                    return;
                if (mOriginalIntent.getDataString() != null && mOriginalIntent.getDataString().contains(":"))
                    return;

                Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                String mAimPackageName = (String) getObjectField(param.getThisObject(), "mAimPackageName");
                if (mContext == null || mAimPackageName == null) return;
                Set<String> selectedApps = PrefsBridge.getStringSet("prefs_key_system_framework_clean_share_apps");
                View mRootView = (View) getObjectField(param.getThisObject(), "mRootView");
                int appResId1 = mContext.getResources().getIdentifier("app1", "id", "android.miui");
                int appResId2 = mContext.getResources().getIdentifier("app2", "id", "android.miui");
                boolean removeOriginal = selectedApps.contains(mAimPackageName) || selectedApps.contains(mAimPackageName + "|0");
                boolean removeDual = selectedApps.contains(mAimPackageName + "|999");
                View originalApp = mRootView.findViewById(appResId1);
                View dualApp = mRootView.findViewById(appResId2);
                if (removeOriginal) dualApp.performClick();
                else if (removeDual) originalApp.performClick();
            }
        });
        // if (!findAndHookMethodSilently(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, int.class, int.class, int.class, boolean.class, boolean.class, hook))
        // findAndHookMethod(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, int.class, int.class, hook);//error
    }
    // if (!findAndHookMethodSilently(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, long.class, long.class, int.class, boolean.class, boolean.class, hook))
    // findAndHookMethod(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, long.class, int.class, hook);
    //}

}
