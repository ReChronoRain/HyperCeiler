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
package com.sevtinge.hyperceiler.module.hook.various;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.BlurUtils;

import java.lang.reflect.Field;

import de.robv.android.xposed.XposedHelpers;

public class NotificationBlur extends BaseHook {

    Class<?> mCls = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.notification.row.NotificationBackgroundView", lpparam.classLoader);
    Class<?> mCls2 = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.notification.policy.AppMiniWindowRowTouchHelper", lpparam.classLoader);
    Class<?> mCls3 = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController", lpparam.classLoader);
    Class<?> mCls4 = XposedHelpers.findClassIfExists("com.android.keyguard.magazine.LockScreenMagazineController", lpparam.classLoader);
    Class<?> mCls5 = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.phone.MiuiNotificationPanelViewController$mBlurRatioChangedListener$1", lpparam.classLoader);
    Class<?> mCls6 = XposedHelpers.findClassIfExists("com.android.systemui.shared.plugins.PluginInstanceManager$PluginHandler", lpparam.classLoader);
    Class<?> mCls7 = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow", lpparam.classLoader);
    Class<?> mCls8 = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout", lpparam.classLoader);

    @Override
    public void init() {

        hookAllMethods(mCls, "setCustomBackground", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                super.after(param);
                Field field = mCls.getDeclaredField("mDrawableAlpha");
                field.setAccessible(true);
                field.set(param.thisObject, 200);
                XposedHelpers.callMethod(param.thisObject, "setDrawableAlpha", 200);
            }
        });

        hookAllMethods(mCls, "draw", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                super.after(param);
                Drawable background = ((View) param.thisObject).getBackground();

                if (background != null && background.getClass().getName().equals("BackgroundBlurDrawable")) {
                    background.setBounds(((Drawable) param.args[1]).getBounds());
                }
            }
        });

        hookAllMethods(mCls2, "onMiniWindowTrackingStart", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                super.before(param);

                Field field = param.thisObject.getClass().getDeclaredField("mPickedMiniWindowChild");
                field.setAccessible(true);
                field.get(param.thisObject);

                Field field2 = param.thisObject.getClass().getDeclaredField("mBackgroundNormal");
                field2.setAccessible(true);
                field2.get(field);


                if (field != null && field2 != null) {
                    View view = (View) (Object) field2;
                    if (view.getBackground().getClass().getName().equals("BackgroundBlurDrawable")) {
                        Drawable background = view.getBackground();
                        XposedHelpers.callMethod(background, "setVisible", false, false);
                        XposedHelpers.callMethod(field2, "setDrawableAlpha", 200 + 30);
                    }
                }

            }
        });

        hookAllMethods(mCls3, "onStateChanged", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                super.after(param);

                int childCount;
                int i = 0;
                Object obj = param.args[0];
                int intValue = (Integer) obj;
                Object obj2 = param.thisObject;

                Object d;
                Field field = param.thisObject.getClass().getDeclaredField("mNotificationStackScroller");
                field.setAccessible(true);
                field.get(obj2);
                d = field;


                ViewGroup viewGroup = (ViewGroup) d;
                if (intValue != 1) {
                    int childCount2 = viewGroup.getChildCount();
                    if (childCount2 >= 0) {
                        int i2 = 0;
                        while (true) {
                            i2++;
                            View childAt = viewGroup.getChildAt(i2);
                            if (childAt != null) {
                                try {
                                    Object callMethod = XposedHelpers.callMethod(childAt, "isHeadsUpState");
                                    if (callMethod != null) {
                                        boolean booleanValue = (Boolean) callMethod;
                                        Object callMethod2 = XposedHelpers.callMethod(childAt, "isPinned");
                                        if (callMethod2 != null) {
                                            boolean booleanValue2 = (Boolean) callMethod2;
                                            if (!booleanValue || !booleanValue2) {
                                                /*new BlurUtils(childAt);*/
                                                new BlurUtils(childAt, "default");
                                            }
                                        } else {
                                            throw new NullPointerException("null cannot be cast to non-null type kotlin.Boolean");
                                        }
                                    } else {
                                        throw new NullPointerException("null cannot be cast to non-null type kotlin.Boolean");
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                            if (i2 == childCount2) {
                                return;
                            }
                        }
                    }
                }
            }
        });


        hookAllMethods(mCls4, "setViewsAlpha", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                super.before(param);

                boolean b;
                Class<?> aClass = XposedHelpers.findClassIfExists("com.android.keyguard.utils.MiuiKeyguardUtils", lpparam.classLoader);
                if (aClass == null) {
                    b = true;
                }
                Object callStaticMethod = XposedHelpers.callStaticMethod(aClass, "isDefaultLockScreenTheme");

                b = (Boolean) callStaticMethod;

                if (b) {

                    int i = 0;

                    float floatValue = (Float) param.args[0] * 255;

                    Object d;
                    Field field = param.thisObject.getClass().getDeclaredField("mNotificationStackScrollLayout");
                    field.setAccessible(true);
                    field.get(param.thisObject);
                    d = field;

                    ViewGroup viewGroup = (ViewGroup) d;
                    int childCount = viewGroup.getChildCount();
                    if (childCount >= 0) {
                        while (true) {
                            i++;
                            View childAt = viewGroup.getChildAt(i);
                            if (childAt != null) {
                                if (floatValue >= 0 && floatValue <= 255) {
                                    if (childAt.getClass().getName().equals("ZenModeView")) {
                                        Object callMethod = XposedHelpers.callMethod(childAt, "getContentView");
                                        if (callMethod != null) {
                                            ViewGroup viewGroup2 = (ViewGroup) callMethod;
                                            Drawable background = viewGroup.getBackground();
                                            if (background != null && background.getClass().getName().equals("BackgroundBlurDrawable")) {
                                                XposedHelpers.callMethod(viewGroup.getBackground(), "setAlpha", i);
                                                return;
                                            }
                                            return;
                                        }
                                        return;
                                    }
                                }
                            }
                            if (i == childCount) {
                                return;
                            }
                        }
                    }


                }
            }
        });




        /*hookAllMethods(mCls,"setHighSamplingFrequency",new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                View view = (View) param.thisObject;
                new BlurUtils(view);
            }
        });

        hookAllMethods(mCls2,"setCustomBackground",new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                super.after(param);
                XposedHelpers.callMethod(param.thisObject, "setDrawableAlpha", new Object[]{160});
            }
        });

        hookAllMethods(mCls2,"draw",new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                super.after(param);

                Object obj = param.thisObject;
                Drawable background = ((View) obj).getBackground();
                if (background != null) {
                    Object obj2 = param.args[1];
                    background.setBounds(((Drawable) obj2).getBounds());
                }
            }
        });



        hookAllMethods(mCls3,"startEnterAndLaunchMiniWindow",new MethodHook() {

            @Override
            protected void before(MethodHookParam param) throws Throwable {
                super.before(param);

                Field declaredField = mCls3.getDeclaredField("mPickedMiniWindowChild");
                declaredField.setAccessible(true);
                Object obj = declaredField.get(param.thisObject);
                if (obj != null) {
                    Object callMethod = XposedHelpers.callMethod(obj, "getAnimatedBackground", new Object[0]);
                    View view = (View) callMethod;
                    view.setBackground(null);
                }
            }
        });


        Class<?> mCls5 = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.notification.row.MiuiExpandableNotificationRow", lpparam.classLoader);


        hookAllMethods(mCls,"updateBackgroundBg",new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                View view = (View) XposedHelpers.getObjectField(param.thisObject,"mBackgroundNormal");
                new BlurUtils(view);
            }
        });*/
    }
}
