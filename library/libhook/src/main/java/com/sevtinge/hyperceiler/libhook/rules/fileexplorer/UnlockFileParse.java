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

package com.sevtinge.hyperceiler.libhook.rules.fileexplorer;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;

import java.lang.reflect.Method;

import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;

public class UnlockFileParse extends BaseHook {
    /**
     * 文件解析（在线格式转换/预览）能力的总开关，新版文件管理器把它放在
     * com.android.fileexplorer.fileparse.util.IFileParseUtils#isSupportFileParse()，
     * 旧版本才叫 com.android.fileexplorer.util.RomUtils#supportFileParse()。
     * <p>
     * OS3.0.319、OS4.0.0.44、OS4.0.5 三份固件里，全 APK 方法名枚举中名字恰为 supportFileParse 的成员为 0 个
     * （按 dexdump 逐方法比对，audit/fileexplorer_fileparse_20261105.md；
     * 44 那一份（MIUIFileExplorer.apk，内容哈希 8d2003d5a67a37b6）已建成员索引复核，三版结论一致
     * （脚本 audit/fileparse_3ver_20261106.py）；
     * 注意字节 grep 不能单独作为判据：短子串 `FileParse` 在三版 dex 里都会命中 127 次（全是无关符号），
     * 而精确名字节 `supportFileParse` 三版都是 0 次、`isSupportFileParse` 3 次；
     * 多 dex 下计数要逐份累加（只拿最后一份会得到假的 0）。）
     * 三版真正存在的开关是 IFileParseUtils#isSupportFileParse()Z（PUBLIC STATIC，三版都是该方法，
     * 全 APK 唯一 invoke 就是 RomUtils#isAllMiuiLite()Z，而它读 miuix.device.DeviceUtils#isMiuiLiteRom()，
     * 即非酷盖版恒为 true、酷盖版被关掉）；
     * 旧的写法在设备上固定报 "Method not found: supportFileParse"，整条规则被跳过。
     * <p>
     * 这里两个命名都尝试，都找不到时只记日志直接返回，不再让反射异常连带其它规则。
     */
    @Override
    public void init() {
        Method method = null;

        Class<?> parseUtils =
                findClassIfExists("com.android.fileexplorer.fileparse.util.IFileParseUtils");
        if (parseUtils != null) {
            method = findMethodExactIfExists(parseUtils, "isSupportFileParse");
        }

        if (method == null) {
            Class<?> romUtils = findClassIfExists("com.android.fileexplorer.util.RomUtils");
            if (romUtils != null) {
                method = findMethodExactIfExists(romUtils, "supportFileParse");
            }
        }

        if (method == null) {
            XposedLog.w(TAG, getPackageName(),
                    "UnlockFileParse: no file parse support gate found, skipped");
            return;
        }

        final Method gateMethod = method;
        XposedLog.d(TAG, getPackageName(),
                "UnlockFileParse: forcing " + gateMethod.getDeclaringClass().getName()
                        + "#" + gateMethod.getName() + " to true");

        hookMethod(gateMethod, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                param.setResult(true);
            }
        });
    }
}
