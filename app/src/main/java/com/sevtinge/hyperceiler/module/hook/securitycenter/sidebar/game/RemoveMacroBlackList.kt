package com.sevtinge.hyperceiler.module.hook.securitycenter.sidebar.game

import com.github.kyuubiran.ezxhelper.EzXHelper
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHooks
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import com.sevtinge.hyperceiler.utils.DexKit.dexKitBridge

class RemoveMacroBlackList : BaseHook() {
    override fun init() {
        dexKitBridge.findMethod {
            matcher {
                addEqString("pref_gb_unsupport_macro_apps")
                paramCount = 0
            }
        }.first().getMethodInstance(safeClassLoader).createHook {
            before {
                returnConstant(ArrayList<String>())
            }
        }

        dexKitBridge.findMethod {
            matcher {
                returnType = "boolean"
                addInvoke {
                    addEqString("pref_gb_unsupport_macro_apps")
                    paramCount = 0
                }
            }
        }.first().getMethodInstance(safeClassLoader).createHook {
            returnConstant(false)
        }

        dexKitBridge.findClass {
            matcher {
                usingStrings =
                    listOf("content://com.xiaomi.macro.MacroStatusProvider/game_macro_change")
            }
        }.first().getInstance(safeClassLoader).apply {
            methodFinder().filterByParamCount(2)
                .toList().createHooks {
                    returnConstant(true)
                }
            methodFinder().filterByParamCount(3)
                .toList().createHooks {
                    returnConstant(true)
                }
        }
    }
}
