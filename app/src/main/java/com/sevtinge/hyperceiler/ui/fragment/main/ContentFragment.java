package com.sevtinge.hyperceiler.ui.fragment.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPagerCompat;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.data.adapter.HomeFragmentPagerAdapter;
import com.sevtinge.hyperceiler.utils.DialogHelper;

import fan.appcompat.app.ActionBar;
import fan.appcompat.app.Fragment;
import fan.navigator.Navigator;
import fan.navigator.NavigatorFragmentListener;
import fan.preference.PreferenceFragment;
import fan.viewpager.widget.ViewPager;

public class ContentFragment extends Fragment implements NavigatorFragmentListener {

    private static final String TAG = "ContentFragment";
    public static final String ARG_PAGE = "page";
    public static final int ARG_PAGE_HOME = 0;
    public static final int ARG_PAGE_WIDGET = 1;
    public static final int ARG_PAGE_LIST = 2;

    protected ViewPager mViewPager;
    protected HomeFragmentPagerAdapter mViewPageFragmentAdapter;

    MenuItem mQuickRestartMenuItem;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeRes(R.style.NavigatorContentFragmentTheme);
    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_content, container, false);
    }

    @Override
    public void onViewInflated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewInflated(view, savedInstanceState);
        mViewPager = view.findViewById(R.id.vp_fragments);
        //mViewPager.setScroll(true);
        setCorrectNestedScrollMotionEventEnabled(true);

        setupViewPager();
    }

    @Override
    public void onUpdateArguments(Bundle args) {
        super.onUpdateArguments(args);
        if (mViewPager != null) {
            int page = args.getInt(ARG_PAGE, mViewPager.getCurrentItem());
            Log.d(TAG, "onUpdateArguments page:" + page);
            mViewPager.setCurrentItem(page, !mViewPager.isDraggable() ? false : false);
        }
    }

    private void setupViewPager() {
        ActionBar actionBar = getActionBar();
        Navigator navigator = Navigator.get(this);

        mViewPageFragmentAdapter = new HomeFragmentPagerAdapter(getChildFragmentManager());

        int fragmentSize = getChildFragmentManager().getFragments().size();
        if (fragmentSize > 0) {
            for (int i = 0; i < fragmentSize; i++) {
                Fragment fragment = (Fragment) getChildFragmentManager().getFragments().get(i);
                if (mViewPageFragmentAdapter.indexNeedReverse) {
                    mViewPageFragmentAdapter.addFragment(0, fragment);
                } else {
                    mViewPageFragmentAdapter.addFragment(fragment);
                }
            }
        } else {
            mViewPageFragmentAdapter.addFragment(0, createHomeFragment());
            mViewPageFragmentAdapter.addFragment(1, createSettingsFragment());
            mViewPageFragmentAdapter.addFragment(2, createAboutFragment());
        }


        mViewPager.setAdapter(mViewPageFragmentAdapter);
        if (getArguments() != null && getArguments().containsKey(ARG_PAGE)) {
            int position = getArguments().getInt(ARG_PAGE);
            mViewPager.setCurrentItem(position);
            if (navigator != null && navigator.getBottomTabMenu().size() > 0) {
                navigator.getBottomTabMenu().getItem(position).setChecked(true);
            }
            setupHyperOsViewPager(position, actionBar);
            switchTabState(position);
        }
        mViewPager.addOnPageChangeListener(new ViewPagerCompat.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected:" + position);
                navigator.selectTab(position);
                if (actionBar != null) {
                    switch (position) {
                        case ARG_PAGE_HOME -> {
                            actionBar.setTitle(getString(R.string.navigation_home_title));
                        }

                        case ARG_PAGE_WIDGET -> {
                            actionBar.setTitle(getString(R.string.navigation_settings_title));
                        }

                        case ARG_PAGE_LIST -> {
                            actionBar.setTitle(getString(R.string.navigation_about_title));
                        }
                    }
                }
                switchTabState(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void setupHyperOsViewPager(int position, ActionBar actionBar) {
        if (actionBar != null) {
            switch (position) {
                case ARG_PAGE_HOME -> actionBar.setTitle(getString(R.string.navigation_home_title));
                case ARG_PAGE_WIDGET -> actionBar.setTitle(getString(R.string.navigation_settings_title));
                case ARG_PAGE_LIST -> actionBar.setTitle(getString(R.string.navigation_about_title));
            }
        }
    }

    private void switchTabState(int page) {
        if (page == 0) {
            mViewPager.setCurrentItem(0);
        } else if (page == 1) {
            mViewPager.setCurrentItem(1);
        } else {
            mViewPager.setCurrentItem(2);
        }
        if (mQuickRestartMenuItem != null) {
            mQuickRestartMenuItem.setVisible(page == 0);
        }
    }

    protected PreferenceFragment createHomeFragment() {
        return new MainFragment();
    }

    protected PreferenceFragment createSettingsFragment() {
        return new ModuleSettingsFragment();
    }

    protected PreferenceFragment createAboutFragment() {
        return new AboutFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navigation_immersion, menu);
        mQuickRestartMenuItem = menu.findItem(R.id.quick_restart);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item == mQuickRestartMenuItem) {
            DialogHelper.showRestartDialog(requireContext());
        }
        return super.onOptionsItemSelected(item);
    }
}
