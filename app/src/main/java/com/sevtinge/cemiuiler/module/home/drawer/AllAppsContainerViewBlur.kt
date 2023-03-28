package com.sevtinge.cemiuiler.module.home.drawer

import android.app.Application
import android.content.Context
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.ViewSwitcher
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.findClass
import com.sevtinge.cemiuiler.utils.getObjectField
import com.sevtinge.cemiuiler.utils.getCornerRadiusTop
import com.sevtinge.cemiuiler.utils.hookBeforeMethod
import com.zhenxiang.blur.BlurFrameLayout
import com.zhenxiang.blur.model.CornersRadius

@RequiresApi(Build.VERSION_CODES.S)
object AllAppsContainerViewBlur : BaseHook() {
    override fun init() {
        Application::class.java.hookBeforeMethod("attach", Context::class.java) {
            EzXHelperInit.initHandleLoadPackage(lpparam)
            EzXHelperInit.setLogTag(TAG)
            EzXHelperInit.setToastTag(TAG)
            EzXHelperInit.initAppContext(it.args[0] as Context)

            findMethod("com.miui.home.launcher.allapps.BaseAllAppsContainerView".findClass(), true) {
                name == "onFinishInflate"
            }.hookAfter { hookParam ->
                val mCategoryContainer = hookParam.thisObject.getObjectField("mCategoryContainer") as ViewSwitcher
                val appsView = mCategoryContainer.parent as RelativeLayout
                val blur = BlurFrameLayout(mCategoryContainer.context)
                val radius = getCornerRadiusTop().toFloat()
                blur.blurController.apply {
                    cornerRadius = CornersRadius.custom(radius, radius, 0f, 0f)
                }
                val view = View(mCategoryContainer.context)
                blur.addView(view)
                (view.layoutParams as FrameLayout.LayoutParams).apply {
                    width = FrameLayout.LayoutParams.MATCH_PARENT
                    height = FrameLayout.LayoutParams.MATCH_PARENT
                }
                appsView.addView(blur, 0)
                findMethod("com.miui.home.launcher.allapps.BaseAllAppsContainerView".findClass(), true) {
                    name == "onResume"
                }.hookAfter {
                    blur.refreshDrawableState()
                }
            }
        }

    }
}