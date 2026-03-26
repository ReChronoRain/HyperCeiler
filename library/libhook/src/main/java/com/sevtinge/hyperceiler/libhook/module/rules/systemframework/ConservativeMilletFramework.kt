package com.sevtinge.hyperceiler.hook.module.rules.systemframework

import com.sevtinge.hyperceiler.hook.module.base.BaseHook

object ConservativeMilletFramework : BaseHook() {
    const val TAG = "ConservativeMilletFramework"

    override fun init() {
        val aurogonImmobulusModeClass =
            findClassIfExists("com.miui.server.greeze.AurogonImmobulusMode")

        val domesticPolicyManagerClass =
            findClassIfExists("com.miui.server.greeze.DomesticPolicyManager")

        try {
            findAndHookMethod(
                aurogonImmobulusModeClass,
                "isNeedRestictNetworkPolicy",
                Int::class.java,
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        param.result = false
                    }
                })
        } catch (e: Exception) {
            logE(TAG, "Hook Failed in isNeedRestictNetworkPolicy: ", e)
        }

        try {
            findAndHookMethod(
                domesticPolicyManagerClass,
                "isAllowBroadcast",
                object : MethodHook() {
                    override fun before(param: MethodHookParam) {
                        param.result = true
                    }
                }
            )
        } catch (e: Exception) {
            logE(TAG, "Hook Failed in isAllowBroadcast: ", e)
        }

        try {
            findAndHookMethod(
                domesticPolicyManagerClass,
                "isRestrictBroadcast",
                Int::class.java, object : MethodHook() {
                    override fun before(param: MethodHookParam) {
//                        val uid = param.args[0] as Int
                        param.result = true
                    }
                }
            )
        } catch (e: Exception) {
            logE(TAG, "Hook Failed in isRestrictBroadcast: ", e)
        }

        try {
            findAndHookMethod(
                domesticPolicyManagerClass,
                "isJobRestrict",
                Int::class.java, object : MethodHook() {
                    override fun before(param: MethodHookParam) {
//                        val uid = param.args[0] as Int
                        param.result = true
                    }
                }
            )
        } catch (e: Exception) {
            logE(TAG, "Hook Failed in isJobRestrict: ", e)
        }

        try {
            findAndHookMethod(
                domesticPolicyManagerClass,
                "deferBroadcast",
                String::class.java, object : MethodHook() {
                    override fun before(param: MethodHookParam) {
//                        val action = param.args[0] as String
                        param.result = false
                    }
                }
            )
        } catch (e: Exception) {
            logE(TAG, "Hook Failed in deferBroadcast: ", e)
        }

        try {
            findAndHookMethod(
                domesticPolicyManagerClass,
                "isRestrictNet",
                Int::class.java, object : MethodHook() {
                    override fun before(param: MethodHookParam) {
//                        val uid = param.args[0] as Int
                        param.result = false
                    }
                }
            )
        } catch (e: Exception) {
            logE(TAG, "Hook Failed in isRestrictNet: ", e)
        }

    }
}
