package com.sevtinge.cemiuiler.module.systemui.statusbar;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class BatteryIcon extends BaseHook {

    TextView mBatteryTextDigitView;
    TextView mBatteryPercentView;
    TextView mBatteryPercentMarkView;
    ImageView mBatteryChargingView;
    ImageView mBatteryChargingInView;

    Class<?> mMiuiBatteryMeterView;

    @Override
    public void init() {
        mMiuiBatteryMeterView = findClassIfExists("com.android.systemui.statusbar.views.MiuiBatteryMeterView");

        findAndHookMethod(mMiuiBatteryMeterView, "updateChargeAndText", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {

                mBatteryTextDigitView = (TextView) XposedHelpers.getObjectField(param.thisObject, "mBatteryTextDigitView");
                mBatteryPercentView = (TextView)XposedHelpers.getObjectField(param.thisObject, "mBatteryPercentView");
                mBatteryPercentMarkView = (TextView)XposedHelpers.getObjectField(param.thisObject, "mBatteryPercentMarkView");

                if (mPrefsMap.getBoolean("system_ui_status_bar_battery_percent")) {
                    mBatteryTextDigitView.setVisibility(View.GONE);
                    mBatteryPercentView.setVisibility(View.GONE);
                }

                if (mPrefsMap.getBoolean("system_ui_status_bar_battery_percent") || mPrefsMap.getBoolean("system_ui_status_bar_battery_percent_mark")) {
                    mBatteryPercentMarkView.setVisibility(View.GONE);
                }

                if (mPrefsMap.getBoolean("system_ui_status_bar_battery_charging")) {
                    mBatteryChargingView = (ImageView)XposedHelpers.getObjectField(param.thisObject, "mBatteryChargingView");
                    mBatteryChargingView.setVisibility(View.GONE);
                    try {
                        mBatteryChargingInView = (ImageView)XposedHelpers.getObjectField(param.thisObject, "mBatteryChargingInView");
                        mBatteryChargingInView.setVisibility(View.GONE);
                    } catch (Throwable ignore) {}
                }
            }
        });
    }
}
