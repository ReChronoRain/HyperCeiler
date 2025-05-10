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

 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */
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
