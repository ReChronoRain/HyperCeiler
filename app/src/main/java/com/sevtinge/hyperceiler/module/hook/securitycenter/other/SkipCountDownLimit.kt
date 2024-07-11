package com.sevtinge.hyperceiler.module.hook.securitycenter.other

import android.app.*
import android.os.*
import com.github.kyuubiran.ezxhelper.ClassLoaderProvider.safeClassLoader
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.*
import com.sevtinge.hyperceiler.module.base.dexkit.*
import com.sevtinge.hyperceiler.module.base.dexkit.DexKitTool.toMethod

/**
 * @author Xzakota
 */
object SkipCountDownLimit : BaseHook() {
    override fun init() {
        when (mPrefsMap.getStringAsInt("security_center_skip_count_down_limit_type", 0)) {
            0 -> {
                loadClass("com.miui.permcenter.privacymanager.model.InterceptBaseActivity").methodFinder()
                    .filterByName("onCreate")
                    .filterByParamTypes(Bundle::class.java)
                    .first().createHook {
                        before { param ->
                            val feat: Bundle.() -> Unit = {
                                // putInt("KEY_STEP_COUNT", 0)
                                putInt("KET_STEP_COUNT", 0)
                                putBoolean("KEY_ALLOW_ENABLE", true)
                            }
                            param.args.first().let {
                                val bundle = it as? Bundle?
                                if (bundle == null) {
                                    param.args[0] = Bundle().apply(feat)
                                } else {
                                    bundle.feat()
                                }
                            }
                        }
                    }
            }

            1 -> {
                val hookClassName = "com.miui.permcenter.privacymanager.InterceptBaseFragment"
                loadClass(hookClassName).methodFinder()
                    .filterByName("onCreate")
                    .filterByParamTypes(Bundle::class.java)
                    .first().createHook {
                        after { param ->
                            val getAppCompatActivity = DexKit.getDexKitBridge("SkipCountDownLimit") {
                                it.findMethod {
                                    matcher {
                                        declaredClass {
                                            className = hookClassName
                                        }
                                        name = "getAppCompatActivity"
                                    }
                                }.single().getMethodInstance(safeClassLoader)
                            }.toMethod()

                            getAppCompatActivity.invoke(param.thisObject).run {
                                this as Activity
                                setResult(-1)
                                finish()
                            }
                        }
                    }
            }
        }
    }
}