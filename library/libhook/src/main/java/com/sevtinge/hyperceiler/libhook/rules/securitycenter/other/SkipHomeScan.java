/* SPDX-License-Identifier: AGPL-3.0-or-later
 * Copyright (C) 2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.securitycenter.other;

import com.sevtinge.hyperceiler.common.log.XposedLog;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class SkipHomeScan extends BaseHook {
    private Method startScan;
    private Field contentFrame;
    private Method setScore;
    private Method setStatus;

    @Override protected boolean useDexKit() { return true; }

    @Override protected boolean initDexKit() {
        contentFrame = null;
        startScan = requiredMember("SecurityHomeStartScan", bridge -> bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create().declaredClass("com.miui.securityscan.MainFragment")
                .usingStrings("incremental_scan_fg", "scan").paramCount(0).returnType(void.class))).singleOrNull());
        // CN and Global use different field/interface names, but the UI contract is shared.
        for (Field field : startScan.getDeclaringClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            try {
                Method score = field.getType().getMethod("setScoreText", int.class);
                Method status = field.getType().getMethod("setStatusTopText", String.class);
                if (contentFrame != null) throw new IllegalStateException("Ambiguous Security home content frame");
                contentFrame = field;
                setScore = score;
                setStatus = status;
            } catch (NoSuchMethodException ignored) { }
        }
        if (contentFrame == null) throw new IllegalStateException("Security home content frame not found");
        return true;
    }

    @Override public void init() {
        hookMethod(startScan, new IMethodHook() {
            @Override public void before(HookParam param) {
                // Do not schedule scan workers or pretend that a scan completed.
                param.setResult(null);
                try {
                    Object frame = getObjectField(param.getThisObject(), contentFrame.getName());
                    if (frame != null) {
                        setScore.invoke(frame, 100);
                        setStatus.invoke(frame, "");
                    }
                } catch (ReflectiveOperationException e) {
                    XposedLog.e(TAG, getPackageName(), e);
                }
                XposedLog.d(TAG, getPackageName(), "Skipped home scan; score fixed at 100");
            }
        });
    }
}
