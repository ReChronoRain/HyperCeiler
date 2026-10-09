/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * HyperCeiler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.systemsettings;

import android.app.admin.DevicePolicyManager;
import android.content.Context;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class UnlockNeverSleepScreenOS4 extends BaseHook {
    @Override
    public void init() {
        findAndHookMethod("com.android.settings.KeyguardTimeoutDropDownPreference",
            "disableUnusableTimeouts", new IMethodHook() {
                @Override
                public void before(HookParam param) {
                    try {
                        Object preference = param.getThisObject();
                        Object contextObject = getObjectField(preference, "mContext");
                        if (!(contextObject instanceof Context context)) return;
                        Object service = context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                        if (service != null && !(service instanceof DevicePolicyManager)) return;
                        if (service instanceof DevicePolicyManager policy
                            && policy.getMaximumTimeToLock(null) != 0L) return;

                        CharSequence[] entries = (CharSequence[]) callMethod(preference, "getEntries");
                        CharSequence[] values = (CharSequence[]) callMethod(preference, "getEntryValues");
                        if (!hasNeverEntry(entries, values)) return;

                        // OS4 reads IS_OLED in <clinit>, then removes 2147483647 here by
                        // imposing an OLED-only maximum of 2147483646. Preserve the ROM's
                        // no_config_screen_timeout restriction update, and skip only this
                        // filtering when no device-policy time limit exists.
                        callMethod(preference, "updateDisableState", true);
                        param.setResult(null);
                    } catch (Throwable error) {
                        // Unknown ROM shapes and inaccessible policy state keep the ROM path.
                        XposedLog.e(TAG, getPackageName(), "Unable to preserve the never-sleep option", error);
                    }
                }
            });
    }

    private static boolean hasNeverEntry(CharSequence[] entries, CharSequence[] values) {
        if (entries == null || values == null || entries.length != values.length) return false;
        int neverEntries = 0;
        for (int i = 0; i < values.length; i++) {
            if (entries[i] == null || values[i] == null) return false;
            long timeout;
            try {
                timeout = Long.parseLong(values[i].toString());
            } catch (NumberFormatException ignored) {
                return false;
            }
            if (timeout <= 0 || timeout > Integer.MAX_VALUE) return false;
            if (timeout == Integer.MAX_VALUE) neverEntries++;
        }
        return neverEntries == 1;
    }
}
