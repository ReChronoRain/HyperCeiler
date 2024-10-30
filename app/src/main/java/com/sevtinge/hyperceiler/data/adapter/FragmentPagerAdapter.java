package com.sevtinge.hyperceiler.data.adapter;

import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager.widget.PagerAdapter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class FragmentPagerAdapter extends PagerAdapter {

    private static final String TAG = "FragmentPagerAdapter";
    @Deprecated
    public static final int BEHAVIOR_SET_USER_VISIBLE_HINT = 0;
    public static final int BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT = 1;
    private static final boolean DEBUG = false;

    private final int mBehavior;
    private boolean mExecutingFinishUpdate;
    private final FragmentManager mFragmentManager;

    private Fragment mCurrentPrimaryItem = null;
    private FragmentTransaction mCurTransaction = null;

    @Retention(RetentionPolicy.SOURCE)
    private @interface Behavior {}

    @Deprecated
    public FragmentPagerAdapter(FragmentManager fm) {
        this(fm, BEHAVIOR_SET_USER_VISIBLE_HINT);
    }


    public FragmentPagerAdapter(FragmentManager fm, int behavior) {
        mFragmentManager = fm;
        mBehavior = behavior;
    }

    public abstract Fragment getItem(int position);
    public abstract String makeFragmentName(int position, long id);


    public long getItemId(int position) {
        return position;
    }

    public int getPositionFromCache(int position) {
        return position;
    }

    @Nullable
    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(@Nullable Parcelable state, @Nullable ClassLoader loader) {

    }

    @Override
    public void startUpdate(@NonNull ViewGroup container) {

    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        long itemId = getItemId(position);
        Fragment fragment = mFragmentManager.findFragmentByTag(makeFragmentName(getPositionFromCache(position), itemId));
        if (fragment != null) {
            mCurTransaction.attach(fragment);
        } else {
            fragment = getItem(position);
            mCurTransaction.add(container.getId(), fragment, makeFragmentName(getPositionFromCache(position), itemId));
        }
        if (fragment != mCurrentPrimaryItem) {
            fragment.setMenuVisibility(false);
            fragment.setUserVisibleHint(false);
        }
        return fragment;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        Fragment fragment = (Fragment) object;
        if (mCurTransaction == null) {
            mCurTransaction = mFragmentManager.beginTransaction();
        }
        mCurTransaction.detach(fragment);
        if (fragment.equals(mCurrentPrimaryItem)) {
            mCurrentPrimaryItem = null;
        }
    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        Fragment fragment = (Fragment) object;
        if (fragment != mCurrentPrimaryItem) {
            if (mCurrentPrimaryItem != null) {
                mCurrentPrimaryItem.setMenuVisibility(false);
                if (mBehavior == 1) {
                    if (mCurTransaction == null) {
                        mCurTransaction = mFragmentManager.beginTransaction();
                    }
                    mCurTransaction.setMaxLifecycle(mCurrentPrimaryItem, Lifecycle.State.STARTED);
                } else {
                    mCurrentPrimaryItem.setUserVisibleHint(false);
                }
            }
            fragment.setMenuVisibility(true);
            if (mBehavior == 1) {
                if (mCurTransaction == null) {
                    mCurTransaction = mFragmentManager.beginTransaction();
                }
                mCurTransaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED);
            } else {
                fragment.setUserVisibleHint(true);
            }
            mCurrentPrimaryItem = fragment;
        }
    }

    @Override
    public void finishUpdate(@NonNull ViewGroup container) {
        if (mCurTransaction != null) {
            if (!mExecutingFinishUpdate) {
                try {
                    mExecutingFinishUpdate = true;
                    mCurTransaction.commitNowAllowingStateLoss();
                } finally {
                    mExecutingFinishUpdate = false;
                }
            }
            mCurTransaction = null;
        }
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return ((Fragment) object).getView() == view;
    }
}
