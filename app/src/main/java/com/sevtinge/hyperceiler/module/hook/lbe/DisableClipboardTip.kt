package com.sevtinge.hyperceiler.module.hook.lbe

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import com.sevtinge.hyperceiler.R
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.Helpers
import com.sevtinge.hyperceiler.utils.log.XposedLogUtils
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage


object DisableClipboardTip : BaseHook() {
    override fun init() {
        if (!lpparam.packageName.equals("com.lbe.security.miui")) return

        val permissionRequestClass =
            XposedHelpers.findClass("com.lbe.security.sdk.PermissionRequest", lpparam.classLoader)

        val mDisableClipboardTip = mPrefsMap.getBoolean("lbe_disable_clipboard_tip")

        XposedHelpers.findAndHookMethod(
            "com.lbe.security.ui.SecurityPromptHandler",
            lpparam.classLoader,
            "handleNewRequest",
            permissionRequestClass,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val permissionRequest = param.args[0]
                    val permission: Long =
                        XposedHelpers.callMethod(permissionRequest, "getPermission") as Long

                    // PermissionManager.PERM_ID_READ_CLIPBOARD
                    if (permission == 274877906944L) {
                        val packageName =
                            XposedHelpers.callMethod(permissionRequest, "getPackage") as String
                        val context =
                            XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                        val appName = getAppName(context, packageName)
                        val modRes = Helpers.getModuleRes(context)

                        if (!mDisableClipboardTip) {
                            Toast.makeText(context, "$appName ${modRes.getString(R.string.lbe_clipboard_tip)}", Toast.LENGTH_SHORT).show()
                        }
                        hideDialog(lpparam, packageName, param)

                        XposedLogUtils.logI(" $packageName -> $appName read clipboard.")
                    }
                }
            })
    }

    fun getAppName(context: Context, packageName: String): String {
        val pm: PackageManager = context.applicationContext.packageManager
        val ai: ApplicationInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                pm.getApplicationInfo(packageName, 0)
            }
        return (pm.getApplicationLabel(ai)) as String
    }

    fun hideDialog(
        lpparam: XC_LoadPackage.LoadPackageParam,
        packageName: String,
        param: XC_MethodHook.MethodHookParam
    ) {
        val clipData = XposedHelpers.findClass(
            "com.lbe.security.utility.AnalyticsHelper",
            lpparam.classLoader
        )
        val hashMap = HashMap<String, String>()
        hashMap["pkgName"] = packageName
        hashMap["count"] = "click"
        XposedHelpers.callStaticMethod(
            clipData,
            "recordCountEvent",
            "clip",
            "ask_allow",
            hashMap
        )

        XposedHelpers.callMethod(param.thisObject, "gotChoice", 3, true, true)
        XposedHelpers.callMethod(param.thisObject, "onStop")
    }
}
