package com.sevtinge.hyperceiler.hook.module.hook.systemframework.corepatch;

import de.robv.android.xposed.XposedHelpers;

public class CorePatchForB extends CorePatchForV {
    @Override
    Class<?> getParsedPackage(ClassLoader classLoader) {
        return XposedHelpers.findClassIfExists("com.android.internal.pm.parsing.pkg.ParsedPackage", classLoader);
    }
}