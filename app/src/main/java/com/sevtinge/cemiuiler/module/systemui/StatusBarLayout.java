package com.sevtinge.cemiuiler.module.systemui;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class StatusBarLayout extends BaseHook {

    int mStatusBarTop = 0;
    int mStatusBarLeft = 0;
    int mStatusBarRight = 0;
    int mStatusBarBottom = 0;

    LinearLayout mLeftLayout = null;
    LinearLayout mRightLayout = null;
    LinearLayout mCenterLayout;
    ViewGroup mStatusBar = null;

    Class<?> mCollapsedStatusBarFragment;

    @Override
    public void init() {

        mCollapsedStatusBarFragment = findClassIfExists("com.android.systemui.statusbar.phone.CollapsedStatusBarFragment");

        findAndHookMethod(mCollapsedStatusBarFragment, "onViewCreated", View.class, Bundle.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ViewGroup mStatusBarView = (ViewGroup) XposedHelpers.getObjectField(param.thisObject,"mStatusBar");
                Context mContext = mStatusBarView.getContext();
                Resources res = mStatusBarView.getResources();

                int mStatusBarId = res.getIdentifier(
                        "status_bar",
                        "id",
                        "com.android.systemui"
                );

                int mStatusBarContentsId = res.getIdentifier(
                        "status_bar_contents",
                        "id",
                        "com.android.systemui"
                );

                int mStatusBarLeftContainerId = res.getIdentifier(
                        "phone_status_bar_left_container",
                        "id",
                        "com.android.systemui"
                );

                int mClockId = res.getIdentifier(
                        "clock",
                        "id",
                        "com.android.systemui"
                );

                int mStatusBarLeftIconAreaId = res.getIdentifier(
                                "drip_left_status_icon_area",
                                "id",
                                "com.android.systemui"
                        );

                int mStatusBarLeftIconId = res.getIdentifier(
                        "drip_left_statusIcon",
                        "id",
                        "com.android.systemui"
                );

                int mLeftNotificationIconAreaId = res.getIdentifier(
                        "drip_notification_icon_area",
                        "id",
                        "com.android.systemui"
                );


                int mSystemIconAreaId = res.getIdentifier(
                        "system_icon_area",
                        "id",
                        "com.android.systemui"
                );

                int mFullScreenNotificationIconAreaId = res.getIdentifier(
                        "fullscreen_notification_icon_area",
                        "id",
                        "com.android.systemui"
                );

                int mSystemIconsId = res.getIdentifier(
                        "system_icons",
                        "id",
                        "com.android.systemui"
                );

                int mNotificationIconAreaInnerId = res.getIdentifier(
                        "notification_icon_area_inner",
                        "id",
                        "com.android.systemui"
                );

                int mStatusIconsId = res.getIdentifier(
                        "statusIcons",
                        "id",
                        "com.android.systemui"
                );

                int mBatteryId = res.getIdentifier(
                        "battery",
                        "id",
                        "com.android.systemui"
                );

                mStatusBar = mStatusBarView.findViewById(mStatusBarId);

                ViewGroup mStatusBarContents = mStatusBarView.findViewById(mStatusBarContentsId);
                if (mStatusBar == null) return;

                ViewGroup mStatusBarLeftContainer = mStatusBarView.findViewById(mStatusBarLeftContainerId);

                TextView mClock = mStatusBarView.findViewById(mClockId);
                ViewGroup mStatusBarLeftIconArea = mStatusBarView.findViewById(mStatusBarLeftIconAreaId);
                ViewGroup mStatusBarLeftIcon = mStatusBarView.findViewById(mStatusBarLeftIconId);
                ViewGroup mLeftNotificationIconArea = mStatusBarView.findViewById(mLeftNotificationIconAreaId);
                ViewGroup mSystemIconArea = mStatusBarView.findViewById(mSystemIconAreaId);
                ViewGroup mSystemIcons = mStatusBarView.findViewById(mSystemIconsId);
                ViewGroup mFullScreenNotificationIconArea = mStatusBarView.findViewById(mFullScreenNotificationIconAreaId);
                ViewGroup mBattery = mStatusBarView.findViewById(mBatteryId);

                /*ViewGroup mNotificationIconAreaInner = mStatusBarView.findViewById(mNotificationIconAreaInnerId);
                ViewGroup mStatusIcons = mStatusBarView.findViewById(mStatusIconsId);*/

                ((ViewGroup) mClock.getParent()).removeView(mClock);

                ((ViewGroup) mStatusBarLeftContainer.getParent()).removeView(mStatusBarLeftContainer);

                ((ViewGroup) mSystemIconArea.getParent()).removeView(mSystemIconArea);
                ((ViewGroup) mSystemIcons.getParent()).removeView(mSystemIcons);
                ((ViewGroup) mFullScreenNotificationIconArea.getParent()).removeView(mFullScreenNotificationIconArea);
                ((ViewGroup) mBattery.getParent()).removeView(mBattery);

                /*((ViewGroup) mNotificationIconAreaInner.getParent()).removeView(mNotificationIconAreaInner);
                ((ViewGroup) mStatusIcons.getParent()).removeView(mStatusIcons);*/

                FrameLayout mConstraintLayout = new FrameLayout(mContext);

                /*ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                );
                mConstraintLayout.setLayoutParams(layoutParams);

                mConstraintLayout.addView(mSystemIconArea);
                mConstraintLayout.addView(mBattery);


                ConstraintLayout.LayoutParams mBatteryLayoutParams = new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                );
                mBatteryLayoutParams.endToEnd = 0;
                mBattery.setLayoutParams(mBatteryLayoutParams);


                ConstraintLayout.LayoutParams mFullScreenNotificationIconAreaLayoutParams = new ConstraintLayout.LayoutParams(
                        0,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                );*/
                /*mFullScreenNotificationIconAreaLayoutParams.startToEnd = mBatteryId;
                mFullScreenNotificationIconAreaLayoutParams.endToEnd = 0;
                mFullScreenNotificationIconArea.setLayoutParams(mFullScreenNotificationIconAreaLayoutParams);*/


                //增加一个左对齐布局
                mLeftLayout = getLeftLayout(mContext);
                mLeftLayout.addView(mClock,0);
                mLeftLayout.addView(mStatusBarLeftContainer);
                /*mLeftLayout.addView(mNotificationIconAreaInner);*/

                //增加一个居中布局
                mCenterLayout = getCenterLayout(mContext);

                //增加一个右布局
                mRightLayout = getRightLayout(mContext);

                /*mRightLayout.addView(mStatusIcons);*/
                mRightLayout.addView(mConstraintLayout);
                mFullScreenNotificationIconArea.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

                //添加所有布局
                mStatusBarContents.addView(mLeftLayout, 0);
                mStatusBarContents.addView(mCenterLayout);
                mStatusBarContents.addView(mRightLayout);

                mStatusBarTop = mStatusBar.getPaddingTop();
                mStatusBarLeft = mStatusBar.getPaddingLeft();
                mStatusBarRight = mStatusBar.getPaddingRight();
                mStatusBarBottom = mStatusBar.getPaddingBottom();

                /*updateLayout(mContext);*/
            }
        });



        //解决重叠
        findAndHookMethod("com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment", "showClock", boolean.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {

                ViewGroup mStatusBarView = (ViewGroup) XposedHelpers.getObjectField(param.thisObject,"mStatusBar");
                Context mContext = mStatusBarView.getContext();
                Resources res = mStatusBarView.getResources();

                int mStatusBarId = res.getIdentifier(
                        "status_bar",
                        "id",
                        "com.android.systemui"
                );

                //非锁屏下整个状态栏布局
                ViewGroup mStatusBar = mStatusBarView.findViewById(mStatusBarId);
                KeyguardManager keyguardMgr = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
                mStatusBar.setVisibility(keyguardMgr.isKeyguardLocked() ? View.GONE : View.VISIBLE);
            }
        });
    }

    LinearLayout getLeftLayout(Context context) {
        LinearLayout mLeftLayout = new LinearLayout(context);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        );
        mLeftLayout.setLayoutParams(leftLp);
        mLeftLayout.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        return mLeftLayout;
    }

    LinearLayout getCenterLayout(Context context) {
        LinearLayout mCenterLayout = new LinearLayout(context);
        LinearLayout.LayoutParams centerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        mCenterLayout.setLayoutParams(centerLp);
        mCenterLayout.setGravity(Gravity.CENTER|Gravity.CENTER_VERTICAL);
        return mCenterLayout;
    }

    LinearLayout getRightLayout(Context context) {
        LinearLayout mRightLayout = new LinearLayout(context);
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
        );
        mRightLayout.setLayoutParams(rightLp);
        mRightLayout.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        return mRightLayout;
    }

    void updateLayout(Context context) {
        //判断屏幕方向
        Configuration mConfiguration = context.getResources().getConfiguration();
        boolean isOrientationPortrait = mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT;
        mLeftLayout.setPadding(isOrientationPortrait ? mStatusBarLeft : 175, 0, 0, 0);
        mRightLayout.setPadding(0, 0, isOrientationPortrait ? mStatusBarRight : 175, 0);
        mStatusBar.setPadding(0, mStatusBarTop, 0, mStatusBarBottom);
    }
}
