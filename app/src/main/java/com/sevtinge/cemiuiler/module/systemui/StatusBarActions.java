package com.sevtinge.cemiuiler.module.systemui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Process;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;
import moralnorm.os.SdkVersion;

public class StatusBarActions extends BaseHook {

    Class<?> mStatusBarClass;
    public static Object mStatusBar = null;

    @Override
    public void init() {

        if (SdkVersion.isAndroidT) {
            mStatusBarClass = findClassIfExists("com.android.systemui.statusbar.phone.CentralSurfacesImpl");
        } else {
            mStatusBarClass = findClassIfExists("com.android.systemui.statusbar.phone.StatusBar");
        }

        setupStatusBarAction();
        setupRestartSystemUIAction();
    }

    //StatusBarActions
    public void setupStatusBarAction() {

        findAndHookMethod(mStatusBarClass, "start", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
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
                case ACTION_PREFIX + "OpenNotificationCenter":
                    try {
                        Object mNotificationPanel = XposedHelpers.getObjectField(mStatusBar, "mNotificationPanel");
                        boolean mPanelExpanded = (boolean)XposedHelpers.getObjectField(mNotificationPanel, "mPanelExpanded");
                        boolean mQsExpanded = (boolean)XposedHelpers.getObjectField(mNotificationPanel, "mQsExpanded");
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
                    break;
            }
        }
    };

    public void setupRestartSystemUIAction() {
        if (mStatusBarClass == null) return;
        findAndHookMethod(mStatusBarClass, "start", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
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
