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
package com.sevtinge.hyperceiler.hook.module.skip;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Handler;
import android.os.Process;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

// com.android.systemui
public class StatusBarActions extends BaseHook {

    // 统一常量管理
    static final class StatusBarActionConstants {
        static final String ACTION_OPEN_NOTIFICATION_CENTER = ACTION_PREFIX + "OpenNotificationCenter";
        static final String ACTION_EXPAND_SETTINGS = ACTION_PREFIX + "ExpandSettings";
        static final String ACTION_OPEN_RECENTS = ACTION_PREFIX + "OpenRecents";
        static final String ACTION_OPEN_VOLUME_DIALOG = ACTION_PREFIX + "OpenVolumeDialog";
        static final String ACTION_TOGGLE_GPS = ACTION_PREFIX + "ToggleGPS";
        static final String ACTION_TOGGLE_HOTSPOT = ACTION_PREFIX + "ToggleHotspot";
        static final String ACTION_TOGGLE_FLASHLIGHT = ACTION_PREFIX + "ToggleFlashlight";
        static final String ACTION_SHOW_QUICK_RECENTS = ACTION_PREFIX + "ShowQuickRecents";
        static final String ACTION_HIDE_QUICK_RECENTS = ACTION_PREFIX + "HideQuickRecents";
        static final String ACTION_CLEAR_MEMORY = ACTION_PREFIX + "ClearMemory";
        static final String ACTION_COLLECT_XPOSED_LOG = ACTION_PREFIX + "CollectXposedLog";
        static final String ACTION_RESTART_LAUNCHER = ACTION_PREFIX + "RestartLauncher";
        static final String ACTION_COPY_TO_EXTERNAL = ACTION_PREFIX + "CopyToExternal";
        static final String ACTION_RESTART_SYSTEM_UI = ACTION_PREFIX + "RestartSystemUI";

        static final String INTENT_CLEAR_MEMORY = "com.android.systemui.taskmanager.Clear";
        static final String INTENT_SYSTEM_ACTION_RECENTS = "SYSTEM_ACTION_RECENTS";
        static final String INTENT_SYSTEMUI_PACKAGE = "com.android.systemui";
        static final String EXTRA_SHOW_TOAST = "show_toast";
        static final String EXTRA_EXPAND_ONLY = "expand_only";
        private StatusBarActionConstants() {}
    }
    public static Object mStatusBar = null;

    @Override
    public void init() {
        setupStatusBarAction();
        setupRestartSystemUIAction();
    }

