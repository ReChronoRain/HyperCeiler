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
package com.sevtinge.hyperceiler.module.hook.camera;

import com.sevtinge.hyperceiler.module.base.BaseHook;


public class UnlockCvlens extends BaseHook {
    @Override
    public void init() {
        hookAllMethods("com.android.camera.CameraSettings", "isSupportCvLensDevice", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) {
                param.setResult(true);
            }
        });
        try {
            hookAllMethods("com.android.camera.CameraSettings", "getCvLensVersion", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(2);
                }
            });
            hookAllMethods("com.android.camera2.CameraCapabilities", "getCvLensVersion", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(2);
                }
            });
            hookAllMethods("com.android.camera2.CameraCapabilitiesUtil", "getCvLensVersion", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) {
                    param.setResult(2);
                }
            });
        } catch (Exception e) {
            logE(TAG, this.lpparam.packageName, "try to hook CvLensVersion failed" + e);
            throw new RuntimeException(e);
        }
    }
}
