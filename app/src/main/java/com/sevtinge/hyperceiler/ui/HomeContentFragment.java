package com.sevtinge.hyperceiler.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.about.controller.BgEffectController;
import com.sevtinge.hyperceiler.ui.adapter.HomeContentAdapter;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;
import com.sevtinge.hyperceiler.ui.page.AboutSettingsFragment;
import com.sevtinge.hyperceiler.ui.page.HomePageFragment;
import com.sevtinge.hyperceiler.ui.page.SettingsPageFragment;

import fan.appcompat.app.ActionBar;
import fan.appcompat.app.Fragment;
import fan.appcompat.internal.app.widget.ActionBarImpl;
import fan.appcompat.internal.app.widget.ActionBarView;
import fan.device.DeviceUtils;
import fan.navigator.Navigator;
import fan.navigator.NavigatorFragmentListener;
import fan.navigator.navigatorinfo.UpdateFragmentNavInfo;
import fan.viewpager2.widget.ViewPager2;

public class HomeContentFragment extends Fragment implements NavigatorFragmentListener {

    private static final String TAG = "HomeContentFragment";
    private static final String ARG_PAGE = "page";

    private FrameLayout mContentView;
    private View mBgEffectView;
    private BgEffectController mBgEffectController;

    public ViewPager2 mViewPager;
    public HomeContentAdapter mContentAdapter;

    public ViewPagerChangeListener mPageChangeCallback;

