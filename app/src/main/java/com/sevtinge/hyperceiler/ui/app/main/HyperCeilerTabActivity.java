package com.sevtinge.hyperceiler.ui.app.main;

import static com.sevtinge.hyperceiler.safe.CrashHandlerDialog.CrashHandlerBroadcastReceiver.CRASH_HANDLER;
import static com.sevtinge.hyperceiler.ui.app.main.utils.PersistConfig.isLunarNewYearThemeView;
import static com.sevtinge.hyperceiler.ui.app.main.utils.PersistConfig.isNeedGrayView;
import static com.sevtinge.hyperceiler.utils.XposedActivateHelper.isModuleActive;
import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.isTablet;
import static com.sevtinge.hyperceiler.utils.log.LogManager.IS_LOGGER_ALIVE;
import static com.sevtinge.hyperceiler.utils.log.LogManager.isLoggerAlive;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.callback.IResult;
import com.sevtinge.hyperceiler.module.base.tool.AppsTool;
import com.sevtinge.hyperceiler.prefs.PreferenceHeader;
import com.sevtinge.hyperceiler.prefs.XmlPreference;
import com.sevtinge.hyperceiler.safe.CrashHandlerDialog;
import com.sevtinge.hyperceiler.ui.app.holiday.HolidayHelper;
import com.sevtinge.hyperceiler.ui.app.main.utils.LanguageHelper;
import com.sevtinge.hyperceiler.safe.CrashData;
import com.sevtinge.hyperceiler.ui.base.NaviBaseActivity;
import com.sevtinge.hyperceiler.ui.base.SubSettings;
import com.sevtinge.hyperceiler.utils.BackupUtils;
import com.sevtinge.hyperceiler.utils.DialogHelper;
import com.sevtinge.hyperceiler.utils.PermissionUtils;
import com.sevtinge.hyperceiler.utils.PropUtils;
import com.sevtinge.hyperceiler.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.utils.XposedActivateHelper;
import com.sevtinge.hyperceiler.utils.api.ProjectApi;
import com.sevtinge.hyperceiler.utils.prefs.PrefsUtils;
import com.sevtinge.hyperceiler.utils.search.SearchHelper;
import com.sevtinge.hyperceiler.utils.shell.ShellInit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import fan.appcompat.app.AlertDialog;
import fan.navigator.Navigator;
import fan.navigator.NavigatorFragmentListener;
import fan.navigator.navigatorinfo.UpdateDetailFragmentNavInfo;
import fan.preference.PreferenceFragment;

