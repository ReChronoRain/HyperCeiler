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
package com.sevtinge.hyperceiler.module.base;

import static com.sevtinge.hyperceiler.callback.ITAG.TAG;
import static com.sevtinge.hyperceiler.utils.Helpers.getPackageVersionCode;
import static com.sevtinge.hyperceiler.utils.Helpers.getPackageVersionName;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getMiuiVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.log.LogManager.logLevelDesc;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logI;

import android.os.Process;

import androidx.annotation.CallSuper;

import com.sevtinge.hyperceiler.module.app.AiAsst;
import com.sevtinge.hyperceiler.module.app.Aod;
import com.sevtinge.hyperceiler.module.app.Backup;
import com.sevtinge.hyperceiler.module.app.Barrage;
import com.sevtinge.hyperceiler.module.app.Browser;
import com.sevtinge.hyperceiler.module.app.Calendar;
import com.sevtinge.hyperceiler.module.app.Camera;
import com.sevtinge.hyperceiler.module.app.ContentExtension;
import com.sevtinge.hyperceiler.module.app.Creation;
import com.sevtinge.hyperceiler.module.app.Demo;
import com.sevtinge.hyperceiler.module.app.Downloads;
import com.sevtinge.hyperceiler.module.app.ExternalStorage;
import com.sevtinge.hyperceiler.module.app.FileExplorer;
import com.sevtinge.hyperceiler.module.app.Gallery;
import com.sevtinge.hyperceiler.module.app.GetApps;
import com.sevtinge.hyperceiler.module.app.GuardProvider;
import com.sevtinge.hyperceiler.module.app.Home;
import com.sevtinge.hyperceiler.module.app.Huanji;
import com.sevtinge.hyperceiler.module.app.InCallUi;
import com.sevtinge.hyperceiler.module.app.Joyose;
import com.sevtinge.hyperceiler.module.app.Lbe;
import com.sevtinge.hyperceiler.module.app.MediaEditor;
import com.sevtinge.hyperceiler.module.app.MiCloudService;
import com.sevtinge.hyperceiler.module.app.MiLink;
import com.sevtinge.hyperceiler.module.app.MiSettings;
import com.sevtinge.hyperceiler.module.app.MiShare;
import com.sevtinge.hyperceiler.module.app.MiSound;
import com.sevtinge.hyperceiler.module.app.MiWallpaper;
import com.sevtinge.hyperceiler.module.app.Mms;
import com.sevtinge.hyperceiler.module.app.Mtb;
import com.sevtinge.hyperceiler.module.app.Nfc;
import com.sevtinge.hyperceiler.module.app.Notes;
import com.sevtinge.hyperceiler.module.app.PackageInstaller;
import com.sevtinge.hyperceiler.module.app.PersonalAssistant;
import com.sevtinge.hyperceiler.module.app.Phone;
import com.sevtinge.hyperceiler.module.app.PowerKeeper;
import com.sevtinge.hyperceiler.module.app.Scanner;
import com.sevtinge.hyperceiler.module.app.ScreenRecorder;
import com.sevtinge.hyperceiler.module.app.ScreenShot;
import com.sevtinge.hyperceiler.module.app.SecurityCenter;
import com.sevtinge.hyperceiler.module.app.SystemFramework;
import com.sevtinge.hyperceiler.module.app.SystemSettings;
import com.sevtinge.hyperceiler.module.app.SystemUI;
import com.sevtinge.hyperceiler.module.app.ThemeManager;
import com.sevtinge.hyperceiler.module.app.TrustService;
import com.sevtinge.hyperceiler.module.app.TsmClient;
import com.sevtinge.hyperceiler.module.app.Updater;
import com.sevtinge.hyperceiler.module.app.VariousSystemApps;
import com.sevtinge.hyperceiler.module.app.VariousThirdApps;
import com.sevtinge.hyperceiler.module.app.VoiceAssist;
import com.sevtinge.hyperceiler.module.app.Weather;
import com.sevtinge.hyperceiler.module.base.tool.ResourcesTool;
import com.sevtinge.hyperceiler.safe.CrashHook;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.utils.prefs.PrefsMap;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;

