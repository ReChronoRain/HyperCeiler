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
import java.util.TreeMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [DexKitCacheBridge.Cache] 的 JSON 文件实现。
 *
 * 缓存数据保存在 JSON 文件中，并在构造阶段做版本校验。
 * 所有写入先进入建议队列，再由单消费者串行应用到内存映射表，
 * 最后在 [flush] 时统一落盘。
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

    private enum class WriteType {
        PUT_STRING,
        PUT_LIST,
        REMOVE,
        CLEAR,
    }

    private data class WriteSuggestion(
        val type: WriteType,
        val key: String? = null,
        val stringValue: String? = null,
        val listValue: List<String>? = null,
    )

    private val ioLock = Any()
    private val strings = LinkedHashMap<String, String>()
    private val lists = LinkedHashMap<String, List<String>>()
    private val writeSuggestions = ConcurrentLinkedQueue<WriteSuggestion>()

    private val dirty = AtomicBoolean(false)

    init {
        loadAndValidate()
    }

    // ======================== 缓存接口实现 ========================

    override fun getString(key: String, default: String?): String? {
        synchronized(ioLock) {
            applyWriteSuggestionsLocked()
            return strings[key] ?: default
        }
    }

    override fun putString(key: String, value: String) {
        enqueueWriteSuggestion(WriteSuggestion(WriteType.PUT_STRING, key = key, stringValue = value))
    }

    override fun getStringList(key: String, default: List<String>?): List<String>? {
        synchronized(ioLock) {
            applyWriteSuggestionsLocked()
            return lists[key]?.let(::ArrayList) ?: default
        }
    }

    override fun putStringList(key: String, value: List<String>) {
        enqueueWriteSuggestion(
            WriteSuggestion(
                WriteType.PUT_LIST,
                key = key,
                listValue = ArrayList(value)
            )
        )
    }

    override fun remove(key: String) {
        enqueueWriteSuggestion(WriteSuggestion(WriteType.REMOVE, key = key))
    }

    override fun getAllKeys(): Collection<String> {
        synchronized(ioLock) {
            applyWriteSuggestionsLocked()
            val keys = LinkedHashSet<String>(strings.size + lists.size)
            keys.addAll(strings.keys)
            keys.addAll(lists.keys)
            return keys
        }
    }

    override fun clearAll() {
        enqueueWriteSuggestion(WriteSuggestion(WriteType.CLEAR))
    }

    // ======================== 文件操作 ========================

    /**
     * 把脏数据刷新到磁盘。
     * 一般在当前 Hook 会话结束时调用。
     */
    fun flush() {
        synchronized(ioLock) {
            while (true) {
                val suggestionCount = applyWriteSuggestionsLocked()
                if (suggestionCount == 0 && !dirty.get()) return

                val stringsSnapshot = snapshotStringsLocked()
                val listsSnapshot = snapshotListsLocked()
                dirty.set(false)
                if (!saveToDisk(stringsSnapshot, listsSnapshot)) {
                    dirty.set(true)
                    return
                }
                if (suggestionCount > 0) {
                    XposedLog.d(tag, "JsonFileCache: applied $suggestionCount queued writes")
                }
                if (writeSuggestions.isEmpty() && !dirty.get()) return
            }
        }
    }

    private fun loadAndValidate() {
        synchronized(ioLock) {
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
                    dirty.set(true)
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
                dirty.set(true)
            }
        }
    }

    private fun saveToDisk(
        stringsSnapshot: Map<String, String>,
        listsSnapshot: Map<String, List<String>>,
    ): Boolean {
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
            for ((k, v) in stringsSnapshot) {
                strObj.put(k, v)
            }
            root.put(KEY_STRINGS, strObj)

            val listObj = JSONObject()
            for ((k, v) in listsSnapshot) {
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

            XposedLog.d(tag, "JsonFileCache: saved ${stringsSnapshot.size} strings, ${listsSnapshot.size} lists")
            return true
        } catch (t: Throwable) {
            XposedLog.w(tag, "JsonFileCache: failed to save cache", t)
            return false
        }
    }

    private fun enqueueWriteSuggestion(suggestion: WriteSuggestion) {
        writeSuggestions.offer(suggestion)
        dirty.set(true)
    }

    private fun applyWriteSuggestionsLocked(): Int {
        var count = 0
        while (true) {
            val suggestion = writeSuggestions.poll() ?: break
            when (suggestion.type) {
                WriteType.PUT_STRING -> {
                    strings[suggestion.key!!] = suggestion.stringValue!!
                }

                WriteType.PUT_LIST -> {
                    lists[suggestion.key!!] = suggestion.listValue!!
                }

                WriteType.REMOVE -> {
                    val key = suggestion.key ?: continue
                    strings.remove(key)
                    lists.remove(key)
                }

                WriteType.CLEAR -> {
                    strings.clear()
                    lists.clear()
                }
            }
            count++
        }
        return count
    }

    private fun snapshotStringsLocked(): Map<String, String> = TreeMap(strings)

    private fun snapshotListsLocked(): Map<String, List<String>> = TreeMap(lists)
}
