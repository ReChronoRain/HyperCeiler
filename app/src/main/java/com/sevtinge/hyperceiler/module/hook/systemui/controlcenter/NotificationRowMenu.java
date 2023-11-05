package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isMoreHyperOSVersion;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.Helpers;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.function.Consumer;

import de.robv.android.xposed.XposedHelpers;

public class NotificationRowMenu extends BaseHook {
    @Override
    public void init() {
        int appInfoIconResId = mResHook.addResource("ic_appinfo", R.drawable.ic_appinfo12);
        int forceCloseIconResId = mResHook.addResource("ic_forceclose", R.drawable.ic_forceclose12);
        int openInFwIconResId = mResHook.addResource("ic_openinfw", R.drawable.ic_openinfw);
        int appInfoDescId = mResHook.addResource("miui_notification_menu_appinfo_title", R.string.system_notifrowmenu_appinfo);
        int forceCloseDescId = mResHook.addResource("miui_notification_menu_forceclose_title", R.string.system_notifrowmenu_forceclose);
        int openInFwDescId = mResHook.addResource("miui_notification_menu_openinfw_title", R.string.system_notifrowmenu_openinfw);
        mResHook.setDensityReplacement("com.android.systemui", "dimen", "notification_menu_icon_padding", 0);
        mResHook.setDensityReplacement("com.android.systemui", "dimen", "miui_notification_modal_menu_margin_left_right", 3);
        mResHook.setResReplacement("com.android.systemui", "drawable", "miui_notification_menu_ic_bg_active", R.drawable.miui_notification_menu_ic_bg_active);
        mResHook.setResReplacement("com.android.systemui", "drawable", "miui_notification_menu_ic_bg_inactive", R.drawable.miui_notification_menu_ic_bg_inactive);

        Class<?> MiuiNotificationMenuItem = findClass("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow.MiuiNotificationMenuItem", lpparam.classLoader);
        hookAllMethods("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews", new MethodHook() {
            @Override
            @SuppressWarnings("unchecked")
            protected void after(final MethodHookParam param) throws Throwable {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                ArrayList<Object> mMenuItems = (ArrayList<Object>)XposedHelpers.getObjectField(param.thisObject, "mMenuItems");

                Object infoBtn = null;
                Object forceCloseBtn = null;
                Object openFwBtn = null;
                Constructor MenuItem = MiuiNotificationMenuItem.getConstructors()[0];
                try {
                    infoBtn = MenuItem.newInstance(param.thisObject, mContext, appInfoDescId, null, appInfoIconResId);
                    forceCloseBtn = MenuItem.newInstance(param.thisObject, mContext, forceCloseDescId, null, forceCloseIconResId);
                    openFwBtn = MenuItem.newInstance(param.thisObject, mContext, openInFwDescId, null, openInFwIconResId);
                } catch (Throwable t1) {
                    logW(TAG, "com.android.systemui", t1);
                }
                if (infoBtn == null || forceCloseBtn == null || openFwBtn == null) return;
                Object notification = XposedHelpers.getObjectField(param.thisObject, "mSbn");
                Object expandNotifyRow = XposedHelpers.getObjectField(param.thisObject, "mParent");
                mMenuItems.add(infoBtn);
                mMenuItems.add(forceCloseBtn);
                mMenuItems.add(openFwBtn);
                XposedHelpers.setObjectField(param.thisObject, "mMenuItems", mMenuItems);
                int menuMargin = (int) XposedHelpers.getObjectField(param.thisObject, "mMenuMargin");
                LinearLayout mMenuContainer = (LinearLayout)XposedHelpers.getObjectField(param.thisObject, "mMenuContainer");
                View mInfoBtn = (View) XposedHelpers.callMethod(infoBtn, "getMenuView");
                View mForceCloseBtn = (View) XposedHelpers.callMethod(forceCloseBtn, "getMenuView");
                View mOpenFwBtn = (View) XposedHelpers.callMethod(openFwBtn, "getMenuView");

                View.OnClickListener itemClick = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (view == null) return;
                        String pkgName = (String)XposedHelpers.callMethod(notification, "getPackageName");
                        int uid = (int)XposedHelpers.callMethod(notification, "getAppUid");
                        int user = 0;
                        try {
                            user = (int)XposedHelpers.callStaticMethod(UserHandle.class, "getUserId", uid);
                        } catch (Throwable t) {
                            logW(TAG, "com.android.systemui", t);
                        }

                        if (view == mInfoBtn) {
                            Helpers.openAppInfo(mContext, pkgName, user);
                            mContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                        } else if (view == mForceCloseBtn) {
                            ActivityManager am = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
                            if (user != 0)
                                XposedHelpers.callMethod(am, "forceStopPackageAsUser", pkgName, user);
                            else
                                XposedHelpers.callMethod(am, "forceStopPackage", pkgName);
                            try {
                                CharSequence appName = mContext.getPackageManager().getApplicationLabel(mContext.getPackageManager().getApplicationInfo(pkgName, 0));
                                //Toast.makeText(mContext, Helpers.getModuleRes(mContext).getString(R.string.force_closed, appName), Toast.LENGTH_SHORT).show();
                            } catch (Throwable ignore) {}
                        }
                        else if (view == mOpenFwBtn) {
                            Class<?> Dependency = findClass("com.android.systemui.Dependency", lpparam.classLoader);
                            Object AppMiniWindowManager = XposedHelpers.callStaticMethod(Dependency, "get", findClassIfExists("com.android.systemui.statusbar.notification.policy.AppMiniWindowManager", lpparam.classLoader));
                            String miniWindowPkg = (String) XposedHelpers.callMethod(expandNotifyRow, "getMiniWindowTargetPkg");
                            PendingIntent notifyIntent = (PendingIntent) XposedHelpers.callMethod(expandNotifyRow, "getPendingIntent");
                            String ModalControllerForDep = "com.android.systemui.statusbar.notification.modal.ModalController";
                            Object ModalController = XposedHelpers.callStaticMethod(Dependency, "get", findClass(ModalControllerForDep, lpparam.classLoader));
                            XposedHelpers.callMethod(ModalController, "animExitModelCollapsePanels");
                            XposedHelpers.callMethod(AppMiniWindowManager, "launchMiniWindowActivity", miniWindowPkg, notifyIntent);
//                            mContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                        }
                    }
                };
                mInfoBtn.setOnClickListener(itemClick);
                mForceCloseBtn.setOnClickListener(itemClick);
                mOpenFwBtn.setOnClickListener(itemClick);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, -2);
                layoutParams.leftMargin = menuMargin;
                layoutParams.rightMargin = menuMargin;
                mMenuContainer.addView(mInfoBtn, layoutParams);
                mMenuContainer.addView(mForceCloseBtn, layoutParams);
                mMenuContainer.addView(mOpenFwBtn, layoutParams);
                int menuWidth = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    52,
                    mContext.getResources().getDisplayMetrics()
                );
                int titleId = mContext.getResources().getIdentifier("modal_menu_title", "id", lpparam.packageName);
                mMenuItems.forEach(new Consumer() {
                    @Override
                    public void accept(Object obj) {
                        View menuView = (View) XposedHelpers.callMethod(obj, "getMenuView");
                        ((TextView) menuView.findViewById(titleId)).setMaxWidth(menuWidth);
                    }
                });
            }
        });
    }
}
