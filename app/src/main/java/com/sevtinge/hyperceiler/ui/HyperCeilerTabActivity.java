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
package com.sevtinge.hyperceiler.ui;

import static com.sevtinge.hyperceiler.Application.isModuleActivated;
import static com.sevtinge.hyperceiler.common.utils.DialogHelper.showUserAgreeDialog;
import static com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper.mDisableOrHiddenApp;
import static com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper.mUninstallApp;
import static com.sevtinge.hyperceiler.common.utils.PersistConfig.isLunarNewYearThemeView;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Hardware.isTablet;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.prefs.XmlPreference;
import com.sevtinge.hyperceiler.common.utils.CtaUtils;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.common.utils.LanguageHelper;
import com.sevtinge.hyperceiler.common.utils.search.SearchHelper;
import com.sevtinge.hyperceiler.holiday.HolidayHelper;
import com.sevtinge.hyperceiler.libhook.callback.IResult;
import com.sevtinge.hyperceiler.libhook.safecrash.CrashScope;
import com.sevtinge.hyperceiler.libhook.utils.api.BackupUtils;
import com.sevtinge.hyperceiler.libhook.utils.api.ThreadPoolManager;
import com.sevtinge.hyperceiler.libhook.utils.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.utils.pkg.CheckModifyUtils;
import com.sevtinge.hyperceiler.libhook.utils.shell.ShellInit;
import com.sevtinge.hyperceiler.main.NaviBaseActivity;
import com.sevtinge.hyperceiler.main.fragment.DetailFragment;
import com.sevtinge.hyperceiler.utils.LogServiceUtils;
import com.sevtinge.hyperceiler.utils.NoticeProcessor;
import com.sevtinge.hyperceiler.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import fan.appcompat.app.AlertDialog;
import fan.navigator.Navigator;
import fan.navigator.NavigatorFragmentListener;
import fan.navigator.navigatorinfo.UpdateDetailFragmentNavInfo;
import fan.preference.PreferenceFragment;

