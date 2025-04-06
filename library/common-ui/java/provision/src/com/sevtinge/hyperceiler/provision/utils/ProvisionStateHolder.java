package com.sevtinge.hyperceiler.provision.utils;

import com.sevtinge.hyperceiler.provision.activity.ProvisionActivity.StateMachine;

public class ProvisionStateHolder {

    private static ProvisionStateHolder sInstance;
    private StateMachine mStateMachine;

    public static ProvisionStateHolder getInstance() {
        if (sInstance == null) {
            synchronized (ProvisionStateHolder.class) {
                try {
                    if (sInstance == null) {
                        sInstance = new ProvisionStateHolder();
                    }
                } finally {}
            }
        }
        return sInstance;
    }


    public void setStateMachine(StateMachine stateMachine) {
        mStateMachine = stateMachine;
    }


}
