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
package com.sevtinge.hyperceiler.libhook.rules.passwords

import com.sevtinge.hyperceiler.common.log.XposedLog
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.LinkedHashSet
import java.util.zip.ZipFile

/**
 * 极简 DEX 解析器：找出"方法字节码里 const-string 引用了某常量"的类，
 * 或"定义了某形态方法"的类。
 *
 * 用于不写死混淆名地定位目标类（抗混淆定位）。
 * string_ids 每项 4 字节；指令线性扫描，结果只当候选类，调用方按方法形态二次确认。
 */
object DexStringLocator {

    private const val TAG = "DexStringLocator"

    fun findClassesWithConstant(apkPath: String, needle: String): Set<String> {
        val result: MutableSet<String> = LinkedHashSet()
        val needleBytes = needle.toByteArray(StandardCharsets.UTF_8)
        try {
            ZipFile(apkPath).use { zip ->
                for (n in listDexNames(zip)) {
                    val e = zip.getEntry(n)
                    zip.getInputStream(e).use { scanDex(readAll(it), needleBytes, result) }
                }
            }
        } catch (e: IOException) {
            XposedLog.e(TAG, "io error", e)
        }
        return result
    }

    /**
     * 找出"方法字节码里 const-string 引用了某常量"的方法，返回 `类名#方法名`。
     *
     * 比 [findClassesWithConstant] 细一级：一个类里可能有多个方法碰过
     * 同一个常量（如 syncExtensionStr1 被序列化/反序列化/游标读写引用），
     * 调用方按方法形态（参数/返回值）二次确认唯一目标。
     */
    fun findMethodsWithConstant(apkPath: String, needle: String): Set<String> {
        val result: MutableSet<String> = LinkedHashSet()
        val needleBytes = needle.toByteArray(StandardCharsets.UTF_8)
        try {
            ZipFile(apkPath).use { zip ->
                for (n in listDexNames(zip)) {
                    val e = zip.getEntry(n)
                    zip.getInputStream(e).use { scanDexMethods(readAll(it), needleBytes, result) }
                }
            }
        } catch (e: IOException) {
            XposedLog.e(TAG, "io error", e)
        }
        return result
    }

    /**
     * 找出"定义了指定形态方法"的方法，返回 `类名#方法名`。
     * 形态用 dex 类型描述符表达（如 `Ljava/lang/String;`、`[B`、`I`），
     * 参数位传 `null` 表示该位任意（通配）；
     * 只看类自己定义的 encoded method（不含外部引用），调用方用反射+探针二次确认。
     */
    fun findMethodsWithShape(
        apkPath: String,
        returnTypeDesc: String,
        vararg paramTypeDescs: String?
    ): Set<String> {
        val result: MutableSet<String> = LinkedHashSet()
        try {
            ZipFile(apkPath).use { zip ->
                for (n in listDexNames(zip)) {
                    val e = zip.getEntry(n)
                    zip.getInputStream(e).use {
                        scanDexShapes(readAll(it), returnTypeDesc, paramTypeDescs, result)
                    }
                }
            }
        } catch (e: IOException) {
            XposedLog.e(TAG, "io error", e)
        }
        return result
    }

    private fun listDexNames(zip: ZipFile): List<String> {
        val dexNames: MutableList<String> = ArrayList()
        val en = zip.entries()
        while (en.hasMoreElements()) {
            val n = en.nextElement().name
            if (n == "classes.dex" || (n.startsWith("classes") && n.endsWith(".dex"))) {
                dexNames.add(n)
            }
        }
        Collections.sort(dexNames)
        return dexNames
    }

    // -------------------- dex 解析 --------------------

