/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.module.hook.various.dialog;

import android.os.Build;
import android.view.View;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.blur.BlurUtils;

import de.robv.android.xposed.XposedHelpers;

public class DialogBlur extends BaseHook {

    final Class<?> mDialogCls = findClassIfExists("miuix.appcompat.app.AlertController");

    @Override
    public void init() {
        hookAllMethods(mDialogCls, "installContent", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {

                View mParentPanel = (View) XposedHelpers.getObjectField(param.thisObject, "mParentPanel");

                if (mParentPanel != null) {
                    /*new BlurUtils(mParentPanel);*/
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        new BlurUtils(mParentPanel, "default");
                    }
                }
            }
        });

        hookAllMethods(mDialogCls, "dismiss", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                super.after(param);
                View mParentPanel = (View) XposedHelpers.getObjectField(param.thisObject, "mParentPanel");
                mParentPanel.setVisibility(View.INVISIBLE);
            }
        });
    }
}
