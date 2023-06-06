package com.sevtinge.cemiuiler.module.base;

import com.sevtinge.cemiuiler.BuildConfig;
import com.sevtinge.cemiuiler.module.AiAsst;
import com.sevtinge.cemiuiler.module.Aireco;
import com.sevtinge.cemiuiler.module.Aod;
import com.sevtinge.cemiuiler.module.Browser;
import com.sevtinge.cemiuiler.module.Camera;
import com.sevtinge.cemiuiler.module.Clock;
import com.sevtinge.cemiuiler.module.ContentExtension;
import com.sevtinge.cemiuiler.module.Downloads;
import com.sevtinge.cemiuiler.module.ExternalStorage;
import com.sevtinge.cemiuiler.module.FileExplorer;
import com.sevtinge.cemiuiler.module.Gallery;
import com.sevtinge.cemiuiler.module.GuardProvider;
import com.sevtinge.cemiuiler.module.Home;
import com.sevtinge.cemiuiler.module.InCallUi;
import com.sevtinge.cemiuiler.module.InputSettings;
import com.sevtinge.cemiuiler.module.Joyose;
import com.sevtinge.cemiuiler.module.Lbe;
import com.sevtinge.cemiuiler.module.Market;
import com.sevtinge.cemiuiler.module.MediaEditor;
import com.sevtinge.cemiuiler.module.MiLink;
import com.sevtinge.cemiuiler.module.MiSettings;
import com.sevtinge.cemiuiler.module.MiShare;
import com.sevtinge.cemiuiler.module.MiWallpaper;
import com.sevtinge.cemiuiler.module.Mms;
import com.sevtinge.cemiuiler.module.Mtb;
import com.sevtinge.cemiuiler.module.Music;
import com.sevtinge.cemiuiler.module.PackageInstaller;
import com.sevtinge.cemiuiler.module.PersonalAssistant;
import com.sevtinge.cemiuiler.module.Phone;
import com.sevtinge.cemiuiler.module.PowerKeeper;
import com.sevtinge.cemiuiler.module.Scanner;
import com.sevtinge.cemiuiler.module.ScreenRecorder;
import com.sevtinge.cemiuiler.module.ScreenShot;
import com.sevtinge.cemiuiler.module.SecurityCenter;
import com.sevtinge.cemiuiler.module.Settings;
import com.sevtinge.cemiuiler.module.SystemFramework;
import com.sevtinge.cemiuiler.module.SystemSettings;
import com.sevtinge.cemiuiler.module.SystemUI;
import com.sevtinge.cemiuiler.module.ThemeManager;
import com.sevtinge.cemiuiler.module.TsmClient;
import com.sevtinge.cemiuiler.module.Updater;
import com.sevtinge.cemiuiler.module.Various;
import com.sevtinge.cemiuiler.module.VoiceAssist;
import com.sevtinge.cemiuiler.module.Weather;
import com.sevtinge.cemiuiler.utils.Helpers;
import com.sevtinge.cemiuiler.utils.LogUtils;
import com.sevtinge.cemiuiler.utils.PrefsMap;
import com.sevtinge.cemiuiler.utils.PrefsUtils;
import com.sevtinge.cemiuiler.utils.ResourcesHook;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.io.File;
import java.util.Map;