    // StatusBarActions
    public void setupStatusBarAction() {

        findAndHookMethod("com.android.systemui.statusbar.phone.CentralSurfacesImpl", "start", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                mStatusBar = param.thisObject;
                Context mStatusBarContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                IntentFilter intentfilter = new IntentFilter();

                intentfilter.addAction(StatusBarActionConstants.ACTION_OPEN_NOTIFICATION_CENTER);
                intentfilter.addAction(StatusBarActionConstants.ACTION_EXPAND_SETTINGS);
                intentfilter.addAction(StatusBarActionConstants.ACTION_OPEN_RECENTS);
                intentfilter.addAction(StatusBarActionConstants.ACTION_OPEN_VOLUME_DIALOG);

                intentfilter.addAction(StatusBarActionConstants.ACTION_TOGGLE_GPS);
                intentfilter.addAction(StatusBarActionConstants.ACTION_TOGGLE_HOTSPOT);
                intentfilter.addAction(StatusBarActionConstants.ACTION_TOGGLE_FLASHLIGHT);
                intentfilter.addAction(StatusBarActionConstants.ACTION_SHOW_QUICK_RECENTS);
                intentfilter.addAction(StatusBarActionConstants.ACTION_HIDE_QUICK_RECENTS);

                intentfilter.addAction(StatusBarActionConstants.ACTION_CLEAR_MEMORY);
                intentfilter.addAction(StatusBarActionConstants.ACTION_COLLECT_XPOSED_LOG);
                intentfilter.addAction(StatusBarActionConstants.ACTION_RESTART_LAUNCHER);
                intentfilter.addAction(StatusBarActionConstants.ACTION_COPY_TO_EXTERNAL);

                ContextCompat.registerReceiver(mStatusBarContext, mStatusBarReceiver, intentfilter, ContextCompat.RECEIVER_NOT_EXPORTED);
            }
        });
    }


    private static final BroadcastReceiver mStatusBarReceiver = new BroadcastReceiver() {
        @SuppressLint("WrongConstant")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case StatusBarActionConstants.ACTION_CLEAR_MEMORY -> {
                    Intent clearIntent = new Intent(StatusBarActionConstants.INTENT_CLEAR_MEMORY);
                    clearIntent.putExtra(StatusBarActionConstants.EXTRA_SHOW_TOAST, true);
                    // clearIntent.putExtra("clean_type", -1);
                    context.sendBroadcast(clearIntent);
                }
                case StatusBarActionConstants.ACTION_OPEN_RECENTS -> {
                    Intent recentIntent = new Intent(StatusBarActionConstants.INTENT_SYSTEM_ACTION_RECENTS);
                    recentIntent.setPackage(StatusBarActionConstants.INTENT_SYSTEMUI_PACKAGE);
                    context.sendBroadcast(recentIntent);
                }

                case StatusBarActionConstants.ACTION_OPEN_VOLUME_DIALOG -> OpenVolumeDialogs(context);

                case StatusBarActionConstants.ACTION_OPEN_NOTIFICATION_CENTER -> {
                    try {
                        Object mNotificationPanel = XposedHelpers.getObjectField(mStatusBar, "mNotificationPanel");
                        boolean mPanelExpanded = (boolean) XposedHelpers.getObjectField(mNotificationPanel, "mPanelExpanded");
                        boolean mQsExpanded = (boolean) XposedHelpers.getObjectField(mNotificationPanel, "mQsExpanded");
                        boolean expandOnly = intent.getBooleanExtra(StatusBarActionConstants.EXTRA_EXPAND_ONLY, false);
                        if (mPanelExpanded) {
                            if (!expandOnly) {
                                if (mQsExpanded) {
                                    XposedHelpers.callMethod(mStatusBar, "closeQs");
                                } else {
                                    XposedHelpers.callMethod(mStatusBar, "animateCollapsePanels");
                                }
                            }
                        } else {
                            XposedHelpers.callMethod(mStatusBar, "animateExpandNotificationsPanel");
                        }
                    } catch (Throwable t) {
                        // Expand only
                        long token = Binder.clearCallingIdentity();
                        XposedHelpers.callMethod(context.getSystemService("statusbar"), "expandNotificationsPanel");
                        Binder.restoreCallingIdentity(token);
                    }
                }
            }
        }
    };

    public static void OpenVolumeDialogs(Context context) {
        try {
            Object mVolumeComponent = XposedHelpers.getObjectField(mStatusBar, "mVolumeComponent");
            Object mVolumeDialogPlugin = XposedHelpers.getObjectField(mVolumeComponent, "mDialog");
            Object miuiVolumeDialog = XposedHelpers.getObjectField(mVolumeDialogPlugin, "mVolumeDialogImpl");
            if (miuiVolumeDialog == null) {
                logI("OpenVolumeDialog", "com.android.systemui", "MIUI volume dialog is NULL!");
                return;
            }

            Handler mHandler = (Handler) XposedHelpers.getObjectField(miuiVolumeDialog, "mHandler");
            mHandler.post(() -> {
                boolean mShowing = XposedHelpers.getBooleanField(miuiVolumeDialog, "mShowing");
                boolean mExpanded = XposedHelpers.getBooleanField(miuiVolumeDialog, "mExpanded");

                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                boolean isInCall = am.getMode() == AudioManager.MODE_IN_CALL || am.getMode() == AudioManager.MODE_IN_COMMUNICATION;
                if (mShowing) {
                    if (mExpanded || isInCall)
                        XposedHelpers.callMethod(miuiVolumeDialog, "dismissH", 1);
                    else {
                        Object mDialogView = XposedHelpers.getObjectField(miuiVolumeDialog, "mDialogView");
                        View mExpandButton = (View) XposedHelpers.getObjectField(mDialogView, "mExpandButton");
                        View.OnClickListener mClickExpand = (View.OnClickListener) XposedHelpers.getObjectField(mDialogView, "expandListener");
                        mClickExpand.onClick(mExpandButton);
                    }
                } else {
                    Object mController = XposedHelpers.getObjectField(mVolumeDialogPlugin, "mController");
                    if (isInCall) {
                        XposedHelpers.callMethod(mController, "setActiveStream", 0);
                        XposedHelpers.setBooleanField(miuiVolumeDialog, "mNeedReInit", true);
                    } else if (am.isMusicActive()) {
                        XposedHelpers.callMethod(mController, "setActiveStream", 3);
                        XposedHelpers.setBooleanField(miuiVolumeDialog, "mNeedReInit", true);
                    }
                    XposedHelpers.callMethod(miuiVolumeDialog, "showH", 1);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    public void setupRestartSystemUIAction() {
        findAndHookMethod("com.android.systemui.statusbar.phone.CentralSurfacesImpl", "start", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                mStatusBar = param.thisObject;
                Context mStatusBarContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                IntentFilter intentfilter = new IntentFilter();

                intentfilter.addAction(StatusBarActionConstants.ACTION_RESTART_SYSTEM_UI);
                ContextCompat.registerReceiver(mStatusBarContext, mRestartSystemUIReceiver, intentfilter, ContextCompat.RECEIVER_NOT_EXPORTED);
            }
        });
    }

    private final BroadcastReceiver mRestartSystemUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            if (action.equals(StatusBarActionConstants.ACTION_RESTART_SYSTEM_UI)) {
                Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL);
            }
        }
    };
}
