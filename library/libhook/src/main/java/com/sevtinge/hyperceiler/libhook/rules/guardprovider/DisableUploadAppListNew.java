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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.guardprovider;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IReplaceHook;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

import java.lang.reflect.Method;
import java.util.ArrayList;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;

/**
 * 阻止「系统安全组件」(com.miui.guardprovider) 上传已安装应用列表。
 *
 * <p>guardprovider 3.x 的上报链路：
 * <pre>
 *   Lie2.d(ctx)  枚举第三方应用 -> List&lt;he2&gt;(pkg / versionCode / 签名MD5 / 应用名)
 *   Lie2.a(list) 组装 JSON      -> {timestamp, os, biz_id:"virus_scan", uuid, content:[...]}
 *   Lls1.d(app, json)           -> POST https://flash.sec.miui.com/detect/app
 * </pre>
 *
 * <p>旧实现使用 {@code usingStrings("AntiDefraudAppManager", "https://flash.sec.miui.com/detect/app")}，
 * 但 3.1.4 起网络代码已从 AntiDefraudAppManager 抽离到独立的网络助手类
 * （日志 tag {@code NetworkApiHelper}），两个字符串不再共存于同一方法体内，
 * 导致匹配结果为 0、{@code requiredMember} 抛异常、整个 Hook 被静默跳过。
 *
 * <p>本实现改为按「外发出口」定位，并以「采集源头」作为第二道拦截。
 * 命中特征（对 3.1.4-20260902.0 / versionCode 330 实测）：
 * <ul>
 *   <li>{@code "https://flash.sec.miui.com/detect/app" + "NetworkApiHelper"} → 唯一命中 Lls1.d</li>
 *   <li>{@code "AntiDefraudAppManager" + "getUnSystemAppList error, "} → 唯一命中 Lie2.d</li>
 * </ul>
 */
public class DisableUploadAppListNew extends BaseHook {

    /** 外发出口：Lls1.d(GuardApplication, JSONObject) -> String */
    private Method mUploadEgressMethod;
    /** 采集源头：Lie2.d(GuardApplication) -> ArrayList */
    private Method mAppListCollectorMethod;

    @Override
    protected boolean useDexKit() {
        return true;
    }

    @Override
    protected boolean initDexKit() {
        mUploadEgressMethod = findUploadEgress();
        mAppListCollectorMethod = findAppListCollector();

        XposedLog.d(TAG, getPackageName(), "DexKit resolved upload egress = " + mUploadEgressMethod
            + ", app list collector = " + mAppListCollectorMethod);

        if (mUploadEgressMethod == null && mAppListCollectorMethod == null) {
            throw new IllegalStateException(TAG
                + ": no DexKit matcher matched the app list upload path of " + getPackageName()
                + " (unsupported app version?)");
        }
        return true;
    }

    /**
     * 定位外发出口。按可靠性依次回退，任一命中即可。
     */
    private Method findUploadEgress() {
        // 首选：URL + 网络助手日志 tag，3.1.4 起唯一命中 Lls1.d
        Method m = optionalMember("UploadEgressByUrlAndTag", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("https://flash.sec.miui.com/detect/app", "NetworkApiHelper")
            )).singleOrNull());
        if (m != null) return m;

        // 回退 1：URL + 该方法的签名盐（同方法体内硬编码）
        m = optionalMember("UploadEgressByUrlAndSalt", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("https://flash.sec.miui.com/detect/app", "6988567a-4220-4b51-bc2d-ccdec27a74a1")
            )).singleOrNull());
        if (m != null) return m;

        // 回退 2：仅 URL。实测全 DEX 中该 URL 只出现 1 次
        return optionalMember("UploadEgressByUrl", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("https://flash.sec.miui.com/detect/app")
            )).singleOrNull());
    }

    /**
     * 定位采集源头（返回 ArrayList 的枚举方法）。允许失败，仅作纵深防御。
     */
    private Method findAppListCollector() {
        // 首选：类名 tag + 该方法独有日志串，唯一命中 Lie2.d
        Method m = optionalMember("AppListCollectorByLog", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("AntiDefraudAppManager", "getUnSystemAppList error, ")
            )).singleOrNull());
        if (m != null) return m;

        // 回退：请求组装方法 Lie2.a（命中它同样可掐断上报）
        return optionalMember("AppListCollectorByBuilder", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .usingStrings("AntiDefraudAppManager", "getAllUnSystemAppsStatus error, ")
            )).singleOrNull());
    }

    @Override
    public void init() {
        if (mUploadEgressMethod != null) {
            hookMethod(mUploadEgressMethod, new IReplaceHook() {
                @Override
                public Object replace(HookParam param) {
                    // 返回 null：Lie2.e 侧 TextUtils.isEmpty(null) 为 true，直接跳过写库与后续流程，
                    // 宿主不会抛 NPE。
                    XposedLog.i(TAG, getPackageName(),
                        "Blocked app list upload to https://flash.sec.miui.com/detect/app");
                    return null;
                }
            });
        }

        if (mAppListCollectorMethod != null) {
            hookMethod(mAppListCollectorMethod, new IReplaceHook() {
                @Override
                public Object replace(HookParam param) {
                    // 返回空集合而非 null，避免调用方遍历时 NPE。
                    // 同时让 getInstalledPackages() 根本不被执行，从源头阻止枚举。
                    return new ArrayList<>();
                }
            });
        }
    }
}
