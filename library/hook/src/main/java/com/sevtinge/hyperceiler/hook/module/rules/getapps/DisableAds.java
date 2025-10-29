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
 * Copyright (C) 2023-2025 HyperCeiler Contributions
 */

package com.sevtinge.hyperceiler.hook.module.rules.getapps;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.MiDeviceAppUtilsKt.isLargeUI;

import com.sevtinge.hyperceiler.hook.module.base.BaseHook;

public class DisableAds extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Class<?> appDetailV3Cls = findClassIfExists("com.xiaomi.market.common.network.retrofit.response.bean.AppDetailV3");
        Class<?> detailSplashAdManagerCls = findClassIfExists("com.xiaomi.market.ui.splash.DetailSplashAdManager");
        Class<?> splashManagerCls = findClassIfExists("com.xiaomi.market.ui.splash.SplashManager");

        if (isLargeUI()) {
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
            hookAllMethods(appDetailV3Cls, method, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        }

        for (String method : appDetailMethodsFalse) {
            hookAllMethods(appDetailV3Cls, method, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
        }

        for (String method : splashMethodsFalse) {
            hookAllMethods(splashManagerCls, method, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
        }

        if (isLargeUI()) return;
        for (String method : new String[]{
                "canRequestSplashAd",
                "isRequesting",
                "isOpenFromMsa"
        }) {
            hookAllMethods(detailSplashAdManagerCls, method, new MethodHook() {
                @Override
                protected void after(MethodHookParam param) throws Throwable {
                    param.setResult(false);
                }
            });
        }

        hookAllMethods(detailSplashAdManagerCls, "tryToRequestSplashAd", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                param.setResult(null);
            }
        });
    }
}
