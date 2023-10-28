package com.sevtinge.hyperceiler.module.hook.systemframework;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.BadParcelableException;
import android.os.Handler;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.Helpers;
import com.sevtinge.hyperceiler.utils.PrefsUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XposedHelpers;

public class CleanShareMenu extends BaseHook {

    Class<?> mPackageManagerService;

    @Override
    public void init() {

        mPackageManagerService = findClassIfExists("com.android.server.pm.PackageManagerService");

        findAndHookMethod(mPackageManagerService, "systemReady", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");

                new PrefsUtils.SharedPrefsObserver(mContext, mHandler, "prefs_key_system_framework_clean_share_apps") {
                    @Override
                    public void onChange(String name) {
                        mPrefsMap.put(name, PrefsUtils.getSharedStringSetPrefs(mContext, name));
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
                    Intent origIntent = (Intent) param.args[0];
                    String action = origIntent.getAction();
                    if (action == null) return;
                    if (!action.equals(Intent.ACTION_SEND) && !action.equals(Intent.ACTION_SENDTO) && !action.equals(Intent.ACTION_SEND_MULTIPLE))
                        return;
                    Intent intent = (Intent) origIntent.clone();
                    if (intent.getDataString() != null && intent.getDataString().contains(":")) return;
                    if (intent.hasExtra("HyperCeiler") && intent.getBooleanExtra("HyperCeiler", false)) return;
                    Set<String> selectedApps = mPrefsMap.getStringSet("system_framework_clean_share_apps");
                    List<ResolveInfo> resolved = (List<ResolveInfo>) param.getResult();
                    ResolveInfo resolveInfo;
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    PackageManager pm = mContext.getPackageManager();
                    Iterator<ResolveInfo> itr = resolved.iterator();
                    while (itr.hasNext()) {
                        resolveInfo = itr.next();
                        boolean removeOriginal = selectedApps.contains(resolveInfo.activityInfo.packageName) || selectedApps.contains(resolveInfo.activityInfo.packageName + "|0");
                        boolean removeDual = selectedApps.contains(resolveInfo.activityInfo.packageName + "|999");
                        boolean hasDual = false;
                        try {
                            hasDual = XposedHelpers.callMethod(pm, "getPackageInfoAsUser", resolveInfo.activityInfo.packageName, 0, 999) != null;
                        } catch (Throwable ignore) {
                        }
                        if ((removeOriginal && !hasDual) || removeOriginal && hasDual && removeDual) itr.remove();
                    }
                    param.setResult(resolved);
                } catch (Throwable t) {
                    if (!(t instanceof BadParcelableException))
                        logE(TAG, CleanShareMenu.this.lpparam.packageName, t);
                }
            }
        };

        String ActQueryService = isMoreAndroidVersion(33) ? "com.android.server.pm.ComputerEngine" : "com.android.server.pm.PackageManagerService$ComputerEngine";
        Helpers.hookAllMethods(ActQueryService, lpparam.classLoader, "queryIntentActivitiesInternal", hook);

        // if (!findAndHookMethodSilently(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, int.class, int.class, int.class, boolean.class, boolean.class, hook))
        // findAndHookMethod(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, int.class, int.class, hook);//error
    }
    // if (!findAndHookMethodSilently(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, long.class, long.class, int.class, boolean.class, boolean.class, hook))
    // findAndHookMethod(mPackageManagerService, "queryIntentActivitiesInternal", Intent.class, String.class, long.class, int.class, hook);
    //}

    public static void initRes() {

        Helpers.hookAllMethods("miui.securityspace.XSpaceResolverActivityHelper.ResolverActivityRunner", null, "run", new Helpers.MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Intent mOriginalIntent = (Intent) XposedHelpers.getObjectField(param.thisObject, "mOriginalIntent");
                if (mOriginalIntent == null) return;
                String action = mOriginalIntent.getAction();
                if (action == null) return;
                if (!action.equals(Intent.ACTION_SEND) && !action.equals(Intent.ACTION_SENDTO) && !action.equals(Intent.ACTION_SEND_MULTIPLE))
                    return;
                if (mOriginalIntent.getDataString() != null && mOriginalIntent.getDataString().contains(":")) return;

                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                String mAimPackageName = (String) XposedHelpers.getObjectField(param.thisObject, "mAimPackageName");
                if (mContext == null || mAimPackageName == null) return;
                Set<String> selectedApps = PrefsUtils.getSharedStringSetPrefs(mContext, "prefs_key_system_framework_clean_share_apps");
                View mRootView = (View) XposedHelpers.getObjectField(param.thisObject, "mRootView");
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
    }

}
