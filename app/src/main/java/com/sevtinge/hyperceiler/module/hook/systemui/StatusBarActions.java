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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.module.hook.systemui;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreAndroidVersion;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Process;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class StatusBarActions extends BaseHook {

    Class<?> mStatusBarClass;
    public static Object mStatusBar = null;

    @Override
    public void init() {

        if (isMoreAndroidVersion(Build.VERSION_CODES.TIRAMISU)) {
            mStatusBarClass = findClassIfExists("com.android.systemui.statusbar.phone.CentralSurfacesImpl");
        } else {
            mStatusBarClass = findClassIfExists("com.android.systemui.statusbar.phone.StatusBar");
        }

        setupStatusBarAction();
        setupRestartSystemUIAction();
    }

    // StatusBarActions
    public void setupStatusBarAction() {

        findAndHookMethod(mStatusBarClass, "start", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                mStatusBar = param.thisObject;
                Context mStatusBarContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                IntentFilter intentfilter = new IntentFilter();

                intentfilter.addAction(ACTION_PREFIX + "OpenNotificationCenter");
                intentfilter.addAction(ACTION_PREFIX + "ExpandSettings");
                intentfilter.addAction(ACTION_PREFIX + "OpenRecents");
                intentfilter.addAction(ACTION_PREFIX + "OpenVolumeDialog");

                intentfilter.addAction(ACTION_PREFIX + "ToggleGPS");
                intentfilter.addAction(ACTION_PREFIX + "ToggleHotspot");
                intentfilter.addAction(ACTION_PREFIX + "ToggleFlashlight");
                intentfilter.addAction(ACTION_PREFIX + "ShowQuickRecents");
                intentfilter.addAction(ACTION_PREFIX + "HideQuickRecents");

                intentfilter.addAction(ACTION_PREFIX + "ClearMemory");
                intentfilter.addAction(ACTION_PREFIX + "CollectXposedLog");
                intentfilter.addAction(ACTION_PREFIX + "RestartLauncher");
                intentfilter.addAction(ACTION_PREFIX + "CopyToExternal");

                mStatusBarContext.registerReceiver(mStatusBarReceiver, intentfilter);
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
                case ACTION_PREFIX + "ClearMemory" -> {
                    Intent clearIntent = new Intent("com.android.systemui.taskmanager.Clear");
                    clearIntent.putExtra("show_toast", true);
                    // clearIntent.putExtra("clean_type", -1);
                    context.sendBroadcast(clearIntent);
                }
                case ACTION_PREFIX + "OpenRecents" -> {
                    Intent recentIntent = new Intent("SYSTEM_ACTION_RECENTS");
                    recentIntent.setPackage("com.android.systemui");
                    context.sendBroadcast(recentIntent);
                }

                case ACTION_PREFIX + "OpenVolumeDialog" -> OpenVolumeDialogs(context);

                case ACTION_PREFIX + "OpenNotificationCenter" -> {
                    try {
                        Object mNotificationPanel = XposedHelpers.getObjectField(mStatusBar, "mNotificationPanel");
                        boolean mPanelExpanded = (boolean) XposedHelpers.getObjectField(mNotificationPanel, "mPanelExpanded");
                        boolean mQsExpanded = (boolean) XposedHelpers.getObjectField(mNotificationPanel, "mQsExpanded");
                        boolean expandOnly = intent.getBooleanExtra("expand_only", false);
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
        if (mStatusBarClass == null) return;
        findAndHookMethod(mStatusBarClass, "start", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                mStatusBar = param.thisObject;
                Context mStatusBarContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                IntentFilter intentfilter = new IntentFilter();

                intentfilter.addAction(ACTION_PREFIX + "RestartSystemUI");
                mStatusBarContext.registerReceiver(mRestartSystemUIReceiver, intentfilter);
            }
        });
    }

    private final BroadcastReceiver mRestartSystemUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            if (action.equals(ACTION_PREFIX + "RestartSystemUI")) {
                Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL);
            }
        }
    };
}
