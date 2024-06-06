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
package com.sevtinge.hyperceiler.module.hook.systemui.controlcenter;

import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XCallback;

public class CustomCardTiles {
    private static final String TAG = "CustomCardTiles";
    private static final String TAG_BT = "bt";
    private static final String TAG_CELL = "cell";
    private static final String TAG_FLASHLIGHT = "flashlight";
    private static final String TAG_VOWIFI1 = "vowifi1";
    private static final String TAG_VOWIFI2 = "vowifi2";
    private static final String TAG_WIFI = "wifi";

    private static int idEnable = -1;
    private static int idDisabled = -1;

    private static float cornerRadiusF = -1;


    public static void initCustomCardTiles(ClassLoader classLoader, List<String> cardStyleTiles) {
        findAndHookMethod("miui.systemui.controlcenter.qs.QSController", classLoader,
                "getCardStyleTileSpecs",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param)  {
                        param.setResult(cardStyleTiles);
                    }
                });

        XposedHelpers.findAndHookConstructor("miui.systemui.controlcenter.qs.tileview.QSCardItemView", classLoader,
                Context.class, AttributeSet.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        LinearLayout linearLayout = (LinearLayout) param.thisObject;
                        idEnable = linearLayout.getContext().getResources().getIdentifier("qs_card_wifi_background_enabled",
                                "drawable", "miui.systemui.plugin");
                        idDisabled = linearLayout.getContext().getResources().getIdentifier("qs_card_wifi_background_disabled",
                                "drawable", "miui.systemui.plugin");
                        int cornerRadius = linearLayout.getContext().getResources().getIdentifier(
                                "control_center_universal_corner_radius", "dimen", "miui.systemui.plugin");
                        cornerRadiusF = linearLayout.getContext().getResources().getDimensionPixelSize(cornerRadius);
                    }
                }
        );

        findAndHookMethod("miui.systemui.controlcenter.qs.tileview.QSCardItemView", classLoader,
                "updateBackground", new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object state = XposedHelpers.getObjectField(param.thisObject, "state");
                        String spec = (String) XposedHelpers.getObjectField(state, "spec");
                        int i = XposedHelpers.getIntField(state, "state");
                        switch (spec) {
                            case TAG_BT, TAG_CELL, TAG_FLASHLIGHT, TAG_WIFI, TAG_VOWIFI1, TAG_VOWIFI2:
                                break;
                            default:
                                LinearLayout linearLayout = (LinearLayout) param.thisObject;
                                if (i == 2) {
                                    if (idDisabled == -1 || idEnable == -1) {
                                        logE(TAG, "id is -1!!");
                                        return;
                                    }
                                    Drawable enable = linearLayout.getContext().getTheme().
                                            getResources().getDrawable(idEnable, linearLayout.getContext().getTheme());
                                    linearLayout.setBackground(enable);
                                    XposedHelpers.callMethod(param.thisObject, "setCornerRadius", cornerRadiusF);
                                } else if (i == 1) {
                                    Drawable disabled = linearLayout.getContext().getTheme().
                                            getResources().getDrawable(idDisabled, linearLayout.getContext().getTheme());
                                    linearLayout.setBackground(disabled);
                                    XposedHelpers.callMethod(param.thisObject, "setCornerRadius", cornerRadiusF);
                                }
                        }
                    }
                }
        );
    }
}
