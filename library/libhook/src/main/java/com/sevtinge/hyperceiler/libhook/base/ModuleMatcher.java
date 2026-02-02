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
package com.sevtinge.hyperceiler.libhook.base;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getAndroidVersion;
import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.getHyperOSVersion;

import com.sevtinge.hyperceiler.libhook.utils.api.PropUtils;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.pkg.CheckModifyUtils;
import com.sevtinge.hyperceiler.libhook.utils.pkg.DebugModeUtils;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * 模块加载条件匹配器
 *
 * @author HyperCeiler
 */
public class ModuleMatcher {

    private static final String TAG = "ModuleMatcher";

    public static final String VARIOUS_THIRD_APPS = "VariousThirdApps";
    public static final String VARIOUS_SYSTEM_APPS = "VariousSystemApps";

    // 设备类型常量
    public static final int DEVICE_ALL = 0;
    public static final int DEVICE_PAD_ONLY = 1;
    public static final int DEVICE_PHONE_ONLY = 2;
    private static final String PROP_SAFE_MODE = "persist.service.hyperceiler.crash.report";

    private static final Map<String, String> SAFE_MODE_ALIAS = Map.of(
        "com.android.systemui", "systemui",
        "com.miui.home", "home",
        "com.android.settings", "settings",
        "com.miui.securitycenter", "securitycenter"
    );

    private static final Map<String, String> SAFE_MODE_CONFIG = Map.of(
        "com.android.systemui", "system_ui_safe_mode_enable",
        "com.miui.home", "home_safe_mode_enable",
        "com.android.settings", "settings_safe_mode_enable",
        "com.miui.securitycenter", "security_center_safe_mode_enable"
    );

    private static final Set<String> SECURITY_CHECK_PACKAGES = Set.of(
        "com.miui.securitycenter",
        "com.android.camera",
        "com.miui.home"
    );

    private final MatchContext context;

    public ModuleMatcher(MatchContext context) {
        this.context = context;
    }

    /**
     * 判断模块是否应该加载
     */
    public boolean shouldLoad(DataBase data, String packageName) {
        // 安全模式检查（最高优先级）
        if (isInSafeMode(packageName)) {
            return false;
        }

        // 安全检查（修改检测）
        if (!passSecurityCheck(packageName)) {
            return false;
        }

        // 包名匹配
        if (!matchPackage(data, packageName)) {
            return false;
        }

        // SDK 版本匹配
        if (!matchSdkVersion(data)) {
            return false;
        }

        // OS 版本匹配
        if (!matchOSVersion(data)) {
            return false;
        }

        // 设备类型匹配
        return matchDeviceType(data);
    }

    private boolean isInSafeMode(String packageName) {
        if (isInSafeModeByConfig(packageName)) {
            XposedLog.d(TAG, packageName + " is in safe mode (config), skip all hooks");
            return true;
        }

        if (isInSafeModeByProp(packageName)) {
            XposedLog.d(TAG, packageName + " is in safe mode (prop), skip all hooks");
            return true;
        }

        return false;
    }

    private boolean isInSafeModeByConfig(String packageName) {
        String configKey = SAFE_MODE_CONFIG.get(packageName);
        if (configKey == null) return false;
        return PrefsUtils.mPrefsMap.getBoolean(configKey);
    }

    private boolean isInSafeModeByProp(String packageName) {
        String alias = SAFE_MODE_ALIAS.get(packageName);
        if (alias == null) return false;

        String currentProp = PropUtils.getProp(PROP_SAFE_MODE, "");
        if (currentProp.isEmpty()) return false;

        return Arrays.asList(currentProp.split(",")).contains(alias);
    }

    private boolean passSecurityCheck(String packageName) {
        // 不在检查列表中，直接通过
        if (!SECURITY_CHECK_PACKAGES.contains(packageName)) {
            return true;
        }

        // SystemServer 不检查
        if (context.isSystemServer) {
            return true;
        }

        // 执行检查
        boolean isModified = CheckModifyUtils.INSTANCE.getCheckResult(packageName);
        boolean isVersionZero = DebugModeUtils.INSTANCE.getChooseResult(packageName) == 0;

        if (!context.isDebugMode & isModified && isVersionZero) {
            XposedLog.d(TAG, packageName + " failed security check, skip all hooks");
            return false;
        }

        return true;
    }

    private boolean matchPackage(DataBase data, String packageName) {
        if (data.targetPackage == null) return false;

        // 精确匹配
        if (data.targetPackage.equals(packageName)) {
            return true;
        }

        // SystemServer 不参与通配匹配
        if (context.isSystemServer) {
            return false;
        }

        // 系统应用通配匹配（与精确匹配模块并行加载）
        if (VARIOUS_SYSTEM_APPS.equals(data.targetPackage) && isSystemApp(packageName)) {
            return true;
        }

        // 第三方应用通配匹配（仅在无精确匹配时作为兜底）
        return !context.hasExactMatch && VARIOUS_THIRD_APPS.equals(data.targetPackage);
    }

    /**
     * 判断是否为系统应用
     */
    private boolean isSystemApp(String packageName) {
        return packageName.startsWith("com.miui") || packageName.startsWith("com.xiaomi") || packageName.startsWith("com.android");
    }

    private boolean matchSdkVersion(DataBase data) {
        int currentSdk = getAndroidVersion();
        if (data.minSdk != -1 && currentSdk < data.minSdk) return false;
        if (data.maxSdk != -1 && currentSdk > data.maxSdk) return false;
        return true;
    }

    private boolean matchOSVersion(DataBase data) {
        float currentOS = getHyperOSVersion();
        if (data.minOSVersion != -1F && currentOS < data.minOSVersion) return false;
        if (data.maxOSVersion != -1F && currentOS > data.maxOSVersion) return false;
        return true;
    }

    private boolean matchDeviceType(DataBase data) {
        return switch (data.deviceType) {
            case DEVICE_PAD_ONLY -> isPad();
            case DEVICE_PHONE_ONLY -> !isPad();
            default -> true;
        };
    }

    public static class MatchContext {
        public final boolean isSystemServer;
        public final boolean hasExactMatch;
        public final boolean isDebugMode;

        private MatchContext(Builder builder) {
            this.isSystemServer = builder.isSystemServer;
            this.hasExactMatch = builder.hasExactMatch;
            this.isDebugMode = builder.isDebugMode;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean isSystemServer;
            private boolean hasExactMatch;
            private boolean isDebugMode;

            public Builder systemServer(boolean isSystemServer) {
                this.isSystemServer = isSystemServer;
                return this;
            }

            public Builder exactMatch(boolean hasExactMatch) {
                this.hasExactMatch = hasExactMatch;
                return this;
            }

            public Builder debugMode(boolean isDebugMode) {
                this.isDebugMode = isDebugMode;
                return this;
            }

            public MatchContext build() {
                return new MatchContext(this);
            }
        }
    }
}
