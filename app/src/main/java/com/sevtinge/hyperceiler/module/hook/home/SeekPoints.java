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
package com.sevtinge.hyperceiler.module.hook.home;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class SeekPoints extends BaseHook {

    public int points = mPrefsMap.getStringAsInt("home_other_seek_points", 0);

    @Override
    public void init() {
        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "setSeekBarPosition",
            "android.widget.FrameLayout$LayoutParams",
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    showSeekBar((View) param.thisObject);
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "refreshScrollBound",
            new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    if (points == 2) showSeekBar((View) param.thisObject);
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "updateSeekPoints", int.class,
            new MethodHook() {
                @Override
                protected void before(final MethodHookParam param) {
                    showSeekBar((View) param.thisObject);
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "addView", View.class, int.class, ViewGroup.LayoutParams.class,
            new MethodHook() {
                @Override
                protected void before(final MethodHookParam param) {
                    showSeekBar((View) param.thisObject);
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "removeScreen", int.class,
            new MethodHook() {
                @Override
                protected void before(final MethodHookParam param) {
                    showSeekBar((View) param.thisObject);
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "removeScreensInLayout", int.class, int.class,
            new MethodHook() {
                @Override
                protected void before(final MethodHookParam param) {
                    showSeekBar((View) param.thisObject);
                }
            }
        );
    }

    private void showSeekBar(View workspace) {
        if (!"Workspace".equals(workspace.getClass().getSimpleName())) return;
        boolean isInEditingMode = (boolean) XposedHelpers.callMethod(workspace, "isInNormalEditingMode");
        View mScreenSeekBar = (View) XposedHelpers.getObjectField(workspace, "mScreenSeekBar");
        if (mScreenSeekBar == null) {
            logI(TAG, this.lpparam.packageName, "showSeekBar HideSeekPointsHook Cannot find seekbar");
            return;
        }
        Context mContext = workspace.getContext();
        Handler mHandler = (Handler) XposedHelpers.getAdditionalInstanceField(workspace, "mHandlerEx");
        if (mHandler == null) {
            mHandler = new Handler(mContext.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    View seekBar = (View) msg.obj;
                    if (seekBar != null)
                        seekBar.animate().alpha(0.0f).setDuration(600).withEndAction(() -> seekBar.setVisibility(View.GONE));
                }
            };
            XposedHelpers.setAdditionalInstanceField(workspace, "mHandlerEx", mHandler);
        }
        if (mHandler == null) {
            logI(TAG, this.lpparam.packageName, "showSeekBar HideSeekPointsHook Cannot create handler");
            return;
        }
        if (mHandler.hasMessages(666)) mHandler.removeMessages(666);
        mScreenSeekBar.animate().cancel();
        if (!isInEditingMode && points == 2) {
            mScreenSeekBar.setAlpha(0.0f);
            mScreenSeekBar.setVisibility(View.GONE);
            return;
        }
        mScreenSeekBar.setVisibility(View.VISIBLE);
        mScreenSeekBar.animate().alpha(1.0f).setDuration(300);
        if (!isInEditingMode) {
            Message msg = Message.obtain(mHandler, 666);
            msg.obj = mScreenSeekBar;
            mHandler.sendMessageDelayed(msg, 1500);
        }
    }
}
