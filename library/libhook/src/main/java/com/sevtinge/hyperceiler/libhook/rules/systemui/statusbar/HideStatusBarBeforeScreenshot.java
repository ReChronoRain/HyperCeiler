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
package com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.SystemClock;
import android.view.SurfaceControl;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.core.java.Methods;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

import java.lang.reflect.Method;

public class HideStatusBarBeforeScreenshot extends BaseHook {

    private static final String LEGACY_COLLAPSED_STATUS_BAR_CLASS =
        "com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment";
    private static final String OS4_STATUS_BAR_VIEW_CLASS =
        "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView";
    private static final String ACTION_TAKE_SCREENSHOT = "miui.intent.TAKE_SCREENSHOT";
    private static final String EXTRA_IS_FINISHED = "IsFinished";
    private static final String HOT_RELOAD_VIEW_KEY =
        "HideStatusBarBeforeScreenshot.statusBarView";
    private View mReceiverView;
    private int mVisibilityBeforeScreenshot = View.VISIBLE;
    private boolean mStatusBarHiddenForScreenshot;
    private SurfaceControl mStatusBarSurface;
    private boolean mSurfaceHiddenForScreenshot;
    private boolean mLastSurfaceApplySynchronous;
    private long mHideStartedAt;

    @Override
    public void init() {
        View restoredView = getHotReloadRuntimeState(HOT_RELOAD_VIEW_KEY, View.class);
        if (restoredView != null) {
            registerScreenshotReceiver(restoredView);
        }

        if (Build.VERSION.SDK_INT >= 37) {
            XposedLog.d(TAG, getPackageName(),
                "HOOK_STATE=SELECTED target=" + OS4_STATUS_BAR_VIEW_CLASS
                    + "#onAttachedToWindow sdk=" + Build.VERSION.SDK_INT);
            hookAllMethods(OS4_STATUS_BAR_VIEW_CLASS, "onAttachedToWindow", new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    Object thisObject = param.getThisObject();
                    if (thisObject instanceof View view) {
                        registerScreenshotReceiver(view);
                    }
                }
            });
        } else {
            XposedLog.d(TAG, getPackageName(),
                "HOOK_STATE=SELECTED target=" + LEGACY_COLLAPSED_STATUS_BAR_CLASS
                    + "#onViewCreated sdk=" + Build.VERSION.SDK_INT);
            hookAllMethods(LEGACY_COLLAPSED_STATUS_BAR_CLASS, "onViewCreated", new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    View view = (View) param.getArgs()[0];
                    registerScreenshotReceiver(view);
                }
            });
        }
    }

    private void registerScreenshotReceiver(View view) {
        if (view == null || mReceiverView == view) return;
        Context context = view.getContext();
        if (context == null) return;

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (!ACTION_TAKE_SCREENSHOT.equals(intent.getAction())) return;

                boolean finished = intent.getBooleanExtra(EXTRA_IS_FINISHED, true);
                if (finished) {
                    if (mStatusBarHiddenForScreenshot) {
                        view.setVisibility(mVisibilityBeforeScreenshot);
                        if (mSurfaceHiddenForScreenshot) {
                            setStatusBarSurfaceAlpha(view, 1.0f);
                        }
                        mStatusBarHiddenForScreenshot = false;
                        mSurfaceHiddenForScreenshot = false;
                    }
                } else {
                    if (!mStatusBarHiddenForScreenshot) {
                        mVisibilityBeforeScreenshot = view.getVisibility();
                        mStatusBarHiddenForScreenshot = true;
                        mHideStartedAt = SystemClock.uptimeMillis();
                    }
                    view.setVisibility(View.INVISIBLE);
                    mSurfaceHiddenForScreenshot = setStatusBarSurfaceAlpha(view, 0.0f);
                }

                XposedLog.d(TAG, getPackageName(),
                    "HOOK_STATE=CALLBACK_HIT finished=" + finished
                        + " view=" + view.getClass().getName()
                        + " visibility=" + view.getVisibility()
                        + " surface=" + mSurfaceHiddenForScreenshot
                        + " surfaceSync=" + mLastSurfaceApplySynchronous
                        + " elapsedMs=" + (finished && mHideStartedAt > 0
                            ? SystemClock.uptimeMillis() - mHideStartedAt : 0));
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_TAKE_SCREENSHOT);
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED);
        mReceiverView = view;
        putHotReloadRuntimeState(HOT_RELOAD_VIEW_KEY, view);
        XposedLog.d(TAG, getPackageName(),
            "HOOK_STATE=INSTALLED view=" + view.getClass().getName());
        registerReceiverHotReloadCleanup(context, receiver);
        registerHotReloadCleanup(() -> {
            if (mReceiverView != null && mStatusBarHiddenForScreenshot) {
                mReceiverView.setVisibility(mVisibilityBeforeScreenshot);
                if (mSurfaceHiddenForScreenshot) {
                    setStatusBarSurfaceAlpha(mReceiverView, 1.0f);
                }
            }
            mStatusBarHiddenForScreenshot = false;
            mSurfaceHiddenForScreenshot = false;
            mStatusBarSurface = null;
            mReceiverView = null;
        });
    }

    private boolean setStatusBarSurfaceAlpha(View view, float alpha) {
        try {
            SurfaceControl surface = mStatusBarSurface;
            if (surface == null || !surface.isValid()) {
                Object viewRoot = Methods.callMethod(view, "getViewRootImpl");
                Object surfaceObject = Methods.callMethod(viewRoot, "getSurfaceControl");
                if (!(surfaceObject instanceof SurfaceControl candidate) || !candidate.isValid()) {
                    return false;
                }
                surface = candidate;
                mStatusBarSurface = candidate;
            }

            SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
            try {
                transaction.setAlpha(surface, alpha);
                mLastSurfaceApplySynchronous = applySurfaceTransaction(transaction);
            } finally {
                transaction.close();
            }
            if (alpha == 1.0f) {
                mStatusBarSurface = null;
            }
            return true;
        } catch (Throwable t) {
            mLastSurfaceApplySynchronous = false;
            XposedLog.w(TAG, getPackageName(),
                "HOOK_STATE=SURFACE_APPLY_FAILED alpha=" + alpha, t);
            return false;
        }
    }

    private boolean applySurfaceTransaction(SurfaceControl.Transaction transaction) {
        try {
            Method applySynchronously = SurfaceControl.Transaction.class
                .getDeclaredMethod("apply", boolean.class);
            applySynchronously.setAccessible(true);
            applySynchronously.invoke(transaction, true);
            return true;
        } catch (Throwable ignored) {
            transaction.apply();
            return false;
        }
    }
}
