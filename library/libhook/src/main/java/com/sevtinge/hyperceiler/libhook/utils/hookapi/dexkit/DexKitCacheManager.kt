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
package com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKitCacheManager.clearAllCache
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKitCacheManager.findMember
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKitCacheManager.findMemberList
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKitCacheManager.init
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.DexKitCacheManager.releaseBridge
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.AppsTool
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import org.luckypray.dexkit.DexKitCacheBridge
import org.luckypray.dexkit.DexKitCacheBridge.RecyclableBridge
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.ClassDataList
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.FieldDataList
import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.result.MethodDataList
import org.luckypray.dexkit.result.base.BaseData
import org.luckypray.dexkit.wrap.DexClass
import org.luckypray.dexkit.wrap.DexField
import org.luckypray.dexkit.wrap.DexMethod
import org.luckypray.dexkit.wrap.ISerializable
import java.io.File

/**
 * 管理 DexKit CacheBridge 生命周期，以及带缓存的成员解析流程。
 *
 * 缓存命中时：直接从 [JsonFileCache] 反序列化，不创建原生桥。
 * 缓存未命中时：通过 [RecyclableBridge.withBridge] 获取原生桥，执行用户查询，
 * 再把结果序列化后写回缓存。
 *
 * 线程安全约束：所有公开方法都可以跨线程调用。
 * [findMember] / [findMemberList] 整体同步执行（@Synchronized）
 * 生命周期方法（[init] / [releaseBridge] / [clearAllCache]）使用内部锁保护状态。
 *
 * @author Ling Qiqi
 */
@OptIn(DexKitExperimentalApi::class)
internal object DexKitCacheManager {

    private const val TAG_DEFAULT = "DexKit"
    private const val DEXKIT_CACHE_DIR = "hyperceiler"
    private const val DEXKIT_CACHE_FILE = "dexkit_cache.json"

    private val lock = Any()

    @Volatile
    private var tag: String = TAG_DEFAULT

    @Volatile
    private var param: PackageReadyParam? = null

    @Volatile
    private var bridge: RecyclableBridge? = null

    @Volatile
    private var cache: JsonFileCache? = null

    @Volatile
    private var cacheInitialized = false

    // ======================== 生命周期 ========================

    /**
     * 准备 DexKit 会话。
     *
     * 必须在任何 [findMember] / [findMemberList] 调用之前执行。
     * 每个进程首次初始化时注册 [JsonFileCache] 到 [DexKitCacheBridge]，
     * 然后为当前目标应用创建一个 [RecyclableBridge]。
     */
    fun init(param: PackageReadyParam, tag: String) {
        synchronized(lock) {
            this.param = param
            this.tag = tag.ifEmpty { TAG_DEFAULT }

            val appInfo = param.applicationInfo

            // 初始化 CacheBridge 缓存（每个进程只做一次）
            if (!cacheInitialized) {
                System.loadLibrary("dexkit")

                val jsonCache = createJsonFileCache(appInfo, param)
                try {
                    DexKitCacheBridge.init(jsonCache)
                } catch (_: IllegalStateException) {
                    // 当前进程里已经初始化过，直接复用即可
                }
                cache = jsonCache
                cacheInitialized = true
            }

            // 创建或复用 RecyclableBridge
            bridge = createRecyclableBridge(appInfo, param.classLoader)

            XposedLog.d(this.tag, "DexKitCacheManager initialized for ${param.packageName}")
        }
    }

