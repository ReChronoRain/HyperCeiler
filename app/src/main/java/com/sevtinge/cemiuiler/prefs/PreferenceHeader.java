package com.sevtinge.cemiuiler.prefs;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.cemiuiler.R;

import moralnorm.preference.Preference;

public class PreferenceHeader extends Preference {

    public PreferenceHeader(@NonNull Context context) {
        super(context);
        init();
    }

    public PreferenceHeader(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_header);
    }
}
