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
package com.sevtinge.hyperceiler.module.hook.systemui.navigation;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XposedHelpers;

public class RotationButton extends BaseHook {
    boolean isListen = false;

    boolean enable = mPrefsMap.getBoolean("system_framework_other_rotation_button");

    @Override
    public void init() throws NoSuchMethodException {
        findAndHookConstructor("com.android.systemui.navigationbar.NavigationBarView",
                Context.class, AttributeSet.class, new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        if (!enable) return;
                        Context mContext = (Context) param.args[0];
                        if (!isListen) {
                            if (mContext == null) {
                                logE(TAG, "context can't is null!");
                                return;
                            }
                            ContentObserver contentObserver = new ContentObserver(new Handler(mContext.getMainLooper())) {
                                @Override
                                public void onChange(boolean selfChange, @Nullable Uri uri) {
                                    boolean isShow = getBoolean(mContext);
                                    int rotation = getInt(mContext);
                                    Object mRotationButtonController = XposedHelpers.getObjectField(param.thisObject, "mRotationButtonController");
                                    try {
                                        XposedHelpers.callMethod(mRotationButtonController, "onRotationProposal", rotation, isShow);
                                    } catch (Throwable e) {
                                        Object mUpdateActiveTouchRegionsCallback = XposedHelpers.getObjectField(param.thisObject, "mUpdateActiveTouchRegionsCallback");
                                        Object NavigationBar = XposedHelpers.getObjectField(mUpdateActiveTouchRegionsCallback, "f$0");
                                        XposedHelpers.callMethod(NavigationBar, "onRotationProposal", rotation, isShow);
                                    }
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
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            if (!enable) {
                                return;
                            }
                            Context mLightContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mLightContext");
                            Integer intValue = switch (getScreenOrientation(mLightContext)) {
                                case 0 -> 1;
                                case 1 -> 0;
                                default -> -1;
                            };
                            if (intValue == -1) {
                                logE(TAG, "Unknown parameters, unable to continue execution, execute the original method!");
                                return;
                            }
                            param.setResult(intValue);
                        }
                    }
            );
        } catch (Throwable e) {
            findAndHookMethod("com.android.systemui.navigationbar.NavigationBarView$$ExternalSyntheticLambda1",
                    "get", new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            if (!enable) {
                                return;
                            }
                            Object NavigationBarView = XposedHelpers.getObjectField(param.thisObject, "f$0");
                            Context mLightContext = (Context) XposedHelpers.getObjectField(NavigationBarView, "mLightContext");
                            Integer intValue = switch (getScreenOrientation(mLightContext)) {
                                case 0 -> 1;
                                case 1 -> 0;
                                default -> -1;
                            };
                            if (intValue == -1) {
                                logE(TAG, "Unknown parameters, unable to continue execution, execute the original method!");
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
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            if (enable) param.setResult(null);
                        }
                    }
            );

            findAndHookMethod("com.android.systemui.navigationbar.NavigationBar",
                    "onRotationProposal", int.class, boolean.class,
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            if (!enable) {
                                param.setResult(null);
                            }
                        }
                    }
            );
        } catch (Throwable e) {
            findAndHookMethod("com.android.systemui.shared.rotation.RotationButtonController",
                    "onRotationProposal", int.class, boolean.class,
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            if (!enable) {
                                param.setResult(null);
                                // return;
                            }
                        /*Context mContext =
                                (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                        Object mWindowRotationProvider = XposedHelpers.getObjectField(param.thisObject, "mWindowRotationProvider");
                        int i = (int) param.args[0];
                        boolean z = (boolean) param.args[1];
                        int intValue = switch (getScreenOrientation(mContext)) {
                            case 0 -> 1;
                            case 1 -> 0;
                            default -> -1;
                        };
                        if (intValue == -1) {
                            logE(TAG, "Unknown parameters, unable to continue execution, execute the original method!");
                            return;
                        }
                        Object mRotationButton = XposedHelpers.getObjectField(param.thisObject, "mRotationButton");
                        boolean acceptRotationProposal = false;
                        try {
                            acceptRotationProposal = (boolean) XposedHelpers.callMethod(mRotationButton, "acceptRotationProposal");
                        } catch (Throwable e) {
                            // logE(TAG, "E: " + e);
                        }
                        if (true) {
                            boolean mHomeRotationEnabled = XposedHelpers.getBooleanField(param.thisObject, "mHomeRotationEnabled");
                            boolean mIsRecentsAnimationRunning = XposedHelpers.getBooleanField(param.thisObject, "mIsRecentsAnimationRunning");
                            Handler mMainThreadHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mMainThreadHandler");
                            if (mHomeRotationEnabled || !mIsRecentsAnimationRunning) {
                                if (!z) {
                                    XposedHelpers.callMethod(param.thisObject, "setRotateSuggestionButtonState", false);
                                    param.setResult(null);
                                } else if (i == intValue) {
                                    mMainThreadHandler.removeCallbacks((Runnable) XposedHelpers.getObjectField(param.thisObject, "mRemoveRotationProposal"));
                                    XposedHelpers.callMethod(param.thisObject, "setRotateSuggestionButtonState", false);
                                    param.setResult(null);
                                } else {
                                    XposedHelpers.setIntField(param.thisObject, "mLastRotationSuggestion", i);
                                    boolean isRotationAnimationCCW = isRotationAnimationCCW(intValue, i);
                                    if (intValue == 0) {
                                        int mIconCcwStart0ResId = XposedHelpers.getIntField(param.thisObject, "mIconCcwStart0ResId");
                                        int mIconCwStart0ResId = XposedHelpers.getIntField(param.thisObject, "mIconCwStart0ResId");
                                        XposedHelpers.setIntField(param.thisObject, "mIconResId", isRotationAnimationCCW ? mIconCcwStart0ResId : mIconCwStart0ResId);
                                    } else {
                                        int mIconCcwStart90ResId = XposedHelpers.getIntField(param.thisObject, "mIconCcwStart90ResId");
                                        int mIconCwStart90ResId = XposedHelpers.getIntField(param.thisObject, "mIconCwStart90ResId");
                                        XposedHelpers.setIntField(param.thisObject, "mIconResId", isRotationAnimationCCW ? mIconCcwStart90ResId : mIconCwStart90ResId);
                                    }
                                    int mLightIconColor = XposedHelpers.getIntField(param.thisObject, "mLightIconColor");
                                    int mDarkIconColor = XposedHelpers.getIntField(param.thisObject, "mDarkIconColor");
                                    XposedHelpers.callMethod(mRotationButton, "updateIcon", mLightIconColor, mDarkIconColor);
                                    if ((boolean) XposedHelpers.callMethod(param.thisObject, "canShowRotationButton")) {
                                        XposedHelpers.callMethod(param.thisObject, "showAndLogRotationSuggestion");
                                        param.setResult(null);
                                        return;
                                    }
                                    XposedHelpers.setBooleanField(param.thisObject, "mPendingRotationSuggestion", true);
                                    Runnable mCancelPendingRotationProposal = (Runnable) XposedHelpers.getObjectField(param.thisObject, "mCancelPendingRotationProposal");
                                    mMainThreadHandler.removeCallbacks(mCancelPendingRotationProposal);
                                    mMainThreadHandler.postDelayed(mCancelPendingRotationProposal, 20000L);
                                    param.setResult(null);
                                }
                            }
                        }*/
                        }
                    }
            );

            findAndHookMethod("com.android.systemui.navigationbar.NavigationBar",
                    "onRotationProposal", int.class, boolean.class,
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            if (enable) param.setResult(null);
                        }
                    }
            );
        }

        try {
            findAndHookMethod("com.android.systemui.shared.rotation.RotationButtonController",
                    "onRotateSuggestionClick", View.class,
                    new MethodHook() {
                        @Override
                        protected void after(MethodHookParam param) {
                            if (enable)
                                XposedHelpers.callMethod(param.thisObject, "setRotateSuggestionButtonState", false);
                        }
                    }
            );
        } catch (Throwable e) {
            findAndHookMethod("com.android.systemui.shared.rotation.RotationButtonController$$ExternalSyntheticLambda1",
                    "onClick", View.class,
                    new MethodHook() {
                        @Override
                        protected void before(MethodHookParam param) {
                            if (enable)
                                XposedHelpers.callMethod(param.thisObject, "setRotateSuggestionButtonState", false, false);
                        }
                    }
            );
        }
    }

    public int getScreenOrientation(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        int rotation = display.getRotation();
        int orientation;

        // 获取屏幕方向
        switch (rotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; // 1 竖屏
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; // 0 横屏
                break;
            default:
                orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        }

        return orientation;
    }

    public static boolean isRotationAnimationCCW(int i, int i2) {
        if (i == 0 && i2 == 1) {
            return false;
        }
        if (i == 0 && i2 == 2) {
            return true;
        }
        if (i == 0 && i2 == 3) {
            return true;
        }
        if (i == 1 && i2 == 0) {
            return true;
        }
        if (i == 1 && i2 == 2) {
            return false;
        }
        if (i == 1 && i2 == 3) {
            return true;
        }
        if (i == 2 && i2 == 0) {
            return true;
        }
        if (i == 2 && i2 == 1) {
            return true;
        }
        if (i == 2 && i2 == 3) {
            return false;
        }
        if (i == 3 && i2 == 0) {
            return false;
        }
        if (i == 3 && i2 == 1) {
            return true;
        }
        return i == 3 && i2 == 2;
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
