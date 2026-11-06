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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.libhook.rules.packageinstaller

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
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.R
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.api.DisplayUtils.dp2px
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool.getModuleRes
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHook
import org.luckypray.dexkit.query.enums.StringMatchType
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.text.DecimalFormat
import kotlin.math.roundToInt

object DisplayMoreApkInfoNew : BaseHook() {
    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        viewExcludeMethod2
        viewExcludeMethod1
        viewMethod
        entryMethod
        viewHolderField
        return true
    }
    private var mApkInfo: Class<*>? = null
    private var mAppInfoViewObject: Class<*>? = null
    private var mAppInfoViewObjectViewHolder: Class<*>? = null

    // 以下四个查询共用同一个类锚：包含字面量 "context.ge…ta.versionName" 的类。
    // 该锚点是混淆产物：MIUIPackageInstaller 在 OS3 里确实带这个串（含 U+2026 省略号），
    // OS4.0.5 里已经不存在（dex 字节直查 HIT 1 → HIT 0），过去全部挂 required + .single()，
    // 一个锚点失效就让整条规则被跳过（OS4.0.4.0 设备日志：NoResultException）。
    // 改成 optional + singleOrNull 只是第一步：init() 里的反射回退还需要去掉
    // “没拿到 viewHolderField 就直接 return”这道闸（见 init() 处说明），否则回退仍不可达。
    private val viewExcludeMethod2 by lazy<Method?> {
        optionalMember("ViewExcludeMethod2") {
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
            }.singleOrNull()
        }
    }

    private val viewExcludeMethod1 by lazy<Method?> {
        optionalMember("ViewExcludeMethod1") {
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
            }.singleOrNull()
        }
    }

    private val viewMethod by lazy<List<Method>> {
        optionalMemberList("ViewMethod") {
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

    private val entryMethod by lazy<Method?> {
        optionalMember("EntryMethod") {
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
            }.singleOrNull()
        }
    }

    private val viewHolderField by lazy<Field?> {
        optionalMember("ViewHolder") { bridge ->
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
        }
    }

    @SuppressLint("SetTextI18n")
    override fun init() {
        // pick the first view method that is not excluded
        val reallyViewMethod: Method? = viewMethod.firstOrNull { it != viewExcludeMethod1 && it != viewExcludeMethod2 }

        // 旧写法这里先 `if (viewHolderField == null) return`。但 viewHolderField 只能来自那个
        // 已失效的串锚（它在两版里都声明在 AppInfoViewObject 的**子类**里，基类本身没有
        // ViewHolder 类型字段：OS3 是 listcomponets/d#q、listcomponets/r#p，OS4.0.5 是
        // listcomponets/B#p、listcomponets/g#q），所以 OS4.0.5 上这道 return 会把下面的
        // 反射回退（candidateMethods[0] + args[0]）一起堵死，整条规则仍然完全不生效。
        // 该字段只服务于 reallyViewMethod 分支，回退分支不需要它，因此这里不再提前返回。

        // if we have a specific view method, ensure entry calls it
        reallyViewMethod?.let { method ->
            entryMethod?.createBeforeHook {
                callMethod(method.name)
            }
        }

        mApkInfo = findClass("com.miui.packageInstaller.model.ApkInfo")
        mAppInfoViewObject = findClassIfExists("com.miui.packageInstaller.ui.listcomponets.AppInfoViewObject")
        if (mAppInfoViewObject == null) {
            XposedLog.e(TAG, "mAppInfoViewObject not found")
            return
        }

        XposedLog.d(TAG, this.lpparam.packageName, "mAppInfoViewObject is $mAppInfoViewObject")
        mAppInfoViewObjectViewHolder =
            findClassIfExists($$"com.miui.packageInstaller.ui.listcomponets.AppInfoViewObject$ViewHolder")

        val candidateMethods: Array<Method> =
            findMethodsByExactParameters(mAppInfoViewObject!!, Void.TYPE, mAppInfoViewObjectViewHolder)
        if (candidateMethods.isEmpty()) {
            XposedLog.e(TAG, "Cannot find methods with expected signature")
            return
        }

        // find apkInfo field name inside AppInfoViewObject
        val apkInfoFieldName = mAppInfoViewObject!!.declaredFields
            .firstOrNull { f -> mApkInfo?.isAssignableFrom(f.type) == true }?.name
            ?: run {
                XposedLog.e(TAG, "Cannot find ApkInfo field")
                return
            }

        val methodToHook = reallyViewMethod ?: candidateMethods[0]
        // by lazy 委托属性不能智能转换，先取局部变量
        val holderField = viewHolderField
        XposedLog.d(
            TAG,
            this.lpparam.packageName,
            "viewMethod is ${methodToHook.declaringClass}.${methodToHook.name}"
        )

        methodToHook.createAfterHook { hookParam ->
            val holderInstance: Any? = if (reallyViewMethod != null && holderField != null) {
                hookParam.thisObject.getObjectField(holderField.name)
            } else {
                // 挂的是基类 AppInfoViewObject 的 (ViewHolder)V 方法（OS3 叫 E、OS4.0.5 叫 D，
                // 两版都是除构造器外唯一该方法），holder 直接取第一个参数；args 可能为空
                // （挂在无参的 view 方法上时），用 getOrNull 避免越界。
                hookParam.args.getOrNull(0)
            }
            if (holderInstance == null) return@createAfterHook

            val mAppSizeTv = (holderInstance.callMethod("getTvAppVersion") as? TextView)
                ?: (holderInstance.callMethod("getAppSize") as? TextView)
                ?: return@createAfterHook

            val mContext = mAppSizeTv.context
            val isDarkMode =
                mContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

            val apkInfo: Any = hookParam.thisObject.getObjectFieldAs(apkInfoFieldName) as Any
            val mAppInfo = runCatching {
                apkInfo.callMethod("getInstalledPackageInfo") as? ApplicationInfo
            }.getOrNull()
            val mPkgInfo = apkInfo.callMethod("getPackageInfo") as PackageInfo

            val modRes = getModuleRes(mContext) as Resources

            val layout = mAppSizeTv.parent as? LinearLayout ?: return@createAfterHook
            layout.removeAllViews()
            val mContainerView = layout.parent as? ViewGroup ?: return@createAfterHook

            val mRoundImageView = mContainerView.getChildAt(0) as? ImageView
            val mAppNameView = (holderInstance.callMethod("getTvAppName") as? TextView)
                ?: (mContainerView.getChildAt(1) as? TextView)
            if (mAppNameView == null) return@createAfterHook

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
