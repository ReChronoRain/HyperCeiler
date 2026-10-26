/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.appbase.input;

import android.graphics.Insets;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import java.lang.ref.WeakReference;

/** MIUI's separate bottom area already reserves space for the navigation bar. */
public final class MiuiImeInsets {
    private WeakReference<ViewGroup> inputFrame = new WeakReference<>(null);
    private WeakReference<ViewGroup> bottomArea = new WeakReference<>(null);
    private int loggedBottom;
    private boolean bottomReady;
    private ViewTreeObserver layoutObserver;
    private final ViewTreeObserver.OnGlobalLayoutListener layoutListener = () -> {
        ViewGroup area = bottomArea.get();
        boolean ready = area != null && area.getVisibility() == View.VISIBLE && area.getChildCount() > 0;
        boolean becameReady = ready && !bottomReady;
        bottomReady = ready;
        if (becameReady) requestApplyInsets();
    };

    public void bind(ViewGroup frame, ViewGroup area) {
        int sdk = frame.getContext().getApplicationInfo().targetSdkVersion;
        ModuleLog.i("HyperCeiler.ImeBottom", "Keyboard inset binding targetSdk=" + sdk
            + " bottomChildren=" + area.getChildCount() + " bottomVisible=" + (area.getVisibility() == View.VISIBLE));
        if (sdk < 35) return;
        clear();
        inputFrame = new WeakReference<>(frame);
        bottomArea = new WeakReference<>(area);
        // The bottom area starts GONE on cold start. Its first visible layout
        // must re-dispatch the insets that the keyboard already received.
        layoutObserver = area.getViewTreeObserver();
        layoutObserver.addOnGlobalLayoutListener(layoutListener);
        frame.requestApplyInsets();
    }

    public void requestApplyInsets() {
        ViewGroup frame = inputFrame.get();
        if (frame != null) frame.requestApplyInsets();
    }

    public WindowInsets forInputView(View view, WindowInsets original) {
        ViewGroup frame = inputFrame.get();
        ViewGroup area = bottomArea.get();
        if (frame == null || area == null || view.getParent() != frame
            || area.getVisibility() != View.VISIBLE || area.getChildCount() == 0) return original;
        int type = WindowInsets.Type.navigationBars();
        Insets visible = original.getInsets(type);
        Insets stable = original.getInsetsIgnoringVisibility(type);
        if (visible.bottom == 0 && stable.bottom == 0) return original;
        int bottom = Math.max(visible.bottom, stable.bottom);
        if (loggedBottom != bottom) {
            loggedBottom = bottom;
            ModuleLog.i("HyperCeiler.ImeBottom", "Removed duplicate keyboard navigation inset=" + bottom);
        }
        // Only the keyboard subtree gets this copy. The MIUI buttons, gesture area,
        // horizontal safe insets and the app's existing insets listeners stay intact.
        return new WindowInsets.Builder(original)
            .setInsets(type, Insets.of(visible.left, visible.top, visible.right, 0))
            .setInsetsIgnoringVisibility(type, Insets.of(stable.left, stable.top, stable.right, 0))
            .build();
    }

    public void clear() {
        if (layoutObserver != null && layoutObserver.isAlive()) {
            layoutObserver.removeOnGlobalLayoutListener(layoutListener);
        }
        ViewGroup area = bottomArea.get();
        if (area != null && area.getViewTreeObserver() != layoutObserver && area.getViewTreeObserver().isAlive()) {
            area.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
        }
        layoutObserver = null;
        inputFrame.clear();
        bottomArea.clear();
        bottomReady = false;
    }
}
