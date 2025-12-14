/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.hook.module.rules.packageinstaller

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
import com.sevtinge.hyperceiler.hook.R
import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import com.sevtinge.hyperceiler.hook.module.base.dexkit.DexKit
import com.sevtinge.hyperceiler.hook.module.base.tool.OtherTool.getModuleRes
import com.sevtinge.hyperceiler.hook.utils.callMethod
import com.sevtinge.hyperceiler.hook.utils.callMethodOrNull
import com.sevtinge.hyperceiler.hook.utils.devicesdk.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.hook.utils.findClass
import com.sevtinge.hyperceiler.hook.utils.findClassOrNull
import com.sevtinge.hyperceiler.hook.utils.getObjectField
import com.sevtinge.hyperceiler.hook.utils.getObjectFieldAs
import com.sevtinge.hyperceiler.hook.utils.hookAfterMethod
import com.sevtinge.hyperceiler.hook.utils.hookBeforeMethod
import de.robv.android.xposed.XposedHelpers
import org.luckypray.dexkit.query.enums.StringMatchType
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.text.DecimalFormat
import kotlin.math.roundToInt

object DisplayMoreApkInfoNew : BaseHook() {
    private var mApkInfo: Class<*>? = null
    private var mAppInfoViewObject: Class<*>? = null
    private var mAppInfoViewObjectViewHolder: Class<*>? = null

    private val viewExcludeMethod2 by lazy<Method> {
        DexKit.findMember("ViewExcludeMethod2") {
            it.findClass {
                matcher {
                    addUsingString("context.ge…ta.versionName", StringMatchType.Contains)
                }
            }.findMethod {
                matcher {
                    modifiers = Modifier.PRIVATE or Modifier.FINAL
                    returnType = "void"
                    paramCount = 0
                    addUsingNumber(1)
                }
            }.single()
        }
    }

    private val viewExcludeMethod1 by lazy<Method> {
        DexKit.findMember("ViewExcludeMethod1") {
            it.findClass {
                matcher {
                    addUsingString("context.ge…ta.versionName", StringMatchType.Contains)
                }
            }.findMethod {
                matcher {
                    modifiers = Modifier.PRIVATE or Modifier.FINAL
                    returnType = "void"
                    paramCount = 0
                    addUsingString("")
                }
            }.single()
        }
    }

    private val viewMethod by lazy<List<Method>> {
        DexKit.findMemberList("ViewMethod") {
            it.findClass {
                matcher {
                    addUsingString("context.ge…ta.versionName", StringMatchType.Contains)
                }
            }.findMethod {
                matcher {
                    modifiers = Modifier.PRIVATE or Modifier.FINAL
                    returnType = "void"
                    paramCount = 0
                }
            }
        }
    }

