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
package com.sevtinge.hyperceiler.module.hook.systemframework;

import android.content.Context;
import android.os.Handler;
import android.provider.Settings;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class RotationButton extends BaseHook {
    Context context;

    @Override
    public void init() throws NoSuchMethodException {
        findAndHookConstructor("com.android.server.wm.DisplayRotation$OrientationListener",
                "com.android.server.wm.DisplayRotation", Context.class, Handler.class,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        context = (Context) param.args[1];
                    }
                }
        );

        findAndHookMethod("com.android.server.wm.DisplayRotation$OrientationListener",
                "onProposedRotationChanged", int.class,
                new MethodHook() {
                    @Override
                    protected void before(MethodHookParam param) {
                        int rotation = (int) param.args[0];
                        Object displayRotation = XposedHelpers.getObjectField(param.thisObject, "this$0");
                        int mCurrentAppOrientation = XposedHelpers.getIntField(displayRotation, "mCurrentAppOrientation");
                        boolean result = (boolean) XposedHelpers.callMethod(displayRotation, "isRotationChoicePossible",
                                mCurrentAppOrientation);
                        if (result) {
                            if (context == null) {
                                context = (Context) XposedHelpers.getObjectField(displayRotation, "mContext");
                                if (context == null) {
                                    logE(TAG, "context can't is null!!!");
                                    return;
                                }
                            }
                            boolean isValid = (boolean) XposedHelpers.callMethod(displayRotation, "isValidRotationChoice", rotation);
                            setData(context, rotation + "," + isValid);
                        }
                    }
                }
        );


        findAndHookMethod("com.android.server.wm.DisplayRotation",
                "isRotationChoicePossible", int.class,
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        super.after(param);
                        // int orientation = (int) param.args[0];
                        // boolean result = (boolean) param.getResult();
                        // if (!result && (orientation == 1 || orientation == -1)) {
                        //     param.setResult(true);
                        // }
                    }
                }
        );
    }

    private void setData(Context context, String value) {
        Settings.System.putString(context.getContentResolver(), "rotation_button_data", value);
    }
}
