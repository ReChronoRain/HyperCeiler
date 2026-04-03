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

import com.sevtinge.hyperceiler.common.log.XposedLog
import org.json.JSONArray
import org.json.JSONObject
import org.luckypray.dexkit.DexKitCacheBridge
import org.luckypray.dexkit.annotations.DexKitExperimentalApi
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption

/**
 * [DexKitCacheBridge.Cache] 的 JSON 文件实现。
 *
 * 缓存数据保存在 JSON 文件中，并在构造阶段做版本校验。
 * 内存读取走映射表，写入通过 dirty 标记延迟到 [flush] 时落盘。
 *
 * 当前 JSON 格式示例：
 * ```json
 * {
 *   "version": 11,
 *   "pkgVersion": "1.0(1)",
 *   "osVersion": "V816.0.24.0.UNBCNXM",
 *   "strings": { "BatteryHealth#ChargeFragmentMethod": "Lcom/xxx/Cls;->method()V" },
 *   "lists": { "SomeHook#TargetMethods": ["Lcom/xxx/A;->a()V", "Lcom/xxx/B;->b()V"] }
 * }
 * ```
 *
 * 缓存 key 不含运行时前缀（ceiler:s: / ceiler:l:），
 * 由 `strings` / `lists` 两个分组隐式区分单值和列表。
 *
 * @author Ling Qiqi
 */