    private fun scanDex(dex: ByteArray, needle: ByteArray, out: MutableSet<String>) {
        if (dex.size < 0x70) return
        val stringIdsSize = u4(dex, 0x38)
        val stringIdsOff = u4(dex, 0x3C)
        val typeIdsOff = u4(dex, 0x44)
        val classDefsSize = u4(dex, 0x60)
        val classDefsOff = u4(dex, 0x64)

        // 1) 目标串的 string 索引集合（string_ids 每项 4 字节：u4 string_data_off）
        val target: MutableSet<Int> = HashSet()
        for (i in 0 until stringIdsSize) {
            val dataOff = u4(dex, stringIdsOff + 4 * i)
            val pos = intArrayOf(dataOff)
            readUleb(dex, pos)
            val start = pos[0]
            var end = start
            while (end < dex.size && dex[end] != 0.toByte()) end++
            if (containsBytes(dex, start, end, needle)) target.add(i)
        }
        if (target.isEmpty()) return

        // 2) 每个类的 code 里找引用
        for (k in 0 until classDefsSize) {
            val base = classDefsOff + 32 * k
            val classIdx = u4(dex, base)
            val classDataOff = u4(dex, base + 24)
            if (classDataOff == 0) continue

            val pos = intArrayOf(classDataOff)
            val staticFields = readUleb(dex, pos)
            val instanceFields = readUleb(dex, pos)
            val directMethods = readUleb(dex, pos)
            val virtualMethods = readUleb(dex, pos)

            skipFields(dex, pos, staticFields + instanceFields)

            if (methodsReference(dex, pos, directMethods, target)
                || methodsReference(dex, pos, virtualMethods, target)
            ) {
                out.add(typeToName(dex, stringIdsOff, typeIdsOff, classIdx))
            }
        }
    }

    private fun methodsReference(dex: ByteArray, pos: IntArray, count: Int, target: Set<Int>): Boolean {
        for (m in 0 until count) {
            readUleb(dex, pos) // method_idx_diff
            readUleb(dex, pos) // access_flags
            val codeOff = readUleb(dex, pos)
            if (codeOff != 0 && codeReferences(dex, codeOff, target)) return true
        }
        return false
    }

    /** 方法级扫描：引用了目标常量的方法，输出 "类名#方法名" */
    private fun scanDexMethods(dex: ByteArray, needle: ByteArray, out: MutableSet<String>) {
        if (dex.size < 0x70) return
        val stringIdsSize = u4(dex, 0x38)
        val stringIdsOff = u4(dex, 0x3C)
        val typeIdsOff = u4(dex, 0x44)
        val methodIdsOff = u4(dex, 0x5C)
        val classDefsSize = u4(dex, 0x60)
        val classDefsOff = u4(dex, 0x64)

        // 1) 目标串的 string 索引集合
        val target: MutableSet<Int> = HashSet()
        for (i in 0 until stringIdsSize) {
            val dataOff = u4(dex, stringIdsOff + 4 * i)
            val pos = intArrayOf(dataOff)
            readUleb(dex, pos)
            val start = pos[0]
            var end = start
            while (end < dex.size && dex[end] != 0.toByte()) end++
            if (containsBytes(dex, start, end, needle)) target.add(i)
        }
        if (target.isEmpty()) return

        // 2) 每个类的 code 里找引用，并记下方法名（method_ids 每项 8 字节：u2 class, u2 proto, u4 name）
        for (k in 0 until classDefsSize) {
            val base = classDefsOff + 32 * k
            val classIdx = u4(dex, base)
            val classDataOff = u4(dex, base + 24)
            if (classDataOff == 0) continue

            val pos = intArrayOf(classDataOff)
            val staticFields = readUleb(dex, pos)
            val instanceFields = readUleb(dex, pos)
            val directMethods = readUleb(dex, pos)
            val virtualMethods = readUleb(dex, pos)

            skipFields(dex, pos, staticFields + instanceFields)

            val clsName = typeToName(dex, stringIdsOff, typeIdsOff, classIdx)
            methodsCollect(dex, pos, directMethods, target, methodIdsOff, stringIdsOff, clsName, out)
            methodsCollect(dex, pos, virtualMethods, target, methodIdsOff, stringIdsOff, clsName, out)
        }
    }

