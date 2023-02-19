package com.sevtinge.cemiuiler.module.systemframework;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Handler;
import android.util.Pair;
import android.view.View;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.Helpers;
import com.sevtinge.cemiuiler.utils.PrefsUtils;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class CleanOpenMenu extends BaseHook {

    Class<?> mPackageManagerService;

    @Override
    public void init() {

        Helpers.findAndHookMethod("com.android.server.pm.PackageManagerService", lpparam.classLoader, "systemReady", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context mContext = (Context)XposedHelpers.getObjectField(param.thisObject, "mContext");
                Handler mHandler = (Handler)XposedHelpers.getObjectField(param.thisObject, "mHandler");

                new PrefsUtils.SharedPrefsObserver(mContext, mHandler) {
                    @Override
                    public void onChange(Uri uri) {
                        try {
                            String type = uri.getPathSegments().get(1);
                            String key = uri.getPathSegments().get(2);
                            if (!key.contains("pref_key_system_framework_clean_open_apps")) return;

                            switch (type) {
                                case "stringset":
                                    mPrefsMap.put(key, Helpers.getSharedStringSetPref(mContext, key));
                                    break;
                                case "integer":
                                    mPrefsMap.put(key, Helpers.getSharedIntPref(mContext, key, 0));
                                    break;
                            }
                        } catch (Throwable t) {
                            XposedBridge.log(t);
                        }
                    }
                };
            }
        });

        MethodHook hook = new MethodHook() {
            @Override
            @SuppressWarnings("unchecked")
            protected void after(MethodHookParam param) throws Throwable {
                try {
                    if (param.args[0] == null) return;
                    if (param.args.length < 6) return;
                    Intent origIntent = (Intent)param.args[0];
                    Intent intent = (Intent)origIntent.clone();
                    String action = intent.getAction();
                    //XposedBridge.log(action + ": " + intent.getType() + " | " + intent.getDataString());
                    if (!Intent.ACTION_VIEW.equals(action)) return;
                    if (intent.hasExtra("Cemiuiler") && intent.getBooleanExtra("Cemiuiler", false)) return;
                    String scheme = intent.getScheme();
                    boolean validSchemes = "http".equals(scheme) || "https".equals(scheme) || "vnd.youtube".equals(scheme);
                    if (intent.getType() == null && !validSchemes) return;

                    Context mContext = (Context)XposedHelpers.getObjectField(param.thisObject, "mContext");
                    String mimeType = getContentType(mContext, intent);
                    //XposedBridge.log("mimeType: " + mimeType);

                    String key = "system_framework_clean_open_apps";
                    Set<String> selectedApps = mPrefsMap.getStringSet(key);
                    List<ResolveInfo> resolved = (List<ResolveInfo>)param.getResult();
                    ResolveInfo resolveInfo;
                    PackageManager pm = mContext.getPackageManager();
                    Iterator<ResolveInfo> itr = resolved.iterator();
                    while (itr.hasNext()) {
                        resolveInfo = itr.next();
                        Pair<Boolean, Boolean> isRemove = isRemoveApp(false, mContext, resolveInfo.activityInfo.packageName, selectedApps, mimeType);
                        boolean hasDual = false;
                        try {
                            hasDual = XposedHelpers.callMethod(pm, "getPackageInfoAsUser", resolveInfo.activityInfo.packageName, 0, 999) != null;
                        } catch (Throwable ignore) {}
                        if ((isRemove.first && !hasDual) || isRemove.first && hasDual && isRemove.second) itr.remove();
                    }

                    param.setResult(resolved);
                } catch (Throwable t) {
                    if (!(t instanceof BadParcelableException)) XposedBridge.log(t);
                }
            }
        };

        String ActQueryService = Helpers.isAndroidVersionTiramisu() ? "com.android.server.pm.ComputerEngine" : "com.android.server.pm.PackageManagerService$ComputerEngine";
        Helpers.hookAllMethods(ActQueryService, lpparam.classLoader, "queryIntentActivitiesInternal", hook);

        //if (!findAndHookMethodSilently(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, int.class, int.class, int.class, boolean.class, boolean.class, hook))
        // findAndHookMethod(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, int.class, int.class, hook);//error
    }
    //if (!findAndHookMethodSilently(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, long.class, long.class, int.class, boolean.class, boolean.class, hook))
    //findAndHookMethod(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, long.class, int.class, hook);
    //}

    private static Pair<Boolean, Boolean> isRemoveApp(boolean dynamic, Context context, String pkgName, Set<String> selectedApps, String mimeType) {
        String key = "system_framework_clean_open_apps";
        int mimeFlags0;
        int mimeFlags999;
        if (dynamic) {
            mimeFlags0 = Helpers.getSharedIntPref(context, "pref_key_" + key + "_" + pkgName + "|0", Helpers.MimeType.ALL);
            mimeFlags999 = Helpers.getSharedIntPref(context, "pref_key_" + key + "_" + pkgName + "|999", Helpers.MimeType.ALL);
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
        } catch (Throwable ignore) {}
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
                    mimeType.startsWith("application/vnd.openxmlformats-")) dataType = Helpers.MimeType.DOCUMENT;
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
                    mimeType.startsWith("application/java-archive")) dataType = Helpers.MimeType.ARCHIVE;
            else if (mimeType.startsWith("link/")) dataType = Helpers.MimeType.LINK;
        return (mimeFlags & dataType) == dataType;
    }


}