import java.io.File;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseXposedInit {

    public static boolean isSafeModeOn = false;
    public static String mModulePath = null;
    public static PrefsMap<String, Object> mPrefsMap = new PrefsMap<>();
    public static ResourcesTool mResHook;

    public final SystemFramework mSystemFramework = new SystemFramework();
    public final SystemUI mSystemUI = new SystemUI();
    public final Home mHome = new Home();
    public final ScreenShot mScreenShot = new ScreenShot();

    public final ScreenRecorder mScreenRecorder = new ScreenRecorder();
    public final SecurityCenter mSecurityCenter = new SecurityCenter();
    public final SystemSettings mSystemSettings = new SystemSettings();
    public final PersonalAssistant mPersonalAssistant = new PersonalAssistant();
    public final ThemeManager mThemeManager = new ThemeManager();
    public final Updater mUpdater = new Updater();
    public final GetApps mGetApps = new GetApps();
    public final MediaEditor mMediaEditor = new MediaEditor();
    public final PackageInstaller mPackageInstaller = new PackageInstaller();
    public final PowerKeeper mPowerKeeper = new PowerKeeper();
    public final MiSettings mMiSettings = new MiSettings();
    public final Joyose mJoyose = new Joyose();
    public final VariousThirdApps mVariousThirdApps = new VariousThirdApps();
    public final VariousSystemApps mVariousSystemApps = new VariousSystemApps();
    public final Weather mWeather = new Weather();
    public final FileExplorer mFileExplorer = new FileExplorer();
    public final Gallery mGallery = new Gallery();
    public final AiAsst mAiAsst = new AiAsst();
    public final Scanner mScanner = new Scanner();
    public final MiShare mMiShare = new MiShare();
    public final MiCloudService miCloudService = new MiCloudService();
    public final MiLink mMiLink = new MiLink();
    public final GuardProvider mGuardProvider = new GuardProvider();
    public final Lbe mLbe = new Lbe();
    public final InCallUi mInCallUi = new InCallUi();
    public final TsmClient mTsmClient = new TsmClient();
    public final ContentExtension mContentExtension = new ContentExtension();
    public final VoiceAssist mVoiceAssist = new VoiceAssist();
    public final Mms mMms = new Mms();
    public final ExternalStorage mExternalStorage = new ExternalStorage();
    public final Camera mCamera = new Camera();
    public final Browser mBrowser = new Browser();
    public final Mtb mMtb = new Mtb();
    public final Phone mPhone = new Phone();
    public final MiWallpaper mMiWallpaper = new MiWallpaper();
    public final Downloads mDownloads = new Downloads();
    public final Aod mAod = new Aod();
    public final Barrage mBarrage = new Barrage();
    public final Notes mNotes = new Notes();
    public final Creation mCreation = new Creation();
    public final Demo mDemo = new Demo();
    public final Nfc mNfc = new Nfc();
    public final MiSound mMiSound = new MiSound();
    public final Backup mBackup = new Backup();
    public final Huanji mHuanji = new Huanji();
    public final TrustService mTrustService = new TrustService();
    public final Calendar mCalendar = new Calendar();

    @CallSuper
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        setXSharedPrefs();
        mResHook = new ResourcesTool(startupParam.modulePath);
        mModulePath = startupParam.modulePath;
    }

    private void setXSharedPrefs() {
        if (mPrefsMap.isEmpty()) {
            XSharedPreferences mXSharedPreferences;
            try {
                mXSharedPreferences = new XSharedPreferences(ProjectApi.mAppModulePkg, PrefsUtils.mPrefsName);
                mXSharedPreferences.makeWorldReadable();

                Map<String, ?> allPrefs = mXSharedPreferences == null ? null : mXSharedPreferences.getAll();
                if (allPrefs == null || allPrefs.isEmpty()) {
                    mXSharedPreferences = new XSharedPreferences(new File(PrefsUtils.mPrefsFile));
                    mXSharedPreferences.makeWorldReadable();
                    allPrefs = mXSharedPreferences == null ? null : mXSharedPreferences.getAll();
                    if (allPrefs == null || allPrefs.isEmpty()) {
                        logE(
                                "[UID" + Process.myUid() + "]",
                                "Cannot read module's SharedPreferences, some mods might not work!"
                        );
                    } else {
                        mPrefsMap.putAll(allPrefs);
                    }
                } else {
                    mPrefsMap.putAll(allPrefs);
                }
            } catch (Throwable t) {
                logE("setXSharedPrefs", t);
            }
        }
    }

    public void init(LoadPackageParam lpparam) {
        if (isSafeModeOn) return;
        String packageName = lpparam.packageName;
        if (Objects.equals(packageName, "android"))
            logI(packageName, "androidVersion = " + getAndroidVersion() + ", miuiVersion = " + getMiuiVersion() + ", hyperosVersion = " + getHyperOSVersion());
        else
            logI(packageName, "versionName = " + getPackageVersionName(lpparam) + ", versionCode = " + getPackageVersionCode(lpparam));
        switch (packageName) {
            case "android" -> {
                mSystemFramework.init(lpparam);
                mVariousSystemApps.init(lpparam);
                try {
                    new CrashHook(lpparam);
                    logI(TAG, "Success Hook Crash");
                } catch (Exception e) {
                    logE(TAG, "Hook Crash E: " + e);
                }
            }
            case "com.android.systemui" -> {
                if (isSystemUIModuleEnable()) {
                    mSystemUI.init(lpparam);
                    mVariousSystemApps.init(lpparam);
                }
            }
            case "com.miui.home" -> {
                if (isHomeModuleEnable()) {
                    mHome.init(lpparam);
                    mVariousSystemApps.init(lpparam);
                }
            }
            case "com.miui.securitycenter" -> {
                if (isSecurityCenterModuleEnable()) {
                    mSecurityCenter.init(lpparam);
                    mVariousSystemApps.init(lpparam);
                }
            }
            case "com.android.settings" -> {
                mSystemSettings.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.personalassistant" -> {
                mPersonalAssistant.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.android.thememanager" -> {
                mThemeManager.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.android.browser" -> {
                mBrowser.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.android.nfc" -> {
                mNfc.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.android.calendar" -> {
                mCalendar.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.android.updater" -> {
                mUpdater.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.xiaomi.market" -> mGetApps.init(lpparam);

            case "com.miui.packageinstaller" -> {
                mPackageInstaller.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.powerkeeper" -> mPowerKeeper.init(lpparam);

            case "com.xiaomi.misettings" -> {
                mMiSettings.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.xiaomi.joyose" -> mJoyose.init(lpparam);

            case "com.miui.screenshot" -> mScreenShot.init(lpparam);

            case "com.miui.screenrecorder" -> {
                mScreenRecorder.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.mediaeditor" -> mMediaEditor.init(lpparam);

            case "com.miui.miwallpaper" -> {
                mMiWallpaper.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.weather2" -> {
                mWeather.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.gallery" -> {
                mGallery.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.aod" -> {
                mAod.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.xiaomi.barrage" -> mBarrage.init(lpparam);

            case "com.xiaomi.aiasst.vision" -> {
                mAiAsst.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.xiaomi.scanner" -> {
                mScanner.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.mishare.connectivity" -> {
                mMiShare.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.misound" -> {
                mMiSound.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.milink.service" -> mMiLink.init(lpparam);

            case "com.miui.guardprovider" -> mGuardProvider.init(lpparam);

            case "com.lbe.security.miui" -> {
                if (!isMoreHyperOSVersion(1f)) {
                    mLbe.init(lpparam);
                    mVariousSystemApps.init(lpparam);
                }
            }
            case "com.android.incallui" -> {
                mInCallUi.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.notes" -> {
                mNotes.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.tsmclient" -> {
                mTsmClient.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.contentextension" -> {
                mContentExtension.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.voiceassist" -> {
                mVoiceAssist.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.android.mms" -> {
                mMms.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.android.fileexplorer" -> {
                mFileExplorer.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.android.phone" -> mPhone.init(lpparam);

            case "com.xiaomi.mtb" -> mMtb.init(lpparam);

            case "com.android.externalstorage" -> {
                mExternalStorage.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.android.camera" -> {
                mCamera.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.android.providers.downloads" -> mDownloads.init(lpparam);

            case "com.miui.cloudservice" -> {
                miCloudService.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.creation" -> {
                mCreation.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.backup" -> {
                mBackup.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.miui.huanji" -> {
                mHuanji.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.xiaomi.trustservice" -> {
                mTrustService.init(lpparam);
                mVariousSystemApps.init(lpparam);
            }
            case "com.hchen.demo" -> mDemo.init(lpparam);
            case ProjectApi.mAppModulePkg -> ModuleActiveHook(lpparam);
            default -> mVariousThirdApps.init(lpparam);
        }
    }

    public void ModuleActiveHook(LoadPackageParam lpparam) {
        Class<?> mHelpers = XposedHelpers.findClassIfExists(ProjectApi.mAppModulePkg + ".utils.Helpers", lpparam.classLoader);

        XposedHelpers.setStaticBooleanField(mHelpers, "isModuleActive", true);
        XposedHelpers.setStaticIntField(mHelpers, "XposedVersion", XposedBridge.getXposedVersion());
        XposedBridge.log("[HyperCeiler][I]: Log level is " + logLevelDesc());
    }


    private boolean isSafeModeEnable(String key) {
        return !mPrefsMap.getBoolean(key);
    }

    private boolean isSystemUIModuleEnable() {
        return isSafeModeEnable("system_ui_safe_mode_enable");
    }

    private boolean isHomeModuleEnable() {
        return isSafeModeEnable("home_safe_mode_enable");
    }

    private boolean isSecurityCenterModuleEnable() {
        return isSafeModeEnable("security_center_safe_mode_enable");
    }

}
