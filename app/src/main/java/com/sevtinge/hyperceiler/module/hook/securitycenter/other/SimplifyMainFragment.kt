package com.sevtinge.hyperceiler.module.hook.securitycenter.other

import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.sevtinge.hyperceiler.module.base.BaseHook
import java.util.List

object SimplifyMainFragment : BaseHook() {
    override fun init() {
        if (mPrefsMap.getBoolean("security_center_simplify_home")) {
            val cardViewRvAdapterClassName = "com.miui.common.card.CardViewRvAdapter"
            loadClass(cardViewRvAdapterClassName).methodFinder()
                .filterByName("addAll")
                .filterByParamTypes(List::class.java)
                .single()
                .createHook {
                    before { param ->
                        val oldModelList = param.args[0] as List<*>
                        val removedModel = listOf(
                            // 功能推荐
                            "com.miui.common.card.models.FuncListBannerCardModel",
                            // 常用功能
                            // "com.miui.common.card.models.CommonlyUsedFunctionCardModel",
                            // 大家都在用
                            "com.miui.common.card.models.PopularActionCardModel"
                        )

                        param.args[0] = oldModelList.filterNot { model ->
                            removedModel.contains(model.javaClass.name)
                        }
                    }
                }
        }
    }
}