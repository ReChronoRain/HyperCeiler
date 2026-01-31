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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.provision.state.StartupState;
import com.sevtinge.hyperceiler.provision.state.StateMachine;
import com.sevtinge.hyperceiler.provision.utils.IKeyEvent;
import com.sevtinge.hyperceiler.provision.utils.OobeUtils;
import com.sevtinge.hyperceiler.provision.utils.PageIntercepHelper;
import com.sevtinge.hyperceiler.provision.utils.ProvisionStateHolder;

public class DefaultActivity extends ProvisionBaseActivity {

    private static final String TAG = "DefaultActivity";

    private StateMachine mStateMachine;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isDeviceIsProvisioned()) {
            PageIntercepHelper.getInstance().register(this);
            PageIntercepHelper.getInstance().setCallback(DefaultActivity.this::onActivityResult);

            mStateMachine = new StateMachine(this);
            mStateMachine.start(savedInstanceState == null ||
                    savedInstanceState.getBoolean("com.android.provision:state_enter_currentstate", true));
            ProvisionStateHolder.getInstance().setStateMachine(mStateMachine);
            if (mNewBackBtn != null) {
                Log.i(TAG, "back button set accessibility no");
                mNewBackBtn.setImportantForAccessibility(2);
                if (mNewBackBtn.getParent() != null) {
                    Log.i(TAG, "back button remove");
                    ((ViewGroup) mNewBackBtn.getParent()).removeView(mNewBackBtn);
                }
            }
        } else {
            finishSetup();
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
        return false;
        //return Utils.isProvisioned(this);
    }

    public void finishSetup() {
        PackageManager pm = getPackageManager();
        //Utils.setWallperProvisioned(this, true);
        /*ComponentName componentName = new ComponentName(this, ProvisionActivity.class);
        pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        Intent intent = new Intent("android.provision.action.PROVISION_COMPLETE");
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
        Utils.sendBroadcastAsUser(this, intent);*/
        finish();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("com.android.provision:state_enter_currentstate", mStateMachine.getCurrentState() instanceof StartupState);
    }

    @Override
    public void onDestroy() {
        PageIntercepHelper.getInstance().unregisterReceiver(this);
        //enableStatusBar(true);
        //unRegisterNetworkChangedReceiver();
        super.onDestroy();
        if (isDeviceIsProvisioned()) {
            //Process.killProcess(Process.myPid());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult requestCode: " + requestCode + " resultCode =  " + resultCode);
        if (requestCode == 33 && OobeUtils.shouldNotFinishDefaultActivity()) {
            mStateMachine.resumeState();
        } else if (requestCode == 1) {
            if (resultCode == -1) {
                nextAction(124);
            }
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

    void nextAction(int i) {
        Log.i(TAG, " here is nextAction ");
        //startActivityForResult(WizardManagerHelper.getNextIntent(getIntent(), i), 10000);
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
