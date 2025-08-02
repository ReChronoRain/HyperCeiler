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
package com.sevtinge.hyperceiler.main.adapter;

import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.PagerAdapter;

import com.sevtinge.hyperceiler.main.model.TabViewModel;
import com.sevtinge.hyperceiler.main.page.AboutPage;
import com.sevtinge.hyperceiler.main.page.HomePage;
import com.sevtinge.hyperceiler.main.page.SettingsPage;

import java.util.Map;

import fan.appcompat.app.AppCompatActivity;

public class DynamicFragmentPagerAdapter extends PagerAdapter {

    private static final String TAG = "HC:DynamicFragmentPagerAdapter";

    private final int mFragmentSize = 3;
    private final AppCompatActivity mActivity;
    private final FragmentManager mFragmentManager;
    private Fragment mCurrentPrimaryItem = null;
    private FragmentTransaction mCurTransaction = null;
    private final Map<String, FragmentInfo> mFragmentCache = new ArrayMap<>(mFragmentSize);

    static class FragmentInfo {
        final String tag;
        final Class<? extends Fragment> clazz;
        Fragment fragment;

        FragmentInfo(String tag, Class<? extends Fragment> clazz) {
            this.tag = tag;
            this.clazz = clazz;
        }
    }

    public DynamicFragmentPagerAdapter(Fragment fragment, String tag) {
        Log.d(TAG, "init, currTab: " + tag);
        mActivity = ((fan.appcompat.app.Fragment) fragment).getAppCompatActivity();
        mFragmentManager = fragment.getChildFragmentManager();
        addFragment(TabViewModel.TAB_HOME, HomePage.class);
        addFragment(TabViewModel.TAB_SETTINGS, SettingsPage.class);
        addFragment(TabViewModel.TAB_ABOUT, AboutPage.class);
    }

    public void addFragment(String tag, Class<? extends Fragment> clazz) {
        mFragmentCache.put(tag, new FragmentInfo(tag, clazz));
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
        if (fragment.isAdded()) {
            mCurTransaction.attach(fragment);
        } else {
            mCurTransaction.add(container.getId(), fragment, tag);
        }
        if (fragment != mCurrentPrimaryItem) {
            fragment.setMenuVisibility(false);
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
            }
            fragment.setMenuVisibility(true);
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
            FragmentInfo info = mFragmentCache.get(getTabAt(i));
            if (info != null && object == info.fragment) {
                return i;
            }
        }
        return POSITION_NONE;
    }

    public Fragment getFragment(String tag, boolean z) {
        FragmentInfo fragmentInfo = mFragmentCache.get(tag);
        if (fragmentInfo != null) {
            if (fragmentInfo.fragment == null) {
                fragmentInfo.fragment = mFragmentManager.findFragmentByTag(fragmentInfo.tag);
            }
            if (z && fragmentInfo.fragment == null) {
                fragmentInfo.fragment = mFragmentManager.getFragmentFactory()
                    .instantiate(mActivity.getClassLoader(), fragmentInfo.clazz.getName());
            }
            return fragmentInfo.fragment;
        }
        return null;
    }

    public void reCreateFragment() {
        for (String tag : TabViewModel.TABS) {
            Fragment oldFragment = getFragment(tag, false);
            if (oldFragment == null) continue;
            View view = oldFragment.getView();
            if (view == null || view.getParent() == null) continue;
            int id = ((View) view.getParent()).getId();
            FragmentInfo fragmentInfo = mFragmentCache.get(tag);
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.remove(oldFragment);
            Fragment newFragment = getNewFragment(tag);
            fragmentInfo.fragment = newFragment;
            transaction.add(id, newFragment, tag);
            transaction.commitAllowingStateLoss();
        }
        notifyDataSetChanged();
    }

    private Fragment getNewFragment(String tag) {
        FragmentInfo info = mFragmentCache.get(tag);
        if (info == null) return null;
        return mFragmentManager.getFragmentFactory()
            .instantiate(mActivity.getClassLoader(), info.clazz.getName());
    }
}
