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
package com.sevtinge.hyperceiler.module.hook.systemui.statusbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class BigMobileNetworkType extends BaseHook {

    @Override
    public void init() {

        /*MethodHook showSingleMobileType = new MethodHook(MethodHook.PRIORITY_HIGHEST) {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                Object mobileIconState = param.args[0];
                XposedHelpers.setObjectField(mobileIconState, "showMobileDataTypeSingle", true);
                XposedHelpers.setObjectField(mobileIconState, "fiveGDrawableId", 0);
            }
        };

        MethodHook afterUpdate = new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                Object mMobileLeftContainer = XposedHelpers.getObjectField(param.thisObject, "mMobileLeftContainer");
                XposedHelpers.callMethod(mMobileLeftContainer, "setVisibility", 8);
            }
        };

        hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", "applyMobileState", showSingleMobileType);
        hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", "applyMobileState", afterUpdate);
*/
    }

    @SuppressLint("DiscouragedApi")
    private void old() {
        findAndHookMethod("com.android.systemui.statusbar.StatusBarMobileView", "init", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                ViewGroup mStatusBarMobileView = (ViewGroup) param.thisObject;
                Context mContext = mStatusBarMobileView.getContext();
                Resources mRes = mContext.getResources();

                // 获取组件
                // mMobileLeftContainer
                int mobileContainerLeftId = mRes.getIdentifier("mobile_container_left", "id", "com.android.systemui");
                // mMobileType
                int mobileTypeId = mRes.getIdentifier("mobile_type", "id", "com.android.systemui");
                // mLeftInOut
                int mobileLeftMobileInoutId = mRes.getIdentifier(
                    "mobile_left_mobile_inout",
                    "id",
                    "com.android.systemui");

                ViewGroup mobileContainerLeft = mStatusBarMobileView.findViewById(mobileContainerLeftId);
                TextView mobileType = mStatusBarMobileView.findViewById(mobileTypeId);
                ImageView mobileLeftMobileInout = mStatusBarMobileView.findViewById(mobileLeftMobileInoutId);

                // 获取插入位置
                // mMobileRightContainer
                int mobileContainerRightId = mRes.getIdentifier(
                    "mobile_container_right",
                    "id",
                    "com.android.systemui"
                );
                ViewGroup mobileContainerRight = mStatusBarMobileView.findViewById(mobileContainerRightId);
                ViewGroup rightParentLayout = (ViewGroup) mobileContainerRight.getParent();
                int mobileContainerRightIndex = rightParentLayout.indexOfChild(mobileContainerRight);

                // 创建新布局
                LinearLayout.LayoutParams newLinearLayoutLP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                );

                LinearLayout newLinearlayout = new LinearLayout(mContext);
                newLinearlayout.setLayoutParams(newLinearLayoutLP);
                newLinearlayout.setId(mobileContainerLeftId);
                newLinearlayout.setPadding(0, 0, 0, 0);


                XposedHelpers.setObjectField(param.thisObject, "mMobileLeftContainer", newLinearlayout);
                rightParentLayout.addView(newLinearlayout, mobileContainerRightIndex);

                // 将组件插入新的布局
                ((ViewGroup) mobileType.getParent()).removeView(mobileType);
                ((ViewGroup) mobileLeftMobileInout.getParent()).removeView(mobileLeftMobileInout);
                ((ViewGroup) mobileContainerLeft.getParent()).removeView(mobileContainerLeft);


                // 类型
                newLinearlayout.addView(mobileType);
                LinearLayout.LayoutParams mobileTypeLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );

                mobileTypeLp.gravity = Gravity.CENTER_VERTICAL;
                mobileTypeLp.topMargin = 0;

                mobileType.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
                boolean isBold = true;
                if (isBold) {
                    mobileType.setTypeface(Typeface.DEFAULT_BOLD);
                }
                mobileType.setLayoutParams(mobileTypeLp);


                // 箭头
                newLinearlayout.addView(mobileLeftMobileInout);
                LinearLayout.LayoutParams mobileLeftMobileInoutLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                );
                mobileLeftMobileInout.setLayoutParams(mobileLeftMobileInoutLp);

                // 屏蔽更新布局
                findAndHookMethod("com.android.systemui.statusbar.StatusBarMobileView", "updateMobileTypeLayout", String.class, new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) throws Throwable {
                        param.args[0] = null;
                    }
                });
            }
        });
    }
}
