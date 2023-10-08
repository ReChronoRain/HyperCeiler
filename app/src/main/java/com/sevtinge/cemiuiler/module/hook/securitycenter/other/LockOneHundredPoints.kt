package com.sevtinge.cemiuiler.module.hook.securitycenter.other

import android.view.View
import com.github.kyuubiran.ezxhelper.ClassLoaderProvider.classLoader
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.cemiuiler.module.base.BaseHook
import com.sevtinge.cemiuiler.utils.DexKit.dexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType

object LockOneHundredPoints : BaseHook() {
   /* var mScoreManagerCls: Class<*>? = null
    var mMainContentFrameCls: Class<*>? = null
    override fun init() {
        mScoreManagerCls = findClassIfExists("com.miui.securityscan.scanner.ScoreManager")
        mMainContentFrameCls = findClassIfExists("com.miui.securityscan.ui.main.MainContentFrame")
        try {
            val result: List<DexMethodDescriptor> =
                Objects.requireNonNull<List<DexMethodDescriptor>>(
                    SecurityCenterDexKit.mSecurityCenterResultMap.get("ScoreManager")
                )
            for (descriptor in result) {
                val lockOneHundredPoints: Method = descriptor.getMethodInstance(lpparam.classLoader)
                log("lock 100 points method is $lockOneHundredPoints")
                if (lockOneHundredPoints.returnType == Int::class.javaPrimitiveType) {
                    XposedBridge.hookMethod(
                        lockOneHundredPoints,
                        XC_MethodReplacement.returnConstant(0)
                    )
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        *//*findAndHookMethod(mScoreManagerCls, "B", new MethodHook() {
            @Override
            protected void before(MethodHookParam param) throws Throwable {
                if(PrefsUtils.mSharedPreferences.getBoolean("prefs_key_security_center_score", false)) param.setResult(0);
            }
        });*//*findAndHookMethod(
            mMainContentFrameCls,
            "onClick",
            View::class.java,
            object : BaseHook.MethodHook() {
                @Throws(Throwable::class)
                protected override fun before(param: MethodHookParam) {
                    if (PrefsUtils.mSharedPreferences.getBoolean(
                            "prefs_key_security_center_score",
                            false
                        )
                    ) param.setResult(null)
                }
            })
    }*/
    private val score by lazy {
        dexKitBridge.findMethod {
            matcher {
                addUsingString("getMinusPredictScore", StringMatchType.Contains)
            }
        }.firstOrNull()?.getMethodInstance(classLoader)
    }

    override fun init() {
        loadClass("com.miui.securityscan.ui.main.MainContentFrame").methodFinder()
            .filterByName("onClick")
            .filterByParamTypes(View::class.java)
            .first().createHook {
                before {
                    it.result = null
                }
            }

        score?.createHook {
            returnConstant(0)
        }
    }
}
