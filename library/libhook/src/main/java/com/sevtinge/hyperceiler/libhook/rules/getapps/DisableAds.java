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
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.libhook.rules.getapps;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;

import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;

import io.github.kyuubiran.ezxhelper.xposed.common.AfterHookParam;
import io.github.kyuubiran.ezxhelper.xposed.common.BeforeHookParam;

public class DisableAds extends BaseHook {
    @Override
    public void init() {
        Class<?> appDetailV3Cls = findClassIfExists("com.xiaomi.market.common.network.retrofit.response.bean.AppDetailV3");
        Class<?> detailSplashAdManagerCls = findClassIfExists("com.xiaomi.market.ui.splash.DetailSplashAdManager");
        Class<?> splashManagerCls = findClassIfExists("com.xiaomi.market.ui.splash.SplashManager");

        if (isPad()) {
            appDetailV3Cls = findClassIfExists("com.xiaomi.market.retrofit.response.bean.AppDetailV3");
        }

        String[] appDetailMethodsTrue = {
            "isBrowserMarketAdOff",
            "isBrowserSourceFileAdOff",
            "supportShowCompat64bitAlert"
        };

        String[] appDetailMethodsFalse = {
            "isInternalAd",
            "needShowAds",
            "needShowAdsWithSourceFile",
            "showComment",
            "showRecommend",
            "showTopBanner",
            "showTopVideo",
            "equals",
            "getShowOpenScreenAd",
            "hasGoldLabel",
            "isBottomButtonLayoutType",
            "isPersonalization",
            "isTopButtonLayoutType",
            "isTopSingleTabMultiButtonType",
            "needShowGrayBtn",
            "needShowPISafeModeStyle",
            "supportAutoLoadDeepLink",
            "supportShowCompatAlert",
            "supportShowCompatChildForbidDownloadAlert"
        };

        String[] splashMethodsFalse = {
            "canShowSplash",
            "needShowSplash",
            "needRequestFocusVideo",
            "isPassiveSplashAd"
        };

        for (String method : appDetailMethodsTrue) {
            hookAllMethods(appDetailV3Cls, method, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    param.setResult(true);
                }
            });
        }

        for (String method : appDetailMethodsFalse) {
            hookAllMethods(appDetailV3Cls, method, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    param.setResult(false);
                }
            });
        }

        for (String method : splashMethodsFalse) {
            hookAllMethods(splashManagerCls, method, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    param.setResult(false);
                }
            });
        }

        if (isPad()) return;
        for (String method : new String[]{
            "canRequestSplashAd",
            "isRequesting",
            "isOpenFromMsa"
        }) {
            hookAllMethods(detailSplashAdManagerCls, method, new IMethodHook() {
                @Override
                public void after(AfterHookParam param) {
                    param.setResult(false);
                }
            });
        }

        hookAllMethods(detailSplashAdManagerCls, "tryToRequestSplashAd", new IMethodHook() {
            @Override
            public void before(BeforeHookParam param) {
                param.setResult(null);
            }
        });
    }
}
