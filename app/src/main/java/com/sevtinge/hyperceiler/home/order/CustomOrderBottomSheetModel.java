package com.sevtinge.hyperceiler.home.order;

import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;

import fan.bottomsheet.BottomSheetBehavior;
import fan.core.utils.HyperMaterialUtils;
import fan.core.utils.RomUtils;

public class CustomOrderBottomSheetModel extends CustomBottomSheetModel {


    public CustomOrderBottomSheetModel(FragmentActivity activity) {
        super(activity);
    }

    @Override
    protected void applyBottomSheetStyle(BottomSheetBehavior<FrameLayout> behavior) {
        setCanceledOnTouchOutside(false);
        //setDragHandleViewEnabled(false);
        behavior.setDraggable(false);
        behavior.setFixedHeightRatioEnabled(false);
        behavior.setState(3);

        if (HyperMaterialUtils.isFeatureEnable(mActivity) && RomUtils.getHyperOsVersion() >= 2) {
            behavior.setModeConfig(0);
            applyBlur(true);
        }
    }
}
