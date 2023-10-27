package com.sevtinge.hyperceiler.module.hook.packageinstaller

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.github.kyuubiran.ezxhelper.Log
import com.sevtinge.hyperceiler.R
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DisplayUtils.dip2px
import com.sevtinge.hyperceiler.utils.Helpers
import com.sevtinge.hyperceiler.utils.callMethod
import com.sevtinge.hyperceiler.utils.callMethodOrNull
import com.sevtinge.hyperceiler.utils.hookAfterMethod
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils
import de.robv.android.xposed.XposedHelpers
import java.io.File
import java.lang.reflect.Method
import java.text.DecimalFormat
import kotlin.math.roundToInt

object DisplayMoreApkInfoNew : BaseHook() {
    private var mApkInfo: Class<*>? = null
    private var mAppInfoViewObject: Class<*>? = null
    private var mAppInfoViewObjectViewHolder: Class<*>? = null

    @SuppressLint("SetTextI18n")
    override fun init() {
        // if (!getBoolean("packageinstaller_show_more_apk_info", false)) return
        mApkInfo = findClassIfExists("com.miui.packageInstaller.model.ApkInfo")//.findClassOrNull()
        mAppInfoViewObject =
            findClassIfExists("com.miui.packageInstaller.ui.listcomponets.AppInfoViewObject")//.findClassOrNull()
        if (mAppInfoViewObject != null) {
            XposedLogUtils.logI("mAppInfoViewObject is $mAppInfoViewObject")
            mAppInfoViewObjectViewHolder =
                findClassIfExists("com.miui.packageInstaller.ui.listcomponets.AppInfoViewObject\$ViewHolder")//"com.miui.packageInstaller.ui.listcomponets.AppInfoViewObject\$ViewHolder".findClassOrNull()
            val methods: Array<Method> =
                XposedHelpers.findMethodsByExactParameters(mAppInfoViewObject, Void.TYPE, mAppInfoViewObjectViewHolder)
            if (methods.isNotEmpty()) {
                val fields = mAppInfoViewObject!!.declaredFields
                var apkInfoFieldName: String? = null
                for (field in fields) {
                    if (mApkInfo!!.isAssignableFrom(field.type)) {
                        apkInfoFieldName = field.name
                        break
                    }
                }
                if (apkInfoFieldName == null) return
                val finalApkInfoFieldName: String = apkInfoFieldName
                methods[0].hookAfterMethod { hookParam ->
                    val viewHolder: Any = hookParam.args[0] ?: return@hookAfterMethod
                    val mAppSizeTv =
                        XposedHelpers.callMethod(viewHolder, "getAppSize") as TextView? ?: return@hookAfterMethod
                    val mContext = mAppSizeTv.context
                    val isDarkMode =
                        mAppSizeTv.context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                    val apkInfo: Any = XposedHelpers.getObjectField(hookParam.thisObject, finalApkInfoFieldName)
                    val mAppInfo = apkInfo.callMethodOrNull("getInstalledPackageInfo") as ApplicationInfo?
                    val mPkgInfo = apkInfo.callMethod("getPackageInfo") as PackageInfo
                    val modRes = Helpers.getModuleRes(mAppSizeTv.context) as Resources
                    val layout: LinearLayout = mAppSizeTv.parent as LinearLayout
                    layout.removeAllViews()
                    val mContainerView = layout.parent as ViewGroup
                    val mRoundImageView = mContainerView.getChildAt(0) as ImageView
                    val mAppNameView = mContainerView.getChildAt(1) as TextView
                    mContainerView.removeAllViews()
                    val linearLayout = LinearLayout(mContext)
                    linearLayout.orientation = LinearLayout.VERTICAL
                    linearLayout.gravity = Gravity.CENTER
                    linearLayout.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    val appNameViewParams: LinearLayout.LayoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                    appNameViewParams.setMargins(0, dip2px(mContext, 10f), 0, 0)
                    mAppNameView.layoutParams = appNameViewParams
                    mAppNameView.gravity = Gravity.CENTER
                    val linearLayout2 = LinearLayout(mContainerView.context)
                    // val linearLayout2 = LinearLayout(mContext)
                    linearLayout2.orientation = LinearLayout.VERTICAL
                    linearLayout2.gravity = Gravity.CENTER
                    linearLayout2.setPadding(
                        dip2px(mContext, 18f),
                        dip2px(mContext, 15f),
                        dip2px(mContext, 18f),
                        dip2px(mContext, 15f)
                    )
                    linearLayout2.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    ).also {
                        it.setMargins(0, dip2px(mContext, 13f), 0, 0)
                    }
                    linearLayout2.background =
                        modRes.getDrawable(
                            if (isDarkMode) R.drawable.ic_packageinstaller_background_dark else R.drawable.ic_packageinstaller_background_light,
                            modRes.newTheme()
                        )
                    val mAppPackageNameView = TextView(mContext)
                    mContainerView.removeAllViews()
                    val mAppVersionNameView = TextView(mContainerView.context)
                    val mAppVersionCodeView = TextView(mContainerView.context)
                    val mAppSdkView = TextView(mContainerView.context)
                    val mAppSizeView = TextView(mContainerView.context)
                    setTextAppearance(mAppVersionNameView, mAppSizeTv)
                    setTextAppearance(mAppVersionCodeView, mAppSizeTv)
                    setTextAppearance(mAppSdkView, mAppSizeTv)
                    setTextAppearance(mAppSizeView, mAppSizeTv)
                    mAppPackageNameView.gravity = Gravity.CENTER
                    mAppVersionNameView.gravity = Gravity.START
                    mAppVersionCodeView.gravity = Gravity.START
                    mAppSdkView.gravity = Gravity.START
                    mAppSizeView.gravity = Gravity.START
                    val mPackageName: String = mPkgInfo.applicationInfo.packageName
                    val mAppVersionName: String
                    val mAppVersionCode: String
                    val mAppSdk: String
                    var mOldAppSize = ""
                    val newAppSize = apkInfo.callMethod("getFileSize") as Long
                    val newAppSizeDistance = newAppSize.toFloat().roundToInt() / 1000000f
                    val mNewAppSize = format(newAppSizeDistance)
                    if (mAppInfo != null) {
                        mAppVersionName =
                            apkInfo.callMethod("getInstalledVersionName") as String + " ➟ " + mPkgInfo.versionName
                        mAppVersionCode =
                            apkInfo.callMethod("getInstalledVersionCode").toString() + " ➟ " + mPkgInfo.longVersionCode
                        mAppSdk =
                            mAppInfo.minSdkVersion.toString() + "-" + mAppInfo.targetSdkVersion + " ➟ " + mPkgInfo.applicationInfo.minSdkVersion + "-" + mPkgInfo.applicationInfo.targetSdkVersion
                        val oldAppSize = Integer.valueOf(File(mAppInfo.sourceDir).length().toInt())
                        val oldAppSizeDistance = oldAppSize.toFloat().roundToInt() / 1000000f
                        mOldAppSize = format(oldAppSizeDistance) + " ➟ "
                    } else {
                        mAppVersionName = mPkgInfo.versionName
                        mAppVersionCode = mPkgInfo.longVersionCode.toString()
                        mAppSdk =
                            mPkgInfo.applicationInfo.minSdkVersion.toString() + "-" + mPkgInfo.applicationInfo.targetSdkVersion
                    }
                    mAppVersionNameView.text =
                        modRes.getString(R.string.various_install_app_info_version_name) + ": " + mAppVersionName
                    mAppVersionCodeView.text =
                        modRes.getString(R.string.various_install_app_info_version_code) + ": " + mAppVersionCode
                    mAppSdkView.text = modRes.getString(R.string.various_install_app_info_sdk) + ": " + mAppSdk
                    mAppSizeView.text =
                        modRes.getString(R.string.various_install_app_size) + ": " + mOldAppSize + mNewAppSize
                    mAppPackageNameView.text = mPackageName
                    linearLayout2.addView(mAppVersionNameView, 0)
                    linearLayout2.addView(mAppVersionCodeView, 1)
                    linearLayout2.addView(mAppSdkView, 2)
                    linearLayout2.addView(mAppSizeView, 3)
                    linearLayout.addView(mRoundImageView, 0)
                    linearLayout.addView(mAppNameView, 1)
                    linearLayout.addView(mAppPackageNameView, 2)
                    linearLayout.addView(linearLayout2, 3)
                    mContainerView.addView(linearLayout)
                }
            }
        } else {
            Log.ex("Cannot find appropriate method")
        }
    }
}

private fun setTextAppearance(textView: TextView, textView2: TextView) {
    textView.textSize = 17f
    textView.setTextColor(textView2.textColors)
    textView.ellipsize = TextUtils.TruncateAt.MARQUEE
    textView.isHorizontalFadingEdgeEnabled = true
    textView.setSingleLine()
    textView.marqueeRepeatLimit = -1
    textView.isSelected = true
    textView.setHorizontallyScrolling(true)
}

private fun format(appSize: Float): String {
    val decimalFormat = DecimalFormat("0.00")
    return if (appSize >= 1024f) {
        decimalFormat.format(appSize / 1024f) + "GB"
    } else if (appSize >= 1.024f) {
        decimalFormat.format(appSize) + "MB"
    } else {
        decimalFormat.format(appSize * 1024f) + "KB"
    }
}
