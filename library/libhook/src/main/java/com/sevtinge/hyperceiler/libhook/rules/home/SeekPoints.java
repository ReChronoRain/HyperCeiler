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
package com.sevtinge.hyperceiler.libhook.rules.home;

import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.getAdditionalInstanceField;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.setAdditionalInstanceField;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.utils.prefs.PrefsBridge;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class SeekPoints extends BaseHook {

    public int points = PrefsBridge.getStringAsInt("home_other_seek_points", 0);

    @Override
    public void init() {
        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "setSeekBarPosition",
            "android.widget.FrameLayout$LayoutParams",
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    showSeekBar((View) param.getThisObject());
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "refreshScrollBound",
            new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    if (points == 2) showSeekBar((View) param.getThisObject());
                }
            }
        );

        hookAllMethods("com.miui.home.launcher.ScreenView",
            "updateSeekPoints",
            new IMethodHook() {
                @Override
                public void before(final BeforeHookParam param) {
                    showSeekBar((View) param.getThisObject());
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "addView", View.class, int.class, ViewGroup.LayoutParams.class,
            new IMethodHook() {
                @Override
                public void before(final BeforeHookParam param) {
                    showSeekBar((View) param.getThisObject());
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "removeScreen", int.class,
            new IMethodHook() {
                @Override
                public void before(final BeforeHookParam param) {
                    showSeekBar((View) param.getThisObject());
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "removeScreensInLayout", int.class, int.class,
            new IMethodHook() {
                @Override
                public void before(final BeforeHookParam param) {
                    showSeekBar((View) param.getThisObject());
                }
            }
        );
    }

    private void showSeekBar(View workspace) {
        if (!"Workspace".equals(workspace.getClass().getSimpleName())) return;
        boolean isInEditingMode = (boolean) callMethod(workspace, "isInNormalEditingMode");
        View mScreenSeekBar = (View) getObjectField(workspace, "mScreenSeekBar");
        if (mScreenSeekBar == null) {
            XposedLog.w(TAG, getPackageName(), "showSeekBar HideSeekPointsHook Cannot find seekbar");
            return;
        }
        Context mContext = workspace.getContext();
        Handler mHandler = (Handler) getAdditionalInstanceField(workspace, "mHandlerEx");
        if (mHandler == null) {
            mHandler = new Handler(mContext.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    View seekBar = (View) msg.obj;
                    if (seekBar != null)
                        seekBar.animate().alpha(0.0f).setDuration(600).withEndAction(() -> seekBar.setVisibility(View.GONE));
                }
            };
            setAdditionalInstanceField(workspace, "mHandlerEx", mHandler);
        }
        if (mHandler == null) {
            XposedLog.w(TAG, getPackageName(), "showSeekBar HideSeekPointsHook Cannot create handler");
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
