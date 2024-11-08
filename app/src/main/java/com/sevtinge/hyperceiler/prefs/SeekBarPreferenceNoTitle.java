package com.sevtinge.hyperceiler.prefs;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;

import fan.preference.SeekBarPreferenceCompat;

public class SeekBarPreferenceNoTitle extends SeekBarPreferenceCompat {

    public SeekBarPreferenceNoTitle(@NonNull Context context) {
        this(context, null);
    }

    public SeekBarPreferenceNoTitle(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBarPreferenceNoTitle(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(fan.preference.R.layout.miuix_preference_widget_seekbar_compat_no_title);
    }
}
