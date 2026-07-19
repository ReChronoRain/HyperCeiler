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
package com.sevtinge.hyperceiler.libhook.rules.systemframework.freeform;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import java.util.ArrayList;
import java.util.List;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import miui.app.MiuiFreeFormManager;

public class OpenAppInFreeForm extends BaseHook {

    private static final String HOT_RELOAD_CONTEXT_KEY =
        "OpenAppInFreeForm.systemContext";
    Class<?> mActivityStarter;
    Class<?> mActivityTaskManagerService;
    private volatile boolean mReceiverRegistered;

    @Override
    public void init() {
        if (PrefsBridge.getBoolean("system_framework_freeform_jump")) {
            mActivityStarter = findClassIfExists("com.android.server.wm.ActivityStarter");
            mActivityTaskManagerService = findClassIfExists("com.android.server.wm.ActivityTaskManagerService");
            Context restoredContext = getHotReloadRuntimeState(HOT_RELOAD_CONTEXT_KEY, Context.class);
            if (restoredContext != null) {
                registerFreeFormReceiver(restoredContext);
            }


            findAndHookMethod(mActivityTaskManagerService, "onSystemReady", new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    Context mContext = (Context) com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(param.getThisObject(), "mContext");
                    registerFreeFormReceiver(mContext);
                }
            });

            hookAllMethods(mActivityStarter, "executeRequest", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    Object request = param.getArgs()[0];
                    Intent intent = (Intent) com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(request, "intent");
                    Object safeOptions = com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(request, "activityOptions");
                    if (safeOptions != null) {
                        ActivityOptions ao = (ActivityOptions) com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(safeOptions, "mOriginalOptions");
                        if (ao != null && com.sevtinge.hyperceiler.libhook.base.BaseHook.getIntField(ao, "mLaunchWindowingMode") == 5) {
                            return;
                        }
                    }
                    String callingPackage = (String) com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(request, "callingPackage");
                    boolean openInFw = shouldOpenInFreeForm(intent, callingPackage);

//                Bundle ao = safeOptions != null ? (Bundle) com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(safeOptions, "getActivityOptionsBundle") : null;
//                String reason = (String) com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(request, "reason");
//                Helpers.log("startAct: " + callingPackage
//                    + " reason| " + reason
//                    + " intent| " + intent
//                    + " openInFw| " + openInFw
//                    + " activityOptions| " + Helpers.stringifyBundle(ao)
//                    + " intentExtra| " + Helpers.stringifyBundle(intent.getExtras())
//                );

                    if (openInFw) {
                        Context mContext = (Context) com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(com.sevtinge.hyperceiler.libhook.base.BaseHook.getObjectField(param.getThisObject(), "mService"), "mContext");
                        // ActivityOptions options = MiuiMultiWindowUtils.getActivityOptions(mContext, intent.getComponent().getPackageName(), true, false);
                        ActivityOptions options = (ActivityOptions) com.sevtinge.hyperceiler.libhook.base.BaseHook.callStaticMethod(
                            findClassIfExists("android.util.MiuiMultiWindowUtils"),
                            "getActivityOptions", mContext, intent.getComponent().getPackageName(), true, false);
                        com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(param.getThisObject(), "setActivityOptions", options.toBundle());
                    }
                }
            });
        }
    }

    private void registerFreeFormReceiver(Context context) {
        if (context == null || mReceiverRegistered) {
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PREFIX + "SetFreeFormPackage");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                if (action.equals(ACTION_PREFIX + "SetFreeFormPackage")) {
                    String pkg = intent.getStringExtra("package");
                    com.sevtinge.hyperceiler.libhook.base.BaseHook.setAdditionalInstanceField(
                        MiuiFreeFormManager.class, "nextFreeformPackage", pkg);
                }
            }
        };
        ContextCompat.registerReceiver(context, receiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);
        mReceiverRegistered = true;
        putHotReloadRuntimeState(HOT_RELOAD_CONTEXT_KEY, context);
        registerReceiverHotReloadCleanup(context, receiver);
        registerHotReloadCleanup(() -> mReceiverRegistered = false);
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
        final boolean openFwWhenShare = PrefsBridge.getBoolean("system_framework_freeform_app_share");
        if (openFwWhenShare) {
            if ("com.miui.screenshot".equals(callingPackage)) {
                return false;
            }
            /*if (PrefsBridge.getStringSet("system_fw_forcein_actionsend_apps").contains(pkgName)) return false;*/
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
            Object pkg = com.sevtinge.hyperceiler.libhook.base.BaseHook.getAdditionalInstanceField(MiuiFreeFormManager.class, "nextFreeformPackage");
            openInFw = pkgName.equals(pkg);
            if (openInFw) {
                com.sevtinge.hyperceiler.libhook.base.BaseHook.removeAdditionalInstanceField(MiuiFreeFormManager.class, "nextFreeformPackage");
            }
        }
        return openInFw;
    }
}
