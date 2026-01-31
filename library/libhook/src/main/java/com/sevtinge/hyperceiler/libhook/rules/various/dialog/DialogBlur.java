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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.various.dialog;

import android.view.View;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.blur.BlurUtils;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;


public class DialogBlur extends BaseHook {

    final Class<?> mDialogCls = findClassIfExists("miuix.appcompat.app.AlertController");

    @Override
    public void init() {
        hookAllMethods(mDialogCls, "installContent", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {

                View mParentPanel = (View) getObjectField(param.getThisObject(), "mParentPanel");

                if (mParentPanel != null) {
                    /*new BlurUtils(mParentPanel);*/
                    new BlurUtils(mParentPanel, "default");
                }
            }
        });

        hookAllMethods(mDialogCls, "dismiss", new IMethodHook() {
            @Override
            public void after(AfterHookParam param) {
                View mParentPanel = (View) getObjectField(param.getThisObject(), "mParentPanel");
                mParentPanel.setVisibility(View.INVISIBLE);
            }
        });
    }
}
