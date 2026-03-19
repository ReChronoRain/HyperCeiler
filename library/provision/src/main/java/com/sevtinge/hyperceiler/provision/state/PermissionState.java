package com.sevtinge.hyperceiler.provision.state;

import android.content.Intent;

public class PermissionState extends State {

    @Override
    protected Intent createEnterIntent(boolean canBack, boolean toNext) {
        Intent intent = super.createEnterIntent(canBack, toNext);
        intent.putExtra("isShowDelayAnim", true);
        return intent;
    }
}
