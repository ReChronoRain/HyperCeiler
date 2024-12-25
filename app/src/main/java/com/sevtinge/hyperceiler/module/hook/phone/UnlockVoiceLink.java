/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.module.hook.phone;

import static de.robv.android.xposed.XposedHelpers.setStaticBooleanField;

import com.sevtinge.hyperceiler.module.base.BaseHook;

public class UnlockVoiceLink extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.android.phone.MiuiPhoneUtils", "isSupportVoiceLinkFeature", new MethodHook(){
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(true);
            }
        });
        setStaticBooleanField(findClassIfExists("com.android.phone.CloudController.TelephonyCloudController"), "CLOUD_VOICE_LINK_FEATURE_DISABLED", false);

    }
}
