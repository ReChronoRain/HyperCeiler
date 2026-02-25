package com.sevtinge.hyperceiler.home;

import android.app.Activity;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;

import com.sevtinge.hyperceiler.common.utils.CtaUtils;
import com.sevtinge.hyperceiler.common.utils.DialogHelper;

import fan.provision.OobeUtils;

public class CtaManager {
    public static void launch(FragmentActivity activity) {
        if (OobeUtils.getOperatorState(activity, "cm_pick_status")) return;
        if (!CtaUtils.isCtaNeedShow(activity)) return;

        if (CtaUtils.isCtaBypass()) {
            ActivityResultLauncher<Intent> launcher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                res -> {
                    if (res.getResultCode() == 1) CtaUtils.setCtaValue(activity, true);
                    else { activity.finishAffinity(); System.exit(0); }
                }
            );
            CtaUtils.showCtaDialog(launcher, activity);
        } else {
            DialogHelper.showUserAgreeDialog(activity);
        }
    }
}


