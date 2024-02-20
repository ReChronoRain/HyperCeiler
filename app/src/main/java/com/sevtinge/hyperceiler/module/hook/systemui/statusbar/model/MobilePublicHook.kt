package com.sevtinge.hyperceiler.module.hook.systemui.statusbar.model

import android.view.View
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.setObjectField
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedHelpers

object MobilePublicHook : BaseHook() {
    private val statusBarMobileClass by lazy {
        loadClass("com.android.systemui.statusbar.StatusBarMobileView")
    }

    private val qpt by lazy {
        mPrefsMap.getStringAsInt("system_ui_status_bar_icon_show_mobile_network_type", 0)
    }
    private val hideIndicator by lazy {
        mPrefsMap.getBoolean("system_ui_status_bar_mobile_indicator")
    }
    private val singleMobileType by lazy {
        mPrefsMap.getBoolean("system_ui_statusbar_mobile_type_enable")
    }

    override fun init() {
        updateState()
        applyMobileState()
    }

    private fun updateState() {
        hookAllMethods(statusBarMobileClass, "updateState", object : MethodHook() {
            override fun after(param: MethodHookParam) {
                if ((qpt != 0) || hideIndicator) {
                    hideMobileType(param) // 隐藏网络类型图标及移动网络指示器
                }
            }
        })
    }

    private fun applyMobileState() {
        hookAllMethods(statusBarMobileClass, "applyMobileState", object : MethodHook() {
            override fun before(param: MethodHookParam) {
                if (singleMobileType) {
                    showMobileTypeSingle(param) // 使网络类型单独显示
                }
            }

            override fun after(param: MethodHookParam) {
                if ((qpt != 0) || hideIndicator) {
                    hideMobileType(param) // 隐藏网络类型图标及移动网络指示器
                }
            }
        })
    }

    private fun showMobileTypeSingle(param: MethodHookParam) {
        // 使网络类型单独显示
        try {
            val mobileIconState = param.args[0]
            mobileIconState.setObjectField("showMobileDataTypeSingle", true)
            mobileIconState.setObjectField("fiveGDrawableId", 0)
        } catch (t: Throwable) {
            logE(TAG, "showMobileTypeSingle setObjectField is null, $t")
        }
    }

    private fun hideMobileType(param: MethodHookParam) {
        // 隐藏网络类型图标
        val mMobileType: View = getMobileType(param)
        // 隐藏移动网络活动指示器
        val mLeftInOut = XposedHelpers.getObjectField(param.thisObject, "mLeftInOut") as View
        if (hideIndicator) {
            val mRightInOut = XposedHelpers.getObjectField(param.thisObject, "mRightInOut") as View
            mLeftInOut.visibility = View.GONE
            mRightInOut.visibility = View.GONE
        }
        if (mMobileType.visibility == View.GONE && mLeftInOut.visibility == View.GONE) {
            val mMobileLeftContainer =
                XposedHelpers.getObjectField(param.thisObject, "mMobileLeftContainer") as View
            mMobileLeftContainer.visibility = View.GONE
        }
    }

    private fun getMobileType(param: MethodHookParam): View {
        val mMobileType = XposedHelpers.getObjectField(param.thisObject, "mMobileType") as View
        val dataConnected = XposedHelpers.getObjectField(param.args[0], "dataConnected") as Boolean
        val wifiAvailable = XposedHelpers.getObjectField(param.args[0], "wifiAvailable") as Boolean

        if (qpt != 0) {
            val mMobileTypeSingle =
                if (singleMobileType)
                    XposedHelpers.getObjectField(param.thisObject, "mMobileTypeSingle") as TextView
                else null
            val visibility =
                if (qpt == 1 || (qpt == 2 && !wifiAvailable) || (qpt == 4 && dataConnected && !wifiAvailable)) View.VISIBLE else View.GONE

            if (singleMobileType && mMobileTypeSingle != null) {
                mMobileTypeSingle.visibility = visibility
            } else {
                mMobileType.visibility = visibility
            }
        }
        return mMobileType
    }
}
