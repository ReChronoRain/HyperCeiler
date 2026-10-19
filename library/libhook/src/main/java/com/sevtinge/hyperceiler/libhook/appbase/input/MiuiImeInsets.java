/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.appbase.input;

import android.graphics.Insets;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import com.sevtinge.hyperceiler.common.log.ModuleLog;
import java.lang.ref.WeakReference;

/** MIUI's separate bottom area already reserves space for the navigation bar. */
public final class MiuiImeInsets {
    private WeakReference<ViewGroup> inputFrame = new WeakReference<>(null);
    private WeakReference<ViewGroup> bottomArea = new WeakReference<>(null);
    private int loggedBottom;

    public void bind(ViewGroup frame, ViewGroup area) {
        if (frame.getContext().getApplicationInfo().targetSdkVersion < 35) return;
        inputFrame = new WeakReference<>(frame);
        bottomArea = new WeakReference<>(area);
        frame.requestApplyInsets();
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
        inputFrame.clear();
        bottomArea.clear();
    }
}
