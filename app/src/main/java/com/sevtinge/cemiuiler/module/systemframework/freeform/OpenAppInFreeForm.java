package com.sevtinge.cemiuiler.module.systemframework.freeform;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.MiuiMultiWindowUtils;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import miui.app.MiuiFreeFormManager;

public class OpenAppInFreeForm extends BaseHook {

    Class<?> mActivityStarter;
    Class<?> mActivityTaskManagerService;

    @Override
    public void init() {
        if (mPrefsMap.getBoolean("system_framework_freeform_jump")) {
            mActivityStarter = findClassIfExists("com.android.server.wm.ActivityStarter");
            mActivityTaskManagerService = findClassIfExists("com.android.server.wm.ActivityTaskManagerService");


            findAndHookMethod(mActivityTaskManagerService, "onSystemReady", new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(ACTION_PREFIX + "SetFreeFormPackage");
                    BroadcastReceiver mReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            String action = intent.getAction();
                            if (action == null) return;

                            if (action.equals(ACTION_PREFIX + "SetFreeFormPackage")) {
                                String pkg = intent.getStringExtra("package");
                                XposedHelpers.setAdditionalStaticField(MiuiFreeFormManager.class, "nextFreeformPackage", pkg);
                            }
                        }
                    };
                    mContext.registerReceiver(mReceiver, intentFilter);
                }
            });

            hookAllMethods(mActivityStarter, "executeRequest", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    Object request = param.args[0];
                    Intent intent = (Intent) XposedHelpers.getObjectField(request, "intent");
                    Object safeOptions = XposedHelpers.getObjectField(request, "activityOptions");
                    if (safeOptions != null) {
                        ActivityOptions ao = (ActivityOptions) XposedHelpers.getObjectField(safeOptions, "mOriginalOptions");
                        if (ao != null && XposedHelpers.getIntField(ao, "mLaunchWindowingMode") == 5) {
                            return;
                        }
                    }
                    String callingPackage = (String) XposedHelpers.getObjectField(request, "callingPackage");
                    boolean openInFw = shouldOpenInFreeForm(intent, callingPackage);

//                Bundle ao = safeOptions != null ? (Bundle) XposedHelpers.callMethod(safeOptions, "getActivityOptionsBundle") : null;
//                String reason = (String) XposedHelpers.getObjectField(request, "reason");
//                Helpers.log("startAct: " + callingPackage
//                    + " reason| " + reason
//                    + " intent| " + intent
//                    + " openInFw| " + openInFw
//                    + " activityOptions| " + Helpers.stringifyBundle(ao)
//                    + " intentExtra| " + Helpers.stringifyBundle(intent.getExtras())
//                );

                    if (openInFw) {
                        Context mContext = (Context) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mService"), "mContext");
                        ActivityOptions options = MiuiMultiWindowUtils.getActivityOptions(mContext, intent.getComponent().getPackageName(), true, false);
                        XposedHelpers.callMethod(param.thisObject, "setActivityOptions", options.toBundle());
                    }
                }
            });
        }
    }

    private boolean shouldOpenInFreeForm(Intent intent, String callingPackage) {
        if (intent == null || intent.getComponent() == null) {
            return false;
        }
        final List<String> fwBlackList = new ArrayList<>();
        fwBlackList.add("com.miui.home");
        fwBlackList.add("com.android.camera");
        fwBlackList.add("com.android.systemui");
        String pkgName = intent.getComponent().getPackageName();
        if (fwBlackList.contains(pkgName)) {
            return false;
        }
        boolean openInFw = false;
        final boolean openFwWhenShare = mPrefsMap.getBoolean("system_framework_freeform_app_share");
        if (openFwWhenShare) {
            /*if (mPrefsMap.getStringSet("system_fw_forcein_actionsend_apps").contains(pkgName)) return false;*/
            if ("com.miui.packageinstaller".equals(pkgName) && intent.getComponent().getClassName().contains("com.miui.packageInstaller.NewPackageInstallerActivity")) {
                return true;
            }
            if (Intent.ACTION_SEND.equals(intent.getAction()) && !pkgName.equals(callingPackage)) {
                openInFw = true;
            } else if ("com.tencent.mm".equals(pkgName) && intent.getComponent().getClassName().contains(".plugin.base.stub.WXEntryActivity")) {
                openInFw = true;
            }
        }
        if (!openInFw) {
            Object pkg = XposedHelpers.getAdditionalStaticField(MiuiFreeFormManager.class, "nextFreeformPackage");
            openInFw = pkgName.equals(pkg);
            if (openInFw) {
                XposedHelpers.removeAdditionalStaticField(MiuiFreeFormManager.class, "nextFreeformPackage");
            }
        }
        return openInFw;
    }
}