    private val entryMethod by lazy<Method> {
        DexKit.findMember("EntryMethod") {
            it.findClass {
                matcher {
                    addUsingString("context.ge…ta.versionName", StringMatchType.Contains)
                }
            }.findMethod {
                matcher {
                    modifiers = Modifier.PUBLIC
                    returnType = "void"
                    paramTypes = listOf($$"com.miui.packageInstaller.ui.listcomponets.AppInfoViewObject$ViewHolder")
                }
            }.single()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun init() {
        val viewHolderField = DexKit.findMember<Field?>("ViewHolder") { bridge ->
            bridge.findClass {
                matcher {
                    addUsingString("context.ge…ta.versionName", StringMatchType.Contains)
                }
            }.findField {
                matcher {
                    type =
                        $$"com.miui.packageInstaller.ui.listcomponets.AppInfoViewObject$ViewHolder"
                    modifiers = Modifier.PRIVATE
                }
            }.singleOrNull()
        } ?: return

        // pick the first view method that is not excluded
        val reallyViewMethod: Method? = viewMethod.firstOrNull { it != viewExcludeMethod1 && it != viewExcludeMethod2 }

        // if we have a specific view method, ensure entry calls it
        reallyViewMethod?.let { method ->
            entryMethod.hookBeforeMethod {
                callMethod(method.name)
            }
        }

        mApkInfo = "com.miui.packageInstaller.model.ApkInfo".findClass()
        mAppInfoViewObject = "com.miui.packageInstaller.ui.listcomponets.AppInfoViewObject".findClassOrNull()
        if (mAppInfoViewObject == null) {
            logE(TAG, "mAppInfoViewObject not found")
            return
        }

        logD(TAG, this.lpparam.packageName, "mAppInfoViewObject is $mAppInfoViewObject")
        mAppInfoViewObjectViewHolder =
            $$"com.miui.packageInstaller.ui.listcomponets.AppInfoViewObject$ViewHolder".findClassOrNull()

        val candidateMethods: Array<Method> =
            XposedHelpers.findMethodsByExactParameters(mAppInfoViewObject, Void.TYPE, mAppInfoViewObjectViewHolder)
        if (candidateMethods.isEmpty()) {
            logE(TAG, "Cannot find methods with expected signature")
            return
        }

        // find apkInfo field name inside AppInfoViewObject
        val apkInfoFieldName = mAppInfoViewObject!!.declaredFields
            .firstOrNull { f -> mApkInfo?.isAssignableFrom(f.type) == true }?.name
            ?: run {
                logE(TAG, "Cannot find ApkInfo field")
                return
            }

        val methodToHook = reallyViewMethod ?: candidateMethods[0]
        logD(TAG, this.lpparam.packageName, "viewMethod is ${methodToHook.declaringClass}.${methodToHook.name}")

        methodToHook.hookAfterMethod { hookParam ->
            val holderInstance: Any = if (reallyViewMethod != null) {
                hookParam.thisObject.getObjectField(viewHolderField!!.name) ?: return@hookAfterMethod
            } else {
                hookParam.args[0] ?: return@hookAfterMethod
            }

            val mAppSizeTv = (holderInstance.callMethod("getTvAppVersion") as? TextView)
                ?: (holderInstance.callMethod("getAppSize") as? TextView)
                ?: return@hookAfterMethod

            val mContext = mAppSizeTv.context
            val isDarkMode =
                mContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

            val apkInfo: Any = hookParam.thisObject.getObjectFieldAs(apkInfoFieldName) as Any
            val mAppInfo = apkInfo.callMethodOrNull("getInstalledPackageInfo") as ApplicationInfo?
            val mPkgInfo = apkInfo.callMethod("getPackageInfo") as PackageInfo

            val modRes = getModuleRes(mContext) as Resources

            val layout = mAppSizeTv.parent as? LinearLayout ?: return@hookAfterMethod
            layout.removeAllViews()
            val mContainerView = layout.parent as? ViewGroup ?: return@hookAfterMethod

            val mRoundImageView = mContainerView.getChildAt(0) as? ImageView
            val mAppNameView = (holderInstance.callMethod("getTvAppName") as? TextView)
                ?: (mContainerView.getChildAt(1) as? TextView)
            if (mAppNameView == null) return@hookAfterMethod

            mContainerView.removeAllViews()

            val root = LinearLayout(mContext).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            }

            mAppNameView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            ).also { it.setMargins(0, dp2px(10f), 0, 0) }
            mAppNameView.gravity = Gravity.CENTER

            val infoCard = LinearLayout(mContext).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp2px(18f), dp2px(15f), dp2px(18f), dp2px(15f))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ).also { it.setMargins(0, dp2px(13f), 0, 0) }
                background = modRes.getDrawable(
                    if (isDarkMode) R.drawable.ic_packageinstaller_background_dark else R.drawable.ic_packageinstaller_background_light,
                    mContext.theme
                )
            }

            val mAppPackageNameView = TextView(mContext).apply { gravity = Gravity.CENTER }
            val mAppVersionNameView = TextView(mContext)
            val mAppVersionCodeView = TextView(mContext)
            val mAppSdkView = TextView(mContext)
            val mAppSizeView = TextView(mContext)

            fun applyAppearance(tv: TextView) {
                setTextAppearance(tv, mAppSizeTv)
            }
            listOf(mAppVersionNameView, mAppVersionCodeView, mAppSdkView, mAppSizeView).forEach { applyAppearance(it) }

            mAppVersionNameView.gravity = Gravity.START
            mAppVersionCodeView.gravity = Gravity.START
            mAppSdkView.gravity = Gravity.START
            mAppSizeView.gravity = Gravity.START

            val mPackageName: String? = mPkgInfo.applicationInfo?.packageName
            val mNewAppSize = format((apkInfo.callMethod("getFileSize") as Long).toFloat().roundToInt() / 1000000f)

            val (mAppVersionName, mAppVersionCode, mAppSdk, mOldAppSize) = if (mAppInfo != null) {
                val name = (apkInfo.callMethod("getInstalledVersionName") as String) + " ➟ " + mPkgInfo.versionName
                val code = apkInfo.callMethod("getInstalledVersionCode").toString() + " ➟ " + mPkgInfo.longVersionCode
                val sdk = mAppInfo.minSdkVersion.toString() + "-" + mAppInfo.targetSdkVersion + " ➟ " +
                    (mPkgInfo.applicationInfo?.minSdkVersion ?: "") + "-" + (mPkgInfo.applicationInfo?.targetSdkVersion ?: "")
                val oldSizeFileSize = File(mAppInfo.sourceDir).length().toInt()
                val oldSize = format(oldSizeFileSize.toFloat().roundToInt() / 1000000f) + " ➟ "
                Quad(name, code, sdk, oldSize)
            } else {
                val name = mPkgInfo.versionName.toString()
                val code = mPkgInfo.longVersionCode.toString()
                val sdk = (mPkgInfo.applicationInfo?.minSdkVersion.toString() + "-" + mPkgInfo.applicationInfo?.targetSdkVersion)
                Quad(name, code, sdk, "")
            }

            mAppVersionNameView.text = modRes.getString(R.string.various_install_app_info_version_name) + ": " + mAppVersionName
            mAppVersionCodeView.text = modRes.getString(R.string.various_install_app_info_version_code) + ": " + mAppVersionCode
            mAppSdkView.text = modRes.getString(R.string.various_install_app_info_sdk) + ": " + mAppSdk
            mAppSizeView.text = modRes.getString(R.string.various_install_app_size) + ": " + mOldAppSize + mNewAppSize
            mAppPackageNameView.text = mPackageName

            infoCard.addView(mAppVersionNameView, 0)
            infoCard.addView(mAppVersionCodeView, 1)
            infoCard.addView(mAppSdkView, 2)
            infoCard.addView(mAppSizeView, 3)

            mRoundImageView?.let { root.addView(it, 0) }
            root.addView(mAppNameView, 1)
            root.addView(mAppPackageNameView, 2)
            root.addView(infoCard, 3)

            mContainerView.addView(root)
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

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
