package com.sevtinge.cemiuiler.module.home.folder;

import android.view.ViewGroup;
import android.widget.GridView;

import com.sevtinge.cemiuiler.module.base.BaseHook;
import com.sevtinge.cemiuiler.utils.DisplayUtils;
import com.sevtinge.cemiuiler.utils.Helpers;

import de.robv.android.xposed.XposedHelpers;

public class FolderColumns extends BaseHook {

    @Override
    public void init() {
        findAndHookMethod("com.miui.home.launcher.Folder", "onFinishInflate", new Helpers.MethodHook() {
            @Override
            protected void after(final MethodHookParam param) {

                GridView mContent = (GridView) XposedHelpers.getObjectField(param.thisObject, "mContent");
                int columns = mPrefsMap.getInt("home_folder_columns", 3);
                int mVerticalPadding = mPrefsMap.getInt("home_folder_vertical_padding", 0);
                mContent.setNumColumns(columns);
                if (mVerticalPadding > 0) mContent.setVerticalSpacing(DisplayUtils.dip2px(mContent.getContext(), mVerticalPadding));
                if (columns > 3 && mPrefsMap.getBoolean("home_folder_space")) {
                    ViewGroup mBackgroundView = (ViewGroup)XposedHelpers.getObjectField(param.thisObject, "mBackgroundView");
                    if (mBackgroundView != null) {
                        mBackgroundView.setPadding(
                                mBackgroundView.getPaddingLeft() / 3,
                                mBackgroundView.getPaddingTop(),
                                mBackgroundView.getPaddingRight() / 3,
                                mBackgroundView.getPaddingBottom()
                        );
                    }
                }
            }
        });

        hookAllMethods("com.miui.home.launcher.Folder", "bind", new Helpers.MethodHook() {
            @Override
            protected void after(final MethodHookParam param) {
                if (!mPrefsMap.getBoolean("home_folder_width")) return;
                GridView mContent = (GridView)XposedHelpers.getObjectField(param.thisObject, "mContent");
                ViewGroup.LayoutParams layoutParams = mContent.getLayoutParams();
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                mContent.setPadding(0,0,0,0);
                mContent.setLayoutParams(layoutParams);
            }
        });
    }
}
