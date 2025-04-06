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
package com.sevtinge.hyperceiler.hook.module.hook.voiceassist;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.hook.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

public class UseThirdPartyBrowser extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Method method = DexKit.findMember("StartActivityWithIntent", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                        .matcher(MethodMatcher.create()
                                .usingStrings("IntentUtils", "permission click No Application can handle your intent")
                        )).singleOrNull();
                return methodData;
            }
        });
        hookMethod(method, new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                android.content.Intent intent = (android.content.Intent) param.args[0];
                logI(TAG, lpparam.packageName, "com.miui.voiceassist get Intent String: " + intent.toString());
                try {
                    if ("com.android.browser".equals(intent.getPackage()) && intent.getDataString() != null) {
                        logI(TAG, lpparam.packageName, "com.miui.voiceassist get URL: " + intent.getDataString());
                        android.net.Uri uri = android.net.Uri.parse(intent.getDataString());
                        android.content.Intent newIntent = new android.content.Intent();
                        newIntent.setAction("android.intent.action.VIEW");
                        newIntent.setData(uri);
                        param.args[0] = newIntent;
                    }
                } catch (Exception e) {
                    logE(TAG, lpparam.packageName, e);
                }
            }
        });
    }
}
