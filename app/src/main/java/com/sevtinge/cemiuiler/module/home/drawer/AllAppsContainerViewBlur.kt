package com.sevtinge.cemiuiler.module.home.drawer

import android.os.Build
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.ViewSwitcher
import androidx.annotation.RequiresApi
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookAfter
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.woobox.findClass
import com.sevtinge.cemiuiler.utils.woobox.getObjectField
//import com.yuk.miuiHomeR.mPrefsMap
//import com.yuk.miuiHomeR.utils.ktx.findClass
import com.sevtinge.cemiuiler.utils.woobox.getCornerRadiusTop
//import com.yuk.miuiHomeR.utils.ktx.getObjectField
import com.zhenxiang.blur.BlurFrameLayout
import com.zhenxiang.blur.model.CornersRadius

@RequiresApi(Build.VERSION_CODES.S)
object AllAppsContainerViewBlur : BaseHook() {
    override fun init() {

        //if (!mPrefsMap.getBoolean("home_all_apps_blur")) return
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