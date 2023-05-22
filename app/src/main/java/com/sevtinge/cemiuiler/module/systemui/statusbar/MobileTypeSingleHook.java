package com.sevtinge.cemiuiler.module.systemui.statusbar;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.sevtinge.cemiuiler.module.base.BaseHook;
import de.robv.android.xposed.XposedHelpers;

public class MobileTypeSingleHook extends BaseHook {
    @Override
    public void init() {
        MethodHook showSingleMobileType = new MethodHook(MethodHook.PRIORITY_HIGHEST) {
            @Override
            protected void before(final MethodHookParam param) throws Throwable {
                Object mobileIconState = param.args[0];
                XposedHelpers.setObjectField(mobileIconState, "showMobileDataTypeSingle", true);
                XposedHelpers.setObjectField(mobileIconState, "fiveGDrawableId", 0);
            }
        };
        hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", "applyMobileState", showSingleMobileType);

        MethodHook afterUpdate = new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                Object mMobileLeftContainer = XposedHelpers.getObjectField(param.thisObject, "mMobileLeftContainer");
                XposedHelpers.callMethod(mMobileLeftContainer, "setVisibility", 8);
            }
        };
        hookAllMethods("com.android.systemui.statusbar.StatusBarMobileView", "applyMobileState", afterUpdate);

        findAndHookMethod("com.android.systemui.statusbar.StatusBarMobileView", "init", new MethodHook() {
            @Override
            protected void after(final MethodHookParam param) throws Throwable {
                Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                Resources res = mContext.getResources();
                LinearLayout mMobileGroup = (LinearLayout) XposedHelpers.getObjectField(param.thisObject, "mMobileGroup");
                TextView mMobileTypeSingle = (TextView) XposedHelpers.getObjectField(param.thisObject, "mMobileTypeSingle");
                if (!mPrefsMap.getBoolean("system_ui_status_bar_mobile_network_at_left")) {
                    mMobileGroup.removeView(mMobileTypeSingle);
                    mMobileGroup.addView(mMobileTypeSingle);
                }
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) mMobileTypeSingle.getLayoutParams();
                int leftMargin = mPrefsMap.getInt("system_ui_statusbar_indicator_left_margin", 7);
                float marginLeft = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        leftMargin * 0.5f,
                        res.getDisplayMetrics()
                );
                mlp.leftMargin = (int) marginLeft;
                int rightMargin = mPrefsMap.getInt("system_ui_statusbar_indicator_right_margin", 0);
                if (rightMargin > 0) {
                    float marginRight = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            rightMargin * 0.5f,
                            res.getDisplayMetrics()
                    );
                    mlp.rightMargin = (int) marginRight;
                }
                int verticalOffset = mPrefsMap.getInt("system_ui_statusbar_mobile_type_vertical_offset", 8);
                if (verticalOffset != 8) {
                    float marginTop = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            (verticalOffset - 8) * 0.5f,
                            res.getDisplayMetrics()
                    );
                    mlp.topMargin = (int) marginTop;
                }
                mMobileTypeSingle.setLayoutParams(mlp);
                int fontSize = mPrefsMap.getInt("system_ui_statusbar_mobile_type_font_size", 27);
                try {
                    mMobileTypeSingle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.5f);
                } catch (Exception ignored) {
                }
                if (mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_bold")) {
                    mMobileTypeSingle.setTypeface(Typeface.DEFAULT_BOLD);
                }
            }
        });
    }
}
