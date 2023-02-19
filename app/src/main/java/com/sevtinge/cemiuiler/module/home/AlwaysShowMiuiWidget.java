package com.sevtinge.cemiuiler.module.home;

import android.content.ComponentName;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class AlwaysShowMiuiWidget extends BaseHook {

    Class<?> mWidgetsVerticalAdapter;
    Class<?> mBaseWidgetsVerticalAdapter;
    Class<?> mMIUIAppWidgetInfo;
    Class<?> mMIUIWidgetUtil;

    XC_MethodHook.Unhook hook1;
    XC_MethodHook.Unhook hook2;

    @Override
    public void init() {
        mWidgetsVerticalAdapter = findClassIfExists("com.miui.home.launcher.widget.WidgetsVerticalAdapter");
        mBaseWidgetsVerticalAdapter = findClassIfExists("com.miui.home.launcher.widget.BaseWidgetsVerticalAdapter");
        mMIUIAppWidgetInfo = findClassIfExists("com.miui.home.launcher.widget.MIUIAppWidgetInfo");
        mMIUIWidgetUtil = findClassIfExists("com.miui.home.launcher.MIUIWidgetUtil");


        XposedHelpers.findAndHookMethod(mMIUIAppWidgetInfo, "initMiuiAttribute", ComponentName.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.setBooleanField(param.thisObject, "isMIUIWidget", false);
            }
        });

        XposedHelpers.findAndHookMethod(mMIUIWidgetUtil, "isMIUIWidgetSupport", XC_MethodReplacement.returnConstant(false));

        /*MethodHook m = new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                XposedHelpers.findAndHookMethod(mMIUIAppWidgetInfo, "initMiuiAttribute", ComponentName.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        XposedHelpers.setBooleanField(param.thisObject, "isMIUIWidget", false);
                    }
                });

                XposedHelpers.findAndHookMethod(mMIUIWidgetUtil, "isMIUIWidgetSupport", XC_MethodReplacement.returnConstant(false));
            }

            @Override
            protected void after(MethodHookParam param) throws Throwable {
                XposedHelpers.findAndHookMethod(mMIUIAppWidgetInfo, "initMiuiAttribute", ComponentName.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        XposedHelpers.setBooleanField(param.thisObject, "isMIUIWidget", true);
                    }
                });

                XposedHelpers.findAndHookMethod(mMIUIWidgetUtil, "isMIUIWidgetSupport", XC_MethodReplacement.returnConstant(true));
            }
        };

        try {
            hookAllMethods(mWidgetsVerticalAdapter, "buildAppWidgetsItems", m);
        } catch (Exception e) {
            hookAllMethods(mBaseWidgetsVerticalAdapter, "buildAppWidgetsItems", m);
        }*/

    }
}
