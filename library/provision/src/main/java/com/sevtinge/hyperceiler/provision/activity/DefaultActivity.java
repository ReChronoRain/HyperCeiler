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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.provision.state.StartupState;
import com.sevtinge.hyperceiler.provision.state.StateMachine;
import com.sevtinge.hyperceiler.provision.utils.IKeyEvent;
import com.sevtinge.hyperceiler.provision.utils.PageIntercepHelper;
import com.sevtinge.hyperceiler.provision.utils.ProvisionStateHolder;

import fan.provision.OobeUtils;
import fan.provision.ProvisionBaseActivity;

public class DefaultActivity extends ProvisionBaseActivity {

    private static final String TAG = "DefaultActivity";

    private static final String STATE_ENTER_CURRENTSTATE = "com.android.provision:state_enter_currentstate";
    private static final String STATE_PENDING_REQUEST_CODE = "com.android.provision:state_pending_request_code";

    private StateMachine mStateMachine;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int mPendingRequestCode = -1;
    private final ActivityResultLauncher<Intent> mActivityResultLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            int requestCode = mPendingRequestCode;
            mPendingRequestCode = -1;
            handleActivityResult(requestCode, result.getResultCode(), result.getData());
        }
    );

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isDeviceIsProvisioned() && !OobeUtils.isDebugOobeMode(this)) {
            finishSetup();
            return;
        }
        if (savedInstanceState != null) {
            mPendingRequestCode = savedInstanceState.getInt(STATE_PENDING_REQUEST_CODE, -1);
        }

        PageIntercepHelper.getInstance().register(this);
        PageIntercepHelper.getInstance().setCallback(DefaultActivity.this::handleActivityResult);

        mStateMachine = new StateMachine(this);
        mStateMachine.start(savedInstanceState == null ||
            savedInstanceState.getBoolean(STATE_ENTER_CURRENTSTATE, true));
        ProvisionStateHolder.getInstance().setStateMachine(mStateMachine);
        if (mNewBackBtn != null) {
            AndroidLog.i(TAG, "back button set accessibility no");
            mNewBackBtn.setImportantForAccessibility(2);
            if (mNewBackBtn.getParent() != null) {
                AndroidLog.i(TAG, "back button remove");
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
        if (mStateMachine == null || mStateMachine.getCurrentState() == null) return;
        super.onWindowFocusChanged(hasFocus);
        if (mStateMachine.getCurrentState() instanceof IKeyEvent) {
            Log.i(TAG, " here is onWindowFocusChanged ");
            ((StartupState) mStateMachine.getCurrentState()).onWindowFocusChanged(hasFocus);
        }
    }

    public void run(int i) {
        mStateMachine.run(i);
    }

    public void enterCurrentState(@Nullable Bundle activityOptions) {
        mStateMachine.enterCurrentState(activityOptions);
    }

    public void launchStateActivityForResult(@NonNull Intent intent, int requestCode) {
        launchStateActivityForResult(intent, requestCode, null);
    }

    public void launchStateActivityForResult(@NonNull Intent intent, int requestCode, @Nullable Bundle activityOptions) {
        mPendingRequestCode = requestCode;
        if (activityOptions == null) {
            mActivityResultLauncher.launch(intent);
            return;
        }
        mActivityResultLauncher.launch(intent, new BundleActivityOptionsCompat(activityOptions));
    }

    private boolean isDeviceIsProvisioned() {
        return OobeUtils.isProvisioned(this);
    }

    public void finishSetup() {
        mHandler.postDelayed(this::finish, 0L);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(STATE_ENTER_CURRENTSTATE, mStateMachine.getCurrentState() instanceof StartupState);
        bundle.putInt(STATE_PENDING_REQUEST_CODE, mPendingRequestCode);
    }

    @Override
    public void onDestroy() {
        PageIntercepHelper.getInstance().unregisterReceiver(this);
        super.onDestroy();
    }

    public void handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        AndroidLog.i(TAG, "onActivityResult requestCode: " + requestCode + " resultCode =  " + resultCode);
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

    private static final class BundleActivityOptionsCompat extends ActivityOptionsCompat {

        private final Bundle mOptions;

        private BundleActivityOptionsCompat(@NonNull Bundle options) {
            mOptions = options;
        }

        @Override
        public Bundle toBundle() {
            return mOptions;
        }
    }
}