public class HyperCeilerTabActivity extends NaviBaseActivity
    implements PreferenceFragment.OnPreferenceStartFragmentCallback, IResult {

    private Handler handler;
    private Context context;
    private CrashHandlerDialog.CrashHandlerBroadcastReceiver mCrashHandlerBroadcastReceiver;
    private ArrayList<String> appCrash = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        IS_LOGGER_ALIVE = isLoggerAlive();
        if (isNeedGrayView) {
            applyGrayScaleFilter();
        }

        if (isLunarNewYearThemeView) {
            new HolidayHelper(this);
        }

        mCrashHandlerBroadcastReceiver = new CrashHandlerDialog.CrashHandlerBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter(CRASH_HANDLER);
        registerReceiver(mCrashHandlerBroadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);

        SharedPreferences mPrefs = PrefsUtils.mSharedPreferences;
        String languageSetting = mPrefs.getString("prefs_key_settings_app_language", "-1");
        if (!"-1".equals(languageSetting)) {
            LanguageHelper.setIndexLanguage(this, Integer.parseInt(languageSetting), false);
        }

        handler = new Handler(this.getMainLooper());
        context = this;
        requestPermissions();

        int logLevel = Integer.parseInt(mPrefs.getString("prefs_key_log_level", "3"));
        super.onCreate(savedInstanceState);
        ThreadPoolManager.getInstance().submit(() -> SearchHelper.getAllMods(context, savedInstanceState != null));

        XposedActivateHelper.checkActivateState(this);

        if (shouldShowLogServiceWarnDialog()) {
            handler.post(() -> DialogHelper.showLogServiceWarnDialog(context));
        }

        ShellInit.init(this);
        int effectiveLogLevel = ProjectApi.isCanary() ? (logLevel != 3 && logLevel != 4 ? 3 : logLevel) : logLevel;
        PropUtils.setProp("persist.hyperceiler.log.level", effectiveLogLevel);

        appCrash = CrashData.toPkgList();
        handler.postDelayed(this::showSafeModeDialogIfNeeded, 600);
    }

    private void applyGrayScaleFilter() {
        View decorView = getWindow().getDecorView();
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        decorView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    private boolean shouldShowLogServiceWarnDialog() {
        return !IS_LOGGER_ALIVE && isModuleActive && BuildConfig.BUILD_TYPE != "release" && !PrefsUtils.mSharedPreferences.getBoolean("prefs_key_development_close_log_alert_dialog", false);
    }

    @SuppressLint("StringFormatInvalid")
    private void showSafeModeDialogIfNeeded() {
        if (haveCrashReport()) {
            Map<String, String> appNameMap = createAppNameMap();
            ArrayList<String> appList = getAppListWithCrashReports(appNameMap);
            String appName = String.join(", ", appList);
            String msg = getString(R.string.safe_mode_later_desc, appName);
            msg = cleanUpMessage(msg);
            DialogHelper.showSafeModeDialog(context, msg);
        }
    }

    private Map<String, String> createAppNameMap() {
        Map<String, String> appNameMap = new HashMap<>();
        appNameMap.put("com.android.systemui", getString(R.string.system_ui));
        appNameMap.put("com.android.settings", getString(R.string.system_settings));
        appNameMap.put("com.miui.home", getString(R.string.mihome));
        appNameMap.put("com.hchen.demo", getString(R.string.demo));
        appNameMap.put("com.miui.securitycenter", getString(R.string.security_center_hyperos));

        return appNameMap;
    }

    private ArrayList<String> getAppListWithCrashReports(Map<String, String> appNameMap) {
        ArrayList<String> appList = new ArrayList<>();
        for (String pkg : appCrash) {
            if (appNameMap.containsKey(pkg)) {
                appList.add(appNameMap.get(pkg) + " (" + pkg + ")");
            }
        }
        return appList;
    }

    private String cleanUpMessage(String msg) {
        msg = msg.replace("  ", " ");
        msg = msg.replace("， ", "，");
        msg = msg.replace("、 ", "、");
        msg = msg.replace("[", "");
        msg = msg.replace("]", "");
        msg = msg.trim();
        return msg;
    }

    private boolean haveCrashReport() {
        return !appCrash.isEmpty();
    }

    @Override
    public void error(String reason) {
        handler.post(() -> DialogHelper.showNoRootPermissionDialog(this));
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        if (caller instanceof NavigatorFragmentListener &&
            Navigator.get(caller).getNavigationMode() == Navigator.Mode.NLC &&
            isTablet()) {
            Bundle args = new Bundle();
            Bundle savedInstanceState = new Bundle();
            if (pref instanceof XmlPreference xmlPreference) {
                args.putInt(":settings:fragment_resId", xmlPreference.getInflatedXml());
                savedInstanceState.putInt(":settings:fragment_resId", xmlPreference.getInflatedXml());
            } else {
                Intent intent = pref.getIntent();
                if (intent != null) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        String xmlPath = bundle.getString("inflatedXml");
                        if (!TextUtils.isEmpty(xmlPath)) {
                            String[] split = xmlPath.split("/");
                            String[] split2 = split[2].split("\\.");
                            if (split.length == 3) {
                                args.putInt(":settings:fragment_resId", getResources().getIdentifier(split2[0], split[1], getPackageName()));
                                savedInstanceState.putInt(":settings:fragment_resId", getResources().getIdentifier(split2[0], split[1], getPackageName()));
                            }
                        }
                    }
                }
            }

            String mFragmentName = pref.getFragment();
            savedInstanceState.putString("FragmentName", mFragmentName);
            Navigator.get(caller).navigate(new UpdateDetailFragmentNavInfo(-1, DetailFragment.class, savedInstanceState));
        } else {
            mProxy.onStartSettingsForArguments(SubSettings.class, pref, false);
        }
        return true;
    }

    /*@Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        requestCta();
    }

    private void requestCta() {
        if (!CtaUtils.isCtaEnabled(this)) {
            CtaUtils.showCtaDialog(this, REQUEST_CODE);
        }
    }

    public void test() {
        boolean ls = shellExec.append("ls").sync().isResult();
        AndroidLogUtils.LogI(ITAG.TAG, "ls: " + ls);
        AndroidLogUtils.LogI(ITAG.TAG, shellExec.getOutPut().toString() + shellExec.getError().toString());
        boolean f = shellExec.append("for i in $(seq 1 500); do echo $i; done").isResult();
        AndroidLogUtils.LogI(ITAG.TAG, "for: " + f);
        AndroidLogUtils.LogI(ITAG.TAG, shellExec.getOutPut().toString());
        boolean k = shellExec.append("for i in $(seq 1 500); do echo $i; done").sync().isResult();
        AndroidLogUtils.LogI(ITAG.TAG, "fork: " + k);
        AndroidLogUtils.LogI(ITAG.TAG, shellExec.getOutPut().toString());
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            switch (requestCode) {
                case BackupUtils.CREATE_DOCUMENT_CODE -> {
                    BackupUtils.handleCreateDocument(this, data.getData());
                    alert.setTitle(R.string.backup_success);
                }
                case BackupUtils.OPEN_DOCUMENT_CODE -> {
                    BackupUtils.handleReadDocument(this, data.getData());
                    alert.setTitle(R.string.rest_success);
                }
                default -> {
                    return;
                }
            }
            alert.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            });
            alert.show();
        } catch (Exception e) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            switch (requestCode) {
                case BackupUtils.CREATE_DOCUMENT_CODE -> alert.setTitle(R.string.backup_failed);
                case BackupUtils.OPEN_DOCUMENT_CODE -> alert.setTitle(R.string.rest_failed);
            }
            alert.setMessage(e.toString());
            alert.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            });
            alert.show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isLunarNewYearThemeView) {
            HolidayHelper.resumeAnimation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isLunarNewYearThemeView) {
            HolidayHelper.pauseAnimation();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mCrashHandlerBroadcastReceiver);
        ShellInit.destroy();
        ThreadPoolManager.shutdown();
        PreferenceHeader.mUninstallApp.clear();
        PreferenceHeader.mDisableOrHiddenApp.clear();
    }

    // 权限申请
    public void requestPermissions() {
        PermissionUtils.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1,
            // 实现接口方法
            new PermissionUtils.OnPermissionListener() {
                @Override
                public void onPermissionGranted(Context context) {
                    // 获取权限成功
                }

                @Override
                public void onPermissionDenied() {
                    // 获取权限失败
                    finish();
                }
            }
        );
    }
}
