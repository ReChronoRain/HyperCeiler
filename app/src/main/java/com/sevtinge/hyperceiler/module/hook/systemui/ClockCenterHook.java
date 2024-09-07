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

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class ClockCenterHook extends BaseHook {

    private static LinearLayout mLeftLayout = null;
    private static LinearLayout mRightLayout = null;
    private static LinearLayout mCenterLayout;
    private static ViewGroup statusBar = null;

    Class<?> mStatusBarView;

    @Override
    public void init() {

        mStatusBarView = findClassIfExists("com.android.systemui.statusbar.phone.CollapsedStatusBarFragment");

        findAndHookMethod(mStatusBarView, "onViewCreated", View.class, Bundle.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ViewGroup miuiPhoneStatusBarView = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mStatusBar");
                Context context = miuiPhoneStatusBarView.getContext();
                Resources res = miuiPhoneStatusBarView.getResources();

                int statusBarId = res.getIdentifier("status_bar", "id", "com.android.systemui");

                int statusBarContentsId = res.getIdentifier("status_bar_contents", "id", "com.android.systemui");
                int systemIconAreaId = res.getIdentifier("system_icon_area", "id", "com.android.systemui");
                int clockId = res.getIdentifier("clock", "id", "com.android.systemui");
                int phoneStatusBarLeftContainerId =
                    res.getIdentifier(
                        "phone_status_bar_left_container",
                        "id",
                        "com.android.systemui"
                    );

                int fullscreenNotificationIconAreaId =
                    res.getIdentifier(
                        "fullscreen_notification_icon_area",
                        "id",
                        "com.android.systemui"
                    );
                int statusIconsId =
                    res.getIdentifier(
                        "statusIcons",
                        "id",
                        "com.android.systemui"
                    );
                int systemIconsId =
                    res.getIdentifier(
                        "system_icons",
                        "id",
                        "com.android.systemui"
                    );
                int batteryId =
                    res.getIdentifier(
                        "battery",
                        "id",
                        "com.android.systemui"
                    );

                statusBar = miuiPhoneStatusBarView.findViewById(statusBarId);
                ViewGroup statusBarContents = miuiPhoneStatusBarView.findViewById(statusBarContentsId);


                TextView clock = miuiPhoneStatusBarView.findViewById(clockId);
                ViewGroup phoneStatusBarLeftContainer =
                    miuiPhoneStatusBarView.findViewById(phoneStatusBarLeftContainerId);

                ViewGroup fullscreenNotificationIconArea =
                    miuiPhoneStatusBarView.findViewById(fullscreenNotificationIconAreaId);
                ViewGroup systemIconArea =
                    miuiPhoneStatusBarView.findViewById(systemIconAreaId);
                ViewGroup statusIcons =
                    miuiPhoneStatusBarView.findViewById(statusIconsId);
                ViewGroup systemIcons =
                    miuiPhoneStatusBarView.findViewById(systemIconsId);
                ViewGroup battery =
                    miuiPhoneStatusBarView.findViewById(batteryId);

                ((ViewGroup) clock.getParent()).removeView(clock);
                phoneStatusBarLeftContainer.removeView(phoneStatusBarLeftContainer);

                ((ViewGroup) systemIconArea.getParent()).removeView(systemIconArea);
                ((ViewGroup) statusIcons.getParent()).removeView(statusIcons);
                ((ViewGroup) systemIcons.getParent()).removeView(systemIcons);
                ((ViewGroup) battery.getParent()).removeView(battery);
                ((ViewGroup) fullscreenNotificationIconArea.getParent()).removeView(
                    fullscreenNotificationIconArea
                );

                FrameLayout mConstraintLayout = new FrameLayout(context);

                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

                mConstraintLayout.setLayoutParams(layoutParams);
                mConstraintLayout.addView(fullscreenNotificationIconArea);
                mConstraintLayout.addView(battery);

                FrameLayout.LayoutParams layoutParams2 = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT);
                battery.setLayoutParams(layoutParams2);

                FrameLayout.LayoutParams layoutParams3 = new FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT);
                fullscreenNotificationIconArea.setLayoutParams(layoutParams3);

                fullscreenNotificationIconArea.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

                // 增加一个左对齐布局
                mLeftLayout = new LinearLayout(context);
                LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
                mLeftLayout.setLayoutParams(leftLp);
                mLeftLayout.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

                // 增加一个居中布局
                mCenterLayout = new LinearLayout(context);
                LinearLayout.LayoutParams centerLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                );
                mCenterLayout.setLayoutParams(centerLp);
                mCenterLayout.setGravity(Gravity.CENTER | Gravity.CENTER_VERTICAL);

                // 增加一个右布局
                mRightLayout = new LinearLayout(context);
                LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
                mRightLayout.setLayoutParams(rightLp);
                mRightLayout.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

                mLeftLayout.addView(phoneStatusBarLeftContainer);
                mLeftLayout.addView(statusIcons);
                statusIcons.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

                mCenterLayout.addView(clock);

                mRightLayout.addView(mConstraintLayout);
                fullscreenNotificationIconArea.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

                statusBarContents.addView(mLeftLayout, 0);
                statusBarContents.addView(mCenterLayout);
                statusBarContents.addView(mRightLayout);
            }
        });
    }
}
