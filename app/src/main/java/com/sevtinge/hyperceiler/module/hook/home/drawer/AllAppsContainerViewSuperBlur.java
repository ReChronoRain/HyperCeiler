package com.sevtinge.hyperceiler.module.hook.home.drawer;

import static com.sevtinge.hyperceiler.utils.devicesdk.AppUtilsKt.dp2px;

import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtils;
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtilsKt;

import de.robv.android.xposed.XposedHelpers;

public class AllAppsContainerViewSuperBlur extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        findAndHookMethod("com.miui.home.launcher.allapps.BaseAllAppsContainerView",
                "onFinishInflate",
                new MethodHook() {
                    @Override
                    protected void after(MethodHookParam param) {
                        ViewSwitcher mCategoryContainer = (ViewSwitcher) XposedHelpers.getObjectField(param.thisObject, "mCategoryContainer");
                        RelativeLayout appsView = (RelativeLayout) mCategoryContainer.getParent();
                        FrameLayout frameLayout = new FrameLayout(mCategoryContainer.getContext());
                        View view = new View(mCategoryContainer.getContext());
                        frameLayout.addView(view);
                        view.getLayoutParams().height = FrameLayout.LayoutParams.MATCH_PARENT;
                        view.getLayoutParams().width = FrameLayout.LayoutParams.MATCH_PARENT;
                        view.setBackgroundColor(mPrefsMap.getInt("home_drawer_blur_super_bg_color", -1));
                        // MiBlurUtils.setContainerPassBlur(view, 200);
                        MiBlurUtils.setMiViewBlurMode(view, 2);
                        // MiBlurUtils.clearMiBackgroundBlendColor(view);
                        MiBlurUtils.addMiBackgroundBlendColor(view, Color.argb(255, 0, 0, 0), 103);
                        MiBlurUtilsKt.INSTANCE.setBlurRoundRect(frameLayout, dp2px(
                                mPrefsMap.getInt("home_drawer_blur_super_radius", 30)));
                        appsView.addView(frameLayout, 0);
                    }
                }
        );
    }
}
