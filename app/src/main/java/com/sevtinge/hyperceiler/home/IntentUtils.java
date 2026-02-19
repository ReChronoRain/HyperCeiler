package com.sevtinge.hyperceiler.home;

import androidx.fragment.app.FragmentActivity;

import fan.bottomsheet.BottomSheetModal;

public class IntentUtils {

    public static boolean isBottomSheetDismissed = true;

    public static void goToCustomOrderDialog(FragmentActivity fragmentActivity, OnCompleteCallBack callBack) {
        if (fragmentActivity != null && isBottomSheetDismissed) {
            isBottomSheetDismissed = false;
            CustomOrderBottomSheetModel bottomSheetModel = new CustomOrderBottomSheetModel(fragmentActivity);
            CustomOrderFragment customOrderFragment = new CustomOrderFragment();
            customOrderFragment.setBottomSheetModal(bottomSheetModel);
            customOrderFragment.setCompleteCallBack(callBack);

            bottomSheetModel.setFragment(customOrderFragment, customOrderFragment.getTag(), false);
            bottomSheetModel.show();
            //customBottomSheetModel.getBehavior().setBottomModeMaxWidth(ResourceUtils.getDimentionPixelsSize(0x7f0702f1));
            bottomSheetModel.setOnDismissListener(new BottomSheetModal.OnDismissListener() {
                @Override
                public void onDismiss() {
                    callBack.onDismiss();
                    isBottomSheetDismissed = true;
                    bottomSheetModel.removeFragment();
                }
            });
        }
    }
}
