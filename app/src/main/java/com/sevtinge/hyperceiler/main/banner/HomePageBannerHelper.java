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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.main.banner;

import static com.sevtinge.hyperceiler.common.utils.LSPosedScopeHelper.mNotInSelectedScope;
import static com.sevtinge.hyperceiler.hook.utils.PropUtils.getProp;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getBaseOs;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getRomAuthor;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.getSystemVersionIncremental;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.isFullSupport;
import static com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt.scanModules;
import static com.sevtinge.hyperceiler.hook.utils.log.LogManager.IS_LOGGER_ALIVE;
import static com.sevtinge.hyperceiler.hook.utils.shell.ShellUtils.checkRootPermission;

import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.sevtinge.hyperceiler.BuildConfig;
import com.sevtinge.hyperceiler.common.prefs.LayoutPreference;
import com.sevtinge.hyperceiler.core.R;
import com.sevtinge.hyperceiler.expansion.utils.SignUtils;
import com.sevtinge.hyperceiler.hook.utils.devicesdk.ModuleInfo;
import com.sevtinge.hyperceiler.hook.utils.devicesdk.SystemSDKKt;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import kotlin.text.Charsets;

public class HomePageBannerHelper {

    public static void init(Context context, PreferenceCategory preference) {
        new HomePageBannerHelper(context, preference);
    }

    public HomePageBannerHelper(Context context, PreferenceCategory preference) {
        // 优先级由上往下递减，优先级低的会被覆盖执行
        // HyperCeiler
        isFuckCoolapkSDay(context, preference);
        // Birthday
        isBirthday(context, preference);
        // Notice
        isLoggerAlive(context, preference);
        // Warn
        checkWarnings(context, preference);
        // Tip
        isSupportAutoSafeMode(context, preference);
    }

    private void isFuckCoolapkSDay(Context context, PreferenceCategory preference) {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        if (currentMonth == Calendar.JULY && currentDay == 14) {
            preference.addPreference(createBannerPreference(
                context,
                R.layout.headtip_hyperceiler
            ));
        }
    }

    private void isBirthday(Context context, PreferenceCategory preference) {
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        if (currentMonth == Calendar.MAY && currentDay == 1) {
            preference.addPreference(createBannerPreference(
                context,
                R.layout.headtip_hyperceiler_birthday
            ));
        }
    }

    private void isLoggerAlive(Context context, PreferenceCategory preference) {
        if (!IS_LOGGER_ALIVE && !BuildConfig.BUILD_TYPE.equals("release")) {
            preference.addPreference(createBannerPreference(
                context,
                R.layout.headtip_notice
            ));
        }
    }

    private void checkWarnings(Context context, PreferenceCategory preference) {
        boolean isUnofficialRom = getIsUnofficialRom(context);
        boolean isFullSupport = isFullSupport();
        boolean isWhileXposed = isWhileXposed();
        boolean isSignPass = SignUtils.isSignCheckPass(context);

        if (!isSignPass || !isFullSupport || isUnofficialRom) {
            LayoutPreference layoutPreference = (LayoutPreference) createBannerPreference(
                context,
                R.layout.headtip_warn
            );
            TextView titleView = layoutPreference.findViewById(android.R.id.title);
            if (!isSignPass) {
                titleView.setText(R.string.headtip_warn_sign_verification_failed);
            } else if (isUnofficialRom) {
                titleView.setText(R.string.headtip_warn_not_offical_rom);
            } else if (!isWhileXposed) {
                titleView.setText(R.string.headtip_warn_unsupport_xposed);
            } else if (!isFullSupport) {
                titleView.setText(R.string.headtip_warn_unsupport_sysver);
            }
            preference.addPreference(layoutPreference);
        }
    }

    private boolean isWhileXposed() {
        if (checkRootPermission() != 0) return true; // 没 root 就别走校验了
        try {
            List<ModuleInfo> module = scanModules("/data/adb/modules", Charsets.UTF_8);
            String moduleName = module.getFirst().extractName();
            if (moduleName.contains("nolog") || moduleName.contains("日志")) {
                return false;
            }
            return moduleName.contains("LSPosed IT") || moduleName.equals("LSPosed - Irena");
        } catch (Throwable e) {
            AndroidLogUtils.logE("isWhileXposed", e);
            return true;
        }
    }

    public static boolean getIsUnofficialRom(Context context) {
        String baseOs = getBaseOs();
        String romAuthor = getRomAuthor();
        String systemVersion = getSystemVersionIncremental();
        String host = SystemSDKKt.getHost();

        boolean isNotCustomBaseOs = !baseOs.startsWith("V") &&
            !baseOs.startsWith("Xiaomi") &&
            !baseOs.startsWith("Redmi") &&
            !baseOs.startsWith("POCO") &&
            !"null".equals(baseOs);

        boolean hasRomAuthor = !romAuthor.isEmpty();

        boolean isSystemVersionContains = systemVersion.contains("江南") || systemVersion.contains("月色");

        boolean isNotCustomHost = !host.startsWith("pangu-build-component-system") &&
            !host.startsWith("builder-system") &&
            !host.startsWith("non-pangu-pod") &&
            !host.equals("xiaomi.com");

        boolean hasAdvSettings = isAppInstalled(context, "com.baiyang.settings");

        boolean hasBaiyangLicense = !Objects.equals(getProp("ro.system.baiyang.license", ""), "");

        boolean hasCharacteristics = Objects.equals(getProp("ro.kernel.android.checkjni", ""), "0") &&
            Objects.equals(getProp("ro.kernel.checkjni", ""), "0") &&
            Objects.equals(getProp("vendor.bluetooth.startbtlogger", ""), "false") &&
            Objects.equals(getProp("persist.sys.offlinelog.kernel", ""), "false") &&
            (Objects.equals(getProp("persist.sys.offlinelog.bootlog", ""), "false") || Objects.equals(getProp("persist.sys.offlinelog.bootlog", ""), "=false")) &&
            Objects.equals(getProp("sys.miui.ndcd", ""), "off");

        return hasRomAuthor || isSystemVersionContains || Objects.equals(host, "xiaomi.eu") || (isNotCustomBaseOs && isNotCustomHost) || hasAdvSettings || hasBaiyangLicense || hasCharacteristics;
    }

    private static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void isSupportAutoSafeMode(Context context, PreferenceCategory preference) {
        if (mNotInSelectedScope.contains("android")) {
            preference.addPreference(createBannerPreference(
                context,
                R.layout.headtip_tip
            ));
        }
    }

    private Preference createBannerPreference(Context context, int layoutResId) {
        return new LayoutPreference(context, layoutResId);
    }
}