    private fun methodsCollect(
        dex: ByteArray, pos: IntArray, count: Int, target: Set<Int>,
        methodIdsOff: Int, stringIdsOff: Int, clsName: String, out: MutableSet<String>
    ) {
        var running = 0
        for (m in 0 until count) {
            running += readUleb(dex, pos) // method_idx_diff（累计成绝对 method_idx）
            readUleb(dex, pos) // access_flags
            val codeOff = readUleb(dex, pos)
            if (codeOff != 0 && codeReferences(dex, codeOff, target)) {
                val nameIdx = u4(dex, methodIdsOff + 8 * running + 4)
                out.add(clsName + "#" + readMutf8(dex, stringIdsOff, nameIdx))
            }
        }
    }

    /** 形态级扫描：遍历每个类定义的方法，按 proto（返回值+参数描述符）匹配 */
    private fun scanDexShapes(
        dex: ByteArray, retDesc: String, paramDescs: Array<out String?>, out: MutableSet<String>
    ) {
        if (dex.size < 0x70) return
        val stringIdsOff = u4(dex, 0x3C)
        val typeIdsOff = u4(dex, 0x44)
        val protoIdsOff = u4(dex, 0x4C)
        val methodIdsOff = u4(dex, 0x5C)
        val classDefsSize = u4(dex, 0x60)
        val classDefsOff = u4(dex, 0x64)

        for (k in 0 until classDefsSize) {
            val base = classDefsOff + 32 * k
            val classIdx = u4(dex, base)
            val classDataOff = u4(dex, base + 24)
            if (classDataOff == 0) continue

            val pos = intArrayOf(classDataOff)
            val staticFields = readUleb(dex, pos)
            val instanceFields = readUleb(dex, pos)
            val directMethods = readUleb(dex, pos)
            val virtualMethods = readUleb(dex, pos)
            skipFields(dex, pos, staticFields + instanceFields)

            val clsName = typeToName(dex, stringIdsOff, typeIdsOff, classIdx)
            collectShapes(
                dex, pos, directMethods, methodIdsOff, stringIdsOff, typeIdsOff, protoIdsOff,
                retDesc, paramDescs, clsName, out
            )
            collectShapes(
                dex, pos, virtualMethods, methodIdsOff, stringIdsOff, typeIdsOff, protoIdsOff,
                retDesc, paramDescs, clsName, out
            )
        }
    }

    private fun collectShapes(
        dex: ByteArray, pos: IntArray, count: Int, methodIdsOff: Int, stringIdsOff: Int,
        typeIdsOff: Int, protoIdsOff: Int, retDesc: String, paramDescs: Array<out String?>,
        clsName: String, out: MutableSet<String>
    ) {
        var running = 0
        for (m in 0 until count) {
            running += readUleb(dex, pos) // method_idx_diff（累计成绝对 method_idx）
            readUleb(dex, pos) // access_flags
            readUleb(dex, pos) // code_off（形态判定不需要）
            if (protoMatches(dex, methodIdsOff, running, stringIdsOff, typeIdsOff, protoIdsOff, retDesc, paramDescs)) {
                val nameIdx = u4(dex, methodIdsOff + 8 * running + 4)
                out.add(clsName + "#" + readMutf8(dex, stringIdsOff, nameIdx))
            }
        }
    }

    /** method_ids[idx] 的 proto 是否命中形态（proto_ids 每项 12 字节：u4 shorty, u4 ret, u4 params_off） */
    private fun protoMatches(
        dex: ByteArray, methodIdsOff: Int, methodIdx: Int, stringIdsOff: Int,
        typeIdsOff: Int, protoIdsOff: Int, retDesc: String, paramDescs: Array<out String?>
    ): Boolean {
        val protoIdx = u2(dex, methodIdsOff + 8 * methodIdx + 2)
        val protoBase = protoIdsOff + 12 * protoIdx
        if (typeDesc(dex, stringIdsOff, typeIdsOff, u4(dex, protoBase + 4)) != retDesc) return false
        val paramsOff = u4(dex, protoBase + 8)
        val n = if (paramsOff != 0) u4(dex, paramsOff) else 0
        if (n != paramDescs.size) return false
        for (i in 0 until n) {
            val tIdx = u2(dex, paramsOff + 4 + 2 * i)
            val want = paramDescs[i]
            if (want != null && typeDesc(dex, stringIdsOff, typeIdsOff, tIdx) != want) {
                return false
            }
        }
        return true
    }

