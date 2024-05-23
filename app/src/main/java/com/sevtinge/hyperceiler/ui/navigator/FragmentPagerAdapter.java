package com.sevtinge.hyperceiler.ui.navigator;

import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;

import com.sevtinge.hyperceiler.ui.navigator.page.AboutPageFragment;
import com.sevtinge.hyperceiler.ui.navigator.page.HomePageFragment;
import com.sevtinge.hyperceiler.ui.navigator.page.SettingsPageFragment;

import java.util.Map;

import fan.appcompat.app.AppCompatActivity;

public class FragmentPagerAdapter extends PagerAdapter {

    private static final String TAG = "DynamicFragmentPagerAdapter";

    private String mCurrTab;
    private int mFragmentSize;
    private AppCompatActivity mActivity;
    private FragmentManager mFragmentManager;
    private Fragment mCurrentPrimaryItem = null;
    private FragmentTransaction mCurTransaction = null;
    private final Map<String, FragmentInfo> mFragmentCache;


    public FragmentPagerAdapter(AppCompatActivity activity, FragmentManager fragmentManager, String tag) {
        Log.d(TAG, "init, currTab: " + tag);
        mCurrTab = tag;
        mActivity = activity;
        mFragmentManager = fragmentManager;
        mFragmentCache = new ArrayMap(getCount());
        addFragment("HOME", HomePageFragment.class);
        addFragment("SETTINGS", SettingsPageFragment.class);
        addFragment("ABOUT", AboutPageFragment.class);
        mFragmentSize = 3;
    }

    private String getTabAt(int position) {
        return TabViewModel.getTabAt(position);
    }

    public void addFragment(String tag, Class<? extends Fragment> clazz) {
        mFragmentCache.put(tag, new FragmentInfo(tag, clazz, false));
    }

    public Fragment getFragment(String tag, boolean z) {
        FragmentInfo fragmentInfo = mFragmentCache.get(tag);
        if (fragmentInfo.fragment == null) {
            fragmentInfo.fragment = mFragmentManager.findFragmentByTag(fragmentInfo.tag);
        }
        if (z && fragmentInfo.fragment == null) {
            fragmentInfo.fragment = Fragment.instantiate(mActivity, fragmentInfo.clazz.getName());
        }
        return fragmentInfo.fragment;
    }

    private Fragment getNewFragment(String tag) {
        return Fragment.instantiate(mActivity, mFragmentCache.get(tag).clazz.getName());
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        String tabAt = getTabAt(position);
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        Fragment fragment = getFragment(tabAt, true);
        if (fragment.getFragmentManager() != null) {
            mCurTransaction.attach(fragment);
        } else {
            mCurTransaction.add(container.getId(), fragment, tabAt);
        }
        if (fragment != mCurrentPrimaryItem) {
            fragment.setMenuVisibility(false);
            fragment.setUserVisibleHint(false);
        }
        return fragment;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        mCurTransaction.detach((Fragment) object);
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        Fragment fragment = (Fragment) object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
                mCurrentPrimaryItem.setUserVisibleHint(false);
            }
            if (fragment != null) {
                fragment.setMenuVisibility(true);
                fragment.setUserVisibleHint(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(@NonNull ViewGroup container) {
        if (mActivity != null && !mActivity.isDestroyed() && mCurTransaction != null) {
            mCurTransaction.commitAllowingStateLoss();
            mCurTransaction = null;
            if (!mFragmentManager.isDestroyed()) {
                mFragmentManager.executePendingTransactions();
            }
        }
    }

    @Override
    public int getCount() {
        return mFragmentSize;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        for (int i = 0; i < getCount(); i++) {
            if (object == mFragmentCache.get(getTabAt(i)).fragment) {
                return i;
            }
        }
        return -2;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return ((Fragment) object).getView() == view;
    }

    public class FragmentInfo {

        String tag;
        boolean lazyInit;
        Fragment fragment = null;
        Class<? extends Fragment> clazz;

        FragmentInfo(String tag, Class<? extends Fragment> clazz, boolean lazyInit) {
            this.tag = tag;
            this.clazz = clazz;
            this.lazyInit = lazyInit;
        }
    }

}
