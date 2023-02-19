package com.sevtinge.cemiuiler.module.browser;

import com.sevtinge.cemiuiler.module.base.BaseHook;

public class DebugMode extends BaseHook {
        @Override
        public void init() {
            hookAllMethods("f.b.a.if", "getDebugMode", new MethodHook() {
                @Override
                protected void before(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        }
    }


