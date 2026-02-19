package com.sevtinge.hyperceiler.oldui.main.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.oldui.main.page.HomePageFragment;

import fan.appcompat.app.Fragment;
import fan.nestedheader.widget.NestedHeaderLayout;
import fan.preference.PreferenceFragment;

public class PageFragment extends Fragment {

    protected View mRootView;
    protected ViewGroup mContainer;
    protected ViewGroup mPrefsContainer;
    protected NestedHeaderLayout mNestedHeaderLayout;

    public int getThemeRes() {
        return R.style.Theme_Navigator_ContentChild_Page;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (getThemeRes() != 0) setThemeRes(getThemeRes());
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_page, container, false);

        mNestedHeaderLayout = mRootView.findViewById(R.id.nested_header_layout);
        mContainer = mRootView.findViewById(R.id.container);
        mPrefsContainer = mRootView.findViewById(R.id.prefs_container);

        if (getContentLayoutResId() != 0) {
            mContainer.addView(LayoutInflater.from(requireContext()).inflate(getContentLayoutResId(), mContainer, false));
        }

        registerCoordinateScrollView(mNestedHeaderLayout);
        setSearchViewEnabled();
        setPreferenceFragment();
        return mRootView;
    }

    @Override
    public void onViewInflated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewInflated(view, savedInstanceState);
    }

    public void setSearchViewEnabled() {
        mNestedHeaderLayout.setHeaderViewVisible(this instanceof HomePageFragment);
    }

    public int getContentLayoutResId() {
        return 0;
    }

    public PreferenceFragment getPreferenceFragment() {
        return null;
    }

    public void setPreferenceFragment() {
        if (getPreferenceFragment() != null) {
            getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.prefs_container, getPreferenceFragment())
                .commit();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unregisterCoordinateScrollView(mNestedHeaderLayout);
    }
}
