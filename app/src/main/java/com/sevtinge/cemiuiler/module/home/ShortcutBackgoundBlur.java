package com.sevtinge.cemiuiler.module.home;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.BlurUtils;

import de.robv.android.xposed.XposedHelpers;

public class ShortcutBackgoundBlur extends BaseHook {

    Class<?> mAppListCls;
    Class<?> mBlurCls;

    @Override
    public void init() {
        mAppListCls = findClassIfExists("com.miui.home.launcher.Launcher");
        mBlurCls = findClassIfExists("com.miui.home.launcher.common.BlurUtils");

        findAndHookMethod(mAppListCls, "onCreate", Bundle.class, new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {

                Activity activity = (Activity) param.thisObject;

                findAndHookMethod(mAppListCls,"isInShortcutMenuState", new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) throws Throwable {
                        boolean isInShortcutMenuState = (boolean) param.getResult();
                        FrameLayout mSearchBarContainer = (FrameLayout) XposedHelpers.callMethod(param.thisObject, "getSearchBarContainer");
                        FrameLayout mSearchEdgeLayout = (FrameLayout) mSearchBarContainer.getParent();
                        FrameLayout view = (FrameLayout) mSearchEdgeLayout.getChildAt(3);

                        FrameLayout mBlurView = new FrameLayout(mSearchBarContainer.getContext());
                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        mBlurView.setLayoutParams(layoutParams);

                        FrameLayout frameLayout = new FrameLayout(mSearchBarContainer.getContext());
                        frameLayout.setLayoutParams(layoutParams);
                        mBlurView.addView(frameLayout,0);

                        view.addView(mBlurView,0);

                        if (isInShortcutMenuState) {
                            new BlurUtils(mBlurView,"various_new_box_blur");
                        } else {
                            view.setBackgroundColor(Color.TRANSPARENT);
                        }
                    }
                });
            }
        });
    }
}
