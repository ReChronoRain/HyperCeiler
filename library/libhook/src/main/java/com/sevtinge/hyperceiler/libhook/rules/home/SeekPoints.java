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

import static com.sevtinge.hyperceiler.libhook.base.BaseHook.getAdditionalInstanceField;
import static com.sevtinge.hyperceiler.libhook.base.BaseHook.setAdditionalInstanceField;

import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

public class SeekPoints extends BaseHook {

    private static final String STATE_WORKSPACE = "SeekPoints.workspace";
    private static final String HANDLER_KEY = "mHandlerEx";
    private static final String HANDLER_OWNER_KEY = "mHandlerExOwner";
    public int points = PrefsBridge.getStringAsInt("home_other_seek_points", 0);
    private final Object mHandlerGeneration = new Object();

    @Override
    public void init() {
        View restoredWorkspace = getHotReloadRuntimeState(STATE_WORKSPACE, View.class);
        if (restoredWorkspace != null) {
            ensureCurrentGenerationHandler(restoredWorkspace);
        }

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "setSeekBarPosition",
            "android.widget.FrameLayout$LayoutParams",
            new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    showSeekBar((View) param.getThisObject());
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "refreshScrollBound",
            new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    if (points == 2) showSeekBar((View) param.getThisObject());
                }
            }
        );

        hookAllMethods("com.miui.home.launcher.ScreenView",
            "updateSeekPoints",
            new IMethodHook() {
                @Override
                public void before(final HookParam param) {
                    showSeekBar((View) param.getThisObject());
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "addView", View.class, int.class, ViewGroup.LayoutParams.class,
            new IMethodHook() {
                @Override
                public void before(final HookParam param) {
                    showSeekBar((View) param.getThisObject());
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "removeScreen", int.class,
            new IMethodHook() {
                @Override
                public void before(final HookParam param) {
                    showSeekBar((View) param.getThisObject());
                }
            }
        );

        findAndHookMethod("com.miui.home.launcher.ScreenView",
            "removeScreensInLayout", int.class, int.class,
            new IMethodHook() {
                @Override
                public void before(final HookParam param) {
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
        Handler mHandler = ensureCurrentGenerationHandler(workspace);
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

    private Handler ensureCurrentGenerationHandler(View workspace) {
        Object owner = getAdditionalInstanceField(workspace, HANDLER_OWNER_KEY);
        Handler existing = (Handler) getAdditionalInstanceField(workspace, HANDLER_KEY);
        if (owner == mHandlerGeneration && existing != null) {
            return existing;
        }
        if (existing != null) {
            existing.removeCallbacksAndMessages(null);
        }
        Handler handler = new Handler(workspace.getContext().getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                View seekBar = (View) msg.obj;
                if (seekBar != null) {
                    seekBar.animate().alpha(0.0f).setDuration(600)
                        .withEndAction(() -> seekBar.setVisibility(View.GONE));
                }
            }
        };
        setAdditionalInstanceField(workspace, HANDLER_KEY, handler);
        setAdditionalInstanceField(workspace, HANDLER_OWNER_KEY, mHandlerGeneration);
        registerHandlerHotReloadCleanup(handler);
        registerHotReloadCleanup(() -> {
            if (getAdditionalInstanceField(workspace, HANDLER_OWNER_KEY) == mHandlerGeneration) {
                removeAdditionalInstanceField(workspace, HANDLER_KEY);
                removeAdditionalInstanceField(workspace, HANDLER_OWNER_KEY);
            }
        });
        putHotReloadRuntimeState(STATE_WORKSPACE, workspace);
        return handler;
    }
}