public class HyperCeilerTabActivity extends NaviBaseActivity
    implements PreferenceFragment.OnPreferenceStartFragmentCallback, IResult {

    private static final String TAG = "HyperCeilerTab";
    private static final List<String> CHECK_LIST = List.of(
        "com.miui.securitycenter",
        "com.android.camera",
        "com.miui.home"
    );

    private static final Map<String, Integer> APP_NAME_RES_MAP;
    static {
        APP_NAME_RES_MAP = Map.of(
            "com.android.systemui", R.string.system_ui,
            "com.android.settings", R.string.system_settings,
            "com.miui.home", R.string.mihome,
            "com.hchen.demo", R.string.demo,
            "com.miui.securitycenter", R.string.security_center_hyperos
        );
    }

    private Handler mHandler;
    private volatile List<String> appCrash = Collections.emptyList();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final boolean restored = (savedInstanceState != null);
        final android.content.Context appCtx = getApplicationContext();

        mHandler = new Handler(Looper.getMainLooper());

        applyGrayScaleFilter(this);
        HolidayHelper.init(this);
        LanguageHelper.init(this);
        PermissionUtils.init(this);
        ShellInit.init(this);
        LogServiceUtils.init(appCtx);

        CHECK_LIST.parallelStream().forEach(this::checkAppMod);

        try {
            SearchHelper.init(appCtx, restored);
        } catch (Throwable t) {
            AndroidLog.e(TAG, "SearchHelper: " + t);
        }

        List<String> computedAppCrash = computeCrashList();

        new Thread(() -> {
            NoticeProcessor.NoticeResult result = NoticeProcessor.process(this);
            if (result != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    NoticeProcessor.showNoticeDialog(this, result);
                });
            }
        }).start();

        mHandler.post(() -> {
            appCrash = computedAppCrash;
            mHandler.postDelayed(this::showSafeModeDialogIfNeeded, 600);
            if (!isModuleActivated) DialogHelper.showXposedActivateDialog(this);
            requestCta();
        });
    }

    private List<String> computeCrashList() {
        try {
            List<?> raw = CrashScope.getCrashingPackages();
            if (raw.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> result = new ArrayList<>(raw.size());
            for (Object o : raw) {
                if (o instanceof String s) {
                    result.add(s);
                } else if (o != null) {
                    result.add(o.toString());
                }
            }
            return result;
        } catch (Throwable t) {
            AndroidLog.e(TAG, "CrashData: " + t);
            return Collections.emptyList();
        }
    }

    private void checkAppMod(String pkg) {
        boolean check = CheckModifyUtils.INSTANCE.isApkModified(this, pkg, CheckModifyUtils.XIAOMI_SIGNATURE);
        CheckModifyUtils.INSTANCE.setCheckResult(pkg, check);
    }

    @SuppressLint("StringFormatInvalid")
    private void showSafeModeDialogIfNeeded() {
        if (appCrash.isEmpty()) return;

        String appName = buildCrashAppNames();
        if (appName.isEmpty()) return;

        String msg = cleanUpMessage(getString(R.string.safe_mode_later_desc, appName));
        DialogHelper.showSafeModeDialog(this, msg);
    }

    private String buildCrashAppNames() {
        StringJoiner joiner = new StringJoiner(", ");
        for (String pkg : appCrash) {
            Integer resId = APP_NAME_RES_MAP.get(pkg);
            if (resId != null) {
                joiner.add(getString(resId) + " (" + pkg + ")");
            }
        }
        return joiner.toString();
    }

    private static String cleanUpMessage(String msg) {
        if (msg == null || msg.isEmpty()) return "";

        StringBuilder sb = new StringBuilder(msg.length());
        char prev = 0;
        for (int i = 0; i < msg.length(); i++) {
            char c = msg.charAt(i);
            if (c == '[' || c == ']') continue;
            if (c == ' ' && prev == ' ') continue;
            if (c == ' ' && (prev == '，' || prev == '、')) continue;
            sb.append(c);
            prev = c;
        }
        return sb.toString().trim();
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
            Navigator.get(caller).getNavigationMode() == Navigator.Mode.NLC && isTablet()) {
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
            onStartSubSettingsForArguments(this, pref, false);
        }
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        requestCta();
    }

    private void requestCta() {
        if (CtaUtils.isCtaNeedShow(this)) {
            if (CtaUtils.isCtaBypass()) {
                ActivityResultLauncher<Intent> ctaLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result != null) {
                            if (result.getResultCode() != 1) {
                                finishAffinity();
                                System.exit(0);
                            }
                            CtaUtils.setCtaValue(getApplicationContext(), result.getResultCode() == 1);
                        }
                    }
                );
                CtaUtils.showCtaDialog(ctaLauncher, this);
            } else {
                showUserAgreeDialog(this);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        try {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            switch (requestCode) {
                case BackupUtils.CREATE_DOCUMENT_CODE -> {
                    BackupUtils.handleCreateDocument(this, data.getData());
                    alert.setTitle(com.sevtinge.hyperceiler.core.R.string.backup_success);
                }
                case BackupUtils.OPEN_DOCUMENT_CODE -> {
                    BackupUtils.handleReadDocument(this, data.getData());
                    alert.setTitle(com.sevtinge.hyperceiler.core.R.string.rest_success);
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
                case BackupUtils.CREATE_DOCUMENT_CODE -> alert.setTitle(com.sevtinge.hyperceiler.core.R.string.backup_failed);
                case BackupUtils.OPEN_DOCUMENT_CODE -> alert.setTitle(com.sevtinge.hyperceiler.core.R.string.rest_failed);
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
        ShellInit.destroy();
        ThreadPoolManager.shutdown();
        mUninstallApp.clear();
        mDisableOrHiddenApp.clear();
    }
}
