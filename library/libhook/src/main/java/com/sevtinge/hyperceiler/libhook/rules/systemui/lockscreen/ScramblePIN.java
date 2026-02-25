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
package com.sevtinge.hyperceiler.libhook.rules.systemui.lockscreen;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.System.isMoreAndroidVersion;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;

public class ScramblePIN extends BaseHook {

    Class<?> mKeyguardPINView;

    @Override
    public void init() {
        if (isMoreAndroidVersion(36)) {
            // thanks xzakota
            mKeyguardPINView = findClassIfExists("com.android.keyguard.widget.MiuiKeyguardPINView");
        } else {
            mKeyguardPINView = findClassIfExists("com.android.keyguard.KeyguardPINView");
        }

        findAndHookMethod(mKeyguardPINView, "onFinishInflate", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                View[][] mViews = (View[][]) getObjectField(param.getThisObject(), "mViews");
                ArrayList<View> mRandomViews = collectRandomViews(mViews);

                Collections.shuffle(mRandomViews);
                View pinview = (View) param.getThisObject();
                ViewGroup[] rows = getRowContainers(pinview);
                redistributeViews(mViews, mRandomViews, rows);

                setObjectField(param.getThisObject(), "mViews", mViews);
            }
        });
    }

    /**
     * 收集需要随机排列的视图
     */
    private ArrayList<View> collectRandomViews(View[][] mViews) {
        ArrayList<View> views = new ArrayList<>(10);
        for (int row = 1; row <= 3; row++) {
            for (int col = 0; col <= 2; col++) {
                if (mViews[row][col] != null) {
                    views.add(mViews[row][col]);
                }
            }
        }
        views.add(mViews[4][1]);
        return views;
    }

    /**
     * 获取行容器
     */
    private ViewGroup[] getRowContainers(View pinview) {
        Resources res = pinview.getResources();
        String pkg = "com.android.systemui";

        ViewGroup[] rows = new ViewGroup[5];
        for (int i = 1; i <= 4; i++) {
            rows[i] = pinview.findViewById(res.getIdentifier("row" + i, "id", pkg));
        }
        return rows;
    }

    /**
     * 清空并重新分配视图
     */
    private void redistributeViews(View[][] mViews, List<View> randomViews, ViewGroup[] rows) {
        if (randomViews.size() < 10) return;
        // 清空第1-3行
        for (int i = 1; i <= 3; i++) {
            rows[i].removeAllViews();
        }
        rows[4].removeViewAt(1);

        // 重新分配
        int idx = 0;
        for (int row = 1; row <= 3; row++) {
            mViews[row] = new View[3];
            for (int col = 0; col < 3; col++) {
                mViews[row][col] = randomViews.get(idx);
                rows[row].addView(randomViews.get(idx++));
            }
        }

        mViews[4][1] = randomViews.get(idx);
        rows[4].addView(randomViews.get(idx), 1);
    }
}
