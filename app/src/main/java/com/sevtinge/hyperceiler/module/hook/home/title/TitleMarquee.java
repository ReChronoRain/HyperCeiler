package com.sevtinge.hyperceiler.module.hook.home.title;

import android.text.TextUtils;
import android.widget.TextView;

import com.sevtinge.hyperceiler.module.base.BaseHook;

import de.robv.android.xposed.XposedHelpers;

public class TitleMarquee extends BaseHook {
    TextView mTitle;

    @Override
    public void init() {
        Class<?> mItemIcon = findClassIfExists("com.miui.home.launcher.ItemIcon");

        findAndHookMethod(mItemIcon, "onFinishInflate", new MethodHook() {
            @Override
            protected void after(MethodHookParam param) throws Throwable {
                try {
                    mTitle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mTitleView");
                } catch (Throwable t) {
                    mTitle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mTitle");
                }

                mTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                mTitle.setHorizontalFadingEdgeEnabled(true);
                mTitle.setSingleLine();
                mTitle.setMarqueeRepeatLimit(-1);
                mTitle.setSelected(true);
                mTitle.setHorizontallyScrolling(true);
            }
        });
    }
}
