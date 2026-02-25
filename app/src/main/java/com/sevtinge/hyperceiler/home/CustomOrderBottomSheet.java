package com.sevtinge.hyperceiler.home;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.R;

import fan.bottomsheet.BottomSheetBehavior;
import fan.bottomsheet.BottomSheetDialog;
import fan.bottomsheet.BottomSheetDialogFragment;
import fan.core.utils.HyperMaterialUtils;

public class CustomOrderBottomSheet extends BottomSheetDialogFragment {

    CustomOrderFragment mFragment;
    OnCompleteCallBack mOnCompleteCallBack;

    public CustomOrderBottomSheet(OnCompleteCallBack callBack) {
        super();
        mOnCompleteCallBack =  callBack;
        mFragment = new CustomOrderFragment();
        //mFragment.setBottomSheetModal(customBottomSheetModel);
        mFragment.setCompleteCallBack(callBack);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_fragment_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getChildFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, mFragment)
            .commitAllowingStateLoss();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.MiuixBottomSheetDialog);
        BottomSheetBehavior<FrameLayout> behavior =  dialog.getBehavior();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setDragHandleViewEnabled(true);

        behavior.setDraggable(false);
        behavior.setFixedHeightRatioEnabled(false);
        behavior.setState(3);

        behavior.setForceFullHeight(true);
        behavior.setModeConfig(0);
        behavior.setSkipHalfExpanded(true);
        behavior.setSkipCollapsed(true);

        /*if (HyperMaterialUtils.isFeatureEnable(customOrderFragment.getActivity()) && RomUtils.getHyperOsVersion() >= 2) {
            behavior.setModeConfig(0);
            customBottomSheetModel.applyBlur(true);
        }*/

        return dialog;
    }


}
