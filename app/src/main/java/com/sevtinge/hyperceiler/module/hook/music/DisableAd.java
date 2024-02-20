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
package com.sevtinge.hyperceiler.module.hook.music;

import static com.sevtinge.hyperceiler.utils.log.AndroidLogUtils.LogD;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class DisableAd extends BaseHook {

    Class<?> mCloud;

    @Override
    // by @Yife Playte
    public void init() {
        try {
            findAndHookMethod("com.tencent.qqmusiclite.activity.SplashAdActivity", "onCreate", android.os.Bundle.class, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) {
                    try {
                        Class<?> clazz = findClassIfExists("android.app.Activity");
                        clazz.getMethod("finish").invoke(param.thisObject);
                    } catch (Throwable e) {
                        LogD(TAG, e);
                    }
                }
            });
        } catch (Throwable e) {
            LogD(TAG, e);
        }
    }
}
