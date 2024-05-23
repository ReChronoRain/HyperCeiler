package com.sevtinge.hyperceiler.ui.fragment.settings.widget;

import static com.sevtinge.hyperceiler.ui.fragment.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;

import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.ui.fragment.settings.SettingsPreferenceFragment;

import fan.preference.PreferenceGroup;
import fan.preference.PreferenceGroupAdapter;
import fan.preference.PreferenceScreen;

public class HighlightablePreferenceGroupAdapter extends PreferenceGroupAdapter {

    private final String mHighlightKey;
    private boolean mHighlightRequested;
    private int mHighlightPosition = RecyclerView.NO_POSITION;

    public HighlightablePreferenceGroupAdapter(PreferenceGroup preferenceGroup, String key,
                                               boolean highlightRequested) {
        super(preferenceGroup);
        mHighlightKey = key;
        mHighlightRequested = highlightRequested;
        final Context context = preferenceGroup.getContext();
        final TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                outValue, true /* resolveRefs */);
        //mNormalBackgroundRes = outValue.resourceId;
        //mHighlightColor = context.getColor(R.color.preference_highlight_color);
    }

    /**
     * Tries to override initial expanded child count.
     * <p/>
     * Initial expanded child count will be ignored if:
     * 1. fragment contains request to highlight a particular row.
     * 2. count value is invalid.
     */
    public static void adjustInitialExpandedChildCount(SettingsPreferenceFragment host) {
        if (host == null) {
            return;
        }
        final PreferenceScreen screen = host.getPreferenceScreen();
        if (screen == null) {
            return;
        }
        final Bundle arguments = host.getArguments();
        if (arguments != null) {
            final String highlightKey = arguments.getString(EXTRA_FRAGMENT_ARG_KEY);
            if (!TextUtils.isEmpty(highlightKey)) {
                // Has highlight row - expand everything
                screen.setInitialExpandedChildrenCount(Integer.MAX_VALUE);
                return;
            }
        }

        final int initialCount = host.getInitialExpandedChildCount();
        if (initialCount <= 0) {
            return;
        }
        screen.setInitialExpandedChildrenCount(initialCount);
    }
}
