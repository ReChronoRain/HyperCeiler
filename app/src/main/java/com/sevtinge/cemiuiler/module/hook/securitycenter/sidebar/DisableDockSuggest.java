package com.sevtinge.cemiuiler.module.hook.securitycenter.sidebar;

import com.sevtinge.cemiuiler.module.base.BaseHook;

import java.util.ArrayList;

public class DisableDockSuggest extends BaseHook {
    @Override
    public void init() {
        MethodHook clearHook = new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                ArrayList<String> blackList = new ArrayList<String>();
                blackList.add("xx.yy.zz");
                int topMethod = 10;
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                for (StackTraceElement el: stackTrace) {
                    if (el != null && topMethod < 20
                        && (el.getClassName().contains("edit.DockAppEditActivity") || el.getClassName().contains("BubblesSettings"))
                    ) {
                        return;
                    }
                    topMethod++;
                }
                param.setResult(blackList);
            }
        };
        hookAllMethodsSilently("android.util.MiuiMultiWindowUtils", "getFreeformSuggestionList", clearHook);
    }
}
