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

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getBooleanField;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Process;

import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import io.github.libxposed.api.XposedInterface;

public class StatusBarActionBootstrap extends BaseHook {
    private static final String INTENT_CLEAR_MEMORY = "com.android.systemui.taskmanager.Clear";
    private static final String INTENT_SYSTEM_ACTION_RECENTS = "SYSTEM_ACTION_RECENTS";
    private static final String INTENT_SYSTEMUI_PACKAGE = "com.android.systemui";
    private static final String EXTRA_SHOW_TOAST = "show_toast";
    private static final String EXTRA_CLEAN_TYPE = "clean_type";
    private static final String EXTRA_PROTECTED_PACKAGES = "protected_pkgnames";
    private static final String EXTRA_EXPAND_ONLY = "expand_only";
    private static final String STATUS_BAR_SERVICE = "statusbar";

    private static WeakReference<Object> sStatusBarRef = new WeakReference<>(null);
    private static volatile boolean sReceiverRegistered;

    @Override
    public void init() {
        findAndChainMethod("com.android.systemui.statusbar.phone.CentralSurfacesImpl", "start",
            new XposedInterface.Hooker() {
                @Override
                public Object intercept(XposedInterface.Chain chain) throws Throwable {
                    Object result = chain.proceed();
                    Object statusBar = chain.getThisObject();
                    sStatusBarRef = new WeakReference<>(statusBar);

                    Context context = (Context) getObjectField(statusBar, "mContext");
                    registerReceiver(context);
                    return result;
                }
            }
        );
    }

    private void registerReceiver(Context context) {
        if (context == null || sReceiverRegistered) {
            return;
        }
        synchronized (StatusBarActionBootstrap.class) {
            if (sReceiverRegistered) {
                return;
            }
            ContextCompat.registerReceiver(
                context,
                new UnifiedReceiver(),
                createIntentFilter(),
                ContextCompat.RECEIVER_EXPORTED
            );
            sReceiverRegistered = true;
        }
    }

    private IntentFilter createIntentFilter() {
        IntentFilter filter = new IntentFilter();
        // Only register actions with implemented receiver handling.
        // Add future actions here when UnifiedReceiver gains the matching logic.
        filter.addAction(StatusBarActionBridge.ACTION_OPEN_NOTIFICATION_CENTER);
        filter.addAction(StatusBarActionBridge.ACTION_OPEN_CONTROL_CENTER);
        filter.addAction(StatusBarActionBridge.ACTION_OPEN_RECENTS);
        filter.addAction(StatusBarActionBridge.ACTION_OPEN_VOLUME_DIALOG);
        filter.addAction(StatusBarActionBridge.ACTION_CLEAR_MEMORY);
        filter.addAction(StatusBarActionBridge.ACTION_RESTART_SYSTEM_UI);
        return filter;
    }

    private final class UnifiedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            if (StatusBarActionBridge.ACTION_CLEAR_MEMORY.equals(action)) {
                handleClearMemory(context);
            } else if (StatusBarActionBridge.ACTION_OPEN_CONTROL_CENTER.equals(action)) {
                handleOpenControlCenter(context);
            } else if (StatusBarActionBridge.ACTION_OPEN_RECENTS.equals(action)) {
                handleOpenRecents(context);
            } else if (StatusBarActionBridge.ACTION_OPEN_VOLUME_DIALOG.equals(action)) {
                handleOpenVolumeDialog(context);
            } else if (StatusBarActionBridge.ACTION_OPEN_NOTIFICATION_CENTER.equals(action)) {
                handleOpenNotificationCenter(context, intent);
            } else if (StatusBarActionBridge.ACTION_RESTART_SYSTEM_UI.equals(action)) {
                handleRestartSystemUI();
            }
        }
    }

    private void handleClearMemory(Context context) {
        Intent intent = new Intent(INTENT_CLEAR_MEMORY);
        intent.putExtra(EXTRA_SHOW_TOAST, true);
        intent.putExtra(EXTRA_CLEAN_TYPE, 1);
        intent.putStringArrayListExtra(EXTRA_PROTECTED_PACKAGES, new ArrayList<>());
        context.sendBroadcast(intent);
    }

    private void handleOpenControlCenter(Context context) {
        Object statusBar = sStatusBarRef.get();
        if (statusBar == null) {
            expandSettingsFallback(context);
            return;
        }

        try {
            Object commandQueue = getObjectField(statusBar, "mCommandQueue");
            if (commandQueue != null) {
                callMethod(commandQueue, "animateExpandSettingsPanel", (Object) null);
                return;
            }
        } catch (Throwable ignored) {
        }

        expandSettingsFallback(context);
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
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_SAME,
                    AudioManager.FLAG_SHOW_UI
                );
            }
        } catch (Throwable t) {
            XposedLog.e(TAG, getPackageName(), "OpenVolumeDialog failed", t);
        }
    }

    private void handleOpenNotificationCenter(Context context, Intent intent) {
        Object statusBar = sStatusBarRef.get();
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

    @SuppressWarnings("WrongConstant")
    private void expandSettingsFallback(Context context) {
        long token = Binder.clearCallingIdentity();
        try {
            Object statusBarService = context.getSystemService(STATUS_BAR_SERVICE);
            if (statusBarService != null) {
                callMethod(statusBarService, "expandSettingsPanel", (Object) null);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void handleRestartSystemUI() {
        Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL);
    }
}
