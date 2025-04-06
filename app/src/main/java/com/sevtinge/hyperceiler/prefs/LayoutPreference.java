package com.sevtinge.hyperceiler.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.sevtinge.hyperceiler.R;

import fan.preference.FolmeAnimationController;

public class LayoutPreference extends Preference implements FolmeAnimationController {

    private View mRootView;
    private final View.OnClickListener mClickListener = view -> performClick(view);

    public LayoutPreference(@NonNull Context context, int layoutResId) {
        this(context, LayoutInflater.from(context).inflate(layoutResId, null, false));
    }

    public LayoutPreference(Context context, View v) {
        super(context);
        setView(v);
    }

    public LayoutPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public LayoutPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, fan.preference.R.styleable.Preference, defStyleAttr, 0);
        int layoutResId = a.getResourceId(fan.preference.R.styleable.Preference_android_layout, 0);
        if (layoutResId == 0) throw new IllegalArgumentException("LayoutPreference requires a layout to be defined");
        a.recycle();
        setView(LayoutInflater.from(getContext()).inflate(layoutResId, null, false));
    }


    private void setView(View view) {
        setLayoutResource(R.layout.preference_layout_frame);
        mRootView = view;
        setShouldDisableView(false);
    }

    public <T extends View> T findViewById(int id) {
        return mRootView.findViewById(id);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        View itemView = holder.itemView;
        itemView.setOnClickListener(mClickListener);
        itemView.setFocusable(isSelectable());
        itemView.setClickable(isSelectable());
        FrameLayout frameLayout = (FrameLayout) itemView;
        frameLayout.removeAllViews();
        ViewGroup viewGroup = (ViewGroup) mRootView.getParent();
        if (viewGroup != null) viewGroup.removeView(mRootView);
        frameLayout.addView(mRootView);
    }

    @Override
    public boolean isTouchAnimationEnable() {
        return true;
    }
}
