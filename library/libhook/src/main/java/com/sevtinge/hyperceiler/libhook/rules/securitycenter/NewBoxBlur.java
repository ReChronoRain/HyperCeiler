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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.securitycenter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.BlurUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

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


        hookAllConstructors(mDockLayout, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                ViewGroup view = (ViewGroup) param.getThisObject();
                int paddingVertical = DisplayUtils.dp2px(mPrefsMap.getInt("security_center_newbox_bg_padding_vertical", 10));
                int paddingHorizontal = DisplayUtils.dp2px(mPrefsMap.getInt("security_center_newbox_bg_padding_horizontal", 10));
                view.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
                new BlurUtils(view, "security_center_newbox_bg_custom");
            }
        });

        //
        hookAllConstructors(mTurboLayout, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                ViewGroup view = (ViewGroup) callMethod(param.getThisObject(), "getDockLayout");
                new BlurUtils(view, "security_center_newbox_bg_custom");
            }
        });

        /*findAndHookConstructor(mNewBoxCls, Context.class, boolean.class, String.class, mWindowManager, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                View view = (View) param.getThisObject();
                new BlurUtils(view, "various_new_box_blur");

            }
        });*/


        findAndHookMethod(mTurboaLayout, "a", boolean.class, boolean.class, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                ImageView view = (ImageView) getObjectField(param.getThisObject(), "j");
                GradientDrawable shapeDrawable = new GradientDrawable();
                shapeDrawable.setColor(Color.TRANSPARENT);
                view.setImageDrawable(shapeDrawable);
            }
        });

        findAndHookMethod(mTurboaLayout, "onAttachedToWindow", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                View view = (View) param.getThisObject();
                new BlurUtils(view, "security_center_newbox_bg_custom");
            }
        });

        findAndHookMethod(mVideoBoxCls, "a", Context.class, boolean.class, new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                ViewGroup viewGroup = (ViewGroup) getObjectField(param.getThisObject(), "b");
                new BlurUtils(viewGroup, "security_center_newbox_bg_custom");
            }
        });

    }
}
