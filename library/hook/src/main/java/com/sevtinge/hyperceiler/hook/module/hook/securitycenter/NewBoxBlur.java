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
package com.sevtinge.hyperceiler.hook.module.hook.securitycenter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.utils.blur.BlurUtils;
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils;

import de.robv.android.xposed.XposedHelpers;

public class NewBoxBlur extends BaseHook {

    Class<?> mNewBoxCls;
    Class<?> mTurboaLayout;
    Class<?> mWindowManager;
    Class<?> mVideoBoxCls;


    Class<?> mTurboLayout;
    Class<?> mDockLayout;

    @Override
    public void init() {

        mDockLayout = findClassIfExists("com.miui.gamebooster.windowmanager.newbox.j");
        mTurboLayout = findClassIfExists("com.miui.gamebooster.windowmanager.newbox.TurboLayout");

        mNewBoxCls = findClassIfExists("com.miui.gamebooster.windowmanager.newbox.i");
        mTurboaLayout = findClassIfExists("com.miui.gamebooster.windowmanager.newbox.NewToolBoxTopView");
        mWindowManager = findClassIfExists("com.miui.gamebooster.windowmanager.j");

        mVideoBoxCls = findClassIfExists("com.miui.gamebooster.videobox.adapter.i");


        hookAllConstructors(mDockLayout, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) {
                ViewGroup view = (ViewGroup) param.thisObject;
                int paddingVertical = DisplayUtils.dp2px(mPrefsMap.getInt("security_center_newbox_bg_padding_vertical", 10));
                int paddingHorizontal = DisplayUtils.dp2px(mPrefsMap.getInt("security_center_newbox_bg_padding_horizontal", 10));
                view.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
                new BlurUtils(view, "security_center_newbox_bg_custom");
            }
        });

        //
        hookAllConstructors(mTurboLayout, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ViewGroup view = (ViewGroup) XposedHelpers.callMethod(param.thisObject, "getDockLayout");
                new BlurUtils(view, "security_center_newbox_bg_custom");
            }
        });

        /*findAndHookConstructor(mNewBoxCls, Context.class, boolean.class, String.class, mWindowManager, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                View view = (View) param.thisObject;
                new BlurUtils(view, "various_new_box_blur");

            }
        });*/


        findAndHookMethod(mTurboaLayout, "a", boolean.class, boolean.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ImageView view = (ImageView) XposedHelpers.getObjectField(param.thisObject, "j");
                GradientDrawable shapeDrawable = new GradientDrawable();
                shapeDrawable.setColor(Color.TRANSPARENT);
                view.setImageDrawable(shapeDrawable);
            }
        });

        findAndHookMethod(mTurboaLayout, "onAttachedToWindow", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                View view = (View) param.thisObject;
                new BlurUtils(view, "security_center_newbox_bg_custom");
            }
        });

        findAndHookMethod(mVideoBoxCls, "a", Context.class, boolean.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ViewGroup viewGroup = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "b");
                new BlurUtils(viewGroup, "security_center_newbox_bg_custom");
            }
        });

    }
}
