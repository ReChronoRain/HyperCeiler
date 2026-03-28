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
package com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.tiles;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.R;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileConfig;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileContext;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileState;
import com.sevtinge.hyperceiler.libhook.appbase.systemui.TileUtils;

public class GuidedAccessTile extends TileUtils {
    private static final String SETTING_KEY_LOCK_APP = "key_lock_app";
    private static final String TILE_NAME = "custom_guided_access";

    @NonNull
    @Override
    protected TileConfig onCreateTileConfig() {
        return new TileConfig.Builder()
            .setTileClass(findClassIfExists("com.android.systemui.qs.tiles.NightModeTile"))
            .setTileName(TILE_NAME)
            .setTileProvider("nightModeTileProvider")
            .setLabelResId(R.string.system_framework_guided_access)
            .setIcons(
                R.drawable.ic_control_center_guided_access_on,
                R.drawable.ic_control_center_guided_access_on
            )
            .build();
    }

    @Override
    protected void onTileClick(TileContext ctx) {
        Context context = ctx.getContext();
        int lockTaskId = getLockApp(context);

        if (lockTaskId != -1) {
            stopSystemLockTaskMode();
            ctx.refreshState();
            return;
        }

        ActivityManager.RunningTaskInfo runningTask = getRunningTaskInfo();
        if (runningTask == null || runningTask.topActivity == null) {
            XposedLog.w(TAG, getPackageName(), "GuidedAccessTile runningTask is null");
            return;
        }

        ComponentName topActivity = runningTask.topActivity;
        if ("com.miui.home".equals(topActivity.getPackageName())) {
            XposedLog.w(TAG, getPackageName(), "GuidedAccessTile top app is home, skip");
            return;
        }

        startSystemLockTaskMode(runningTask.taskId);
        ctx.refreshState();
    }

    @Nullable
    @Override
    protected Intent onGetLongClickIntent(TileContext ctx) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.SubSettings"));
        intent.putExtra(":settings:show_fragment", "com.android.settings.security.ScreenPinningSettings");
        return intent;
    }

    @Nullable
    @Override
    protected TileState onUpdateState(TileContext ctx) {
        return TileState.of(getLockApp(ctx.getContext()) != -1);
    }

    @Nullable
    private ActivityManager.RunningTaskInfo getRunningTaskInfo() {
        try {
            Class<?> activityManagerWrapper = findClassIfExists("com.android.systemui.shared.system.ActivityManagerWrapper");
            if (activityManagerWrapper == null) return null;

            try {
                Object instance = callStaticMethod(activityManagerWrapper, "getInstance");
                return (ActivityManager.RunningTaskInfo) callMethod(instance, "getRunningTask");
            } catch (Throwable ignored) {
                Object instance = getStaticObjectField(activityManagerWrapper, "sInstance");
                return (ActivityManager.RunningTaskInfo) callMethod(instance, "getRunningTask");
            }
        } catch (Throwable e) {
            XposedLog.w(TAG, getPackageName(), "GuidedAccessTile getRunningTaskInfo E: " + e);
            return null;
        }
    }

    private void startSystemLockTaskMode(int taskId) {
        try {
            Class<?> activityTaskManager = findClassIfExists("android.app.ActivityTaskManager");
            if (activityTaskManager == null) {
                XposedLog.w(TAG, getPackageName(), "ActivityTaskManager class is null");
                return;
            }
            Object service = callStaticMethod(activityTaskManager, "getService");
            callMethod(service, "startSystemLockTaskMode", taskId);
        } catch (Throwable e) {
            XposedLog.w(TAG, getPackageName(), "GuidedAccessTile startSystemLockTaskMode E: " + e);
        }
    }

    private void stopSystemLockTaskMode() {
        try {
            Class<?> activityTaskManager = findClassIfExists("android.app.ActivityTaskManager");
            if (activityTaskManager == null) {
                XposedLog.w(TAG, getPackageName(), "ActivityTaskManager class is null");
                return;
            }
            Object service = callStaticMethod(activityTaskManager, "getService");
            callMethod(service, "stopSystemLockTaskMode");
        } catch (Throwable e) {
            XposedLog.w(TAG, getPackageName(), "GuidedAccessTile stopSystemLockTaskMode E: " + e);
        }
    }

    private static int getLockApp(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), SETTING_KEY_LOCK_APP);
        } catch (Settings.SettingNotFoundException e) {
            return -1;
        }
    }
}
