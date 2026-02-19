package com.sevtinge.hyperceiler.home;

import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.sevtinge.hyperceiler.R;

import fan.bottomsheet.BottomSheetBehavior;
import fan.bottomsheet.BottomSheetModal;

public class CustomBottomSheetModel extends BottomSheetModal {

    public Fragment mFragment = null;
    public FragmentActivity mActivity;

    public CustomBottomSheetModel(FragmentActivity activity) {
        super(activity);
        mActivity = activity;
        init();
    }

    public final void init() {
        setContentView(R.layout.bottom_sheet_fragment_container);
        setDragHandleViewEnabled(true);
        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        behavior.setState(3);
        behavior.setForceFullHeight(true);
        behavior.setModeConfig(0);
        behavior.setDraggable(true);
        behavior.setSkipHalfExpanded(true);
        behavior.setSkipCollapsed(true);
        applyBottomSheetStyle(behavior);
    }

    protected void applyBottomSheetStyle(BottomSheetBehavior<FrameLayout> behavior) {}

    public void setFragment(Fragment fragment, String tag, boolean addToBackStack) {
        mFragment = fragment;
        if (fragment != null) {
            FragmentTransaction beginTransaction = mActivity.getSupportFragmentManager().beginTransaction();
            if (addToBackStack) {
                beginTransaction.addToBackStack(tag);
            }
            beginTransaction.replace(R.id.fragment_container, mFragment, tag);
            beginTransaction.commitAllowingStateLoss();
        }
    }

    public void removeFragment() {
        if (mFragment != null) {
            FragmentTransaction beginTransaction = mActivity.getSupportFragmentManager().beginTransaction();
            beginTransaction.remove(mFragment);
            beginTransaction.commitAllowingStateLoss();
            mFragment = null;
        }
    }

    public void release() {
        mActivity = null;
    }
}
