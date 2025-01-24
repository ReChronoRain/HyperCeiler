package com.sevtinge.hyperceiler.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.app.dashboard.DashboardFragment;

import fan.preference.TextPreference;

public class XmlPreference extends TextPreference {

    private int mInflatedXml;

    public XmlPreference(@NonNull Context context) {
        this(context, null);
    }

    public XmlPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XmlPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Preference, defStyleAttr, 0);
        mInflatedXml = a.getResourceId(R.styleable.Preference_inflatedXml, 0);
        a.recycle();
        if (getFragment() == null) {
            setFragment(DashboardFragment.class.getName());
        }
    }

    public int getInflatedXml() {
        return mInflatedXml;
    }
}
