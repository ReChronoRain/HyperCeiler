/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.appbase.systemui;

import android.content.Context;
import android.content.Intent;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

public final class StatusBarActionBridge {
    // Keep this bridge limited to actions with real receiver handling.
    // When adding a new action later, define it here and implement the
    // matching branch in StatusBarActionBootstrap.UnifiedReceiver together.
    public static final String ACTION_OPEN_NOTIFICATION_CENTER = BaseHook.ACTION_PREFIX + "OpenNotificationCenter";
    public static final String ACTION_OPEN_CONTROL_CENTER = BaseHook.ACTION_PREFIX + "OpenControlCenter";
    public static final String ACTION_OPEN_RECENTS = BaseHook.ACTION_PREFIX + "OpenRecents";
    public static final String ACTION_OPEN_VOLUME_DIALOG = BaseHook.ACTION_PREFIX + "OpenVolumeDialog";
    public static final String ACTION_CLEAR_MEMORY = BaseHook.ACTION_PREFIX + "ClearMemory";
    public static final String ACTION_RESTART_SYSTEM_UI = BaseHook.ACTION_PREFIX + "RestartSystemUI";
    private static final String EXTRA_EXPAND_ONLY = "expand_only";

    private StatusBarActionBridge() {
    }

    public static boolean openNotificationCenter(Context context) {
        return openNotificationCenter(context, false);
    }

    public static boolean openNotificationCenter(Context context, boolean expandOnly) {
        Intent intent = new Intent(ACTION_OPEN_NOTIFICATION_CENTER);
        if (expandOnly) {
            intent.putExtra(EXTRA_EXPAND_ONLY, true);
        }
        return sendBroadcast(context, intent);
    }

    public static boolean clearMemory(Context context) {
        return sendBroadcast(context, new Intent(ACTION_CLEAR_MEMORY));
    }

    public static boolean openControlCenter(Context context) {
        return sendBroadcast(context, new Intent(ACTION_OPEN_CONTROL_CENTER));
    }

    public static boolean openRecents(Context context) {
        return sendBroadcast(context, new Intent(ACTION_OPEN_RECENTS));
    }

    public static boolean openVolumeDialog(Context context) {
        return sendBroadcast(context, new Intent(ACTION_OPEN_VOLUME_DIALOG));
    }

    public static boolean restartSystemUI(Context context) {
        return sendBroadcast(context, new Intent(ACTION_RESTART_SYSTEM_UI));
    }

    private static boolean sendBroadcast(Context context, Intent intent) {
        try {
            context.sendBroadcast(intent);
            return true;
        } catch (Throwable t) {
            AndroidLog.w("StatusBarActionBridge", "systemui", "sendBroadcast", t);
            return false;
        }
    }
}
