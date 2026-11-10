/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.app;

import com.hchen.database.HookBase;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.rules.securitycore.RearAlipayBackTapOption;
import com.sevtinge.hyperceiler.libhook.rules.systemframework.input.RearAlipayGestures;

@HookBase(targetPackage = "com.miui.securitycore", minSdk = 37)
public class SecurityCore extends BaseLoad {
    @Override
    public void onPackageLoaded() {
        initHook(new RearAlipayBackTapOption(), PrefsBridge.getBoolean(RearAlipayGestures.BACK_PREF));
    }
}