    /**
     * 查找单个成员（Method / Field / Class），并带缓存支持。
     *
     * 缓存命中时：从 JSON 反序列化并结合 classLoader 解析，不创建原生桥。
     * 缓存未命中时：获取原生桥，执行 [iDexKit]，然后序列化并写入缓存。
     *
     * 整个方法同步执行：DexKit 内部使用多线程 native 扫描，
     * 同一个桥并发调用会导致线程争抢，性能急剧恶化。
     */
    @Synchronized
    @Suppress("UNCHECKED_CAST")
    fun <T> findMember(key: String, iDexKit: IDexKit): T {
        val currentParam = param ?: throw IllegalStateException("DexKit not ready")
        val classLoader = currentParam.classLoader
        val currentBridge = bridge ?: throw IllegalStateException("DexKit not initialized")

        // 缓存 key 不带前缀，由 strings / lists 分组隐式区分
        // 先尝试命中内存缓存，命中时不创建原生桥
        cache?.getString(key, null)?.let { cached ->
            return deserializeAndResolve(cached, classLoader) as T
        }

        // 缓存未命中，获取原生桥执行查询
        var result: Any? = null
        currentBridge.withBridge { rawBridge ->
            val baseData: BaseData = try {
                iDexKit.dexkit(rawBridge)
            } catch (e: ReflectiveOperationException) {
                throw RuntimeException(e)
            }
            result = resolveAndCache(baseData, key, classLoader)
        }

        return result as T
    }

    /**
     * 查找成员列表，并带缓存支持。
     *
     * 同步约束同 [findMember]。
     */
    @Synchronized
    @Suppress("UNCHECKED_CAST")
    fun <T> findMemberList(key: String, iDexKitList: IDexKitList): List<T> {
        val currentParam = param ?: throw IllegalStateException("DexKit not ready")
        val classLoader = currentParam.classLoader
        val currentBridge = bridge ?: throw IllegalStateException("DexKit not initialized")

        // 缓存 key 不带前缀，由 strings / lists 分组隐式区分
        // 先尝试命中缓存
        cache?.getStringList(key, null)?.let { cachedList ->
            return cachedList.map { deserializeAndResolve(it, classLoader) as T }
        }

        // 缓存未命中，获取原生桥执行查询
        val resultList = mutableListOf<T>()
        currentBridge.withBridge { rawBridge ->
            val baseDataList = try {
                iDexKitList.dexkit(rawBridge)
            } catch (e: ReflectiveOperationException) {
                throw RuntimeException(e)
            }

            val serializedList = mutableListOf<String>()
            when (baseDataList) {
                is FieldDataList -> for (f in baseDataList) {
                    serializedList.add(f.toDexField().serialize())
                    resultList.add(f.getFieldInstance(classLoader) as T)
                }
                is MethodDataList -> for (m in baseDataList) {
                    serializedList.add(m.toDexMethod().serialize())
                    resultList.add(m.getMethodInstance(classLoader) as T)
                }
                is ClassDataList -> for (c in baseDataList) {
                    serializedList.add(c.toDexClass().serialize())
                    resultList.add(c.getInstance(classLoader) as T)
                }
            }
            cache?.putStringList(key, serializedList)
        }

        return resultList
    }

    /**
     * 释放原生桥，并把缓存刷新到磁盘。
     *
     * 由每次 Hook 会话结束时的 [DexKit.close] 调用。
     */
    fun releaseBridge() {
        synchronized(lock) {
            cache?.flush()
            bridge?.close()
            bridge = null
            param = null

            XposedLog.d(tag, "DexKitCacheManager: bridge closed")
        }
    }

    /**
     * 通过 [DexKitCacheBridge] 清空全部 DexKit 缓存。
     *
     * 仅在当前进程已经初始化 CacheBridge 时可用。
     */
    fun clearAllCache() {
        synchronized(lock) {
            if (cacheInitialized) {
                DexKitCacheBridge.clearAllCache()
                cache?.flush()
            }
        }
    }

