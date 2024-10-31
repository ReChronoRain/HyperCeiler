package com.sevtinge.hyperceiler.utils;

import android.view.View;
import android.view.ViewGroup;

import fan.appcompat.internal.app.widget.ActionBarOverlayLayout;

public abstract class ActionBarUtils {

    public static ViewGroup getActionBarOverlayLayout(View view) {
        while (view != null) {
            if (view instanceof ActionBarOverlayLayout) {
                return (ActionBarOverlayLayout) view;
            }
            view = view.getParent() instanceof View ? (View) view.getParent() : null;
        }
        return null;
    }
}
