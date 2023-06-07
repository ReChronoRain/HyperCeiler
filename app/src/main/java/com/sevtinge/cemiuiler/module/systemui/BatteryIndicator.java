package com.sevtinge.cemiuiler.module.systemui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.FrameLayout;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.view.BatteryIndicatorView;

import de.robv.android.xposed.XposedHelpers;

public class BatteryIndicator extends BaseHook {

    @Override
    @SuppressLint("DiscouragedApi")
    public void init() {
        hookAllMethods("com.android.systemui.statusbar.phone.StatusBar", "makeStatusBarView", new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                FrameLayout mStatusBarWindow = (FrameLayout) XposedHelpers.getObjectField(param.thisObject, "mPhoneStatusBarWindow");
                BatteryIndicatorView indicator = new BatteryIndicatorView(mContext);
                View panel = mStatusBarWindow.findViewById(mContext.getResources().getIdentifier("notification_panel", "id", lpparam.packageName));
                mStatusBarWindow.addView(indicator, panel != null ? mStatusBarWindow.indexOfChild(panel) + 1 : Math.max(mStatusBarWindow.getChildCount() - 1, 2));
                indicator.setAdjustViewBounds(false);
                indicator.init(param.thisObject);
                XposedHelpers.setAdditionalInstanceField(param.thisObject, "mBatteryIndicator", indicator);
                Object mNotificationIconAreaController = XposedHelpers.getObjectField(param.thisObject, "mNotificationIconAreaController");
                XposedHelpers.setAdditionalInstanceField(mNotificationIconAreaController, "mBatteryIndicator", indicator);
                Object mBatteryController = XposedHelpers.getObjectField(param.thisObject, "mBatteryController");
                XposedHelpers.setAdditionalInstanceField(mBatteryController, "mBatteryIndicator", indicator);
                XposedHelpers.callMethod(mBatteryController, "fireBatteryLevelChanged");
                XposedHelpers.callMethod(mBatteryController, "firePowerSaveChanged");
//                XposedHelpers.callMethod(mBatteryController, "fireExtremePowerSaveChanged");
            }
        });

        findAndHookMethod("com.android.systemui.statusbar.phone.StatusBar", "setPanelExpanded", boolean.class, new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                boolean isKeyguardShowing = (boolean) XposedHelpers.callMethod(param.thisObject, "isKeyguardShowing");
                BatteryIndicatorView indicator = (BatteryIndicatorView) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mBatteryIndicator");
                if (indicator != null) indicator.onExpandingChanged(!isKeyguardShowing && (boolean) param.args[0]);
            }
        });

        findAndHookMethod("com.android.systemui.statusbar.phone.StatusBar", "setQsExpanded", boolean.class, new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                boolean isKeyguardShowing = (boolean) XposedHelpers.callMethod(param.thisObject, "isKeyguardShowing");
                if (!isKeyguardShowing) return;
                BatteryIndicatorView indicator = (BatteryIndicatorView) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mBatteryIndicator");
                if (indicator != null) indicator.onExpandingChanged((boolean) param.args[0]);
            }
        });

        findAndHookMethod("com.android.systemui.statusbar.phone.StatusBar", "updateKeyguardState", new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                boolean isKeyguardShowing = (boolean) XposedHelpers.callMethod(param.thisObject, "isKeyguardShowing");
                BatteryIndicatorView indicator = (BatteryIndicatorView) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mBatteryIndicator");
                if (indicator != null) indicator.onKeyguardStateChanged(isKeyguardShowing);
            }
        });

        findAndHookMethod("com.android.systemui.statusbar.phone.NotificationIconAreaController", "onDarkChanged", Rect.class, float.class, int.class, new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                BatteryIndicatorView indicator = (BatteryIndicatorView) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mBatteryIndicator");
                if (indicator != null) indicator.onDarkModeChanged((float) param.args[1], (int) param.args[2]);
            }
        });

        findAndHookMethod("com.android.systemui.statusbar.policy.MiuiBatteryControllerImpl", "fireBatteryLevelChanged", new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                BatteryIndicatorView indicator = (BatteryIndicatorView) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mBatteryIndicator");
                int mLevel = XposedHelpers.getIntField(param.thisObject, "mLevel");
                boolean mCharging = XposedHelpers.getBooleanField(param.thisObject, "mCharging");
                boolean mCharged = XposedHelpers.getBooleanField(param.thisObject, "mCharged");
                if (indicator != null) indicator.onBatteryLevelChanged(mLevel, mCharging, mCharged);
            }
        });

        findAndHookMethod("com.android.systemui.statusbar.policy.BatteryControllerImpl", "firePowerSaveChanged", new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                BatteryIndicatorView indicator = (BatteryIndicatorView) XposedHelpers.getAdditionalInstanceField(param.thisObject, "mBatteryIndicator");
                if (indicator != null)
                    indicator.onPowerSaveChanged(XposedHelpers.getBooleanField(param.thisObject, "mPowerSave"));
            }
        });
    }
}