public abstract class BaseXposedInit implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    public static ResourcesHook mResHook;
    public static String mModulePath = null;
    public static PrefsMap<String, Object> mPrefsMap = new PrefsMap<String, Object>();

    public SystemFramework mSystemFramework = new SystemFramework();
    //public SystemFrameworkForCorepatch mSystemFrameworkForCorepatch = new SystemFrameworkForCorepatch();
    public SystemUI mSystemUI = new SystemUI();
    public Home mHome = new Home();
    public ScreenShot mScreenShot = new ScreenShot();

    public ScreenRecorder mScreenRecorder = new ScreenRecorder();
    public SecurityCenter mSecurityCenter = new SecurityCenter();
    public SystemSettings mSystemSettings = new SystemSettings();
    public Settings mSettings = new Settings();
    public PersonalAssistant mPersonalAssistant = new PersonalAssistant();
    public ThemeManager mThemeManager = new ThemeManager();
    public Updater mUpdater = new Updater();
    public Market mMarket = new Market();
    public MediaEditor mMediaEditor = new MediaEditor();
    public PackageInstaller mPackageInstaller = new PackageInstaller();
    public PowerKeeper mPowerKeeper = new PowerKeeper();
    public MiSettings mMiSettings = new MiSettings();
    public Joyose mJoyose = new Joyose();
    public Various mVarious = new Various();
    public Weather mWeather = new Weather();
    public Clock mClock = new Clock();
    public FileExplorer mFileExplorer = new FileExplorer();
    public Music mMusic = new Music();
    public Gallery mGallery = new Gallery();
    public Aireco mAireco = new Aireco();
    public AiAsst mAiAsst = new AiAsst();
    public Scanner mScanner = new Scanner();
    public MiShare mMiShare = new MiShare();
    public MiLink mMiLink = new MiLink();
    public GuardProvider mGuardProvider = new GuardProvider();
    public Lbe mLbe = new Lbe();
    public InCallUi mInCallUi = new InCallUi();
    public InputSettings mInputSettings = new InputSettings();
    public TsmClient mTsmClient = new TsmClient();
    public ContentExtension mContentExtension = new ContentExtension();
    public VoiceAssist mVoiceAssist = new VoiceAssist();
    public Mms mMms = new Mms();
    public ExternalStorage mExternalStorage = new ExternalStorage();
    public Camera mCamera = new Camera();
    public Browser mBrowser = new Browser();
    public Mtb mMtb = new Mtb();
    public Phone mPhone = new Phone();
    public MiWallpaper mMiWallpaper = new MiWallpaper();
    public Downloads mDownloads = new Downloads();
    public Aod mAod = new Aod();
    //public SystemSettings mSystemSettings = new SystemSettings();
    /*public void init(BaseModule... baseModules) {
        mPkgName = mLoadPackageParam.packageName;
        for (BaseModule app : baseModules) {
            String packageName = app.getAppPackageName();
            String mSimpleName = app.getClass().getSimpleName();
            if (TextUtils.isEmpty(packageName) && mSimpleName.equals("Various")) {
                app.init(mLoadPackageParam, true);
            } else {
                if (mPkgName.equals(packageName)) {
                    app.init(mLoadPackageParam, false);
                }
            }
        }
    }*/

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        setXSharedPrefs();
        mResHook = new ResourcesHook();
        mModulePath = startupParam.modulePath;
    }

    public void setXSharedPrefs() {
        if (mPrefsMap.size() == 0) {
            XSharedPreferences mXSharedPreferences = null;
            try {
                if (XposedBridge.getXposedVersion() >= 93) {
                    mXSharedPreferences = new XSharedPreferences(Helpers.mAppModulePkg, PrefsUtils.mPrefsName);
                } else {
                    mXSharedPreferences = new XSharedPreferences(new File(PrefsUtils.mPrefsFile));
                }
                mXSharedPreferences.makeWorldReadable();
            } catch (Throwable t) {
                XposedBridge.log(t);
            }

            Map<String, ?> allPrefs = mXSharedPreferences == null ? null : mXSharedPreferences.getAll();
            if (allPrefs == null || allPrefs.size() == 0) {
                LogUtils.log("[UID " + android.os.Process.myUid() + "] Cannot read module's SharedPreferences, some mods might not work!");
            } else {
                mPrefsMap.putAll(allPrefs);
            }
        }
    }

    public void init(LoadPackageParam lpparam) {
        String packageName = lpparam.packageName;
        XposedBridge.log("Cemiuiler: packageName " + packageName);
        //XposedBridge.log("Cemiuiler: R=" + Build.VERSION_CODES.R + " S=" + Build.VERSION_CODES.S + " T=" + Build.VERSION_CODES.TIRAMISU + " This=" + Build.VERSION.SDK_INT);
        switch (packageName) {
            case "android":
                XposedBridge.log("Cemiuiler: Hook android");
                mSystemFramework.init(lpparam);
                mVarious.init(lpparam);
                //mSystemFrameworkForCorepatch.init(lpparam);
                break;

            case "com.android.systemui":
                if (isSystemUIModuleEnable()) {
                    XposedBridge.log("Cemiuiler: Hook com.android.systemui");
                    //ALPermissionManager.RootCommand(android.content.ContextWrapper.getPackageCodePath());
                    mSystemUI.init(lpparam);
                    mVarious.init(lpparam);
                    //mSystemUIPlugin.init(lpparam);
                }
                break;

            case "com.miui.home":
                if (isHomeModuleEnable()) {
                    XposedBridge.log("Cemiuiler: Hook com.miui.home");
                    mHome.init(lpparam);
                    mVarious.init(lpparam);
                }
                break;

            case "com.miui.securitycenter":
                if (isSecurityCenterModuleEnable()) {
                    XposedBridge.log("Cemiuiler: Hook com.miui.securitycenter");
                    mSecurityCenter.init(lpparam);
                    mVarious.init(lpparam);
                }
                break;

            case "com.android.settings":
                XposedBridge.log("Cemiuiler: Hook com.android.settings");
                mSystemSettings.init(lpparam);
                mSettings.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.personalassistant":
                XposedBridge.log("Cemiuiler: Hook com.miui.personalassistant");
                mPersonalAssistant.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.android.thememanager":
                XposedBridge.log("Cemiuiler: Hook com.android.thememanager");
                mThemeManager.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.android.browser":
                XposedBridge.log("Cemiuiler: Hook com.android.browse");
                mBrowser.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.android.updater":
                XposedBridge.log("Cemiuiler: Hook com.android.updater");
                mUpdater.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.xiaomi.market":
                XposedBridge.log("Cemiuiler: Hook com.xiaomi.market");
                mMarket.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.packageinstaller":
                XposedBridge.log("Cemiuiler: Hook com.miui.packageinstaller");
                mPackageInstaller.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.powerkeeper":
                XposedBridge.log("Cemiuiler: Hook com.miui.powerkeeper");
                mPowerKeeper.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.xiaomi.misettings":
                XposedBridge.log("Cemiuiler: Hook com.xiaomi.misettings");
                mMiSettings.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.xiaomi.joyose":
                XposedBridge.log("Cemiuiler: Hook com.xiaomi.joyose");
                mJoyose.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.screenshot":
                XposedBridge.log("Cemiuiler: Hook com.miui.screenshot");
                mScreenShot.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.screenrecorder":
                XposedBridge.log("Cemiuiler: Hook com.miui.screenrecorder");
                mScreenRecorder.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.mediaeditor":
                XposedBridge.log("Cemiuiler: Hook com.miui.mediaeditor");
                mMediaEditor.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.miwallpaper":
                XposedBridge.log("Cemiuiler: Hook com.miui.miwallpaper");
                mMiWallpaper.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.weather2":
                XposedBridge.log("Cemiuiler: Hook com.miui.weather2");
                mWeather.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.android.deskclock":
                XposedBridge.log("Cemiuiler: Hook com.android.deskclock");
                mClock.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.player":
                XposedBridge.log("Cemiuiler: Hook com.miui.player");
                mMusic.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.gallery":
                XposedBridge.log("Cemiuiler: Hook com.miui.gallery");
                mGallery.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.aod":
                XposedBridge.log("Cemiuiler: Hook com.miui.aod");
                mAod.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.xiaomi.aireco":
                XposedBridge.log("Cemiuiler: Hook com.xiaomi.aireco");
                mAireco.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.xiaomi.aiasst.vision":
                XposedBridge.log("Cemiuiler: Hook com.xiaomi.aiasst.vision");
                mAiAsst.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.xiaomi.scanner":
                XposedBridge.log("Cemiuiler: Hook com.xiaomi.scanner");
                mScanner.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.miinput":
                XposedBridge.log("Cemiuiler: Hook com.miui.miinput");
                mInputSettings.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.mishare.connectivity":
                XposedBridge.log("Cemiuiler: Hook com.miui.mishare.connectivity");
                mMiShare.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.milink.service":
                XposedBridge.log("Cemiuiler: Hook com.milink.service");
                mMiLink.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.guardprovider":
                XposedBridge.log("Cemiuiler: Hook com.miui.guardprovider");
                mGuardProvider.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.lbe.security.miui":
                XposedBridge.log("Cemiuiler: Hook com.lbe.security.miui");
                mLbe.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.android.incallui":
                XposedBridge.log("Cemiuiler: Hook com.android.incallui");
                mInCallUi.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.tsmclient":
                XposedBridge.log("Cemiuiler: Hook com.miui.tsmclient");
                mTsmClient.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.contentextension":
                XposedBridge.log("Cemiuiler: Hook com.miui.contentextension");
                mContentExtension.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.miui.voiceassist":
                XposedBridge.log("Cemiuiler: Hook com.miui.voiceassist");
                mVoiceAssist.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.android.mms":
                XposedBridge.log("Cemiuiler: Hook com.android.mms");
                mMms.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.android.fileexplorer":
                XposedBridge.log("Cemiuiler: Hook com.android.fileexplorer");
                mFileExplorer.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.android.phone":
                XposedBridge.log("Cemiuiler: Hook com.android.phone");
                mPhone.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.xiaomi.mtb":
                XposedBridge.log("Cemiuiler: Hook com.xiaomi.mtb");
                mMtb.init(lpparam);
                break;

            case "com.android.externalstorage":
                XposedBridge.log("Cemiuiler: Hook com.android.externalstorage");
                mExternalStorage.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.android.camera":
                XposedBridge.log("Cemiuiler: Hook com.android.camera");
                mCamera.init(lpparam);
                mVarious.init(lpparam);
                break;

            case "com.android.providers.downloads":
                XposedBridge.log("Cemiuiler: Hook com.android.providers.downloads");
                mDownloads.init(lpparam);
                mVarious.init(lpparam);
                break;

            case BuildConfig.APPLICATION_ID:
                ModuleActiveHook(lpparam);
                break;

            default:
                mVarious.init(lpparam);
                break;
        }
    }

    public void ModuleActiveHook(LoadPackageParam lpparam) {
        Class<?> mHelpers = XposedHelpers.findClassIfExists(Helpers.mAppModulePkg + ".utils.Helpers", lpparam.classLoader);

        XposedHelpers.setStaticBooleanField(mHelpers, "isModuleActive", true);
        XposedHelpers.setStaticIntField(mHelpers, "XposedVersion", XposedBridge.getXposedVersion());
    }


    public boolean isSafeModeEnable(String key) {
        return mPrefsMap.getBoolean(key);
    }

    public boolean isSystemUIModuleEnable() {
        return !isSafeModeEnable("system_ui_safe_mode_enable");
    }

    public boolean isHomeModuleEnable() {
        return !isSafeModeEnable("home_safe_mode_enable");
    }

    public boolean isSecurityCenterModuleEnable() {
        return !isSafeModeEnable("security_center_safe_mode_enable");
    }

}
