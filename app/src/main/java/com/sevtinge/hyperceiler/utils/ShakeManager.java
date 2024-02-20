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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import com.sevtinge.hyperceiler.module.app.GlobalActions;

public class ShakeManager implements SensorEventListener {

    private float xAccel;
    private float yAccel;
    private float zAccel;

    private float xPreviousAccel;
    private float yPreviousAccel;
    private float zPreviousAccel;

    private boolean firstUpdate = true;
    private boolean shakeInitiated = false;
    private long lastShakeEvent = System.currentTimeMillis();

    private final Context helperContext;

    public ShakeManager(Context helpercontext) {
        this.helperContext = helpercontext;
    }

    public void reset() {
        xAccel = 0;
        yAccel = 0;
        zAccel = 0;
        xPreviousAccel = 0;
        yPreviousAccel = 0;
        zPreviousAccel = 0;
        firstUpdate = true;
        shakeInitiated = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Don't care...
    }

    @Override
    public void onSensorChanged(SensorEvent se) {
        updateAccelParameters(se.values[0], se.values[1], se.values[2]);
        if (!shakeInitiated && isAccelerationChanged())
            shakeInitiated = true;
        else if (shakeInitiated && isAccelerationChanged())
            executeShakeActionDelayed();
        else if (shakeInitiated && (!isAccelerationChanged()))
            shakeInitiated = false;
    }

    private void updateAccelParameters(float xNewAccel, float yNewAccel, float zNewAccel) {
        if (firstUpdate) {
            xPreviousAccel = xNewAccel;
            yPreviousAccel = yNewAccel;
            zPreviousAccel = zNewAccel;
            firstUpdate = false;
        } else {
            xPreviousAccel = xAccel;
            yPreviousAccel = yAccel;
            zPreviousAccel = zAccel;
        }
        xAccel = xNewAccel;
        yAccel = yNewAccel;
        zAccel = zNewAccel;
    }

    private boolean isAccelerationChanged() {
        float deltaX = Math.abs(xPreviousAccel - xAccel);
        float deltaY = Math.abs(yPreviousAccel - yAccel);
        float deltaZ = Math.abs(zPreviousAccel - zAccel);
        float shakeThresholdX = 4f;
        float shakeThresholdY = 4f;
        float shakeThresholdZ = 8f;
        return (deltaX > shakeThresholdX && deltaY > shakeThresholdY)
            || (deltaX > shakeThresholdX && deltaZ > shakeThresholdZ)
            || (deltaY > shakeThresholdY && deltaZ > shakeThresholdZ);
    }

    private void executeShakeActionDelayed() {
        long now = System.currentTimeMillis();
        int shakeEventThrottle = 750;
        if (now - lastShakeEvent > shakeEventThrottle) {
            lastShakeEvent = now;
            executeShakeAction();
        }
    }

    private void executeShakeAction() {
        GlobalActions.handleAction(helperContext, "prefs_key_home_gesture_shake");
    }
}