@OptIn(DexKitExperimentalApi::class)
internal class JsonFileCache(
    private val cacheFile: File,
    private val pkgVersion: String?,
    private val osVersion: String?,
    private val tag: String,
) : DexKitCacheBridge.Cache {

    companion object {
        private const val CACHE_VERSION = 11
        private const val KEY_VERSION = "version"
        private const val KEY_PKG_VERSION = "pkgVersion"
        private const val KEY_OS_VERSION = "osVersion"
        private const val KEY_STRINGS = "strings"
        private const val KEY_LISTS = "lists"
    }

    private val lock = Any()
    private val strings = LinkedHashMap<String, String>()
    private val lists = LinkedHashMap<String, List<String>>()

    @Volatile
    private var dirty = false

    init {
        loadAndValidate()
    }

    // ======================== 缓存接口实现 ========================

    override fun getString(key: String, default: String?): String? {
        synchronized(lock) {
            return strings[key] ?: default
        }
    }

    override fun putString(key: String, value: String) {
        synchronized(lock) {
            strings[key] = value
            dirty = true
        }
    }

    override fun getStringList(key: String, default: List<String>?): List<String>? {
        synchronized(lock) {
            return lists[key]?.let(::ArrayList) ?: default
        }
    }

    override fun putStringList(key: String, value: List<String>) {
        synchronized(lock) {
            lists[key] = ArrayList(value)
            dirty = true
        }
    }

    override fun remove(key: String) {
        synchronized(lock) {
            val removedString = strings.remove(key) != null
            val removedList = lists.remove(key) != null
            if (removedString || removedList) {
                dirty = true
            }
        }
    }

    override fun getAllKeys(): Collection<String> {
        synchronized(lock) {
            val keys = LinkedHashSet<String>(strings.size + lists.size)
            keys.addAll(strings.keys)
            keys.addAll(lists.keys)
            return keys
        }
    }

    override fun clearAll() {
        synchronized(lock) {
            strings.clear()
            lists.clear()
            dirty = true
        }
    }

    // ======================== 文件操作 ========================

    /**
     * 把脏数据刷新到磁盘。
     * 一般在当前 Hook 会话结束时调用。
     */
    fun flush() {
        synchronized(lock) {
            if (!dirty) return
            if (saveToDisk()) {
                dirty = false
            }
        }
    }

    private fun loadAndValidate() {
        synchronized(lock) {
            if (!cacheFile.exists()) {
                XposedLog.d(tag, "JsonFileCache: no cache file, starting fresh")
                return
            }

            try {
                val text = cacheFile.readText(Charsets.UTF_8)
                if (text.isBlank()) return

                val root = JSONObject(text)
                val fileVersion = root.optInt(KEY_VERSION, 0)
                val filePkgVersion = root.takeIf { it.has(KEY_PKG_VERSION) }?.getString(KEY_PKG_VERSION)
                val fileOsVersion = root.takeIf { it.has(KEY_OS_VERSION) }?.getString(KEY_OS_VERSION)

                // 版本失效判断
                var needClear = false

                if (fileVersion != CACHE_VERSION) {
                    XposedLog.d(tag, "JsonFileCache: version changed $fileVersion -> $CACHE_VERSION")
                    needClear = true
                }
                if (pkgVersion != null && pkgVersion != filePkgVersion) {
                    XposedLog.d(tag, "JsonFileCache: pkgVersion changed $filePkgVersion -> $pkgVersion")
                    needClear = true
                }
                if (osVersion != null && osVersion != fileOsVersion) {
                    XposedLog.d(tag, "JsonFileCache: osVersion changed $fileOsVersion -> $osVersion")
                    needClear = true
                }

                if (needClear) {
                    dirty = true
                    return
                }

                // 读取单值缓存
                root.optJSONObject(KEY_STRINGS)?.let { strObj ->
                    for (k in strObj.keys()) {
                        strings[k] = strObj.getString(k)
                    }
                }

                // 读取列表缓存
                root.optJSONObject(KEY_LISTS)?.let { listObj ->
                    for (k in listObj.keys()) {
                        val arr = listObj.getJSONArray(k)
                        val list = ArrayList<String>(arr.length())
                        for (i in 0 until arr.length()) {
                            list.add(arr.getString(i))
                        }
                        lists[k] = list
                    }
                }

                XposedLog.d(tag, "JsonFileCache: loaded ${strings.size} strings, ${lists.size} lists")
            } catch (t: Throwable) {
                XposedLog.w(tag, "JsonFileCache: failed to load cache, starting fresh", t)
                strings.clear()
                lists.clear()
                dirty = true
            }
        }
    }

    private fun saveToDisk(): Boolean {
        try {
            val dir = cacheFile.parentFile
            if (dir != null && !dir.exists()) {
                if (!dir.mkdirs() && !dir.exists()) {
                    XposedLog.w(tag, "JsonFileCache: failed to create cache dir: ${dir.absolutePath}")
                    return false
                }
            }

            val root = JSONObject()
            root.put(KEY_VERSION, CACHE_VERSION)
            if (pkgVersion != null) root.put(KEY_PKG_VERSION, pkgVersion)
            if (osVersion != null) root.put(KEY_OS_VERSION, osVersion)

            val strObj = JSONObject()
            for ((k, v) in strings) {
                strObj.put(k, v)
            }
            root.put(KEY_STRINGS, strObj)

            val listObj = JSONObject()
            for ((k, v) in lists) {
                val arr = JSONArray()
                for (s in v) arr.put(s)
                listObj.put(k, arr)
            }
            root.put(KEY_LISTS, listObj)

            val json = root.toString(2)
                // JSONObject.toString() 会把 / 转义为 \/，手动还原以提高可读性
                .replace("\\/", "/")

            if (!cacheFile.exists()) {
                if (!cacheFile.createNewFile() && !cacheFile.exists()) {
                    XposedLog.w(tag, "JsonFileCache: failed to create cache file")
                    return false
                }
            }

            FileChannel.open(
                cacheFile.toPath(),
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            ).use { channel ->
                var lock: FileLock? = null
                try {
                    lock = channel.lock()
                    val bytes = json.toByteArray(Charsets.UTF_8)
                    val buf = ByteBuffer.wrap(bytes)
                    while (buf.hasRemaining()) {
                        channel.write(buf)
                    }
                    channel.force(false)
                } finally {
                    lock?.release()
                }
            }

            XposedLog.d(tag, "JsonFileCache: saved ${strings.size} strings, ${lists.size} lists")
            return true
        } catch (t: Throwable) {
            XposedLog.w(tag, "JsonFileCache: failed to save cache", t)
            return false
        }
    }
}