    public SwitchManager mSwitchManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeRes(R.style.NavigatorContentTheme);
    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_content, container, false);
        mSwitchManager = new SwitchManager((ViewGroup) view);
        mSwitchManager.addSwitchView(R.menu.bottom_nav_menu, NavigationStyle.BOTTOM_LABEL);
        return view;
    }

    @Override
    public void onViewInflated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewInflated(view, savedInstanceState);
        setShaderBackGround();

        mSwitchManager.setOnSwitchChangeListener(new OnSwitchChangeListener() {
            @Override
            public void onSwitchChange(int position, int itemId) {
                setCurrentItem(position, true);
            }
        });

        mContentAdapter = new HomeContentAdapter(requireActivity());

        mContentAdapter.addFragment(new HomePageFragment());
        mContentAdapter.addFragment(new SettingsPageFragment());
        mContentAdapter.addFragment(new AboutSettingsFragment());

        mViewPager = view.findViewById(R.id.vp_fragments);
        mViewPager.setAdapter(mContentAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setSpringEnabled(false);
        mViewPager.registerOnPageChangeCallback(getPagerChangeListener());

        Navigator navigator = Navigator.get(this);
        if (navigator != null) {
            if (navigator.getBottomTabView() != null) {
                navigator.getBottomTabView().show(true);
            }
        }

        registerCoordinateScrollView(mViewPager);

        ((Switch)view.findViewById(R.id.update)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                mSwitchManager.setFloatingStyle(isChecked);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        startRuntimeShader();
    }

    @Override
    public void onUpdateArguments(Bundle args) {
        super.onUpdateArguments(args);
        if (mViewPager != null && args != null) {
            int position = args.getInt(ARG_PAGE, mViewPager.getCurrentItem());
            Log.d(TAG, "onUpdateArguments page:" + position);
            if (mViewPager.getCurrentItem() != position) {
                mViewPager.setCurrentItem(position, mViewPager.isUserInputEnabled());
            }
        }
    }

    public void handleFragmentChange(int position) {
        androidx.fragment.app.Fragment fragment = mContentAdapter.getFragment(position);
        Log.d(TAG, "fragment: " + fragment);
        if (fragment instanceof IFragmentChange iFragmentChange) {
            iFragmentChange.onFragmentChange(getActionBar(), mBgEffectView);
        }
    }

    public void refreshActionBar(int position) {
        ActionBar actionBar = getActionBar();
        if (getAppCompatActivity() == null || actionBar == null) return;
        refreshActionBarView(actionBar, position);
        refreshActionBarTitle(actionBar, position);
    }

    public void refreshActionBarView(ActionBar actionBar, int position) {
        int expandState = actionBar.getExpandState();
        View view = actionBar != null ? actionBar.getActionBarView() : null;
        ActionBarView actionBarView = view instanceof ActionBarView ? (ActionBarView) view : null;
        if (position == 0 || position == 1) {
            actionBar.setExpandState(expandState);
            actionBar.setResizable(true);
            if (actionBar instanceof ActionBarImpl impl) {
                impl.setBlur(Boolean.TRUE);
            }
            if (actionBarView != null) {
                actionBarView.getTitleView(0).setAlpha(1f);
            }
        } else {
            actionBar.setExpandState(0);
            actionBar.setResizable(false);
            if (actionBar instanceof ActionBarImpl impl) {
                impl.setBlur(Boolean.FALSE);
            }
            if (actionBarView != null) {
                actionBarView.getTitleView(0).setAlpha(0f);
            }
        }
    }

    public void refreshActionBarTitle(ActionBar actionBar, int position) {
        if (position == 0) {
            actionBar.setTitle(R.string.navigation_home_title);
        } else if (position == 1) {
            actionBar.setTitle(R.string.navigation_settings_title);
        } if (position == 2) {
            actionBar.setTitle(R.string.navigation_about_title);
        }
    }

    @Override
    public void onNavigationMenuSelected(MenuItem menuItem) {
        /*int itemId = menuItem.getItemId();
        if (itemId == R.id.home_page) {
            mViewPager.setCurrentItem(0, true);
        } else if (itemId == R.id.settings_page) {
            mViewPager.setCurrentItem(1, true);
        } else if (itemId == R.id.about_page) {
            mViewPager.setCurrentItem(2, true);
        }
        super.onNavigationMenuSelected(menuItem);*/
    }

    public final ViewPagerChangeListener getPagerChangeListener() {
        if (mPageChangeCallback == null) {
            mPageChangeCallback = new ViewPagerChangeListener();
        }
        return mPageChangeCallback;
    }

    public void setCurrentItem(int position, boolean smoothScroll) {
        setCurrentItem(position, smoothScroll, false);
    }

    public final void setCurrentItem(int position, boolean smoothScroll, boolean z2) {
        Log.d(TAG, "setCurrentItem item: " + position);
        Navigator navigator = Navigator.get(this);
        if (navigator == null || mViewPager == null) return;
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, position);
        if (navigator.getNavigationMode() == Navigator.Mode.NC && z2) {
            navigator.navigate(new UpdateFragmentNavInfo(position + 1000, getClass(), args, true));
        } else {
            mViewPager.setCurrentItem(position, smoothScroll);
            navigator.navigate(new UpdateNavigationSelectNavInfo(position + 1000, getClass(), args));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navigation_option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.quick_restart) {
            DialogHelper.showRestartDialog(requireContext());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class ViewPagerChangeListener extends ViewPager2.OnPageChangeCallback {

        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            Navigator navigator = Navigator.get(HomeContentFragment.this);
            if (navigator != null) {
                navigator.selectTab(position);
                Bundle args = new Bundle();
                args.putInt(ARG_PAGE, position);
                navigator.navigate(new UpdateFragmentNavInfo(position + 1000, HomeContentFragment.this.getClass(), args));
            }
            handleFragmentChange(position);
            refreshActionBar(position);

           mSwitchManager.setSelectedPosition(position, true);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            if (position == 0) {
                // 处于第1页向第2页滑动过程中：背景完全隐藏
                mBgEffectView.setAlpha(0f);
            } else if (position == 1) {
                // 处于第2页向第3页滑动过程中
                // positionOffset 会从 0.0f (第2页全显) 变化到 1.0f (第3页全显)
                mBgEffectView.setAlpha(positionOffset);
            } else if (position >= 2) {
                // 已完全到达第3页或往后：背景完全显示
                mBgEffectView.setAlpha(1f);
            }
        }
    }

    private void setShaderBackGround() {
        mContentView = requireActivity().findViewById(android.R.id.content);
        if (mBgEffectView == null) {
            mBgEffectView = LayoutInflater.from(getContext()).inflate(R.layout.app_about_bg, mContentView, false);
            mContentView.addView(mBgEffectView, 0);
            mBgEffectView = requireActivity().findViewById(R.id.bgEffectView);
            mBgEffectController = new BgEffectController(mBgEffectView);
        }
        startRuntimeShader();
    }

    private void startRuntimeShader() {
        if (mBgEffectView == null) return;
        if (!DeviceUtils.isMiuiLiteRom()) {
            mBgEffectView.post(() -> {
                if (getContext() != null) {
                    mBgEffectController.start();
                    mBgEffectController.setType(getContext().getApplicationContext(), mBgEffectView, null);
                }
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBgEffectController != null) {
            mBgEffectController.stop();
        }
    }
}
