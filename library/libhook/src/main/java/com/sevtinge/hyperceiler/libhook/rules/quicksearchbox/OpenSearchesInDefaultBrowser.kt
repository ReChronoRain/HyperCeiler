/*
 * This file is part of HyperCeiler.
 *
 * HyperCeiler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.libhook.rules.quicksearchbox

import android.app.Activity
import android.app.Instrumentation
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.BrowserLinks
import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook
import java.lang.reflect.Method

object OpenSearchesInDefaultBrowser : BaseHook() {
    private const val XIAOMI_BROWSER = "com.android.browser"
    private const val FULL_SEARCH_ACTIVITY = "com.android.browser.fullsearch.FullSearchActivity"
    private const val SEARCH_ENGINE = "com.android.quicksearchbox.xiaomi.searchengine.f"
    private const val DEFAULT_SEARCH_URL = "https://www.baidu.com/s?wd={searchTerms}"
    private val browserCapabilities = setOf(
        "support_fullsearch_ai",
        "support_fullsearch_ai_sugwords",
        "support_fullsearch_onesearch"
    )

    private var startActivityNoThrow: Method? = null
    private var startFullSearch: Method? = null
    private var appMetaData: Method? = null

    override fun useDexKit() = true

    override fun initDexKit(): Boolean {
        startActivityNoThrow = optionalMember("startActivityNoThrow") { bridge ->
            bridge.findMethod {
                matcher {
                    returnType = "boolean"
                    paramTypes("android.content.Context", "android.content.Intent")
                    addUsingString("startActivityNoThrow")
                }
            }.singleOrNull()
        }
        startFullSearch = optionalMember("startFullSearch") { bridge ->
            bridge.findMethod {
                matcher {
                    returnType = "void"
                    paramTypes(
                        "android.content.Context", "android.content.Intent",
                        "java.lang.String", "java.util.HashMap"
                    )
                    addUsingString(FULL_SEARCH_ACTIVITY)
                    addUsingString("urlTemp")
                }
            }.singleOrNull()
        }
        appMetaData = optionalMember("browserAppMetaData") { bridge ->
            bridge.findMethod {
                matcher {
                    returnType = "boolean"
                    paramTypes("android.content.Context", "java.lang.String", "java.lang.String")
                    addUsingString("getAppMetaData() failure. error = ")
                }
            }.singleOrNull() ?: bridge.findMethod {
                matcher {
                    returnType = "boolean"
                    paramTypes("android.content.Context", "java.lang.String", "java.lang.String")
                    addInvoke("Landroid/content/pm/PackageManager;->getApplicationInfo(Ljava/lang/String;I)Landroid/content/pm/ApplicationInfo;")
                    addInvoke("Landroid/os/Bundle;->getBoolean(Ljava/lang/String;)Z")
                }
            }.singleOrNull()
        }
        return true
    }

    override fun init() {
        startFullSearch?.createHook {
            before { param ->
                val context = param.args[0] as? Context ?: return@before
                val intent = param.args[1] as? Intent ?: return@before
                val uri = searchUri(context, intent, param.args[2] as? String) ?: return@before
                val flags = if (context is Activity) intent.flags else
                    intent.flags or Intent.FLAG_ACTIVITY_NEW_TASK
                val target = BrowserLinks.forDefaultBrowser(context, uri, flags) ?: return@before
                if (runCatching { context.startActivity(target) }.isSuccess) param.result = null
            }
        }
        startActivityNoThrow?.createHook {
            before { param ->
                val context = param.args[0] as? Context ?: return@before
                val intent = param.args[1] as? Intent ?: return@before
                redirect(context, intent)?.let { param.args[1] = it }
            }
        }

        // The search box, AI result sheet, and internal WebView launch the browser through
        // different call sites; rewriting the framework call keeps them all in sync.
        hookAllMethods(Instrumentation::class.java, "execStartActivity", object : IMethodHook {
            override fun before(param: HookParam) {
                val context =
                    param.args.firstOrNull() as? Context ?: EzXposed.appContextOrNull ?: return
                for (index in param.args.indices) {
                    val intent = param.args[index] as? Intent ?: continue
                    redirect(context, intent)?.let { param.args[index] = it }
                    return
                }
            }
        })

        // AI and OneSearch are rejected before the intent is built unless the installed
        // browser advertises the corresponding full search capability.
        appMetaData?.createHook {
            before { param ->
                if (param.args[1] != XIAOMI_BROWSER) return@before
                if (param.args[2] !in browserCapabilities) return@before
                param.result = true
            }
        }
    }

    private fun redirect(context: Context, intent: Intent): Intent? {
        if (!BrowserLinks.targetsXiaomiBrowser(intent)) return null
        val uri = searchUri(context, intent) ?: return null
        return BrowserLinks.forDefaultBrowser(context, uri, intent.flags)
    }

    private fun searchUri(context: Context, intent: Intent, fullSearchQuery: String? = null): Uri? {
        val data = intent.data
        if (BrowserLinks.isBrowserDownload(data)) return null
        BrowserLinks.recoverWebUri(intent)?.let { return it }
        val query = when {
            data == null -> fullSearchQuery ?: intent.getStringExtra(SearchManager.QUERY)
            data.scheme == "mibrowser" && data.host == "miui.browser.aisearch" &&
                data.path == "/open" -> data.getQueryParameter("query")

            else -> return null
        }
        BrowserLinks.webUri(query)?.takeUnless(BrowserLinks::isBrowserDownload)?.let { return it }
        return queryUri(context, query, intent.getStringExtra("urlTemp"))
    }

    private fun queryUri(context: Context, query: String?, intentTemplate: String?): Uri? {
        val keyword = query?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val encoded = Uri.encode(keyword)
        intentTemplate?.let { BrowserLinks.webUri(it.replace("{searchTerms}", encoded)) }
            ?.let { return it }
        val engineTemplate = runCatching {
            val engine = Class.forName(SEARCH_ENGINE, false, context.classLoader)
                .getMethod("b", Context::class.java)
                .invoke(null, context)
            engine?.javaClass?.getMethod("h", Context::class.java)?.invoke(engine, context) as? String
        }.getOrNull()
        return engineTemplate?.let { BrowserLinks.webUri(it.replace("{searchTerms}", encoded)) }
            ?: BrowserLinks.webUri(DEFAULT_SEARCH_URL.replace("{searchTerms}", encoded))
    }
}
