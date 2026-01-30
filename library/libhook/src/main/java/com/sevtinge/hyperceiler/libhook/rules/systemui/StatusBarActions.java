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
package com.sevtinge.hyperceiler.libhook.rules.systemui;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getBooleanField;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Process;

import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.lang.ref.WeakReference;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

// com.android.systemui
public class StatusBarActions extends BaseHook {
    public static final String ACTION_OPEN_NOTIFICATION_CENTER = ACTION_PREFIX + "OpenNotificationCenter";
    public static final String ACTION_EXPAND_SETTINGS = ACTION_PREFIX + "ExpandSettings";
    public static final String ACTION_OPEN_RECENTS = ACTION_PREFIX + "OpenRecents";
    public static final String ACTION_OPEN_VOLUME_DIALOG = ACTION_PREFIX + "OpenVolumeDialog";
    public static final String ACTION_TOGGLE_GPS = ACTION_PREFIX + "ToggleGPS";
    public static final String ACTION_TOGGLE_HOTSPOT = ACTION_PREFIX + "ToggleHotspot";
    public static final String ACTION_TOGGLE_FLASHLIGHT = ACTION_PREFIX + "ToggleFlashlight";
    public static final String ACTION_SHOW_QUICK_RECENTS = ACTION_PREFIX + "ShowQuickRecents";
    public static final String ACTION_HIDE_QUICK_RECENTS = ACTION_PREFIX + "HideQuickRecents";
    public static final String ACTION_CLEAR_MEMORY = ACTION_PREFIX + "ClearMemory";
    public static final String ACTION_COLLECT_XPOSED_LOG = ACTION_PREFIX + "CollectXposedLog";
    public static final String ACTION_RESTART_LAUNCHER = ACTION_PREFIX + "RestartLauncher";
    public static final String ACTION_COPY_TO_EXTERNAL = ACTION_PREFIX + "CopyToExternal";
    public static final String ACTION_RESTART_SYSTEM_UI = ACTION_PREFIX + "RestartSystemUI";

    private static final String INTENT_CLEAR_MEMORY = "com.android.systemui.taskmanager.Clear";
    private static final String INTENT_SYSTEM_ACTION_RECENTS = "SYSTEM_ACTION_RECENTS";
    private static final String INTENT_SYSTEMUI_PACKAGE = "com.android.systemui";
    private static final String EXTRA_SHOW_TOAST = "show_toast";
    private static final String EXTRA_EXPAND_ONLY = "expand_only";
    private static final String STATUS_BAR_SERVICE = "statusbar";

    private static WeakReference<Object> mStatusBarRef = new WeakReference<>(null);

    @Override
    public void init() {
        findAndHookMethod("com.android.systemui.statusbar.phone.CentralSurfacesImpl", "start",new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                mStatusBarRef = new WeakReference<>(param.getThisObject());
                Context context = (Context) getObjectField(param.getThisObject(), "mContext");

                IntentFilter filter = createIntentFilter();ContextCompat.registerReceiver(context, new UnifiedReceiver(),
                    filter, ContextCompat.RECEIVER_EXPORTED);
            }
        });
    }

    private IntentFilter createIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_OPEN_NOTIFICATION_CENTER);
        filter.addAction(ACTION_EXPAND_SETTINGS);
        filter.addAction(ACTION_OPEN_RECENTS);
        filter.addAction(ACTION_OPEN_VOLUME_DIALOG);
        filter.addAction(ACTION_TOGGLE_GPS);
        filter.addAction(ACTION_TOGGLE_HOTSPOT);
        filter.addAction(ACTION_TOGGLE_FLASHLIGHT);
        filter.addAction(ACTION_SHOW_QUICK_RECENTS);
        filter.addAction(ACTION_HIDE_QUICK_RECENTS);
        filter.addAction(ACTION_CLEAR_MEMORY);
        filter.addAction(ACTION_COLLECT_XPOSED_LOG);
        filter.addAction(ACTION_RESTART_LAUNCHER);
        filter.addAction(ACTION_COPY_TO_EXTERNAL);
        filter.addAction(ACTION_RESTART_SYSTEM_UI);
        return filter;
    }

    private class UnifiedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            if (ACTION_CLEAR_MEMORY.equals(action)) {
                handleClearMemory(context);
            } else if (ACTION_OPEN_RECENTS.equals(action)) {
                handleOpenRecents(context);
            } else if (ACTION_OPEN_VOLUME_DIALOG.equals(action)) {
                handleOpenVolumeDialog(context);
            } else if (ACTION_OPEN_NOTIFICATION_CENTER.equals(action)) {
                handleOpenNotificationCenter(context, intent);
            } else if (ACTION_RESTART_SYSTEM_UI.equals(action)) {
                handleRestartSystemUI();
            }
        }
    }

    private void handleClearMemory(Context context) {
        Intent intent = new Intent(INTENT_CLEAR_MEMORY);
        intent.putExtra(EXTRA_SHOW_TOAST, true);
        context.sendBroadcast(intent);
    }

    private void handleOpenRecents(Context context) {
        Intent intent = new Intent(INTENT_SYSTEM_ACTION_RECENTS);
        intent.setPackage(INTENT_SYSTEMUI_PACKAGE);
        context.sendBroadcast(intent);
    }

    private void handleOpenVolumeDialog(Context context) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
            }
        } catch (Throwable t) {
            XposedLog.e(TAG, getPackageName(), "OpenVolumeDialog failed", t);
        }
    }

    private void handleOpenNotificationCenter(Context context, Intent intent) {
        Object statusBar = mStatusBarRef.get();
        if (statusBar == null) {
            expandNotificationsFallback(context);
            return;
        }

        try {
            Object panel = getObjectField(statusBar, "mNotificationPanel");
            boolean panelExpanded = getBooleanField(panel, "mPanelExpanded");
            boolean qsExpanded = getBooleanField(panel, "mQsExpanded");
            boolean expandOnly = intent.getBooleanExtra(EXTRA_EXPAND_ONLY, false);

            if (panelExpanded) {
                if (!expandOnly) {
                    if (qsExpanded) {
                        callMethod(statusBar, "closeQs");
                    } else {
                        callMethod(statusBar, "animateCollapsePanels");
                    }
                }
            } else {
                callMethod(statusBar, "animateExpandNotificationsPanel");
            }
        } catch (Throwable t) {
            expandNotificationsFallback(context);
        }
    }

    @SuppressWarnings("WrongConstant")
    private void expandNotificationsFallback(Context context) {
        long token = Binder.clearCallingIdentity();
        try {
            Object statusBarService = context.getSystemService(STATUS_BAR_SERVICE);
            if (statusBarService != null) {
                callMethod(statusBarService, "expandNotificationsPanel");
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void handleRestartSystemUI() {
        Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL);}
}
