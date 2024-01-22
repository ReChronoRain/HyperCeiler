package com.sevtinge.hyperceiler.module.base;

import static com.sevtinge.hyperceiler.utils.Helpers.getPackageVersionCode;
import static com.sevtinge.hyperceiler.utils.Helpers.getPackageVersionName;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getAndroidVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getHyperOSVersion;
import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.getMiuiVersion;
import static com.sevtinge.hyperceiler.utils.log.AndroidLogUtils.LogD;
import static com.sevtinge.hyperceiler.utils.log.LogManager.logLevelDesc;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logI;

import com.sevtinge.hyperceiler.CrashHook;
import com.sevtinge.hyperceiler.callback.ITAG;
import com.sevtinge.hyperceiler.module.app.AiAsst;
import com.sevtinge.hyperceiler.module.app.Aod;
import com.sevtinge.hyperceiler.module.app.Backup;
import com.sevtinge.hyperceiler.module.app.Barrage;
import com.sevtinge.hyperceiler.module.app.Browser;
import com.sevtinge.hyperceiler.module.app.Camera;
import com.sevtinge.hyperceiler.module.app.Clock;
import com.sevtinge.hyperceiler.module.app.ContentExtension;
import com.sevtinge.hyperceiler.module.app.Creation;
import com.sevtinge.hyperceiler.module.app.Demo;
import com.sevtinge.hyperceiler.module.app.Downloads;
import com.sevtinge.hyperceiler.module.app.ExternalStorage;
import com.sevtinge.hyperceiler.module.app.FileExplorer;
import com.sevtinge.hyperceiler.module.app.Gallery;
import com.sevtinge.hyperceiler.module.app.GuardProvider;
import com.sevtinge.hyperceiler.module.app.Home;
import com.sevtinge.hyperceiler.module.app.Huanji;
import com.sevtinge.hyperceiler.module.app.InCallUi;
import com.sevtinge.hyperceiler.module.app.Joyose;
import com.sevtinge.hyperceiler.module.app.Lbe;
import com.sevtinge.hyperceiler.module.app.Market;
import com.sevtinge.hyperceiler.module.app.MediaEditor;
import com.sevtinge.hyperceiler.module.app.MiCloudService;
import com.sevtinge.hyperceiler.module.app.MiLink;
import com.sevtinge.hyperceiler.module.app.MiSettings;
import com.sevtinge.hyperceiler.module.app.MiShare;
import com.sevtinge.hyperceiler.module.app.MiSound;
import com.sevtinge.hyperceiler.module.app.MiWallpaper;
import com.sevtinge.hyperceiler.module.app.Mms;
import com.sevtinge.hyperceiler.module.app.Mtb;
import com.sevtinge.hyperceiler.module.app.Music;
import com.sevtinge.hyperceiler.module.app.NetworkBoost;
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
import com.sevtinge.hyperceiler.module.app.SystemVarious;
import com.sevtinge.hyperceiler.module.app.ThemeManager;
import com.sevtinge.hyperceiler.module.app.TrustService;
import com.sevtinge.hyperceiler.module.app.TsmClient;
import com.sevtinge.hyperceiler.module.app.Updater;
import com.sevtinge.hyperceiler.module.app.Various;
import com.sevtinge.hyperceiler.module.app.VoiceAssist;
import com.sevtinge.hyperceiler.module.app.Weather;
import com.sevtinge.hyperceiler.utils.PrefsMap;
import com.sevtinge.hyperceiler.utils.PrefsUtils;
import com.sevtinge.hyperceiler.utils.ResourcesHook;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;

