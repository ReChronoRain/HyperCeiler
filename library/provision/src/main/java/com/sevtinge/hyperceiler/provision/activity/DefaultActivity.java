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

 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.provision.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.provision.state.StartupState;
import com.sevtinge.hyperceiler.provision.state.StateMachine;
import com.sevtinge.hyperceiler.provision.utils.IKeyEvent;
import fan.provision.OobeUtils;
import fan.provision.ProvisionBaseActivity;

import com.sevtinge.hyperceiler.provision.utils.PageIntercepHelper;
import com.sevtinge.hyperceiler.provision.utils.ProvisionStateHolder;
import com.sevtinge.hyperceiler.provision.utils.Utils;

public class DefaultActivity extends ProvisionBaseActivity {

    private static final String TAG = "DefaultActivity";

    private static final String STATE_ENTER_CURRENTSTATE = "com.android.provision:state_enter_currentstate";

    private StateMachine mStateMachine;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isDeviceIsProvisioned()) {
            finishSetup();
            return;
        }

        PageIntercepHelper.getInstance().register(this);
        PageIntercepHelper.getInstance().setCallback(DefaultActivity.this::onActivityResult);

        mStateMachine = new StateMachine(this);
        mStateMachine.start(savedInstanceState == null ||
            savedInstanceState.getBoolean(STATE_ENTER_CURRENTSTATE, true));
        ProvisionStateHolder.getInstance().setStateMachine(mStateMachine);
        if (mNewBackBtn != null) {
            Log.i(TAG, "back button set accessibility no");
            mNewBackBtn.setImportantForAccessibility(2);
            if (mNewBackBtn.getParent() != null) {
                Log.i(TAG, "back button remove");
                ((ViewGroup) mNewBackBtn.getParent()).removeView(mNewBackBtn);
            }
        }

        if (mConfirmButton != null) {
            mConfirmButton.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean hasNavigationButton() {
        return false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mStateMachine.getCurrentState() instanceof IKeyEvent) {
            Log.i(TAG, " here is onWindowFocusChanged ");
            ((StartupState) mStateMachine.getCurrentState()).onWindowFocusChanged(hasFocus);
        }
    }

    public void run(int i) {
        mStateMachine.run(i);
    }

    private boolean isDeviceIsProvisioned() {
        return OobeUtils.isProvisioned(this);
    }

    public void finishSetup() {

        mHandler.postDelayed(() -> finish(), 0L);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(STATE_ENTER_CURRENTSTATE, mStateMachine.getCurrentState() instanceof StartupState);
    }

    @Override
    public void onDestroy() {
        PageIntercepHelper.getInstance().unregisterReceiver(this);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult requestCode: " + requestCode + " resultCode =  " + resultCode);
        if (requestCode == 33 && OobeUtils.shouldNotFinishDefaultActivity()) {
            mStateMachine.resumeState();
        } else if (requestCode == 1) {

        } else if (requestCode == 3510) {
            //onAuraActivityResult(requestCode, resultCode);
        } else {
            mStateMachine.onResult(resultCode, data);
            if (data != null) {
                //mStateMachine.setMultiSimSettingsSkiped(data.getBooleanExtra("extra_mutisimsettings_force_skiped", false));
                //mStateMachine.setBootVideoSkiped(data.getBooleanExtra("extra_bootvideo_force_skiped", false));
            }
            mStateMachine.run(resultCode);
        }
    }

    @Override
    public boolean hasPreview() {
        return false;
    }

    @Override
    public boolean hasTitle() {
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
