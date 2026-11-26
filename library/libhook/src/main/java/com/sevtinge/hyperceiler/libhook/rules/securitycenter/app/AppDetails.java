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

import androidx.core.content.FileProvider;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class AppDetails extends BaseHook {

    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean sPreparing = new AtomicBoolean(false);

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
            public void before(HookParam param) {
                Field piField = com.sevtinge.hyperceiler.libhook.base.BaseHook.findFirstFieldByExactType(
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
            public void after(HookParam param) {
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
        Field piField = com.sevtinge.hyperceiler.libhook.base.BaseHook.findFirstFieldByExactType(
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
        Method[] addPrefMethods = com.sevtinge.hyperceiler.libhook.base.BaseHook.findMethodsByExactParameters(
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
        com.sevtinge.hyperceiler.libhook.base.BaseHook.hookMethod(addPrefMethod, new IMethodHook() {
            @Override
            public void before(HookParam param) {
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
        Object prefManager = com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(thiz, "getPreferenceManager");
        Method[] ctxMethods = com.sevtinge.hyperceiler.libhook.base.BaseHook.findMethodsByExactParameters(
            prefManager.getClass(), Context.class);

        if (ctxMethods.length == 0) {
            return null;
        }

        return (Context) com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(prefManager, ctxMethods[0].getName());
    }

    /**
     * 创建 TextPreference
     */
    private Object createTextPreference(Context ctx, ClassLoader cl, Object[] args) {
        Class<?> textPrefCls = com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass("miuix.preference.TextPreference", cl);
        Object textPref = com.sevtinge.hyperceiler.libhook.base.BaseHook.newInstance(textPrefCls, ctx);

        com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(textPref, "setKey", args[0]);
        com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(textPref, "setTitle", args[1]);
        com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(textPref, "setText", args[2]);

        return textPref;
    }

    /**
     * 添加 Preference 到 Category
     */
    private void addPreferenceToCategory(Object thiz, ClassLoader cl, Object textPref) throws Exception {
        Field prefField = com.sevtinge.hyperceiler.libhook.base.BaseHook.findFirstFieldByExactType(
            thiz.getClass(),
            com.sevtinge.hyperceiler.libhook.base.BaseHook.findClass("androidx.preference.PreferenceCategory", cl)
        );

        if (prefField == null) {
            XposedLog.e(TAG, "Cannot find PreferenceCategory field!");
            return;
        }

        prefField.setAccessible(true);
        Object prefGroup = prefField.get(thiz);
        com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(prefGroup, "addPreference", textPref);
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
                addPrefMethod.invoke(fragment, "share_apk",
                    modRes.getString(R.string.app_details_share_apk), "");
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
            public void before(HookParam param) {
                String key = (String) com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(param.getArgs()[0], "getKey");
                String title = (String) com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(param.getArgs()[0], "getTitle");

                handlePreferenceClick(key, title, act, modRes, param);
            }
        });
    }

    /**
     * 处理偏好设置点击事件
     */
    private void handlePreferenceClick(String key, String title, Activity act, Resources modRes, HookParam param) {
        switch (key) {
            case "apk_filename" -> handleCopyPath(act, title, mLastPackageInfo.applicationInfo.sourceDir, param);
            case "data_path" -> handleCopyPath(act, title, mLastPackageInfo.applicationInfo.dataDir, param);
            case "open_in_market" -> handleOpenInMarket(act, param);
            case "open_in_app" -> handleOpenInApp(act, modRes, param);
            case "share_apk" -> handleShareApk(act, modRes, param);
        }
    }

    /**
     * 处理复制路径
     */
    private void handleCopyPath(Activity act, String title, String path, HookParam param) {
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
    private void handleOpenInMarket(Activity act, HookParam param) {
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
    private void handleOpenInApp(Activity act, Resources modRes, HookParam param) {
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
            return (int) com.sevtinge.hyperceiler.libhook.base.BaseHook.callStaticMethod(UserHandle.class, "getUserId", uid);
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
            com.sevtinge.hyperceiler.libhook.base.BaseHook.callMethod(act, "startActivityAsUser", intent,
                com.sevtinge.hyperceiler.libhook.base.BaseHook.newInstance(UserHandle.class, user));
        } catch (Throwable t) {
            XposedLog.w(TAG, getPackageName(), "startActivityAsUser error", t);
        }
    }

    private File resolveOriginalApk() {
        if (mLastPackageInfo == null || mLastPackageInfo.applicationInfo == null) {
            return null;
        }
        String sourceDir = mLastPackageInfo.applicationInfo.sourceDir;
        if (sourceDir == null || sourceDir.isEmpty()) {
            return null;
        }
        File apkFile = new File(sourceDir);
        return apkFile.exists() ? apkFile : null;
    }

    private String getPreparingMessage(Resources modRes, PackageInfo pkgInfo) {
        int splitCount = getSplitApkCount(pkgInfo);
        if (splitCount > 0) {
            return modRes.getString(R.string.app_details_share_apk_preparing_split, splitCount);
        }
        return modRes.getString(R.string.app_details_share_apk_preparing);
    }

    private void launchShareChooser(Activity act, File shareFile, String packageName, String failedMsg) {
        try {
            boolean isApks = shareFile.getName().endsWith(".apks");
            String mimeType = isApks ? "application/zip" : "application/vnd.android.package-archive";
            Uri uri = getShareUri(act, shareFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setType(mimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName);
            intent.putExtra("com.miui.mishare.extra.MISHARE_APK_PACKAGE_NAME", packageName);
            act.startActivity(Intent.createChooser(intent, null));
        } catch (Throwable t) {
            XposedLog.e(TAG, getPackageName(), "Failed to launch share intent", t);
            Toast.makeText(act, failedMsg, Toast.LENGTH_SHORT).show();
        } finally {
            sPreparing.set(false);
        }
    }

    private void performShareInBackground(Activity act, File originalApk, PackageInfo pkgInfo, String failedMsg) {
        IO_EXECUTOR.execute(() -> {
            try {
                File shareFile = prepareShareFile(act, originalApk, pkgInfo);
                act.runOnUiThread(() -> launchShareChooser(act, shareFile, pkgInfo.packageName, failedMsg));
            } catch (Throwable t) {
                XposedLog.e(TAG, getPackageName(), "Failed to prepare share file", t);
                act.runOnUiThread(() -> Toast.makeText(act, failedMsg, Toast.LENGTH_SHORT).show());
                sPreparing.set(false);
            }
        });
    }

    private void handleShareApk(Activity act, Resources modRes, HookParam param) {
        File originalApk = resolveOriginalApk();
        if (originalApk == null) {
            Toast.makeText(act, modRes.getString(R.string.app_details_share_apk_failed), Toast.LENGTH_SHORT).show();
            param.setResult(true);
            return;
        }

        if (!sPreparing.compareAndSet(false, true)) {
            param.setResult(true);
            return;
        }

        String failedMsg = modRes.getString(R.string.app_details_share_apk_failed);
        Toast.makeText(act, getPreparingMessage(modRes, mLastPackageInfo), Toast.LENGTH_SHORT).show();
        performShareInBackground(act, originalApk, mLastPackageInfo, failedMsg);
        param.setResult(true);
    }

    private Uri getShareUri(Context context, File file) {
        String[] authorities = {
            "com.miui.securitycenter.zman.fileProvider",
            "com.miui.securitycenter.fileProvider",
            context.getPackageName() + ".fileProvider",
            context.getPackageName() + ".provider"
        };

        for (String authority : authorities) {
            try {
                return FileProvider.getUriForFile(context, authority, file);
            } catch (Throwable ignored) {}
        }

        android.os.StrictMode.setVmPolicy(new android.os.StrictMode.VmPolicy.Builder().build());
        return Uri.fromFile(file);
    }

    private static File getSharedApkDir(Context context) {
        return new File(context.getCacheDir(), "shared_apk");
    }

    private static void cleanupSharedApkDir(Context context) {
        File shareDir = getSharedApkDir(context);
        if (!shareDir.exists()) return;
        File[] files = shareDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            file.delete();
        }
    }

    private static boolean copyFile(File src, File dst) {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            return true;
        } catch (IOException e) {
            XposedLog.e("AppDetails", "Failed to copy file: " + src + " -> " + dst, e);
            return false;
        }
    }

    private String buildShareFileName(Context context, PackageInfo pkgInfo) {
        try {
            String appLabel = pkgInfo.applicationInfo.loadLabel(context.getPackageManager()).toString().trim();
            String versionName = pkgInfo.versionName;
            long versionCode = pkgInfo.getLongVersionCode();
            String safeName = appLabel.replaceAll("[/\\\\:*?\"<>|]", "_");
            if (versionName != null && !versionName.isEmpty()) {
                return safeName + "_" + versionName + "(" + versionCode + ")";
            } else {
                return safeName + "_(" + versionCode + ")";
            }
        } catch (Throwable t) {
            return pkgInfo.packageName;
        }
    }

    private static int getSplitApkCount(PackageInfo pkgInfo) {
        if (pkgInfo != null && pkgInfo.applicationInfo != null &&
            pkgInfo.applicationInfo.splitSourceDirs != null &&
            pkgInfo.applicationInfo.splitSourceDirs.length > 0) {
            return 1 + pkgInfo.applicationInfo.splitSourceDirs.length;
        }
        return 0;
    }

    private static long computeCrc32(File file) throws IOException {
        CRC32 crc = new CRC32();
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                crc.update(buf, 0, len);
            }
        }
        return crc.getValue();
    }

    private static void addStoredFileToZip(ZipOutputStream zos, File file, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(file.length());
        entry.setCompressedSize(file.length());
        entry.setCrc(computeCrc32(file));
        zos.putNextEntry(entry);
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                zos.write(buf, 0, len);
            }
        }
        zos.closeEntry();
    }

    private File createApksZip(PackageInfo pkgInfo, File outputFile) {
        try {
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
                zos.setMethod(ZipOutputStream.STORED);
                File baseApk = new File(pkgInfo.applicationInfo.sourceDir);
                addStoredFileToZip(zos, baseApk, "base.apk");

                if (pkgInfo.applicationInfo.splitSourceDirs != null) {
                    for (String splitPath : pkgInfo.applicationInfo.splitSourceDirs) {
                        File splitApk = new File(splitPath);
                        addStoredFileToZip(zos, splitApk, splitApk.getName());
                    }
                }
            }
            return outputFile;
        } catch (Exception e) {
            XposedLog.e(TAG, getPackageName(), "Failed to create APKS zip", e);
            return null;
        }
    }

    private File prepareShareFile(Context context, File originalApkFile, PackageInfo pkgInfo) {
        cleanupSharedApkDir(context);
        File shareDir = getSharedApkDir(context);
        if (!shareDir.exists() && !shareDir.mkdirs()) {
            return originalApkFile;
        }

        String baseName = buildShareFileName(context, pkgInfo);

        if (getSplitApkCount(pkgInfo) > 0) {
            File apksFile = new File(shareDir, baseName + ".apks");
            File result = createApksZip(pkgInfo, apksFile);
            if (result != null) {
                return result;
            }
        }

        File renamedApk = new File(shareDir, baseName + ".apk");
        if (copyFile(originalApkFile, renamedApk)) {
            return renamedApk;
        }
        return originalApkFile;
    }
}

