package com.sevtinge.hyperceiler.data.adapter;

import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;

import com.sevtinge.hyperceiler.data.model.TabViewModel;
import com.sevtinge.hyperceiler.ui.about.AboutFragment;
import com.sevtinge.hyperceiler.ui.main.HomePageFragment;
import com.sevtinge.hyperceiler.ui.settings.ModuleSettingsFragment;

import java.util.Map;

import fan.appcompat.app.AppCompatActivity;

public class DynamicFragmentPagerAdapter extends PagerAdapter {

    private static final String TAG = "HC:DynamicFragmentPagerAdapter";

    private String mCurrTab;
    private int mFragmentSize;

    private AppCompatActivity mActivity;
    private FragmentManager mFragmentManager;
    private Fragment mCurrentPrimaryItem = null;
    private FragmentTransaction mCurTransaction = null;

    private final Map<String, FragmentInfo> mFragmentCache;

    class FragmentInfo {

        String tag;
        boolean lazyInit;
        Class<? extends Fragment> clazz;

        Fragment fragment = null;

       FragmentInfo(String tag, Class<? extends Fragment> clazz, boolean lazyInit) {
            this.tag = tag;
            this.clazz = clazz;
            this.lazyInit = lazyInit;
        }
    }

    public DynamicFragmentPagerAdapter(Fragment fragment, String tag) {
        Log.d(TAG, "init, currTab: " + tag);
        mCurrTab = tag;
        mActivity = ((fan.appcompat.app.Fragment)fragment).getAppCompatActivity();
        mFragmentManager = fragment.getChildFragmentManager();
        mFragmentCache = new ArrayMap<>(getCount());
        addFragment(TabViewModel.TAB_HOME, HomePageFragment.class);
        addFragment(TabViewModel.TAB_SETTINGS, ModuleSettingsFragment.class);
        addFragment(TabViewModel.TAB_ABOUT, AboutFragment.class);
        mFragmentSize = 3;
    }

    public void addFragment(String tag, Class<? extends Fragment> clazz) {
        mFragmentCache.put(tag, new FragmentInfo(tag, clazz, false));
    }

    private String getTabAt(int position) {
        return TabViewModel.getTabAt(position);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        String tag = getTabAt(position);
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        Fragment fragment = getFragment(tag, true);
        if (fragment.getFragmentManager() != null) {
            mCurTransaction.attach(fragment);
        } else {
            mCurTransaction.add(container.getId(), fragment, tag);
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
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return ((Fragment) object).getView() == view;
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

    public Fragment getFragment(String tag, boolean z) {
        FragmentInfo fragmentInfo = mFragmentCache.get(tag);
        if (fragmentInfo != null) {
            if (fragmentInfo.fragment == null) {
                fragmentInfo.fragment = mFragmentManager.findFragmentByTag(fragmentInfo.tag);
            }
            if (z && fragmentInfo.fragment == null) {
                fragmentInfo.fragment = Fragment.instantiate(mActivity, fragmentInfo.clazz.getName());
            }
            return fragmentInfo.fragment;
        }
        String name = "";
        if (z) {
            switch (tag) {
                case TabViewModel.TAB_HOME -> name = HomePageFragment.class.getName();
                case TabViewModel.TAB_SETTINGS -> name = ModuleSettingsFragment.class.getName();
                case TabViewModel.TAB_ABOUT -> name = AboutFragment.class.getName();
            }
            return Fragment.instantiate(mActivity, name);
        }
        return mFragmentManager.findFragmentByTag(tag);
    }

    public void reCreateFragment() {
        for (String tag : TabViewModel.TABS) {
            Fragment fragment = getFragment(tag, false);
            int id = ((View) fragment.getView().getParent()).getId();
            FragmentInfo fragmentInfo = mFragmentCache.get(tag);
            FragmentTransaction beginTransaction = mFragmentManager.beginTransaction();
            beginTransaction.remove(fragment);
            Fragment newFragment = getNewFragment(tag);
            fragmentInfo.fragment = newFragment;
            fragmentInfo.lazyInit = false;
            beginTransaction.add(id, newFragment, tag);
            beginTransaction.commitAllowingStateLoss();
            notifyDataSetChanged();
        }
    }

    private Fragment getNewFragment(String str) {
        return Fragment.instantiate(mActivity, mFragmentCache.get(str).clazz.getName());
    }
}
