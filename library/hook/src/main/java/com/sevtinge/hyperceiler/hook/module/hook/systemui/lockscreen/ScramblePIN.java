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
package com.sevtinge.hyperceiler.hook.module.hook.systemui.lockscreen;

import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

import java.util.ArrayList;
import java.util.Collections;

import de.robv.android.xposed.XposedHelpers;

public class ScramblePIN extends BaseHook {

    Class<?> mKeyguardPINView;

    @Override
    public void init() {
        mKeyguardPINView = findClassIfExists("com.android.keyguard.KeyguardPINView");

        findAndHookMethod(mKeyguardPINView, "onFinishInflate", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                View[][] mViews = (View[][]) XposedHelpers.getObjectField(param.thisObject, "mViews");
                ArrayList<View> mRandomViews = new ArrayList<>();
                for (int row = 1; row <= 3; row++) {
                    for (int col = 0; col <= 2; col++) {
                        if (mViews[row][col] != null) mRandomViews.add(mViews[row][col]);
                    }
                }
                mRandomViews.add(mViews[4][1]);
                Collections.shuffle(mRandomViews);

                View pinview = (View) param.thisObject;
                ViewGroup row1 = pinview.findViewById(pinview.getResources().getIdentifier("row1", "id", "com.android.systemui"));
                ViewGroup row2 = pinview.findViewById(pinview.getResources().getIdentifier("row2", "id", "com.android.systemui"));
                ViewGroup row3 = pinview.findViewById(pinview.getResources().getIdentifier("row3", "id", "com.android.systemui"));
                ViewGroup row4 = pinview.findViewById(pinview.getResources().getIdentifier("row4", "id", "com.android.systemui"));

                row1.removeAllViews();
                row2.removeAllViews();
                row3.removeAllViews();
                row4.removeViewAt(1);

                mViews[1] = new View[]{mRandomViews.get(0), mRandomViews.get(1), mRandomViews.get(2)};
                row1.addView(mRandomViews.get(0));
                row1.addView(mRandomViews.get(1));
                row1.addView(mRandomViews.get(2));

                mViews[2] = new View[]{mRandomViews.get(3), mRandomViews.get(4), mRandomViews.get(5)};
                row2.addView(mRandomViews.get(3));
                row2.addView(mRandomViews.get(4));
                row2.addView(mRandomViews.get(5));

                mViews[3] = new View[]{mRandomViews.get(6), mRandomViews.get(7), mRandomViews.get(8)};
                row3.addView(mRandomViews.get(6));
                row3.addView(mRandomViews.get(7));
                row3.addView(mRandomViews.get(8));

                mViews[4] = new View[]{null, mRandomViews.get(9), mViews[4][2]};
                row4.addView(mRandomViews.get(9), 1);

                XposedHelpers.setObjectField(param.thisObject, "mViews", mViews);
            }
        });
    }
}
