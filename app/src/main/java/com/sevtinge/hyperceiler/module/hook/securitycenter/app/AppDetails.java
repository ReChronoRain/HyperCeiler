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
package com.sevtinge.hyperceiler.module.hook.securitycenter.app;

import static com.sevtinge.hyperceiler.module.base.tool.OtherTool.getModuleRes;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.widget.Toast;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

public class AppDetails extends BaseHook {

    Class<?> mAmAppInfoCls;
    Class<?> mFragmentCls;

    private Object mSupportFragment = null;
    private PackageInfo mLastPackageInfo;

    @Override
    public void init() {

        mAmAppInfoCls = findClassIfExists("com.miui.appmanager.AMAppInfomationActivity");
        mFragmentCls = findClassIfExists("androidx.fragment.app.Fragment");

        if (mAmAppInfoCls != null) {
            boolean oldMethodFound = false;

            for (Member method : mAmAppInfoCls.getDeclaredMethods()) {
                if (method.getName().equals("onLoadFinished")) {
                    oldMethodFound = true;
                }
            }

            if (mFragmentCls != null) {
                findAndHookConstructor(mFragmentCls, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        Field piField = XposedHelpers.findFirstFieldByExactType(param.thisObject.getClass(), PackageInfo.class);
                        if (piField != null) {
                            mSupportFragment = param.thisObject;
                        }
                    }
                });
            }

            if (!oldMethodFound) {
                findAndHookMethod(mAmAppInfoCls, "onCreate", Bundle.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(() -> {
                            final Activity act = (Activity) param.thisObject;
                            Object contentFrag = act.getFragmentManager().findFragmentById(android.R.id.content);
                            Object frag = contentFrag != null ? contentFrag : mSupportFragment;
                            if (frag == null) {
                                logI(TAG, AppDetails.this.lpparam.packageName, "Unable to find fragment");
                                return;
                            }

                            final Resources modRes;
                            try {
                                modRes = getModuleRes(act);
                                Field piField = XposedHelpers.findFirstFieldByExactType(frag.getClass(), PackageInfo.class);
                                mLastPackageInfo = (PackageInfo) piField.get(frag);
                                Method[] addPref = XposedHelpers.findMethodsByExactParameters(frag.getClass(), void.class, String.class, String.class, String.class);
                                if (mLastPackageInfo == null || addPref.length == 0) {
                                    logI(TAG, AppDetails.this.lpparam.packageName, "Unable to find field/class/method in SecurityCenter to hook");
                                    return;
                                } else {
                                    addPref[0].setAccessible(true);
                                }
                                addPref[0].invoke(frag, "apk_versioncode", modRes.getString(R.string.app_details_apk_version_code), String.valueOf(mLastPackageInfo.getLongVersionCode()));
                                addPref[0].invoke(frag, "app_uid", modRes.getString(R.string.app_details_app_uid), String.valueOf(mLastPackageInfo.applicationInfo.uid));
                                addPref[0].invoke(frag, "data_path", modRes.getString(R.string.app_details_data_path), mLastPackageInfo.applicationInfo.dataDir);
                                addPref[0].invoke(frag, "apk_filename", modRes.getString(R.string.app_details_apk_file), mLastPackageInfo.applicationInfo.sourceDir);
                                addPref[0].invoke(frag, "min_sdk", modRes.getString(R.string.app_details_min_sdk), String.valueOf(mLastPackageInfo.applicationInfo.minSdkVersion));
                                addPref[0].invoke(frag, "target_sdk", modRes.getString(R.string.app_details_sdk), String.valueOf(mLastPackageInfo.applicationInfo.targetSdkVersion));
                                handler.post(() -> {
                                    try {
                                        addPref[0].invoke(frag, "open_in_market", modRes.getString(R.string.app_details_playstore), "");
                                        addPref[0].invoke(frag, "open_in_app", modRes.getString(R.string.app_details_launch), "");
                                    } catch (Throwable t) {
                                        logW(TAG, "1", t);
                                    }
                                });
                            } catch (Throwable t) {
                                logW(TAG, "2", t);
                                return;
                            }

                            hookAllMethods(frag.getClass(), "onPreferenceTreeClick", new MethodHook() {
                                @SuppressLint("DiscouragedApi")
                                @Override
                                protected void before(MethodHookParam param1) throws Throwable {
                                    String key = (String) XposedHelpers.callMethod(param1.args[0], "getKey");
                                    String title = (String) XposedHelpers.callMethod(param1.args[0], "getTitle");
                                    switch (key) {
                                        case "apk_filename" -> {
                                            ((ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(title, mLastPackageInfo.applicationInfo.sourceDir));
                                            Toast.makeText(act, act.getResources().getIdentifier("app_manager_copy_pkg_to_clip", "string", act.getPackageName()), Toast.LENGTH_SHORT).show();
                                            param1.setResult(true);
                                        }
                                        case "data_path" -> {
                                            ((ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText(title, mLastPackageInfo.applicationInfo.dataDir));
                                            Toast.makeText(act, act.getResources().getIdentifier("app_manager_copy_pkg_to_clip", "string", act.getPackageName()), Toast.LENGTH_SHORT).show();
                                            param1.setResult(true);
                                        }
                                        case "open_in_market" -> {
                                            try {
                                                Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + mLastPackageInfo.packageName));
                                                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                                act.startActivity(launchIntent);
                                            } catch (
                                                android.content.ActivityNotFoundException anfe) {
                                                Intent launchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + mLastPackageInfo.packageName));
                                                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                                act.startActivity(launchIntent);
                                            }
                                            param1.setResult(true);
                                        }
                                        case "open_in_app" -> {
                                            Intent launchIntent = act.getPackageManager().getLaunchIntentForPackage(mLastPackageInfo.packageName);
                                            if (launchIntent == null) {
                                                Toast.makeText(act, modRes.getString(R.string.app_details_nolaunch), Toast.LENGTH_SHORT).show();
                                            } else {
                                                int user = 0;
                                                try {
                                                    int uid = act.getIntent().getIntExtra("am_app_uid", -1);
                                                    user = (int) XposedHelpers.callStaticMethod(UserHandle.class, "getUserId", uid);
                                                } catch (Throwable t) {
                                                    logW(TAG, "3", t);
                                                }

                                                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                                if (user != 0) {
                                                    try {
                                                        XposedHelpers.callMethod(act, "startActivityAsUser", launchIntent, XposedHelpers.newInstance(UserHandle.class, user));
                                                    } catch (Throwable t) {
                                                        logW(TAG, "4", t);
                                                    }
                                                } else {
                                                    act.startActivity(launchIntent);
                                                }
                                            }
                                            param.setResult(true);
                                        }
                                    }
                                }
                            });
                        });
                    }
                });
            }
        } else {
            logE(TAG, this.lpparam.packageName, "Cannot find activity class!");
        }
    }
}
