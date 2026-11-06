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

import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setBooleanField
import io.github.lingqiqi5211.ezhooktool.core.findMethodOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import org.luckypray.dexkit.query.enums.UsingType
import org.luckypray.dexkit.query.matchers.FieldMatcher
import org.luckypray.dexkit.result.FieldDataList
import org.luckypray.dexkit.result.MethodDataList
import java.lang.reflect.Field
import java.lang.reflect.Method

object DisableSafeModelTip : BaseHook() {
    override fun useDexKit() = true
    private lateinit var disableSafeModelTipMethod: Method
    private lateinit var recyclerViewFields: List<Field>
    private const val HOLDER_CLASS =
        "com.miui.packageInstaller.ui.listcomponets.SafeModeTipViewObject"

    // init() 靠 hook 「SafeModeTipViewObject 父类里写 boolean 字段的那个方法」来清除提示标记。
    // dexdump 逐方法核验（audit/packageinstaller_appinfoupload_20261105.md）：
    //   OS3     父类 com.miui.packageInstaller.ui.listcomponets.p 只有 p#a()V 会 iput-boolean（写 l）
    //           -> 按名字找 a 正好命中，上游写法成立
    //   OS4.0.5 父类被混淆成 J2/b：整个类**没有名为 a 的方法**（a 是个 PRIVATE FINAL 的
    //           WeakReference 字段），写 boolean 字段的方法变成 J2/b#A(boolean)
    //           -> findMethod 找不到方法是直接抛异常（不是返回空列表）。init() 里前面的
    //              disableSafeModelTipMethod 主 hook 已经装好，但抛异常之后的“清除标记”部分全部丢失，
    //              整条规则被记为 Hook Failed
    // 结构不变量是「父类中写本类 boolean 字段的 void 方法」，两版各只有一个（<init> 除外）。
    // 因此这里额外用 DexKit 取该写手方法作为兜底，仅在按名字 a 找不到时使用，OS3 行为不变。
    private var flagWriterMethods: List<Method> = emptyList()

    // 构造成员也会被“写 boolean 字段”匹配到（OS4.0.5 的 J2/b#<init> 就写 f/i/j），
    // 但 getMethodInstance() 对 <init> 会抛 "not a method"，所以在解析成反射对象之前剔除。
    private fun isInitializer(name: String): Boolean =
        name == "<init>" || name == "<clinit>"

    override fun initDexKit(): Boolean {
        disableSafeModelTipMethod = requiredMember("DisableSafeModelTip") {
            it.findMethod {
                matcher {
                    usingEqStrings($$"android.provider.MiuiSettings$Ad")
                }
            }.singleOrNull()
        }
        // SafeModeTipViewObject 的父类是混淆类（OS4.0.4.0 上为 J2/b;），
        // 它里面有 3 个 boolean 字段（混淆名 f / i / j），
        // 旧的 .single() 结构上必然抛 NonUniqueResultException("unique result: 3")，
        // 导致整条规则被跳过。改成取全量 boolean 字段，在绑定回调里逐个置 false。
        recyclerViewFields = requiredMemberList("RecyclerView") {
            it.findClass {
                matcher {
                    className = HOLDER_CLASS
                }
            }.single().superClass?.findField {
                matcher {
                    type = "boolean"
                }
            } ?: FieldDataList()
        }
        flagWriterMethods = optionalMemberList("SafeModeTipFlagWriters") {
            it.findClass {
                matcher {
                    className = HOLDER_CLASS
                }
            }.singleOrNull()?.superClass?.findMethod {
                matcher {
                    returnType = "void"
                    addUsingField(
                        FieldMatcher.create().apply { type = "boolean" },
                        UsingType.Write
                    )
                }
            }?.apply {
                removeIf { isInitializer(it.name) }
            } ?: MethodDataList()
        }
        return true
    }

    override fun init() {
        disableSafeModelTipMethod.createHook {
            replace { false }
        }
        hookFlagWriterClear()
    }

    // findMethod 找不到时会直接抛异常（不是返回空列表），所以这里用 findMethodOrNull；
    // OS4.0.5 上父类根本没有名为 a 的方法，原写法在此抛 MemberNotFoundException，
    // 使“清除提示标记”这后半段在 init() 阶段丢掉（前面的主 hook 已装好），规则被记为 Hook Failed。
    private fun hookFlagWriterClear() {
        val holderClass = recyclerViewFields.firstOrNull()?.declaringClass ?: return
        val byName = holderClass.findMethodOrNull { name("a") }
        val gateMethods: List<Method> = listOfNotNull(byName).ifEmpty { flagWriterMethods }
        gateMethods.forEach { gate ->
            gate.createAfterHook {
                val thisObject = it.thisObject ?: return@createAfterHook
                recyclerViewFields.forEach { field ->
                    runCatching { thisObject.setBooleanField(field.name, false) }
                }
            }
        }
    }
}
