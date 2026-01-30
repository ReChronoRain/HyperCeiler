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
package com.sevtinge.hyperceiler.libhook.rules.systemui.navigation;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.view.Display;
import android.view.Surface;
import android.view.View;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;

import java.util.ArrayList;
import java.util.Arrays;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;
import io.github.libxposed.api.XposedInterface;

public class RotationButton extends BaseHook {
    boolean isListen = false;
    boolean enable = mPrefsMap.getStringAsInt("system_framework_other_rotation_button_int", 0) != 1;

    @Override
    public void init() {
        hookAllConstructors("com.android.systemui.navigationbar.NavigationBar",
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    if (!enable) return;
                    Context mContext = (Context) getObjectField(param.getThisObject(), "mContext");
                    if (!isListen) {
                        if (mContext == null) {
                            XposedLog.e(TAG, "context can't is null!");
                            return;
                        }
                        ContentObserver contentObserver = new ContentObserver(new Handler(mContext.getMainLooper())) {
                            @Override
                            public void onChange(boolean selfChange, @Nullable Uri uri) {
                                boolean isShow = getBoolean(mContext);
                                int rotation = getInt(mContext);
                                callMethod(param.getThisObject(), "onRotationProposal", rotation, isShow);
                            }
                        };
                        mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("rotation_button_data"),
                            false, contentObserver);
                        isListen = true;
                    }
                }
            }
        );


        try {
            findAndHookMethod("com.android.systemui.navigationbar.NavigationBarView",
                "lambda$new$0",
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        if (!enable) {
                            return;
                        }
                        Context mLightContext = (Context) getObjectField(param.getThisObject(), "mLightContext");
                        Integer intValue = switch (getScreenOrientation(mLightContext)) {
                            case 0 -> 1;
                            case 1 -> 0;
                            default -> -1;
                        };
                        if (intValue == -1) {
                            XposedLog.e(TAG, "Unknown parameters, unable to continue execution, execute the original method!");
                            return;
                        }
                        param.setResult(intValue);
                    }
                }
            );
        } catch (Throwable e) {
            findAndHookMethod("com.android.systemui.navigationbar.NavigationBarView$$ExternalSyntheticLambda1",
                "get", new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        if (!enable) {
                            return;
                        }
                        Object NavigationBarView = getObjectField(param.getThisObject(), "f$0");
                        Context mLightContext = (Context) getObjectField(NavigationBarView, "mLightContext");
                        Integer intValue = switch (getScreenOrientation(mLightContext)) {
                            case 0 -> 1;
                            case 1 -> 0;
                            default -> -1;
                        };
                        if (intValue == -1) {
                            XposedLog.e(TAG, "Unknown parameters, unable to continue execution, execute the original method!");
                            return;
                        }
                        param.setResult(intValue);
                    }
                }
            );
        }

        try {
            findAndHookMethod("com.android.systemui.statusbar.CommandQueue",
                "onProposedRotationChanged", int.class, boolean.class,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        if (!enable) return;
                        Object mDumpHandler = getObjectField(param.getThisObject(), "mDumpHandler");
                        Context context = (Context) getObjectField(mDumpHandler, "context");
                        setData(context, param.getArgs()[0] + "," + param.getArgs()[1]);
                        if (enable) param.setResult(null);
                    }
                }
            );

            findAndHookMethod("com.android.systemui.navigationbar.NavigationBar",
                "onRotationProposal", int.class, boolean.class,
                new IMethodHook() {
                    XposedInterface.MethodUnhooker<?> unhook;

                    @Override
                    public void before(BeforeHookParam param) {
                        if (!enable) {
                            param.setResult(null);
                            return;
                        }
                        unhook = findAndHookMethod(View.class, "isAttachedToWindow",
                            returnConstant(true));
                    }

                    @Override
                    public void after(AfterHookParam param) {
                        if (unhook != null) unhook.unhook();
                    }
                }
            );
        } catch (Throwable e) {
            findAndHookMethod("com.android.systemui.shared.rotation.RotationButtonController",
                "onRotationProposal", int.class, boolean.class,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        if (!enable) {
                            param.setResult(null);
                            // return;
                        }
                    }
                }
            );

            findAndHookMethod("com.android.systemui.navigationbar.NavigationBar",
                "onRotationProposal", int.class, boolean.class,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        if (enable) param.setResult(null);
                    }
                }
            );
        }

        try {
            findAndHookMethod("com.android.systemui.shared.rotation.RotationButtonController",
                "onRotateSuggestionClick", View.class,
                new IMethodHook() {
                    @Override
                    public void after(AfterHookParam param) {
                        if (enable)
                            callMethod(param.getThisObject(), "setRotateSuggestionButtonState", false);
                    }
                }
            );
        } catch (Throwable e) {
            findAndHookMethod("com.android.systemui.shared.rotation.RotationButtonController$$ExternalSyntheticLambda5",
                "onClick", View.class,
                new IMethodHook() {
                    @Override
                    public void before(BeforeHookParam param) {
                        if (enable) {
                            Object rotationButtonController = getObjectField(param.getThisObject(), "f$0");
                            callMethod(rotationButtonController, "setRotateSuggestionButtonState", false, true);
                        }
                    }
                }
            );
        }
    }

    public int getScreenOrientation(Context context) {
        Display display = context.getDisplay();
        int rotation = display.getRotation();
        // 获取屏幕方向
        return switch (rotation) {
            case Surface.ROTATION_0, Surface.ROTATION_180 ->
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; // 1 竖屏
            case Surface.ROTATION_90, Surface.ROTATION_270 ->
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; // 0 横屏
            default -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        };
    }

    private void setData(Context context, String value) {
        Settings.System.putString(context.getContentResolver(), "rotation_button_data", value);
    }

    private String getData(Context context) {
        return Settings.System.getString(context.getContentResolver(), "rotation_button_data");
    }

    private boolean getBoolean(Context context) {
        String data = getData(context);
        if (data == null) return false;
        String[] sp = data.split(",");
        String s = new ArrayList<>(Arrays.asList(sp)).get(1);
        return s.contains("true");
    }

    private int getInt(Context context) {
        String data = getData(context);
        if (data == null) return -1;
        String[] sp = data.split(",");
        return Integer.parseInt(new ArrayList<>(Arrays.asList(sp)).get(0));
    }
}
