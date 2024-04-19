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
package com.sevtinge.hyperceiler.module.app;

import static com.sevtinge.hyperceiler.utils.devicesdk.SystemSDKKt.isAndroidVersion;

import com.sevtinge.hyperceiler.module.base.BaseModule;
import com.sevtinge.hyperceiler.module.base.HookExpand;
import com.sevtinge.hyperceiler.module.hook.contentextension.DoublePress;
import com.sevtinge.hyperceiler.module.hook.contentextension.HorizontalContentExtension;
import com.sevtinge.hyperceiler.module.hook.contentextension.LinkOpenMode;
import com.sevtinge.hyperceiler.module.hook.contentextension.SuperImage;
import com.sevtinge.hyperceiler.module.hook.contentextension.Taplus;
import com.sevtinge.hyperceiler.module.hook.contentextension.UnlockTaplus;
import com.sevtinge.hyperceiler.module.hook.contentextension.UseThirdPartyBrowser;

@HookExpand(pkg = "com.miui.contentextension", isPad = false, tarAndroid = 34)
public class ContentExtension extends BaseModule {

    @Override
    public void handleLoadPackage() {
        initHook(new UseThirdPartyBrowser(), mPrefsMap.getBoolean("content_extension_browser"));
        initHook(new DoublePress(), mPrefsMap.getBoolean("content_extension_double_press"));
        initHook(new SuperImage(), mPrefsMap.getBoolean("content_extension_super_image"));
        initHook(new Taplus(), mPrefsMap.getBoolean("security_center_taplus"));
        initHook(new LinkOpenMode());
        initHook(HorizontalContentExtension.INSTANCE, mPrefsMap.getBoolean("content_extension_unlock_taplus_horizontal"));

        if (!isAndroidVersion(30)) {
            initHook(UnlockTaplus.INSTANCE, mPrefsMap.getBoolean("content_extension_unlock_taplus"));
        }
    }
}