import java.io.File;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public abstract class BaseXposedInit implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    public static boolean isSafeModeOn = false;

    public static ResourcesHook mResHook;
    public static String mModulePath = null;
    public static PrefsMap<String, Object> mPrefsMap = new PrefsMap<>();

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
    public final Market mMarket = new Market();
    public final MediaEditor mMediaEditor = new MediaEditor();
    public final PackageInstaller mPackageInstaller = new PackageInstaller();
    public final PowerKeeper mPowerKeeper = new PowerKeeper();
    public final MiSettings mMiSettings = new MiSettings();
    public final Joyose mJoyose = new Joyose();
    public final Various mVarious = new Various();
    public final SystemVarious mSystemVarious = new SystemVarious();
    public final Weather mWeather = new Weather();
    public final Clock mClock = new Clock();
    public final FileExplorer mFileExplorer = new FileExplorer();
    public final Music mMusic = new Music();
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
    // public final SoGou mSoGou = new SoGou();
    public final Mtb mMtb = new Mtb();
    public final Phone mPhone = new Phone();
    public final MiWallpaper mMiWallpaper = new MiWallpaper();
    public final Downloads mDownloads = new Downloads();
    public final Aod mAod = new Aod();
    public final Barrage mBarrage = new Barrage();
    public final Notes mNotes = new Notes();
    public final NetworkBoost networkBoost = new NetworkBoost();
    public final Creation mCreation = new Creation();
    public final Demo mDemo = new Demo();
    public final Nfc mNfc = new Nfc();
    public final MiSound mMiSound = new MiSound();
    public final Backup mBackup = new Backup();
    public final Huanji mHuanji = new Huanji();
    public final TrustService mTrustService = new TrustService();

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        setXSharedPrefs();
        mResHook = new ResourcesHook();
        mModulePath = startupParam.modulePath;
    }

    public void setXSharedPrefs() {
        if (mPrefsMap.size() == 0) {
            XSharedPreferences mXSharedPreferences;
            try {
                mXSharedPreferences = new XSharedPreferences(ProjectApi.mAppModulePkg, PrefsUtils.mPrefsName);
                mXSharedPreferences.makeWorldReadable();

                Map<String, ?> allPrefs = mXSharedPreferences == null ? null : mXSharedPreferences.getAll();
                if (allPrefs == null || allPrefs.size() == 0) {
                    mXSharedPreferences = new XSharedPreferences(new File(PrefsUtils.mPrefsFile));
                    mXSharedPreferences.makeWorldReadable();
                    allPrefs = mXSharedPreferences == null ? null : mXSharedPreferences.getAll();
                    if (allPrefs == null || allPrefs.size() == 0) {
                        logE(
                            "[UID" + android.os.Process.myUid() + "]",
                            "Cannot read module's SharedPreferences, some mods might not work!"
                        );
                    } else {
                        mPrefsMap.putAll(allPrefs);
                    }
                } else {
                    mPrefsMap.putAll(allPrefs);
                }
            } catch (Throwable t) {
                LogD("setXSharedPrefs", t);
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
                mSystemVarious.init(lpparam);
                try {
                    new CrashHook(lpparam);
                    logI(ITAG.TAG, "Success Hook Crash");
                } catch (Exception e) {
                    logE(ITAG.TAG, "Hook Crash E: " + e);
                }
            }
            case "com.android.systemui" -> {
                if (isSystemUIModuleEnable()) {
                    mSystemUI.init(lpparam);
                    mSystemVarious.init(lpparam);
                }
            }
            case "com.miui.home" -> {
                if (isHomeModuleEnable()) {
                    mHome.init(lpparam);
                    mSystemVarious.init(lpparam);
                }
            }
            case "com.miui.securitycenter" -> {
                if (isSecurityCenterModuleEnable()) {
                    mSecurityCenter.init(lpparam);
                    mSystemVarious.init(lpparam);
                }
            }
            case "com.android.settings" -> {
                mSystemSettings.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.personalassistant" -> {
                mPersonalAssistant.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.android.thememanager" -> {
                mThemeManager.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.android.browser" -> {
                mBrowser.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            /*case "com.sohu.inputmethod.sogou.xiaomi", "com.sohu.inputmethod.sogou" -> {
                mSoGou.init(lpparam);
                mVarious.init(lpparam);
            }*/
            case "com.android.nfc" -> {
                mNfc.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.android.updater" -> {
                mUpdater.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.xiaomi.market" -> mMarket.init(lpparam);

            case "com.miui.packageinstaller" -> {
                mPackageInstaller.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.powerkeeper" -> mPowerKeeper.init(lpparam);

            case "com.xiaomi.misettings" -> {
                mMiSettings.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.xiaomi.joyose" -> mJoyose.init(lpparam);

            case "com.miui.screenshot" -> mScreenShot.init(lpparam);

            case "com.miui.screenrecorder" -> {
                mScreenRecorder.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.mediaeditor" -> mMediaEditor.init(lpparam);

            case "com.miui.miwallpaper" -> {
                mMiWallpaper.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.weather2" -> {
                mWeather.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.android.deskclock" -> {
                mClock.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.player" -> mMusic.init(lpparam);

            case "com.miui.gallery" -> {
                mGallery.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.aod" -> {
                mAod.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.xiaomi.barrage" -> mBarrage.init(lpparam);

            case "com.xiaomi.aiasst.vision" -> {
                mAiAsst.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.xiaomi.scanner" -> {
                mScanner.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.mishare.connectivity" -> {
                mMiShare.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.misound" -> {
                mMiSound.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.milink.service" -> mMiLink.init(lpparam);

            case "com.miui.guardprovider" -> mGuardProvider.init(lpparam);

            case "com.lbe.security.miui" -> {
                mLbe.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.android.incallui" -> {
                mInCallUi.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.notes" -> {
                mNotes.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.tsmclient" -> {
                mTsmClient.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.contentextension" -> {
                mContentExtension.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.voiceassist" -> {
                mVoiceAssist.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.android.mms" -> {
                mMms.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.android.fileexplorer" -> {
                mFileExplorer.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.android.phone" -> mPhone.init(lpparam);

            case "com.xiaomi.mtb" -> mMtb.init(lpparam);

            case "com.android.externalstorage" -> {
                mExternalStorage.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.android.camera" -> {
                mCamera.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.android.providers.downloads" -> mDownloads.init(lpparam);

            case "com.miui.cloudservice" -> {
                miCloudService.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.creation" -> {
                mCreation.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.backup" -> {
                mBackup.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.miui.huanji" -> {
                mHuanji.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.xiaomi.trustservice" -> {
                mTrustService.init(lpparam);
                mSystemVarious.init(lpparam);
            }
            case "com.hchen.demo" -> {
                mDemo.init(lpparam);
            }
            case "com.xiaomi.NetworkBoost" -> networkBoost.init(lpparam);
            case ProjectApi.mAppModulePkg -> ModuleActiveHook(lpparam);
            default -> mVarious.init(lpparam);
        }
    }

    public void ModuleActiveHook(LoadPackageParam lpparam) {
        Class<?> mHelpers = XposedHelpers.findClassIfExists(ProjectApi.mAppModulePkg + ".utils.Helpers", lpparam.classLoader);

        XposedHelpers.setStaticBooleanField(mHelpers, "isModuleActive", true);
        XposedHelpers.setStaticIntField(mHelpers, "XposedVersion", XposedBridge.getXposedVersion());
        XposedBridge.log("[HyperCeiler][I]: Log level is " + logLevelDesc());
    }


    public boolean isSafeModeEnable(String key) {
        return !mPrefsMap.getBoolean(key);
    }

    public boolean isSystemUIModuleEnable() {
        return isSafeModeEnable("system_ui_safe_mode_enable");
    }

    public boolean isHomeModuleEnable() {
        return isSafeModeEnable("home_safe_mode_enable");
    }

    public boolean isSecurityCenterModuleEnable() {
        return isSafeModeEnable("security_center_safe_mode_enable");
    }

}
