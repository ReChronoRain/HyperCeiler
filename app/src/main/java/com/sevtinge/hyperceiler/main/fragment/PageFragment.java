package com.sevtinge.hyperceiler.main.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;
import com.sevtinge.hyperceiler.hook.utils.prefs.PrefsUtils;

import java.lang.reflect.Field;

import fan.nestedheader.widget.NestedHeaderLayout;
import fan.preference.PreferenceFragment;
import fan.springback.view.SpringBackLayout;

public abstract class PageFragment extends PreferenceFragment {

    private static final String TAG = "PageFragment";

    protected View mRootView;
    protected ViewGroup mContainerView;
    protected NestedHeaderLayout mNestedHeaderLayout;

    public int getThemeRes() {
        return R.style.Theme_Navigator_ContentChild_Home;
    }

    public abstract int getPreferenceScreenResId();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (getThemeRes() != 0) setThemeRes(R.style.AppTheme);
        super.onCreate(savedInstanceState);
    }

    public int getContentLayoutResId() {
        return 0;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View onCreateView = super.onCreateView(inflater, container, savedInstanceState);

        mRootView = inflater.inflate(R.layout.fragment_page, container, false);
        mContainerView = mRootView.findViewById(R.id.container);
        if (getContentLayoutResId() != 0) {
            mContainerView.addView(LayoutInflater.from(requireContext()).inflate(getContentLayoutResId(), mContainerView, false));
        }

        ViewGroup prefsContainer = mRootView.findViewById(R.id.prefs_container);
        prefsContainer.addView(onCreateView);

        setOverlayMode();

        mNestedHeaderLayout = mRootView.findViewById(R.id.nested_header_layout);
        setSearchViewEnabled(false);
        registerCoordinateScrollView(mNestedHeaderLayout);

        RecyclerView listView = getListView();
        View parent = (View) listView.getParent();
        if (parent instanceof SpringBackLayout) {
            parent.setEnabled(false);
            listView.setPaddingRelative(listView.getPaddingStart(), 0, listView.getPaddingEnd(), 0);
        }

        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public View getRootView() {
        return mRootView;
    }

    public void setSearchViewEnabled(boolean enabled) {
        mNestedHeaderLayout.setHeaderViewVisible(enabled);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        if (getPreferenceScreenResId() != 0) {
            setPreferencesFromResource(getPreferenceScreenResId(), rootKey);
            initPrefs();
        }
    }

    public void initPrefs() {}

    protected void setOverlayMode() {
        try {
            Field declaredField = PreferenceFragment.class.getDeclaredField("mIsOverlayMode");
            declaredField.setAccessible(true);
            declaredField.set(this, false);
        } catch (Exception e) {
            AndroidLogUtils.logE(TAG, "setOverlayMode error", e);
        }
    }

    public SharedPreferences getSharedPreferences() {
        return PrefsUtils.mSharedPreferences;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterCoordinateScrollView(mNestedHeaderLayout);
    }
}
