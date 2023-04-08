package com.sevtinge.cemiuiler.module.lbe

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.sevtinge.cemiuiler.module.base.BaseHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage


class DisableClipboardTip : BaseHook() {
    override fun init() {
        if (!lpparam.packageName.equals("com.lbe.security.miui")) {
            return
        }

        val permissionRequestClass =
            XposedHelpers.findClass(
                "com.lbe.security.sdk.PermissionRequest",
                lpparam.classLoader
            )

        XposedHelpers.findAndHookMethod("com.lbe.security.ui.SecurityPromptHandler",
            lpparam.classLoader,
            "handleNewRequest",
            permissionRequestClass,
            object : XC_MethodHook() {
                @RequiresApi(Build.VERSION_CODES.TIRAMISU)
                override fun afterHookedMethod(param: MethodHookParam) {
                    val permissionRequest = param.args[0]
                    val permission: Long =
                        XposedHelpers.callMethod(permissionRequest, "getPermission") as Long
                    //PermissionManager.PERM_ID_READ_CLIPBOARD
                    if (permission == 274877906944L) {
                        val packageName =
                            XposedHelpers.callMethod(permissionRequest, "getPackage") as String
                        val context =
                            XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                        val appName = getAppName(context, packageName)

                        //Toast.makeText(context, "$appName 读取了剪贴板", Toast.LENGTH_SHORT).show()
                        hideDialog(lpparam, packageName, param)

                        XposedBridge.log("Cemiuiler: DisableClipboardTip: $packageName -> $appName read clipboard.")
                    }


                }
            })


    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getAppName(context: Context, packageName: String): String {
        val pm: PackageManager = context.applicationContext.packageManager
        val ai: ApplicationInfo =  pm.getApplicationInfo(packageName,  PackageManager.ApplicationInfoFlags.of(0))
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