package com.sevtinge.cemiuiler.module.securitycenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.reflect.Method;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import moralnorm.internal.utils.DisplayUtils;

public class ShowBatteryTemperature extends BaseHook {

    Class<?> mBatteryFragment;
    Class<?> mBatteryFragment$a;

    @Override
    public void init() {
        mBatteryFragment = findClassIfExists("com.miui.powercenter.BatteryFragment", "com.miui.powercenter.a");
        mBatteryFragment$a = findClassIfExists("com.miui.powercenter.BatteryFragment$a", "com.miui.powercenter.a$a");

        Method[] methods = mBatteryFragment.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getReturnType() == String.class && method.getParameterCount() == 1 ) {
                hookMethod(method, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        String mBatteryTemperature = String.valueOf(getBatteryTemperature((Context) param.args[0]));
                        param.setResult(mBatteryTemperature);
                    }
                });
            }
        }

        findAndHookMethod(mBatteryFragment$a, "run", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                View view = (View) XposedHelpers.getObjectField(param.thisObject, "a");
                Context mContext = view.getContext();
                boolean isDarkMode = (mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                int currentTemperatureStateId = mContext.getResources().getIdentifier("current_temperature_state", "id", "com.miui.securitycenter");

                TextView mTemperatureStateTv = view.findViewById(currentTemperatureStateId);
                applyTextView(mContext, mTemperatureStateTv);

                RelativeLayout mContainerView = (RelativeLayout) mTemperatureStateTv.getParent();
                mContainerView.removeView(mTemperatureStateTv);

                TextView mTemperatureUnit = new TextView(mContext);
                applyTemperatureStyle(mContext, mTemperatureUnit, isDarkMode);

                LinearLayout mTemperature = new LinearLayout(mContext);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, mContainerView.getId());
                params.setMargins(0, 0, 0, DisplayUtils.dp2px(mContext, 21.5f));
                mTemperature.setLayoutParams(params);

                mTemperature.addView(mTemperatureStateTv);
                mTemperature.addView(mTemperatureUnit);

                mContainerView.addView(mTemperature);
            }
        });
    }

    private void applyTextView(Context context, TextView tv) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(lp);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 34.6f);
        tv.setGravity(Gravity.BOTTOM);
        tv.setIncludeFontPadding(false);
        tv.setTypeface(Typeface.create(null, 500, false));
    }

    private void applyTemperatureStyle(Context context, TextView tv, boolean isDarkMode) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(DisplayUtils.dp2px(context, 3), 0,0, 0);
        tv.setLayoutParams(lp);
        tv.setText("℃");
        tv.setTextColor(Color.parseColor(isDarkMode ? "#e6e6e6" : "#333333"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        tv.setGravity(Gravity.BOTTOM);
        tv.setTypeface(Typeface.create(null, 700, false));
        tv.setHeight((int) 49f);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
    }

    private int getBatteryTemperature(Context context) {
        return context.registerReceiver((BroadcastReceiver) null, new IntentFilter("android.intent.action.BATTERY_CHANGED")).getIntExtra("temperature", 0) / 10;
    }
}