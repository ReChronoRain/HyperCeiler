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
package com.sevtinge.hyperceiler.libhook.rules.home.other;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getModuleRes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;
import io.github.libxposed.api.XposedInterface;

public class AllowShareApk extends HomeBaseHookNew {

    private static final String[] SYSTEM_APK_PREFIXES = {
        "/system/", "/system_ext/"
    };

    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean sPreparing = new AtomicBoolean(false);
    private static final ThreadLocal<Boolean> sBypassSystemPackageCheck = new ThreadLocal<>();

    private static boolean isSystemApkPath(String path) {
        for (String prefix : SYSTEM_APK_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取 shared_apk 目录。
     *
     * <p>使用 context.getFilesDir() 即可，因为 MIUI Home 设置了
     * defaultToDeviceProtectedStorage，FileProvider 内部解析 {@code <files-path>}
     * 时同样使用 DE 存储路径，两者一致。</p>
     */
    private static File getSharedApkDir(Context context) {
        return new File(context.getFilesDir(), "shared_apk");
    }

    /**
     * 清理 shared_apk 目录中的所有临时复制文件。
     */
    private static void cleanupSharedApkDir(Context context) {
        File shareDir = getSharedApkDir(context);
        if (!shareDir.exists()) return;

        File[] files = shareDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.delete()) {
                XposedLog.d("AllowShareApk", "com.miui.home",
                    "Cleaned up shared APK: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * 将文件复制到指定目标。
     */
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
            XposedLog.e("AllowShareApk", "com.miui.home",
                "Failed to copy file: " + src + " -> " + dst, e);
            return false;
        }
    }

    /**
     * 生成用于分享的文件名前缀：应用名_版本名(版本号)
     */
    private static String buildShareFileName(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            PackageInfo pkgInfo = pm.getPackageInfo(packageName, 0);
            String appLabel = appInfo.loadLabel(pm).toString().trim();
            String versionName = pkgInfo.versionName;
            long versionCode = pkgInfo.getLongVersionCode();

            // 移除文件名中不允许的字符
            String safeName = appLabel.replaceAll("[/\\\\:*?\"<>|]", "_");
            if (versionName != null && !versionName.isEmpty()) {
                return safeName + "_" + versionName + "(" + versionCode + ")";
            } else {
                return safeName + "_(" + versionCode + ")";
            }
        } catch (PackageManager.NameNotFoundException e) {
            XposedLog.e("AllowShareApk", "com.miui.home",
                "Failed to get package info for " + packageName, e);
            return packageName;
        }
    }

    /**
     * 获取 split APK 的总文件数（包含 base APK）。若无 split 则返回 0。
     */
    private static int getSplitApkCount(Context context, String packageName) {
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                .getApplicationInfo(packageName, 0);
            if (appInfo.splitSourceDirs != null && appInfo.splitSourceDirs.length > 0) {
                return 1 + appInfo.splitSourceDirs.length; // base + splits
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return 0;
    }

    /**
     * 计算文件的 CRC32 校验值，用于 STORED 模式的 ZipEntry。
     */
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

    /**
     * 将 base APK 和所有 split APK 打包成 .apks (ZIP) 文件。
     * 使用 STORED（无压缩）模式，因为 APK 本身已经是压缩文件，
     * 跳过重复压缩可以大幅提升打包速度。
     */
    private static File createApksZip(Context context, String packageName, File outputFile) {
        try {
            ApplicationInfo appInfo = context.getPackageManager()
                .getApplicationInfo(packageName, 0);

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
                zos.setMethod(ZipOutputStream.STORED);

                // 添加 base APK
                File baseApk = new File(appInfo.sourceDir);
                addStoredFileToZip(zos, baseApk, "base.apk");

                // 添加所有 split APK
                if (appInfo.splitSourceDirs != null) {
                    for (String splitPath : appInfo.splitSourceDirs) {
                        File splitApk = new File(splitPath);
                        addStoredFileToZip(zos, splitApk, splitApk.getName());
                    }
                }
            }

            XposedLog.i("AllowShareApk", "com.miui.home",
                "Created APKS zip: " + outputFile.getAbsolutePath());
            return outputFile;
        } catch (Exception e) {
            XposedLog.e("AllowShareApk", "com.miui.home",
                "Failed to create APKS zip for " + packageName, e);
            return null;
        }
    }

    /**
     * 以 STORED 模式将文件添加到 ZIP 中（无压缩，仅存储）。
     * 需要预先设置 CRC32、size 和 compressedSize。
     */
    private static void addStoredFileToZip(ZipOutputStream zos, File file, String entryName)
        throws IOException {
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

    /**
     * 准备用于分享的文件：重命名、处理系统 APK 复制、处理 split APK 打包。
     * 返回准备好的文件，如果是 split APK 则返回 .apks 文件，否则返回 .apk 文件。
     */
    private static File prepareShareFile(Context context, File originalApkFile, String packageName) {
        // 清理上一次分享的临时文件
        cleanupSharedApkDir(context);

        File shareDir = getSharedApkDir(context);
        if (!shareDir.exists() && !shareDir.mkdirs()) {
            XposedLog.e("AllowShareApk", "com.miui.home",
                "Failed to create share dir: " + shareDir);
            return originalApkFile;
        }

        String baseName = buildShareFileName(context, packageName);

        // 有 split APK 的情况，打包成 .apks
        if (getSplitApkCount(context, packageName) > 0) {
            File apksFile = new File(shareDir, baseName + ".apks");
            File result = createApksZip(context, packageName, apksFile);
            if (result != null) {
                return result;
            }
            // 打包失败，回退到单个 APK 分享
        }

        // 单个 APK 的情况：复制并重命名
        File renamedApk = new File(shareDir, baseName + ".apk");
        String originalPath = originalApkFile.getAbsolutePath();

        if (isSystemApkPath(originalPath)) {
            // 系统 APK 需要复制
            if (copyFile(originalApkFile, renamedApk)) {
                XposedLog.i("AllowShareApk", "com.miui.home",
                    "Copied and renamed system APK: " + originalPath + " -> " + renamedApk.getAbsolutePath());
                return renamedApk;
            }
        } else {
            // 非系统 APK，复制并重命名到 shared_apk 目录
            if (copyFile(originalApkFile, renamedApk)) {
                XposedLog.i("AllowShareApk", "com.miui.home",
                    "Copied and renamed APK: " + originalPath + " -> " + renamedApk.getAbsolutePath());
                return renamedApk;
            }
        }
        return originalApkFile;
    }

    private void hookIsValidAndShareApk(String isSecurityCenterMethodClass, String itemInfoClass,
                                        String launcherClass) {
        // 绕过安全中心分享检查
        findAndHookMethod(isSecurityCenterMethodClass, "isSecurityCenterSupportShareAPK", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    param.setResult(false);
                }
            }
        );

        findAndChainMethod("com.miui.home.launcher.common.Utilities",
            "isSystemPackage", Context.class, String.class,
            (XposedInterface.Hooker) chain -> {
                if (Boolean.TRUE.equals(sBypassSystemPackageCheck.get())) {
                    return false;
                }
                return chain.proceed();
            }
        );

        // 绕过 isSystemPackage 检查，使系统应用的分享按钮可见
        findAndChainMethod("com.miui.home.launcher.shortcuts.SystemShortcutMenuItem$ShareAppShortcutMenuItem",
            "isValid", itemInfoClass,
            (XposedInterface.Hooker) chain -> {
                sBypassSystemPackageCheck.set(Boolean.TRUE);
                try {
                    return chain.proceed();
                } finally {
                    sBypassSystemPackageCheck.remove();
                }
            }
        );

        // 准备分享文件：异步执行重命名、复制系统 APK、打包 split APK，避免阻塞 UI
        findAndChainMethod("com.miui.home.launcher.shortcuts.SystemShortcutMenuItem$ShareAppShortcutMenuItem",
            "shareApk", launcherClass, File.class, String.class,
            (XposedInterface.Hooker) chain -> {
                Object[] args = chain.getArgs().toArray();
                Activity activity = (Activity) args[0];
                File originalFile = (File) args[1];
                String packageName = (String) args[2];

                // 防止重复点击：如果正在准备中，直接忽略
                if (!sPreparing.compareAndSet(false, true)) {
                    XposedLog.w("AllowShareApk", "com.miui.home",
                        "Already preparing share file, ignoring duplicate click");
                    return null;
                }

                Resources modRes = getModuleRes(activity);

                // 根据是否有 split APK 显示不同的提示
                int splitCount = getSplitApkCount(activity, packageName);
                if (splitCount > 0) {
                    Toast.makeText(activity,
                        modRes.getString(R.string.home_share_apk_preparing_split, splitCount),
                        Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(activity,
                        modRes.getString(R.string.home_share_apk_preparing),
                        Toast.LENGTH_SHORT).show();
                }

                // 获取 FileProvider authority（与原方法一致）
                int providerFileResId = activity.getResources().getIdentifier(
                    "provider_file", "string", activity.getPackageName());
                String authority = activity.getString(providerFileResId);

                // 预先获取错误提示字符串，避免在后台线程加载模块资源的线程安全问题
                String failedMsg = modRes.getString(R.string.home_share_apk_failed);
                String prepareFailedMsg = modRes.getString(R.string.home_share_apk_prepare_failed);

                IO_EXECUTOR.execute(() -> {
                    try {
                        File shareFile = prepareShareFile(activity, originalFile, packageName);
                        boolean isApks = shareFile.getName().endsWith(".apks");
                        String mimeType = isApks
                            ? "application/zip"
                            : "application/vnd.android.package-archive";

                        activity.runOnUiThread(() -> {
                            try {
                                Uri uri = FileProvider.getUriForFile(activity, authority, shareFile);
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                intent.setType(mimeType);
                                intent.putExtra(Intent.EXTRA_STREAM, uri);
                                intent.putExtra(Intent.EXTRA_PACKAGE_NAME, packageName);
                                intent.putExtra("com.miui.mishare.extra.MISHARE_APK_PACKAGE_NAME", packageName);
                                activity.startActivity(Intent.createChooser(intent, null));
                            } catch (Throwable t) {
                                XposedLog.e("AllowShareApk", "com.miui.home",
                                    "Failed to launch share intent", t);
                                Toast.makeText(activity, failedMsg, Toast.LENGTH_SHORT).show();
                            } finally {
                                sPreparing.set(false);
                            }
                        });
                    } catch (Throwable t) {
                        XposedLog.e("AllowShareApk", "com.miui.home",
                            "Failed to prepare share file", t);
                        activity.runOnUiThread(() ->
                            Toast.makeText(activity, prepareFailedMsg, Toast.LENGTH_SHORT).show()
                        );
                        sPreparing.set(false);
                    }
                });

                // 拦截原方法，不执行原始的同步分享逻辑
                return null;
            }
        );
    }

    @Version(isPad = false, min = 600000000)
    private void initOS3Hook() {
        hookIsValidAndShareApk(
            "com.miui.home.common.utils.Utils",
            "com.miui.home.model.api.ItemInfo",
            "com.miui.home.launcher.BaseLauncher"
        );
    }

    @Override
    public void initBase() {
        hookIsValidAndShareApk(
            "com.miui.home.launcher.common.Utilities",
            "com.miui.home.launcher.ItemInfo",
            "com.miui.home.launcher.Launcher"
        );
    }
}
