package com.sevtinge.hyperceiler.ui;

import static com.sevtinge.hyperceiler.common.utils.PersistConfig.isLunarNewYearThemeView;
import static com.sevtinge.hyperceiler.common.utils.PersistConfig.isNeedGrayView;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.isTablet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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

import com.sevtinge.hyperceiler.common.prefs.PreferenceHeader;
import com.sevtinge.hyperceiler.common.prefs.XmlPreference;
import com.sevtinge.hyperceiler.utils.PermissionUtils;
import com.sevtinge.hyperceiler.dashboard.SubSettings;
import com.sevtinge.hyperceiler.hook.callback.IResult;
import com.sevtinge.hyperceiler.hook.utils.log.LogManager;
import com.sevtinge.hyperceiler.safemode.CrashHandlerReceiver;
import com.sevtinge.hyperceiler.ui.holiday.HolidayHelper;
import com.sevtinge.hyperceiler.common.utils.LanguageHelper;
import com.sevtinge.hyperceiler.hook.safe.CrashData;
import com.sevtinge.hyperceiler.hook.utils.BackupUtils;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.hook.utils.ThreadPoolManager;
import com.sevtinge.hyperceiler.utils.LogServiceUtils;
import com.sevtinge.hyperceiler.utils.XposedActivateHelper;
import com.sevtinge.hyperceiler.common.utils.search.SearchHelper;
import com.sevtinge.hyperceiler.hook.utils.shell.ShellInit;

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

    private Handler mHandler;
    private ArrayList<String> appCrash = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mHandler = new Handler(getMainLooper());
        LogManager.init();
        if (isNeedGrayView) {
            applyGrayScaleFilter();
        }
        HolidayHelper.init(this);
        CrashHandlerReceiver.register(this);
        LanguageHelper.init(this);
        PermissionUtils.init(this);
        super.onCreate(savedInstanceState);
        SearchHelper.init(this, savedInstanceState != null);
        XposedActivateHelper.init(this);
        ShellInit.init(this);
        LogServiceUtils.init(this);
        LogManager.setLogLevel();

        appCrash = CrashData.toPkgList();
        mHandler.postDelayed(this::showSafeModeDialogIfNeeded, 600);
    }

    private void applyGrayScaleFilter() {
        View decorView = getWindow().getDecorView();
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        decorView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    @SuppressLint("StringFormatInvalid")
    private void showSafeModeDialogIfNeeded() {
        if (haveCrashReport()) {
            Map<String, String> appNameMap = createAppNameMap();
            ArrayList<String> appList = getAppListWithCrashReports(appNameMap);
            String appName = String.join(", ", appList);
            String msg = getString(com.sevtinge.hyperceiler.ui.R.string.safe_mode_later_desc, appName);
            msg = cleanUpMessage(msg);
            DialogHelper.showSafeModeDialog(this, msg);
        }
    }

    private Map<String, String> createAppNameMap() {
        Map<String, String> appNameMap = new HashMap<>();
        appNameMap.put("com.android.systemui", getString(com.sevtinge.hyperceiler.ui.R.string.system_ui));
        appNameMap.put("com.android.settings", getString(com.sevtinge.hyperceiler.ui.R.string.system_settings));
        appNameMap.put("com.miui.home", getString(com.sevtinge.hyperceiler.ui.R.string.mihome));
        appNameMap.put("com.hchen.demo", getString(com.sevtinge.hyperceiler.ui.R.string.demo));
        appNameMap.put("com.miui.securitycenter", getString(com.sevtinge.hyperceiler.ui.R.string.security_center_hyperos));

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
        mHandler.post(() -> DialogHelper.showNoRootPermissionDialog(this));
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
            CtaUtils.showCtaDialog(this, 10001);
        }
    }*/

    /*public void test() {
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
                    alert.setTitle(com.sevtinge.hyperceiler.ui.R.string.backup_success);
                }
                case BackupUtils.OPEN_DOCUMENT_CODE -> {
                    BackupUtils.handleReadDocument(this, data.getData());
                    alert.setTitle(com.sevtinge.hyperceiler.ui.R.string.rest_success);
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
                case BackupUtils.CREATE_DOCUMENT_CODE -> alert.setTitle(com.sevtinge.hyperceiler.ui.R.string.backup_failed);
                case BackupUtils.OPEN_DOCUMENT_CODE -> alert.setTitle(com.sevtinge.hyperceiler.ui.R.string.rest_failed);
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
        CrashHandlerReceiver.unregister(this);
        ShellInit.destroy();
        ThreadPoolManager.shutdown();
        PreferenceHeader.mUninstallApp.clear();
        PreferenceHeader.mDisableOrHiddenApp.clear();
    }
}
