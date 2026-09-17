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
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.TreeMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * [DexKitCacheBridge.Cache] 的 JSON 文件实现。
 *
 * 缓存数据保存在 JSON 文件中，并在构造阶段做版本校验。
 * 所有写入先进入建议队列，再由单消费者串行应用到内存映射表，
 * 最后在 [flush] 时统一落盘。
 *
 * 多进程宿主会共享同一个应用 dataDir，因此持久化时使用独立 lock 文件串行化写入，
 * 在锁内重新读取最新磁盘状态并合并本进程增量，最后通过同目录临时文件原子替换。
 * 这样可避免不同进程互相覆盖刚生成的 DexKit cache，也避免在拿到锁之前截断主缓存文件。
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

    private data class CacheState(
        val strings: LinkedHashMap<String, String> = LinkedHashMap(),
        val lists: LinkedHashMap<String, List<String>> = LinkedHashMap(),
    )

    private val ioLock = Any()
    private val strings = LinkedHashMap<String, String>()
    private val lists = LinkedHashMap<String, List<String>>()
    private val writeSuggestions = ConcurrentLinkedQueue<WriteSuggestion>()
    private val pendingDiskWrites = ArrayList<WriteSuggestion>()

    /** 当前进程启动时读到旧版本/损坏文件时，确保 session 结束会修复磁盘文件。 */
    private var rewriteRequired = false

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
                if (pendingDiskWrites.isEmpty() && !rewriteRequired) return

                val writesSnapshot = ArrayList(pendingDiskWrites)
                val savedState = saveToDisk(writesSnapshot) ?: return

                replaceMemoryStateLocked(savedState)
                pendingDiskWrites.clear()
                rewriteRequired = false

                if (suggestionCount > 0) {
                    XposedLog.d(tag, "JsonFileCache: applied $suggestionCount queued writes")
                }
                if (writeSuggestions.isEmpty()) return
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
                val state = readValidatedState(cacheFile, logVersionChanges = true)
                if (state == null) {
                    rewriteRequired = true
                    return
                }

                replaceMemoryStateLocked(state)
                XposedLog.d(tag, "JsonFileCache: loaded ${strings.size} strings, ${lists.size} lists")
            } catch (t: Throwable) {
                XposedLog.w(tag, "JsonFileCache: failed to load cache, starting fresh", t)
                strings.clear()
                lists.clear()
                rewriteRequired = true
            }
        }
    }

    /**
     * 在跨进程 writer lock 内读取最新有效状态并应用本进程增量。
     *
     * 若当前进程启动时看到的是旧/损坏文件，但其它进程已经先修复了文件，
     * 这里会复用那个最新有效状态，而不是再次把它清空。
     */
    private fun saveToDisk(writes: List<WriteSuggestion>): CacheState? {
        try {
            val dir = cacheFile.parentFile
            if (dir != null && !dir.exists()) {
                if (!dir.mkdirs() && !dir.exists()) {
                    XposedLog.w(tag, "JsonFileCache: failed to create cache dir: ${dir.absolutePath}")
                    return null
                }
            }
            if (dir == null) {
                XposedLog.w(tag, "JsonFileCache: cache file has no parent directory")
                return null
            }

            val lockFile = File(dir, "${cacheFile.name}.lock")
            FileChannel.open(
                lockFile.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            ).use { lockChannel ->
                val fileLock = lockChannel.lock()
                try {
                    val latestState = try {
                        readValidatedState(cacheFile, logVersionChanges = false)
                    } catch (t: Throwable) {
                        XposedLog.w(tag, "JsonFileCache: failed to merge latest cache, rebuilding", t)
                        null
                    }
                    val mergedState = latestState ?: CacheState()
                    for (suggestion in writes) {
                        applySuggestion(mergedState.strings, mergedState.lists, suggestion)
                    }

                    writeStateAtomically(dir, mergedState)
                    XposedLog.d(
                        tag,
                        "JsonFileCache: saved ${mergedState.strings.size} strings, ${mergedState.lists.size} lists"
                    )
                    return mergedState
                } finally {
                    fileLock.release()
                }
            }
        } catch (t: Throwable) {
            XposedLog.w(tag, "JsonFileCache: failed to save cache", t)
            return null
        }
    }

    private fun writeStateAtomically(dir: File, state: CacheState) {
        val json = serializeState(state)
        val tempPath = Files.createTempFile(dir.toPath(), "${cacheFile.name}.", ".tmp")
        try {
            FileChannel.open(
                tempPath,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            ).use { channel ->
                val bytes = json.toByteArray(Charsets.UTF_8)
                val buf = ByteBuffer.wrap(bytes)
                while (buf.hasRemaining()) {
                    channel.write(buf)
                }
                channel.force(false)
            }

            try {
                Files.move(
                    tempPath,
                    cacheFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    tempPath,
                    cacheFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } finally {
            Files.deleteIfExists(tempPath)
        }
    }

    private fun readValidatedState(file: File, logVersionChanges: Boolean): CacheState? {
        if (!file.exists()) return null

        val text = file.readText(Charsets.UTF_8)
        if (text.isBlank()) return null

        val root = JSONObject(text)
        val fileVersion = root.optInt(KEY_VERSION, 0)
        val filePkgVersion = root.takeIf { it.has(KEY_PKG_VERSION) }?.getString(KEY_PKG_VERSION)
        val fileOsVersion = root.takeIf { it.has(KEY_OS_VERSION) }?.getString(KEY_OS_VERSION)

        var valid = true
        if (fileVersion != CACHE_VERSION) {
            if (logVersionChanges) {
                XposedLog.d(tag, "JsonFileCache: version changed $fileVersion -> $CACHE_VERSION")
            }
            valid = false
        }
        if (pkgVersion != null && pkgVersion != filePkgVersion) {
            if (logVersionChanges) {
                XposedLog.d(tag, "JsonFileCache: pkgVersion changed $filePkgVersion -> $pkgVersion")
            }
            valid = false
        }
        if (osVersion != null && osVersion != fileOsVersion) {
            if (logVersionChanges) {
                XposedLog.d(tag, "JsonFileCache: osVersion changed $fileOsVersion -> $osVersion")
            }
            valid = false
        }
        if (!valid) return null

        val state = CacheState()
        root.optJSONObject(KEY_STRINGS)?.let { strObj ->
            for (k in strObj.keys()) {
                state.strings[k] = strObj.getString(k)
            }
        }
        root.optJSONObject(KEY_LISTS)?.let { listObj ->
            for (k in listObj.keys()) {
                val arr = listObj.getJSONArray(k)
                val list = ArrayList<String>(arr.length())
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                state.lists[k] = list
            }
        }
        return state
    }

    private fun serializeState(state: CacheState): String {
        val root = JSONObject()
        root.put(KEY_VERSION, CACHE_VERSION)
        if (pkgVersion != null) root.put(KEY_PKG_VERSION, pkgVersion)
        if (osVersion != null) root.put(KEY_OS_VERSION, osVersion)

        val strObj = JSONObject()
        for ((k, v) in TreeMap(state.strings)) {
            strObj.put(k, v)
        }
        root.put(KEY_STRINGS, strObj)

        val listObj = JSONObject()
        for ((k, v) in TreeMap(state.lists)) {
            val arr = JSONArray()
            for (s in v) arr.put(s)
            listObj.put(k, arr)
        }
        root.put(KEY_LISTS, listObj)

        return root.toString(2)
            // JSONObject.toString() 会把 / 转义为 \/，手动还原以提高可读性
            .replace("\\/", "/")
    }

    private fun enqueueWriteSuggestion(suggestion: WriteSuggestion) {
        writeSuggestions.offer(suggestion)
    }

    private fun applyWriteSuggestionsLocked(): Int {
        var count = 0
        while (true) {
            val suggestion = writeSuggestions.poll() ?: break
            applySuggestion(strings, lists, suggestion)
            pendingDiskWrites.add(suggestion)
            count++
        }
        return count
    }

    private fun applySuggestion(
        stringMap: MutableMap<String, String>,
        listMap: MutableMap<String, List<String>>,
        suggestion: WriteSuggestion,
    ) {
        when (suggestion.type) {
            WriteType.PUT_STRING -> {
                stringMap[suggestion.key!!] = suggestion.stringValue!!
            }

            WriteType.PUT_LIST -> {
                listMap[suggestion.key!!] = ArrayList(suggestion.listValue!!)
            }

            WriteType.REMOVE -> {
                val key = suggestion.key ?: return
                stringMap.remove(key)
                listMap.remove(key)
            }

            WriteType.CLEAR -> {
                stringMap.clear()
                listMap.clear()
            }
        }
    }

    private fun replaceMemoryStateLocked(state: CacheState) {
        strings.clear()
        strings.putAll(state.strings)
        lists.clear()
        for ((key, value) in state.lists) {
            lists[key] = ArrayList(value)
        }
    }
}
