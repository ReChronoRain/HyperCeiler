/*
 * This file is part of HyperCeiler.

 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.model.adapter.DynamicFragmentPagerAdapter;
import com.sevtinge.hyperceiler.model.TabViewModel;
import com.sevtinge.hyperceiler.common.view.DraggableViewPager;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;

import fan.appcompat.app.ActionBar;
import fan.appcompat.app.Fragment;
import fan.navigator.Navigator;
import fan.navigator.NavigatorFragmentListener;
import fan.navigator.navigatorinfo.DetailFragmentNavInfo;
import fan.navigator.navigatorinfo.UpdateFragmentNavInfo;
import fan.os.Build;
import fan.viewpager.widget.ViewPager;

public class ContentFragment extends Fragment implements NavigatorFragmentListener {

    private static final String TAG = "ContentFragment";

    public static final int ARG_PAGE_HOME = 0;
    public static final int ARG_PAGE_Settings = 1;
    public static final int ARG_PAGE_About = 2;

    public static final String ARG_PAGE = "page";
    public static final String CURRENT_TAB = "current_tab";
    public static String mCurrTab = TabViewModel.TAB_HOME;

    private boolean isInActionMode = false;

    private ActionBar mActionBar;

    protected DraggableViewPager mViewPager;
    protected DynamicFragmentPagerAdapter mViewPagerAdapter;

    MenuItem mQuickRestartMenuItem;

    private String mPage1Name;
    private String mPage2Name;
    private String mPage3Name;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeRes(com.sevtinge.hyperceiler.ui.R.style.NavigatorContentFragmentTheme);
        if (savedInstanceState != null) {
            mCurrTab = savedInstanceState.getString(CURRENT_TAB);
        }
        mPage1Name = getString(com.sevtinge.hyperceiler.ui.R.string.navigation_home_title);
        mPage2Name = getString(com.sevtinge.hyperceiler.ui.R.string.navigation_settings_title);
        mPage3Name = getString(com.sevtinge.hyperceiler.ui.R.string.navigation_about_title);
    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(com.sevtinge.hyperceiler.ui.R.layout.fragment_content, container, false);
    }

    @Override
    public void onViewInflated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewInflated(view, savedInstanceState);
        setCorrectNestedScrollMotionEventEnabled(true);
        mViewPager = view.findViewById(com.sevtinge.hyperceiler.ui.R.id.vp_fragments);
        setupViewPager();
        if (Build.IS_TABLET && Navigator.get(this).getNavigationMode() == Navigator.Mode.NLC) {
            Navigator.get(this).navigate(new DetailFragmentNavInfo(-1, DetailFragment.class, new Bundle()));
        }
    }

    public void selectNavigationItem() {
        if (isInActionMode) {
            destroyActionMode();
        }
        Log.d(TAG, "selectNavigationItem: " + mCurrTab);
        switch (mCurrTab) {
            case TabViewModel.TAB_HOME -> navigateToHome(this);
            case TabViewModel.TAB_SETTINGS -> navigateToSettings(this);
            case TabViewModel.TAB_ABOUT -> navigateToAbout(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        selectNavigationItem();
    }

    private int getIndexByTab(String tag) {
        int i = 1;
        switch (tag) {
            case TabViewModel.TAB_HOME -> i = 0;
            case TabViewModel.TAB_SETTINGS -> i = 1;
            case TabViewModel.TAB_ABOUT -> i = 2;
        }
        Log.d(TAG, "getIndexByTab: " + i);
        return i;
    }

    public void handleFragmentChange(String oldTab, String newTab) {
        Log.d(TAG, "handleFragmentChange: oldTab: " + oldTab + "newTab: " + newTab);
        IFragmentChange oldFragment = (IFragmentChange) mViewPagerAdapter.getFragment(oldTab, false);
        Log.d(TAG, "oldFragment: " + oldFragment);
        if (oldFragment != null) oldFragment.onLeave(mActionBar);

        IFragmentChange newFragment = (IFragmentChange) mViewPagerAdapter.getFragment(newTab, false);
        Log.d(TAG, "newFragment: " + newFragment);
        if (newFragment != null) {
            newFragment.onEnter(mActionBar);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_TAB, mCurrTab);
    }

    @Override
    public void onUpdateArguments(Bundle args) {
        super.onUpdateArguments(args);
        if (mViewPager != null) {
            int page = args.getInt(ARG_PAGE, mViewPager.getCurrentItem());
            Log.d(TAG, "onUpdateArguments page:" + page);
            mViewPager.setCurrentItem(page, mViewPager.isDraggable());
        }
    }

    @Override
    public void onNavigatorModeChanged(Navigator.Mode mode, Navigator.Mode mode2) {
        if (getView() != null) {
            Navigator.get(this);
            mViewPager.setDraggable(true);
            invalidateOptionsMenu();
        }
    }

    private void setupViewPager() {
        mActionBar = getActionBar();
        Navigator navigator = Navigator.get(this);
        mViewPager.setDraggable(true);
        invalidateOptionsMenu();
        mViewPagerAdapter = new DynamicFragmentPagerAdapter(this, mCurrTab);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(mViewPagerAdapter);
        if (getArguments() != null && getArguments().containsKey(ARG_PAGE)) {
            int position = getArguments().getInt(ARG_PAGE);
            mViewPager.setCurrentItem(position);
            switchTabState(position);
        }
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            String leftTab;
            float offSet = 1.0f;
            boolean isHandUp = false;
            boolean isHandScroll = false;
            boolean isPageChanged;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (isHandScroll || (isHandUp && !isPageChanged)) {
                    String tag = TabViewModel.getTabAt(position);
                    leftTab = tag;
                    if (tag.equals(mCurrTab)) {
                        offSet = 1.0f - positionOffset;
                    } else if (position == TabViewModel.getTabPosition(mCurrTab) - 1) {
                        offSet = positionOffset;
                    } else {
                        offSet = 0.5f;
                    }
                    if (offSet < 0.5f) {
                        offSet = 0.5f;
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {
                String tabAt = TabViewModel.getTabAt(position);
                navigator.selectTab(position);
                switchTabState(position);

                handleFragmentChange(mCurrTab, tabAt);
                offSet = 1.0f;
                mCurrTab = tabAt;
                selectNavigationItem();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == 1) {
                    isPageChanged = false;
                    isHandScroll = true;
                    isHandUp = false;
                } else if (state == 2) {
                    isHandScroll = false;
                    isHandUp = true;
                } else {
                    isHandScroll = false;
                    isHandUp = false;
                }
            }
        });
        selectNavigationItem();
    }

    private void destroyActionMode() {
        if (mViewPagerAdapter != null) {
            for (int i = 0; i < TabViewModel.getTabCount(); i++) {
                /*Fragment fragment = mViewPagerAdapter.getFragment(TabViewModel.TABS[i], false);
                if (fragment instanceof PreferenceFragment) {
                    ((PreferenceFragment) fragment).destroyActionMode();
                }*/
            }
        }
    }

    private void switchTabState(int position) {
        Log.d("ContentFragment", "switchTabState:" + position);
        if (mActionBar != null) {
            setActionBarTitle(position);
        }
        if (mQuickRestartMenuItem != null) mQuickRestartMenuItem.setVisible(position == 0);
    }

    private void setActionBarTitle(int position) {
        switch (position) {
            case 0 -> mActionBar.setTitle(mPage1Name);
            case 1 -> mActionBar.setTitle(mPage2Name);
            case 2 -> mActionBar.setTitle(mPage3Name);
        }
    }

    public void resetActionBar() {
        if (mActionBar != null) {
            mActionBar.setExpandState(1);
            mActionBar.setResizable(true);
        }
    }

    public void navigateToHome(ContentFragment contentFragment) {
        Navigator.get(contentFragment).navigate(getUpdateFragmentNavInfoToHome());
    }

    public void navigateToSettings(ContentFragment contentFragment) {
        Navigator.get(contentFragment).navigate(getUpdateFragmentNavInfoToSettings());
    }

    public void navigateToAbout(ContentFragment contentFragment) {
        Navigator.get(contentFragment).navigate(getUpdateFragmentNavInfoToAbout());
    }

    private UpdateFragmentNavInfo getUpdateFragmentNavInfoToHome() {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, ARG_PAGE_HOME);
        return new UpdateFragmentNavInfo(1000, getClass(), args);
    }

    private UpdateFragmentNavInfo getUpdateFragmentNavInfoToSettings() {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, ARG_PAGE_Settings);
        return new UpdateFragmentNavInfo(1001, getClass(), args);
    }

    private UpdateFragmentNavInfo getUpdateFragmentNavInfoToAbout() {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, ARG_PAGE_About);
        return new UpdateFragmentNavInfo(1002, getClass(), args);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.sevtinge.hyperceiler.ui.R.menu.navigation_immersion, menu);
        mQuickRestartMenuItem = menu.findItem(com.sevtinge.hyperceiler.ui.R.id.quick_restart);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item == mQuickRestartMenuItem) {
            DialogHelper.showRestartDialog(requireContext());
        }
        return super.onOptionsItemSelected(item);
    }

    public interface IFragmentChange {
        void onEnter(ActionBar actionBar);
        void onLeave(ActionBar actionBar);
    }
}
