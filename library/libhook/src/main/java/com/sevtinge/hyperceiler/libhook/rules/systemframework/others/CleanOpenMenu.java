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
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Handler;
import android.util.Pair;
import android.view.View;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefType;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsChangeObserver;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class CleanOpenMenu extends BaseHook {

    Class<?> mPackageManagerService;

    @Override
    public void init() {

        mPackageManagerService = findClassIfExists("com.android.server.pm.PackageManagerService");

        findAndHookMethod(mPackageManagerService, "systemReady", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                Handler mHandler = (Handler) getObjectField(param.getThisObject(), "mHandler");
                new PrefsChangeObserver(mContext, mHandler, PrefType.Any, null, null) {
                    @Override
                    public void onChange(PrefType type, Uri uri, String name, Object def) {
                        try {
                            if (!name.contains("pref_key_system_framework_clean_open_apps")) return;

                            switch (type) {
                                case PrefType.StringSet ->
                                    PrefsBridge.put(name, PrefsUtils.getSharedStringSetPrefs(mContext, name));

                                case PrefType.Integer ->
                                    PrefsBridge.put(name, PrefsUtils.getSharedIntPrefs(mContext, name, 0));
                            }
                        } catch (Throwable t) {
                            XposedLog.e(TAG, getPackageName(), t);
                        }
                    }
                };
            }
        });

        IMethodHook hook = new IMethodHook() {
            @Override
            @SuppressWarnings("unchecked")
            public void after(AfterHookParam param) {
                try {
                    if (param.getArgs()[0] == null) return;
                    if (param.getArgs().length < 6) return;
                    Intent origIntent = (Intent) param.getArgs()[0];
                    Intent intent = (Intent) origIntent.clone();
                    String action = intent.getAction();
                    // XposedBridge.log(action + ": " + intent.getType() + " | " + intent.getDataString());
                    if (!Intent.ACTION_VIEW.equals(action)) return;
                    if (intent.hasExtra("HyperCeiler") && intent.getBooleanExtra("HyperCeiler", false))
                        return;
                    String scheme = intent.getScheme();
                    boolean validSchemes = "http".equals(scheme) || "https".equals(scheme) || "vnd.youtube".equals(scheme);
                    if (intent.getType() == null && !validSchemes) return;

                    Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                    String mimeType = getContentType(mContext, intent);
                    // XposedBridge.log("mimeType: " + mimeType);

                    String key = "system_framework_clean_open_apps";
                    Set<String> selectedApps = PrefsBridge.getStringSet(key);
                    List<ResolveInfo> resolved = (List<ResolveInfo>) param.getResult();
                    ResolveInfo resolveInfo;
                    PackageManager pm = mContext.getPackageManager();
                    Iterator<ResolveInfo> itr = resolved.iterator();
                    while (itr.hasNext()) {
                        resolveInfo = itr.next();
                        Pair<Boolean, Boolean> isRemove = isRemoveApp(false, mContext, resolveInfo.activityInfo.packageName, selectedApps, mimeType);
                        boolean hasDual = false;
                        try {
                            hasDual = callMethod(pm, "getPackageInfoAsUser", resolveInfo.activityInfo.packageName, 0, 999) != null;
                        } catch (Throwable ignore) {
                        }
                        if ((isRemove.first && !hasDual) || isRemove.first && hasDual && isRemove.second)
                            itr.remove();
                    }

                    param.setResult(resolved);
                } catch (Throwable t) {
                    if (!(t instanceof BadParcelableException)) XposedLog.e(TAG, getPackageName(), t);
                }
            }
        };

        hookAllMethods("com.android.server.pm.ComputerEngine", "queryIntentActivitiesInternal", hook);

        EzxHelpUtils.hookAllMethods("miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner", null, "run", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Intent mOriginalIntent = (Intent) getObjectField(param.getThisObject(), "mOriginalIntent");
                if (mOriginalIntent == null) return;
                String action = mOriginalIntent.getAction();
                if (!Intent.ACTION_VIEW.equals(action)) return;
                // if (mOriginalIntent.getDataString() != null && mOriginalIntent.getDataString().contains(":")) return;

                Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                String mAimPackageName = (String) getObjectField(param.getThisObject(), "mAimPackageName");
                if (mContext == null || mAimPackageName == null) return;
                Set<String> selectedApps = PrefsUtils.getSharedStringSetPrefs(mContext, "system_framework_clean_open_apps");
                String mimeType = getContentType(mContext, mOriginalIntent);
                Pair<Boolean, Boolean> isRemove = isRemoveApp(true, mContext, mAimPackageName, selectedApps, mimeType);

                View mRootView = (View) getObjectField(param.getThisObject(), "mRootView");
                int appResId1 = mContext.getResources().getIdentifier("app1", "id", "android.miui");
                int appResId2 = mContext.getResources().getIdentifier("app2", "id", "android.miui");
                View originalApp = mRootView.findViewById(appResId1);
                View dualApp = mRootView.findViewById(appResId2);
                if (isRemove.first) dualApp.performClick();
                else if (isRemove.second) originalApp.performClick();
            }
        });
        // if (!findAndHookMethodSilently(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, int.class, int.class, int.class, boolean.class, boolean.class, hook))
        // findAndHookMethod(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, int.class, int.class, hook);//error
    }
    // if (!findAndHookMethodSilently(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, long.class, long.class, int.class, boolean.class, boolean.class, hook))
    // findAndHookMethod(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, long.class, int.class, hook);
    //}


    // 存在问题
    private static Pair<Boolean, Boolean> isRemoveApp(boolean dynamic, Context context, String pkgName, Set<String> selectedApps, String mimeType) {
        String key = "system_framework_clean_open_apps";
        int mimeFlags0;
        int mimeFlags999;
        if (dynamic) {
            mimeFlags0 = PrefsUtils.getSharedIntPrefs(context, "pref_key_" + key + "_" + pkgName + "|0", AppsTool.MimeType.ALL);
            mimeFlags999 = PrefsUtils.getSharedIntPrefs(context, "pref_key_" + key + "_" + pkgName + "|999", AppsTool.MimeType.ALL);
        } else {
            mimeFlags0 = PrefsBridge.getInt(key + "_" + pkgName + "|0", AppsTool.MimeType.ALL);
            mimeFlags999 = PrefsBridge.getInt(key + "_" + pkgName + "|999", AppsTool.MimeType.ALL);
        }
        boolean removeOriginal = (selectedApps.contains(pkgName) || selectedApps.contains(pkgName + "|0")) && hideMimeType(mimeFlags0, mimeType);
        boolean removeDual = selectedApps.contains(pkgName + "|999") && hideMimeType(mimeFlags999, mimeType);
        return new Pair<>(removeOriginal, removeDual);
    }

    private static String getContentType(Context context, Intent intent) {
        String scheme = intent.getScheme();
        boolean linkSchemes = "http".equals(scheme) || "https".equals(scheme) || "vnd.youtube".equals(scheme);
        String mimeType = intent.getType();
        if (mimeType == null && linkSchemes) mimeType = "link/*";
        if (mimeType == null && intent.getData() != null) try {
            mimeType = context.getContentResolver().getType(intent.getData());
        } catch (Throwable ignore) {
        }
        return mimeType;
    }

    private static boolean hideMimeType(int mimeFlags, String mimeType) {
        int dataType = AppsTool.MimeType.OTHERS;
        if (mimeType != null)
            if (mimeType.startsWith("image/")) dataType = AppsTool.MimeType.IMAGE;
            else if (mimeType.startsWith("audio/")) dataType = AppsTool.MimeType.AUDIO;
            else if (mimeType.startsWith("video/")) dataType = AppsTool.MimeType.VIDEO;
            else if (mimeType.startsWith("text/") ||
                    mimeType.startsWith("application/pdf") ||
                    mimeType.startsWith("application/msword") ||
                    mimeType.startsWith("application/vnd.ms-") ||
                    mimeType.startsWith("application/vnd.openxmlformats-"))
                dataType = AppsTool.MimeType.DOCUMENT;
            else if (mimeType.startsWith("application/vnd.android.package-archive") ||
                    mimeType.startsWith("application/zip") ||
                    mimeType.startsWith("application/x-zip") ||
                    mimeType.startsWith("application/octet-stream") ||
                    mimeType.startsWith("application/rar") ||
                    mimeType.startsWith("application/x-rar") ||
                    mimeType.startsWith("application/x-tar") ||
                    mimeType.startsWith("application/x-bzip") ||
                    mimeType.startsWith("application/gzip") ||
                    mimeType.startsWith("application/x-lz") ||
                    mimeType.startsWith("application/x-compress") ||
                    mimeType.startsWith("application/x-7z") ||
                    mimeType.startsWith("application/java-archive"))
                dataType = AppsTool.MimeType.ARCHIVE;
            else if (mimeType.startsWith("link/")) dataType = AppsTool.MimeType.LINK;
        return (mimeFlags & dataType) == dataType;
    }


}
