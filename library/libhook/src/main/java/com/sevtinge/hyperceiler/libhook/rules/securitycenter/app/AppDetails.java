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
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.app;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getModuleRes;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookConstructor;

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

import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class AppDetails extends BaseHook {

    private Class<?> mAmAppInfoCls;
    private Class<?> mFragmentCls;
    private Object mSupportFragment = null;
    private PackageInfo mLastPackageInfo;

    @Override
    public void init() {
        initClasses();
        if (mAmAppInfoCls == null) {
            XposedLog.e(TAG, getPackageName(), "Cannot find activity class!");
            return;
        }

        if (!isOldMethodFound()) {
            hookAppDetailsActivity();
        }
    }

    /**
     * 初始化类引用
     */
    private void initClasses() {
        mAmAppInfoCls = findClassIfExists("com.miui.appmanager.AMAppInfomationActivity");
        mFragmentCls = findClassIfExists("androidx.fragment.app.Fragment");

        if (mFragmentCls != null) {
            hookFragmentConstructor();
        }
    }

    /**
     * 检查是否存在旧方法
     */
    private boolean isOldMethodFound() {
        for (Member method : mAmAppInfoCls.getDeclaredMethods()) {
            if (method.getName().equals("onLoadFinished")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hook Fragment 构造函数
     */
    private void hookFragmentConstructor() {
        findAndHookConstructor(mFragmentCls, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                Field piField = EzxHelpUtils.findFirstFieldByExactType(
                    param.getThisObject().getClass(), PackageInfo.class);
                if (piField != null) {
                    mSupportFragment = param.getThisObject();
                }
            }
        });
    }

    /**
     * Hook 应用详情 Activity 的 onCreate
     */
    private void hookAppDetailsActivity() {
        findAndHookMethod(mAmAppInfoCls, "onCreate", Bundle.class, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> handleActivityCreated((Activity) param.getThisObject()));
            }
        });
    }

    /**
     * 处理 Activity 创建后的逻辑
     */
    private void handleActivityCreated(Activity act) {
        try {
            Object fragment = findFragment(act);
            if (fragment == null) {
                XposedLog.i(TAG, getPackageName(), "Unable to find fragment");
                return;
            }

            Resources modRes = getModuleRes(act);
            if (!initPackageInfo(fragment)) {
                XposedLog.i(TAG, getPackageName(),
                    "Unable to find field/class/method in SecurityCenter to hook");
                return;
            }

            Method addPrefMethod = getAddPreferenceMethod(fragment);
            if (addPrefMethod == null) {
                return;
            }

            hookAddPreferenceMethod(addPrefMethod);
            addCustomPreferences(addPrefMethod, fragment, modRes);
            hookPreferenceTreeClick(fragment, act, modRes);} catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "handleActivityCreated error", t);
        }
    }

    /**
     * 查找 Fragment
     */
    private Object findFragment(Activity act) {
        Object contentFrag = act.getFragmentManager().findFragmentById(android.R.id.content);
        return contentFrag != null ? contentFrag : mSupportFragment;
    }

    /**
     * 初始化 PackageInfo
     */
    private boolean initPackageInfo(Object fragment) throws IllegalAccessException {
        Field piField = EzxHelpUtils.findFirstFieldByExactType(
            fragment.getClass(), PackageInfo.class);
        if (piField == null) {
            return false;
        }
        mLastPackageInfo = (PackageInfo) piField.get(fragment);
        return mLastPackageInfo != null;
    }

    /**
     * 获取添加偏好设置的方法
     */
    private Method getAddPreferenceMethod(Object fragment) {
        Method[] addPrefMethods = EzxHelpUtils.findMethodsByExactParameters(
            fragment.getClass(), void.class, String.class, String.class, String.class);

        if (addPrefMethods.length == 0) {
            return null;
        }

        addPrefMethods[0].setAccessible(true);
        return addPrefMethods[0];
    }

    /**
     * Hook 添加偏好设置方法 - 替换 noClickTextPreference 为 TextPreference
     */
    private void hookAddPreferenceMethod(Method addPrefMethod) {
        EzxHelpUtils.hookMethod(addPrefMethod, new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(null);

                Object thiz = param.getThisObject();
                ClassLoader cl = thiz.getClass().getClassLoader();

                try {
                    // 获取 Context
                    Context ctx = getContextFromPreferenceManager(thiz);
                    if (ctx == null) {
                        XposedLog.e(TAG, "Cannot find Context from PreferenceManager!");
                        return;
                    }

                    // 创建 TextPreference
                    Object textPref = createTextPreference(ctx, cl, param.getArgs());

                    // 添加到 PreferenceCategory
                    addPreferenceToCategory(thiz, cl, textPref);

                } catch (Throwable t) {
                    XposedLog.e(TAG, "Error in hookAddPreferenceMethod", t);
                }
            }
        });
    }

    /**
     * 从 PreferenceManager 获取 Context
     */
    private Context getContextFromPreferenceManager(Object thiz) {
        Object prefManager = EzxHelpUtils.callMethod(thiz, "getPreferenceManager");
        Method[] ctxMethods = EzxHelpUtils.findMethodsByExactParameters(
            prefManager.getClass(), Context.class);

        if (ctxMethods.length == 0) {
            return null;
        }

        return (Context) EzxHelpUtils.callMethod(prefManager, ctxMethods[0].getName());
    }

    /**
     * 创建 TextPreference
     */
    private Object createTextPreference(Context ctx, ClassLoader cl, Object[] args) {
        Class<?> textPrefCls = EzxHelpUtils.findClass("miuix.preference.TextPreference", cl);
        Object textPref = EzxHelpUtils.newInstance(textPrefCls, ctx);

        EzxHelpUtils.callMethod(textPref, "setKey", args[0]);
        EzxHelpUtils.callMethod(textPref, "setTitle", args[1]);
        EzxHelpUtils.callMethod(textPref, "setText", args[2]);

        return textPref;
    }

    /**
     * 添加 Preference 到 Category
     */
    private void addPreferenceToCategory(Object thiz, ClassLoader cl, Object textPref) throws Exception {
        Field prefField = EzxHelpUtils.findFirstFieldByExactType(
            thiz.getClass(),
            EzxHelpUtils.findClass("androidx.preference.PreferenceCategory", cl)
        );

        if (prefField == null) {
            XposedLog.e(TAG, "Cannot find PreferenceCategory field!");
            return;
        }

        prefField.setAccessible(true);
        Object prefGroup = prefField.get(thiz);
        EzxHelpUtils.callMethod(prefGroup, "addPreference", textPref);
    }

    /**
     * 添加自定义偏好设置项
     */
    private void addCustomPreferences(Method addPrefMethod, Object fragment, Resources modRes) throws Exception {
        Handler handler = new Handler(Looper.getMainLooper());

        // 立即添加的项
        addPrefMethod.invoke(fragment, "apk_versioncode",
            modRes.getString(R.string.app_details_apk_version_code),
            String.valueOf(mLastPackageInfo.getLongVersionCode()));

        addPrefMethod.invoke(fragment, "app_uid",
            modRes.getString(R.string.app_details_app_uid),
            String.valueOf(mLastPackageInfo.applicationInfo.uid));

        addPrefMethod.invoke(fragment, "data_path",
            modRes.getString(R.string.app_details_data_path),
            mLastPackageInfo.applicationInfo.dataDir);

        addPrefMethod.invoke(fragment, "apk_filename",
            modRes.getString(R.string.app_details_apk_file),
            mLastPackageInfo.applicationInfo.sourceDir);

        addPrefMethod.invoke(fragment, "min_sdk",
            modRes.getString(R.string.app_details_min_sdk),
            String.valueOf(mLastPackageInfo.applicationInfo.minSdkVersion));

        addPrefMethod.invoke(fragment, "target_sdk",
            modRes.getString(R.string.app_details_sdk),
            String.valueOf(mLastPackageInfo.applicationInfo.targetSdkVersion));

        // 延迟添加的项
        handler.post(() -> {
            try {
                addPrefMethod.invoke(fragment, "open_in_market",
                    modRes.getString(R.string.app_details_playstore), "");
                addPrefMethod.invoke(fragment, "open_in_app",
                    modRes.getString(R.string.app_details_launch), "");
            } catch (Throwable t) {
                XposedLog.w(TAG, getPackageName(), "addCustomPreferences error", t);
            }
        });
    }

    /**
     * Hook 偏好设置树点击事件
     */
    private void hookPreferenceTreeClick(Object fragment, Activity act, Resources modRes) {
        hookAllMethods(fragment.getClass(), "onPreferenceTreeClick", new IMethodHook() {
            @SuppressLint("DiscouragedApi")
            @Override
            public void before(BeforeHookParam param) {
                String key = (String) EzxHelpUtils.callMethod(param.getArgs()[0], "getKey");
                String title = (String) EzxHelpUtils.callMethod(param.getArgs()[0], "getTitle");

                handlePreferenceClick(key, title, act, modRes, param);
            }
        });
    }

    /**
     * 处理偏好设置点击事件
     */
    private void handlePreferenceClick(String key, String title, Activity act, Resources modRes, BeforeHookParam param) {
        switch (key) {
            case "apk_filename" -> handleCopyPath(act, title, mLastPackageInfo.applicationInfo.sourceDir, param);
            case "data_path" -> handleCopyPath(act, title, mLastPackageInfo.applicationInfo.dataDir, param);
            case "open_in_market" -> handleOpenInMarket(act, param);
            case "open_in_app" -> handleOpenInApp(act, modRes, param);
        }
    }

    /**
     * 处理复制路径
     */
    private void handleCopyPath(Activity act, String title, String path, BeforeHookParam param) {
        ClipboardManager clipboard = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(title, path));

        Toast.makeText(act, act.getResources().getIdentifier(
                "app_manager_copy_pkg_to_clip", "string", act.getPackageName()),
            Toast.LENGTH_SHORT).show();

        param.setResult(true);
    }

    /**
     * 处理在应用市场打开
     */
    private void handleOpenInMarket(Activity act, BeforeHookParam param) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + mLastPackageInfo.packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            act.startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // 降级到 Google Play 网页版
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=" + mLastPackageInfo.packageName));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            act.startActivity(intent);
        }
        param.setResult(true);
    }

    /**
     * 处理启动应用
     */
    private void handleOpenInApp(Activity act, Resources modRes, BeforeHookParam param) {
        Intent launchIntent = act.getPackageManager().getLaunchIntentForPackage(mLastPackageInfo.packageName);

        if (launchIntent == null) {
            Toast.makeText(act, modRes.getString(R.string.app_details_nolaunch),
                Toast.LENGTH_SHORT).show();
            return;
        }

        int user = getUserIdFromIntent(act);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        if (user != 0) {
            startActivityAsUser(act, launchIntent, user);
        } else {
            act.startActivity(launchIntent);
        }

        param.setResult(true);
    }

    /**
     * 从 Intent 获取用户 ID
     */
    private int getUserIdFromIntent(Activity act) {
        try {
            int uid = act.getIntent().getIntExtra("am_app_uid", -1);
            return (int) EzxHelpUtils.callStaticMethod(UserHandle.class, "getUserId", uid);
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "getUserIdFromIntent error", t);
            return 0;
        }
    }

    /**
     * 以指定用户身份启动 Activity
     */
    private void startActivityAsUser(Activity act, Intent intent, int user) {
        try {
            EzxHelpUtils.callMethod(act, "startActivityAsUser", intent,
                EzxHelpUtils.newInstance(UserHandle.class, user));
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "startActivityAsUser error", t);
        }
    }
}

