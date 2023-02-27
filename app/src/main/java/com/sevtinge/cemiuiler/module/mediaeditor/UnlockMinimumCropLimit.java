package com.sevtinge.cemiuiler.module.mediaeditor;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;

public class UnlockMinimumCropLimit extends BaseHook {

    Class<?> mScreenCropView;
    Class<?> mCrop;

    @Override
    public void init() {
        /*mScreenCropView = findClassIfExists("com.miui.gallery.editor.photo.screen.crop.ScreenCropView$h");
        returnIntConstant(mScreenCropView, "e");
        mCrop = findClassIfExists("com.miui.gallery.editor.photo.core.imports.obsoletes.Crop$r");
        returnIntConstant(mCrop, "a");
        XposedBridge.log("Cemiuiler: com.miui.mediaeditor Ver 1.0.x UnlockMinimumCropLimit hook success!");*/
        String appVersionName = getPackageVersion(lpparam);
        XposedBridge.log("Cemiuiler: com.miui.mediaeditor app Ver is " + appVersionName);
        if (appVersionName.startsWith("1.0")) {
            //1.0
            mScreenCropView = findClassIfExists("com.miui.gallery.editor.photo.screen.crop.ScreenCropView$h");
            returnIntConstant(mScreenCropView, "e");
            mCrop = findClassIfExists("com.miui.gallery.editor.photo.core.imports.obsoletes.Crop$r");
            returnIntConstant(mCrop, "a");
            XposedBridge.log("Cemiuiler: com.miui.mediaeditor Ver 1.0.x UnlockMinimumCropLimit hook success!");
        } else if (appVersionName.startsWith("0.9") ||appVersionName.startsWith("1.2") || appVersionName.startsWith("Mod")) {
            //0.9
            mScreenCropView = findClassIfExists("com.miui.gallery.editor.photo.screen.crop.ScreenCropView$OooOOO0");
            returnIntConstant(mScreenCropView, "OooO0o0");
            mCrop = findClassIfExists("com.miui.gallery.editor.photo.core.imports.obsoletes.Crop$o00Oo0");
            returnIntConstant(mCrop, "OooO00o");
            XposedBridge.log("Cemiuiler: com.miui.mediaeditor Ver 0.9.x/1.2.x UnlockMinimumCropLimit hook success!");
        }

    }

    private void returnIntConstant(Class<?> cls, String methodName) {
        findAndHookMethod(cls, methodName, XC_MethodReplacement.returnConstant(0));
    }

    public static String getAppVersionName(Context context, String packagename) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packagename, 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getPackageVersion(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> parserCls = XposedHelpers.findClass("android.content.pm.PackageParser", lpparam.classLoader);
            Object parser = parserCls.newInstance();
            File apkPath = new File(lpparam.appInfo.sourceDir);
            Object pkg = XposedHelpers.callMethod(parser, "parsePackage", apkPath, 0);
            String versionName = (String) XposedHelpers.getObjectField(pkg, "mVersionName");
            int versionCode = XposedHelpers.getIntField(pkg, "mVersionCode");
            XposedBridge.log("Cemiuiler: " + String.format("%s (%d", versionName, versionCode));
            return String.format("%s (%d", versionName, versionCode);
        } catch (Throwable e) {
            XposedBridge.log("Cemiuiler: Unknown Version.");
            XposedBridge.log(e);
            return "null";
        }
    }
}