    private fun typeDesc(dex: ByteArray, stringIdsOff: Int, typeIdsOff: Int, typeIdx: Int): String {
        return readMutf8(dex, stringIdsOff, u4(dex, typeIdsOff + 4 * typeIdx))
    }

    private fun skipFields(dex: ByteArray, pos: IntArray, count: Int) {
        for (i in 0 until count) {
            readUleb(dex, pos)
            readUleb(dex, pos)
        }
    }

    private fun codeReferences(dex: ByteArray, codeOff: Int, target: Set<Int>): Boolean {
        val insnsSize = u4(dex, codeOff + 12)
        val insnsOff = codeOff + 16
        val limit = minOf(insnsSize, (dex.size - insnsOff) / 2)
        for (i in 0 until limit) {
            val op = u2(dex, insnsOff + 2 * i) and 0xFF
            if (op == 0x1a && i + 1 < limit) { // const-string vAA, idx@BBBB
                if (target.contains(u2(dex, insnsOff + 2 * (i + 1)))) return true
            } else if (op == 0x1b && i + 2 < limit) { // const-string-jumbo
                val idx = u2(dex, insnsOff + 2 * (i + 1)) or
                        (u2(dex, insnsOff + 2 * (i + 2)) shl 16)
                if (target.contains(idx)) return true
            }
        }
        return false
    }

    private fun typeToName(dex: ByteArray, stringIdsOff: Int, typeIdsOff: Int, classIdx: Int): String {
        val descIdx = u4(dex, typeIdsOff + 4 * classIdx)
        val desc = readMutf8(dex, stringIdsOff, descIdx)
        return if (desc.length > 2 && desc[0] == 'L' && desc.endsWith(";")) {
            desc.substring(1, desc.length - 1).replace('/', '.')
        } else {
            desc
        }
    }

    private fun readMutf8(dex: ByteArray, stringIdsOff: Int, idx: Int): String {
        val dataOff = u4(dex, stringIdsOff + 4 * idx)
        val pos = intArrayOf(dataOff)
        readUleb(dex, pos)
        val start = pos[0]
        var end = start
        while (end < dex.size && dex[end] != 0.toByte()) end++
        return String(dex, start, end - start, StandardCharsets.UTF_8)
    }

    // -------------------- 基础工具 --------------------

    private fun containsBytes(hay: ByteArray, from: Int, to: Int, needle: ByteArray): Boolean {
        val last = to - needle.size
        for (i in from..last) {
            var j = 0
            while (j < needle.size && hay[i + j] == needle[j]) j++
            if (j == needle.size) return true
        }
        return false
    }

    private fun readUleb(d: ByteArray, pos: IntArray): Int {
        var result = 0
        var shift = 0
        var b: Int
        do {
            b = d[pos[0]++].toInt() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            shift += 7
        } while ((b and 0x80) != 0 && shift < 35)
        return result
    }

    private fun u2(d: ByteArray, off: Int): Int {
        return (d[off].toInt() and 0xFF) or ((d[off + 1].toInt() and 0xFF) shl 8)
    }

    private fun u4(d: ByteArray, off: Int): Int {
        return (d[off].toInt() and 0xFF) or ((d[off + 1].toInt() and 0xFF) shl 8) or
                ((d[off + 2].toInt() and 0xFF) shl 16) or ((d[off + 3].toInt() and 0xFF) shl 24)
    }

    private fun readAll(input: InputStream): ByteArray {
        val bos = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var n: Int
        while (input.read(buf).also { n = it } > 0) bos.write(buf, 0, n)
        return bos.toByteArray()
    }
}
