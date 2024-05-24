package com.sevtinge.hyperceiler.ui.navigator;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.utils.DialogHelper;

import fan.appcompat.app.ActionBar;
import fan.appcompat.app.AlertDialog;
import fan.appcompat.app.Fragment;
import fan.navigator.Navigator;
import fan.navigator.NavigatorFragmentListener;
import fan.navigator.navigatorinfo.UpdateFragmentNavInfo;

public class ContentFragment extends Fragment implements NavigatorFragmentListener {

    private static final String TAG = "ContentFragment";
    public static final String ARG_PAGE = "page";
    public static String mCurrTab = "HOME";
    private ActionBar mActionBar;
    private TextView mCanaryTips;
    private DraggableViewPager mViewPager;
    private FragmentPagerAdapter mViewPagerAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeRes(R.style.TabNavigatorContentFragmentTheme);
    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_content, container, false);
    }

    @Override
    public void onUpdateArguments(Bundle args) {
        super.onUpdateArguments(args);
        if (mViewPager != null) {
            int position = args.getInt(ARG_PAGE, mViewPager.getCurrentItem());
            mViewPager.setCurrentItem(position, mViewPager.isDraggable());
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

    @Override
    public void onViewInflated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewInflated(view, savedInstanceState);
        setCorrectNestedScrollMotionEventEnabled(true);
        mCanaryTips = view.findViewById(R.id.canary_tips);
        mCanaryTips.setAlpha(0.5f);
        mCanaryTips.setOnClickListener(v -> DialogHelper.showCanaryTipsDialog(requireActivity(), "当前版本是UI线路，可能包含各种不稳定因素"));
        mViewPager = view.findViewById(R.id.viewpager);
        registerCoordinateScrollView(mViewPager);
        Navigator.get(this).setTabSelectListener((menuItem, info) -> {
            int position = 0;
            switch (info.getNavigationId()) {
                case 1000 -> position = 0;
                case 1001 -> position = 1;
                case 1002 -> position = 2;
            }
            mViewPager.setCurrentItem(position, mViewPager.isDraggable());
            return true;
        });
        setupViewPager();
    }

    private void setupViewPager() {
        mActionBar = getActionBar();
        mActionBar.setResizable(true);
        mActionBar.setDisplayShowCustomEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(false);
        Navigator navigator = Navigator.get(this);
        mViewPager.setDraggable(false);
        mViewPagerAdapter = new FragmentPagerAdapter(getAppCompatActivity(), getChildFragmentManager(), mCurrTab);
        mViewPager.setOffscreenPageLimit(4);
        mViewPager.setAdapter(mViewPagerAdapter);
        if (getArguments() != null && getArguments().containsKey(ARG_PAGE)) {
            int item = getArguments().getInt(ARG_PAGE);
            mViewPager.setCurrentItem(item);
            if (navigator != null && navigator.getBottomTabMenu().size() > 0) {
                navigator.getBottomTabMenu().getItem(item).setChecked(true);
            }
            setupHyperOSViewPager(item);
        }

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            String leftTab;
            float offSet = 1.0f;
            boolean isHandUp = false;
            boolean isHandScroll = false;
            boolean isPageChanged;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (isHandScroll || (isHandUp && !isPageChanged)) {
                    String tabAt = TabViewModel.getTabAt(position);
                    leftTab = tabAt;
                    if (tabAt.equals(mCurrTab)) {
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
                setupHyperOSViewPager(position);
                handleFragmentChange(mCurrTab, tabAt);
                offSet = 1.0f;
                mCurrTab = tabAt;
                selectNavigationItem(position);
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
    }

    private void setupHyperOSViewPager(int position) {
        if (mActionBar != null) {
            if (position == 0) {
                mActionBar.setTitle(R.string.home);
            } else if (position == 1) {
                mActionBar.setTitle(R.string.settings);
            } else {
                mActionBar.setTitle(R.string.about);
            }
        }
    }

    public void selectNavigationItem(int position) {
        switch (mCurrTab) {
            case "HOME" -> navigateToHome();
            case "SETTINGS" -> navigateToSettings();
            case "ABOUT" -> navigateToAbout();
            default -> {}
        }
    }

    public void handleFragmentChange(String oldTab, String newTab) {
        Log.d(TAG, "handleFragmentChange: oldTab: " + oldTab + "newTab: " + newTab);
        if (newTab != null && oldTab != null && !newTab.equals(oldTab)) {
            androidx.fragment.app.Fragment oldFragment = mViewPagerAdapter.getFragment(oldTab, false);
            Log.d(TAG, "oldFragment: " + oldFragment);
            androidx.fragment.app.Fragment newFragment = mViewPagerAdapter.getFragment(newTab, false);
            Log.d(TAG, "newFragment: " + newFragment);
        }
    }

    public void navigateToHome() {
        Navigator.get(this).navigate(getUpdateFragmentNavInfoToHome());
    }

    public void navigateToSettings() {
        Navigator.get(this).navigate(getUpdateFragmentNavInfoToSettings());
    }

    public void navigateToAbout() {
        Navigator.get(this).navigate(getUpdateFragmentNavInfoToAbout());
    }

    private UpdateFragmentNavInfo getUpdateFragmentNavInfoToHome() {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, 0);
        return new UpdateFragmentNavInfo(1000, getClass(), args);
    }

    private UpdateFragmentNavInfo getUpdateFragmentNavInfoToSettings() {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, 1);
        return new UpdateFragmentNavInfo(1001, getClass(), args);
    }

    private UpdateFragmentNavInfo getUpdateFragmentNavInfoToAbout() {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, 2);
        return new UpdateFragmentNavInfo(1002, getClass(), args);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.restart) {
            DialogHelper.showRestartDialog(requireContext());
        }
        return super.onOptionsItemSelected(item);
    }
}