    /**
     * 直接从磁盘删除所有缓存文件。
     *
     * 主要用于设置界面进程，因为那里未必初始化了 CacheBridge。
     * [scopeList] 由上层传入（通常来自 ScopeManager.getScopeSync()）。
     */
    fun deleteAllCacheFiles(context: Context, scopeList: Collection<String>?) {
        if (scopeList.isNullOrEmpty()) {
            XposedLog.w(TAG_DEFAULT, "deleteAllCacheFiles: scopeList is null or empty, skip")
            return
        }
        for (folderName in scopeList) {
            try {
                val baseDir = File(
                    context.filesDir.parent,
                    "../$folderName/cache/$DEXKIT_CACHE_DIR"
                )
                deleteRecursively(baseDir)
            } catch (t: Throwable) {
                XposedLog.w(TAG_DEFAULT, "Failed to delete cache for $folderName", t)
            }
        }
    }

    // ======================== 内部辅助方法 ========================

    private fun createJsonFileCache(
        appInfo: ApplicationInfo,
        param: PackageReadyParam
    ): JsonFileCache {
        val cacheDir = File(File(appInfo.dataDir, "cache"), DEXKIT_CACHE_DIR)
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val cacheFile = File(cacheDir, DEXKIT_CACHE_FILE)

        val pkgVersionName = AppsTool.getPackageVersionName(param)
        val pkgVersionCode = AppsTool.getPackageVersionCode(param)
        val hasPkgVersion = pkgVersionName.isNotEmpty() && pkgVersionCode != -1
        val pkgVersion = if (hasPkgVersion) "$pkgVersionName($pkgVersionCode)" else null

        val isSystemUI = "com.android.systemui" == param.packageName
        val osVersion = if (isSystemUI) Build.VERSION.INCREMENTAL else null

        return JsonFileCache(cacheFile, pkgVersion, osVersion, tag)
    }

    private fun createRecyclableBridge(
        appInfo: ApplicationInfo,
        classLoader: ClassLoader?
    ): RecyclableBridge {
        val appTag = tag
        val splitDirs = appInfo.splitSourceDirs
        return if (!splitDirs.isNullOrEmpty() && classLoader != null) {
            XposedLog.d(tag, "DexKit loading by classLoader for split APK, splitCount=${splitDirs.size}")
            DexKitCacheBridge.create(appTag, classLoader)
        } else {
            DexKitCacheBridge.create(appTag, appInfo.sourceDir)
        }
    }

    /**
     * 把缓存里的序列化字符串还原成真实反射对象（Method / Field / Class）。
     *
     * [ISerializable.deserialize] 会根据描述符格式自动判断类型：
     * - 不含 `->`：视为 [DexClass]
     * - 含 `->` 但不含 `:`：视为 [DexMethod]
     * - 同时含 `->` 和 `:`：视为 [DexField]
     */
    private fun deserializeAndResolve(serialized: String, classLoader: ClassLoader): Any {
        return when (val wrapper = ISerializable.deserialize(serialized)) {
            is DexMethod -> wrapper.getMethodInstance(classLoader)
            is DexField -> wrapper.getFieldInstance(classLoader)
            is DexClass -> wrapper.getInstance(classLoader)
            else -> throw IllegalStateException("Unknown ISerializable type: ${wrapper.javaClass}")
        }
    }

    /**
     * 把新查询得到的 [BaseData] 解析为反射对象，同时缓存其序列化结果。
     */
    private fun resolveAndCache(
        baseData: BaseData,
        key: String,
        classLoader: ClassLoader
    ): Any {
        return when (baseData) {
            is FieldData -> {
                cache?.putString(key, baseData.toDexField().serialize())
                baseData.getFieldInstance(classLoader)
            }
            is MethodData -> {
                cache?.putString(key, baseData.toDexMethod().serialize())
                baseData.getMethodInstance(classLoader)
            }
            is ClassData -> {
                cache?.putString(key, baseData.toDexClass().serialize())
                baseData.getInstance(classLoader)
            }
            else -> throw IllegalStateException("Unknown BaseData type: ${baseData.javaClass}")
        }
    }

    private fun deleteRecursively(file: File) {
        if (!file.exists()) return
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }

}
