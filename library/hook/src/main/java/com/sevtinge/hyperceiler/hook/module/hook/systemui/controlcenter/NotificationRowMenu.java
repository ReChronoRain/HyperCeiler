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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.controlcenter;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sevtinge.hyperceiler.hook.R;
import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.module.base.tool.AppsTool;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.function.Consumer;

import de.robv.android.xposed.XposedHelpers;

public class NotificationRowMenu extends BaseHook {
    @Override
    public void init() {
        int appInfoIconResId = R.drawable.ic_appinfo12;
        int forceCloseIconResId = R.drawable.ic_forceclose12;
        int openInFwIconResId = R.drawable.ic_openinfw;
        int appInfoDescId = R.string.system_notifrowmenu_appinfo;
        int forceCloseDescId = R.string.system_notifrowmenu_forceclose;
        int openInFwDescId = R.string.system_notifrowmenu_openinfw;
        mResHook.setDensityReplacement("com.android.systemui", "dimen", "notification_menu_icon_padding", 0);
        mResHook.setDensityReplacement("com.android.systemui", "dimen", "miui_notification_modal_menu_margin_left_right", 3);
        mResHook.setResReplacement("com.android.systemui", "drawable", "miui_notification_menu_ic_bg_active", R.drawable.miui_notification_menu_ic_bg_active);
        mResHook.setResReplacement("com.android.systemui", "drawable", "miui_notification_menu_ic_bg_inactive", R.drawable.miui_notification_menu_ic_bg_inactive);

        Class<?> MiuiNotificationMenuItem = findClass("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow.MiuiNotificationMenuItem", lpparam.classLoader);
        hookAllMethods("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow", "createMenuViews", new MethodHook() {
            @Override
            @SuppressWarnings("unchecked")
            protected void after(final MethodHookParam param) {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                ArrayList<Object> mMenuItems = (ArrayList<Object>) XposedHelpers.getObjectField(param.thisObject, "mMenuItems");

                Object infoBtn = null;
                Object forceCloseBtn = null;
                Object openFwBtn = null;
                Constructor<?> MenuItem = MiuiNotificationMenuItem.getConstructors()[0];
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
                LinearLayout mMenuContainer = (LinearLayout) XposedHelpers.getObjectField(param.thisObject, "mMenuContainer");
                View mInfoBtn = (View) XposedHelpers.callMethod(infoBtn, "getMenuView");
                View mForceCloseBtn = (View) XposedHelpers.callMethod(forceCloseBtn, "getMenuView");
                View mOpenFwBtn = (View) XposedHelpers.callMethod(openFwBtn, "getMenuView");

                OnClickListener itemClick = view -> {
                    if (view == null) return;
                    String pkgName = (String) XposedHelpers.callMethod(notification, "getPackageName");
                    int uid = (int) XposedHelpers.callMethod(notification, "getAppUid");
                    int user = 0;
                    try {
                        user = (int) XposedHelpers.callStaticMethod(UserHandle.class, "getUserId", uid);
                    } catch (Throwable t) {
                        logW(TAG, "com.android.systemui", t);
                    }

                    if (view == mInfoBtn) {
                        AppsTool.openAppInfo(mContext, pkgName, user);
                        mContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                    } else if (view == mForceCloseBtn) {
                        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                        if (user != 0)
                            XposedHelpers.callMethod(am, "forceStopPackageAsUser", pkgName, user);
                        else
                            XposedHelpers.callMethod(am, "forceStopPackage", pkgName);
                        try {
                            CharSequence appName = mContext.getPackageManager().getApplicationLabel(mContext.getPackageManager().getApplicationInfo(pkgName, 0));
                            // Toast.makeText(mContext, Helpers.getModuleRes(mContext).getString(R.string.force_closed, appName), Toast.LENGTH_SHORT).show();
                        } catch (Throwable ignore) {
                        }
                    } else if (view == mOpenFwBtn) {
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
                mMenuItems.forEach((Consumer) obj -> {
                    View menuView = (View) XposedHelpers.callMethod(obj, "getMenuView");
                    ((TextView) menuView.findViewById(titleId)).setMaxWidth(menuWidth);
                });
            }
        });
    }
}
