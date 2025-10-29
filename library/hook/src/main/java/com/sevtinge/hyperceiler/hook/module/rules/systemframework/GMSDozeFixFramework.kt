package com.sevtinge.hyperceiler.hook.module.rules.systemframework

import com.sevtinge.hyperceiler.hook.module.base.BaseHook
import de.robv.android.xposed.XposedHelpers

object GMSDozeFixFramework : BaseHook() {
    const val TAG = "GMSDozeFixFramework"

    override fun init() {
        val greezeManagerServiceClass =
            findClassIfExists("com.miui.server.greeze.GreezeManagerService")

        val listAppsManagerClass = findClassIfExists("com.miui.server.greeze.power.ListAppsManager")

        val awareResourceControlClass =
            findClassIfExists("com.miui.server.greeze.power.AwareResourceControl")

        try {
            findAndHookMethod(
                greezeManagerServiceClass,
                "isAllowBroadcast",
                Int::class.java,
                String::class.java,
                Int::class.java,
                String::class.java,
                String::class.java,
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
//                        val callerUid = param.args[0] as Int
//                        val callerPkgName = param.args[1] as String
//                        val calleeUid = param.args[2] as Int
                        val calleePkgName = param.args[3] as String
//                        val action = param.args[4] as String

                        if (calleePkgName.contains("com.google.android.gms")) param.result = true
                    }
                })
        } catch (e: Exception) {
            logE(TAG, "Hook Failed in isAllowBroadcast: ", e)
        }

        try {
            findAndHookMethod(
                greezeManagerServiceClass,
                "triggerGMSLimitAction",
                Boolean::class.java,
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
//                        val allow = param.args[0] as Boolean
                        param.args[0] = true
                    }
                })
        } catch (e: Exception) {
            logE(TAG, "Hook Failed in triggerGMSLimitAction: ", e)
        }

        try {
            hookAllConstructors(listAppsManagerClass, object : MethodHook() {
                @Suppress("UNCHECKED_CAST")
                override fun before(param: MethodHookParam) {
                    val targetFields = setOf(
                        "mSystemBlackList",
                        "SLEEP_MODE_LIST",
                        "mMiDataWhiteList",
                        "mDataWhiteList"
                    )

                    listAppsManagerClass.declaredFields
                        .map { it.name }
                        .filter { it in targetFields }
                        .forEach { fieldName ->
                            val list = XposedHelpers.getObjectField(
                                param.thisObject,
                                fieldName
                            ) as MutableList<String>

                            when (fieldName) {
                                "mSystemBlackList" -> list.remove("com.google.android.gms")
                                else -> list.add("com.google.android.gms")
                            }

                            XposedHelpers.setObjectField(param.thisObject, fieldName, list)
                        }
                }
            })
        } catch (e: Exception) {
            logE(TAG, "Hook Failed in listAppsManagerClass constructor", e)
        }

        try {
            findAndHookMethod(
                listAppsManagerClass,
                "isInCloudWhiteList",
                String::class.java,
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        val name = param.args[0] as String
                        if (name.contains("com.google.android.gms")) param.result = true
                    }
                })
        } catch (e: Exception) {
            logE(TAG, "Hook Failed in isInCloudWhiteList", e)
        }

        try {
            findAndHookMethod(
                listAppsManagerClass, "isInWhiteList", String::class.java, object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        val name = param.args[0] as String
                        if (name.contains("com.google.android.gms")) param.result = true
                    }
                })
        } catch (e: Exception) {
            logE(TAG, "Hook Failed in isInWhiteList", e)
        }

        try {
            findAndHookMethod(
                awareResourceControlClass,
                "isNetworkDisableAble",
                String::class.java,
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
//                        val name = param.args[0] as String
                        param.result = false
                    }
                })
        } catch (e: Exception) {
            logE(TAG, "Hook Failed in isNetworkDisableAble", e)
        }
    }

}
