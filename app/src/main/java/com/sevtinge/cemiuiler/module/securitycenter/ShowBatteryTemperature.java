package com.sevtinge.cemiuiler.module.securitycenter;

import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.DisplayUtils;

import de.robv.android.xposed.XposedHelpers;

public class ShowBatteryTemperature extends BaseHook {

    Class<?> m;
    Class<?> m2;

    @Override
    public void init() {

        MethodHook methodHook = new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(String.valueOf(getBatteryTemperature((Context) param.args[0])));
            }
        };

        try {
            m = findClassIfExists("com.miui.powercenter.a");
            findAndHookMethod(m, "b", Context.class ,methodHook);
        } catch (Throwable t) {
            m = findClassIfExists("com.miui.powercenter.BatteryFragment");
            findAndHookMethod(m, "b", Context.class ,methodHook);
        }

        try {
            m2 = findClass("com.miui.powercenter.a$a");
        } catch (Throwable t) {
            m2 = findClass("com.miui.powercenter.BatteryFragment$a");
        }

        findAndHookMethod(m2, "run", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                Context context = AndroidAppHelper.currentApplication().getApplicationContext();
                boolean isDarkMode = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                int currentTemperatureValue = context.getResources().getIdentifier(
                        "current_temperature_value",
                        "id",
                        "com.miui.securitycenter"
                );

                View view = (View) XposedHelpers.getObjectField(param.thisObject, "a");

                TextView textView = view.findViewById(currentTemperatureValue);
                ((LinearLayout.LayoutParams) textView.getLayoutParams()).setMarginStart(DisplayUtils.dip2px(context, 25f));
                ((LinearLayout.LayoutParams) textView.getLayoutParams()).topMargin = 0;

                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 36.399998f);
                textView.setPadding(0, 0, 0, 0);
                textView.setGravity(Gravity.NO_GRAVITY);
                textView.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                textView.setHeight(DisplayUtils.dip2px(context, 49.099983f));
                textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

                TextView tempView = new  TextView(context);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, DisplayUtils.dip2px(context, 49.099983f));
                layoutParams.setMarginStart(DisplayUtils.dip2px(context, 3.599976f));

                tempView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.099977f);
                tempView.setTextColor(Color.parseColor(isDarkMode ? "#e6e6e6" : "#333333"));
                tempView.setPadding(0, DisplayUtils.dip2px(context, 25f), 0, 0);
                tempView.setText("℃");
                tempView.setTypeface(Typeface.create(null, 500, false));
                tempView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

                int tempeValueContainer = context.getResources().getIdentifier(
                        "tempe_value_container",
                        "id",
                        "com.miui.securitycenter"
                );

                LinearLayout linearLayout = view.findViewById(tempeValueContainer);
                linearLayout.addView(tempView);
            }
        });
    }

    private int getBatteryTemperature(Context context) {
        return context.registerReceiver((BroadcastReceiver) null, new IntentFilter("android.intent.action.BATTERY_CHANGED")).getIntExtra("temperature", 0) / 10;
    }
}
