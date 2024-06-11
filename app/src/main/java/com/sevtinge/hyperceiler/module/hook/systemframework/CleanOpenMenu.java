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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Handler;
import android.util.Pair;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.prefs.PrefType;
import com.sevtinge.hyperceiler.utils.prefs.PrefsChangeObserver;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class CleanOpenMenu extends BaseHook {

    Class<?> mPackageManagerService;

    @Override
    public void init() {

        mPackageManagerService = findClassIfExists("com.android.server.pm.PackageManagerService");

        findAndHookMethod(mPackageManagerService, "systemReady", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                new PrefsChangeObserver(mContext, mHandler, PrefType.Any, null, null) {
                    @Override
                    public void onChange(PrefType type, Uri uri, String name, Object def) {
                        try {
                            if (!name.contains("pref_key_system_framework_clean_open_apps")) return;

                            switch (type) {
                                case PrefType.StringSet ->
                                        mPrefsMap.put(name, PrefsUtils.getSharedStringSetPrefs(mContext, name));

                                case PrefType.Integer ->
                                        mPrefsMap.put(name, PrefsUtils.getSharedIntPrefs(mContext, name, 0));
                            }
                        } catch (Throwable t) {
                            logE(TAG, CleanOpenMenu.this.lpparam.packageName, t);
                        }
                    }
                };
            }
        });

        MethodHook hook = new MethodHook() {
            @Override
            @SuppressWarnings("unchecked")
            protected void after(MethodHookParam param) {
                try {
                    if (param.args[0] == null) return;
                    if (param.args.length < 6) return;
                    Intent origIntent = (Intent) param.args[0];
                    Intent intent = (Intent) origIntent.clone();
                    String action = intent.getAction();
                    // XposedBridge.log(action + ": " + intent.getType() + " | " + intent.getDataString());
                    if (!Intent.ACTION_VIEW.equals(action)) return;
                    if (intent.hasExtra("HyperCeiler") && intent.getBooleanExtra("HyperCeiler", false))
                        return;
                    String scheme = intent.getScheme();
                    boolean validSchemes = "http".equals(scheme) || "https".equals(scheme) || "vnd.youtube".equals(scheme);
                    if (intent.getType() == null && !validSchemes) return;

                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    String mimeType = getContentType(mContext, intent);
                    // XposedBridge.log("mimeType: " + mimeType);

                    String key = "system_framework_clean_open_apps";
                    Set<String> selectedApps = mPrefsMap.getStringSet(key);
                    List<ResolveInfo> resolved = (List<ResolveInfo>) param.getResult();
                    ResolveInfo resolveInfo;
                    PackageManager pm = mContext.getPackageManager();
                    Iterator<ResolveInfo> itr = resolved.iterator();
                    while (itr.hasNext()) {
                        resolveInfo = itr.next();
                        Pair<Boolean, Boolean> isRemove = isRemoveApp(false, mContext, resolveInfo.activityInfo.packageName, selectedApps, mimeType);
                        boolean hasDual = false;
                        try {
                            hasDual = XposedHelpers.callMethod(pm, "getPackageInfoAsUser", resolveInfo.activityInfo.packageName, 0, 999) != null;
                        } catch (Throwable ignore) {
                        }
                        if ((isRemove.first && !hasDual) || isRemove.first && hasDual && isRemove.second)
                            itr.remove();
                    }

                    param.setResult(resolved);
                } catch (Throwable t) {
                    if (!(t instanceof BadParcelableException)) XposedBridge.log(t);
                }
            }
        };

        String ActQueryService = isMoreAndroidVersion(33) ? "com.android.server.pm.ComputerEngine" : "com.android.server.pm.PackageManagerService$ComputerEngine";
        hookAllMethods(ActQueryService, lpparam.classLoader, "queryIntentActivitiesInternal", hook);

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
            mimeFlags0 = PrefsUtils.getSharedIntPrefs(context, "pref_key_" + key + "_" + pkgName + "|0", Helpers.MimeType.ALL);
            mimeFlags999 = PrefsUtils.getSharedIntPrefs(context, "pref_key_" + key + "_" + pkgName + "|999", Helpers.MimeType.ALL);
        } else {
            mimeFlags0 = mPrefsMap.getInt(key + "_" + pkgName + "|0", Helpers.MimeType.ALL);
            mimeFlags999 = mPrefsMap.getInt(key + "_" + pkgName + "|999", Helpers.MimeType.ALL);
        }
        boolean removeOriginal = (selectedApps.contains(pkgName) || selectedApps.contains(pkgName + "|0")) && hideMimeType(mimeFlags0, mimeType);
        boolean removeDual = selectedApps.contains(pkgName + "|999") && hideMimeType(mimeFlags999, mimeType);
        return new Pair<Boolean, Boolean>(removeOriginal, removeDual);
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
        int dataType = Helpers.MimeType.OTHERS;
        if (mimeType != null)
            if (mimeType.startsWith("image/")) dataType = Helpers.MimeType.IMAGE;
            else if (mimeType.startsWith("audio/")) dataType = Helpers.MimeType.AUDIO;
            else if (mimeType.startsWith("video/")) dataType = Helpers.MimeType.VIDEO;
            else if (mimeType.startsWith("text/") ||
                    mimeType.startsWith("application/pdf") ||
                    mimeType.startsWith("application/msword") ||
                    mimeType.startsWith("application/vnd.ms-") ||
                    mimeType.startsWith("application/vnd.openxmlformats-"))
                dataType = Helpers.MimeType.DOCUMENT;
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
                dataType = Helpers.MimeType.ARCHIVE;
            else if (mimeType.startsWith("link/")) dataType = Helpers.MimeType.LINK;
        return (mimeFlags & dataType) == dataType;
    }

    public static void initRes() {
        hookAllMethods("miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner", null, "run", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Intent mOriginalIntent = (Intent) XposedHelpers.getObjectField(param.thisObject, "mOriginalIntent");
                if (mOriginalIntent == null) return;
                String action = mOriginalIntent.getAction();
                if (!Intent.ACTION_VIEW.equals(action)) return;
                // if (mOriginalIntent.getDataString() != null && mOriginalIntent.getDataString().contains(":")) return;

                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                String mAimPackageName = (String) XposedHelpers.getObjectField(param.thisObject, "mAimPackageName");
                if (mContext == null || mAimPackageName == null) return;
                Set<String> selectedApps = PrefsUtils.getSharedStringSetPrefs(mContext, "system_framework_clean_open_apps");
                String mimeType = getContentType(mContext, mOriginalIntent);
                Pair<Boolean, Boolean> isRemove = isRemoveApp(true, mContext, mAimPackageName, selectedApps, mimeType);

                View mRootView = (View) XposedHelpers.getObjectField(param.thisObject, "mRootView");
                int appResId1 = mContext.getResources().getIdentifier("app1", "id", "android.miui");
                int appResId2 = mContext.getResources().getIdentifier("app2", "id", "android.miui");
                View originalApp = mRootView.findViewById(appResId1);
                View dualApp = mRootView.findViewById(appResId2);
                if (isRemove.first) dualApp.performClick();
                else if (isRemove.second) originalApp.performClick();
            }
        });
    }


}
