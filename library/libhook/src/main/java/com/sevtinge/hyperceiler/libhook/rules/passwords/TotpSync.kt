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

import android.content.ContentValues
import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import com.sevtinge.hyperceiler.common.log.XposedLog
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHook
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createHook
import org.json.JSONObject
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * TOTP 云同步桥——补上密码管理同步时丢弃的动态令牌四列
 * （totpSecret/totpAlgorithm/totpIssuer/totpDigits）。
 *
 * 载体："password" 字段的 E2EE blob JSON 新增 `"t"` 键，值是
 * **一个完整的第二信封**（与密码 blob 同构的 encryptedPassword/
 * encryptedRecordKey/recordIV/appKeyVersion，encryptedPassword 槽位承载
 * totpJson）——对 TOTP 字符串原样跑官方的 j()/g() 封装机器生成
 * （每次调用产生独立 record key + IV，与密码同层同钥匙栈，模块零密钥）。
 * 无模块/旧版客户端原生忽略 "t"，密码明文全程纯净——云端恢复、
 * 旧版本共存均不污染密码。
 *
 * 三个主 hook（字符串锚点/形态定位）：
 * - 上传包装入口 `static boolean j(record, Cursor)`（锚点
 *   "decrypted password error by local key"）：after 用合成 record
 *   封装 totpJson 生成信封，写进 record.q 的 blob["t"]；
 * - 下载解包出口 `static boolean g(record)`（锚点 "decrypted
 *   password is null"）：before 从 blob["t"] 取信封经合成 record 解出
 *   totpJson；after 把它写进 record 的 ContentValues 成员；
 * - 落库出口 `static ContentValues d(record)`（锚点
 *   "syncExtensionStr1"）after：成员里的 totp 四列带进落库值。
 *
 * 兼容：解密边界（H3.c.b mode=2）保留一个剥离钩，把 1.0.4 时代
 * "复合明文"格式的旧云端数据拆回纯密码 + totp（重传一轮后自动绝版）。
 *
 * 清空语义：totp 移除后不再生成 "t"，对端落库四列自然为 NULL。
 */
object TotpSync : BaseHook() {

    private const val PAYLOAD_MARKER = "__mfptp"
    /** blob JSON 里的 TOTP 信封键 */
    private const val BLOB_TOTP_KEY = "t"
    /** 密码 setter 探针哨兵值 */
    private const val SETTER_SENTINEL = "__mfptp_setter_probe__"

    /** 抗混淆定位锚点：序列化 put / 解析 optString 处均引用该常量 */
    private const val EXT_STR_NEEDLE = "syncExtensionStr1"

    /** password_record 表 URI（一次性 resync 标脏用） */
    private const val PASSWORD_RECORD_URI = "content://miui.autofill/password_record"

    /** 上传：真实 record → 合成信封流程的 totp payload */
    private val TL_TOTP = ThreadLocal<String>()
    /** 标记当前 j()/g() 调用是合成 record 的信封流程（hook 须让路） */
    private val TL_FAKE = ThreadLocal<Boolean>()
    /** 下载：旧版复合明文剥离出的 totp payload（兼容路径） */
    private val TL_LEGACY = ThreadLocal<String>()

    private var wrapM: Method? = null
    private var unwrapM: Method? = null
    private var writeM: Method? = null
    private var cryptoM: Method? = null
    private var recordClass: Class<*>? = null
    /** 本地加密/解密对（明文→本地密文+iv / 本地密文+iv→明文）：形态+round-trip 探针自定位 */
    private var localEncM: Method? = null
    private var localDecM: Method? = null
    /** record 密码字段 setter（写 ContentValues["password"]）：哨兵探针自定位 */
    private var pwdSetterM: Method? = null

    override fun init() {
        val apkPath = getLpparam().applicationInfo?.sourceDir
        if (apkPath == null) {
            XposedLog.w(TAG, packageName, "totp-sync: 取不到 APK 路径，跳过")
            return
        }
        val classLoader = getClassLoader()

        // ---------- 定位 ----------
        // 落库出口：syncExtensionStr1 锚点 + 形态（顺带拿到 record 类）
        val refs = DexStringLocator.findMethodsWithConstant(apkPath, EXT_STR_NEEDLE)
        XposedLog.i(TAG, packageName, "locator[totp-sync] anchor hits: $refs")
        for (ref in refs) {
            val hash = ref.lastIndexOf('#')
            if (hash <= 0) continue
            val cls = try {
                findClassIfExists(ref.substring(0, hash), classLoader)
            } catch (t: Throwable) {
                null
            } ?: continue
            for (m in cls.declaredMethods) {
                if (m.name != ref.substring(hash + 1)) continue
                val p = m.parameterTypes
                if (Modifier.isStatic(m.modifiers) && p.size == 1
                    && m.returnType == ContentValues::class.java && writeM == null
                ) {
                    writeM = m
                    recordClass = p[0]
                }
            }
        }

        // 上传包装入口 j(record, Cursor) / 下载解包出口 g(record)
        wrapM = findStatic(
            DexStringLocator.findMethodsWithConstant(apkPath, "decrypted password error by local key"),
            arrayOf(recordClass, android.database.Cursor::class.java),
            Boolean::class.javaPrimitiveType!!, classLoader
        )
        unwrapM = findStatic(
            DexStringLocator.findMethodsWithConstant(apkPath, "decrypted password is null"),
            arrayOf(recordClass),
            Boolean::class.javaPrimitiveType!!, classLoader
        )

        // 解密边界（仅旧版复合体兼容用）：形态 (String, ?, int)→String 定位
        //（实测该形态全 dex 唯一命中 H3.c.b；多候选时告警取首个，hook 自带 mode+标记双保险）
        try {
            val stripHits = DexStringLocator.findMethodsWithShape(
                apkPath,
                "Ljava/lang/String;", "Ljava/lang/String;", null, "I"
            )
            if (stripHits.size > 1) {
                XposedLog.w(
                    TAG, packageName,
                    "totp-sync: legacy-strip 形态候选 ${stripHits.size} 个，取首个：$stripHits"
                )
            }
            for (ref in stripHits) {
                val hash = ref.lastIndexOf('#')
                if (hash <= 0) continue
                val cls = findClassIfExists(ref.substring(0, hash), classLoader) ?: continue
                for (m in cls.declaredMethods) {
                    val p = m.parameterTypes
                    if (!Modifier.isStatic(m.modifiers) && p.size == 3
                        && p[0] == String::class.java && p[2] == Int::class.javaPrimitiveType
                        && m.returnType == String::class.java
                        && m.name == ref.substring(hash + 1)
                    ) {
                        cryptoM = m
                        break
                    }
                }
                if (cryptoM != null) break
            }
        } catch (t: Throwable) {
            XposedLog.e(TAG, "totp-sync: legacy-strip 形态定位 err", t)
        }
        if (cryptoM == null) {
            try {
                val cryptoCls = findClassIfExists("H3.c", classLoader)
                if (cryptoCls != null) {
                    for (m in cryptoCls.declaredMethods) {
                        val p = m.parameterTypes
                        if (!Modifier.isStatic(m.modifiers) && p.size == 3
                            && p[0] == String::class.java && p[2] == Int::class.javaPrimitiveType
                            && m.returnType == String::class.java
                        ) {
                            cryptoM = m
                            break
                        }
                    }
                }
            } catch (t: Throwable) {
                XposedLog.w(TAG, packageName, "totp-sync: 直名兜底 H3.c 未命中（混淆名已变？）")
            }
        }
        if (cryptoM == null) {
            XposedLog.w(TAG, packageName, "totp-sync: legacy-strip 未定位，旧版复合明文将不剥离（仅影响历史格式）")
        }

        // 本地加解密对 + 密码 setter：形态/探针自定位（旧构建写死 w2.e 与 s()，
        // 目标 APK 改混淆名即静默失效）；保留旧直名兜底，全落空则响亮告警
        locateLocalCryptoPair(apkPath, classLoader)
        if (localEncM == null || localDecM == null) {
            locateLocalCryptoByName("w2.e", classLoader)
        }
        pwdSetterM = findPasswordSetter(recordClass)
        if (pwdSetterM == null && recordClass != null) {
            pwdSetterM = findNamedSetter(recordClass!!, "s")
        }
        if (localEncM == null || localDecM == null || pwdSetterM == null) {
            XposedLog.w(
                TAG, packageName,
                "totp-sync: 信封工具定位失败（localEnc=${localEncM != null} localDec=${localDecM != null}" +
                    " pwdSetter=${pwdSetterM != null}），TOTP 云同步将不生效——发模块日志重新定位"
            )
        } else {
            XposedLog.i(
                TAG, packageName,
                "totp-sync: 信封工具就位 local=${localEncM!!.declaringClass.name}" +
                    ".${localEncM!!.name}/${localDecM!!.name} pwdSetter=${pwdSetterM!!.name}"
            )
        }

        // ---------- 注册 ----------
        var hooked = 0
        wrapM?.let { m ->
            m.createHook {
                before { param ->
                    if (TL_FAKE.get() == true) return@before   // 合成信封流程让路
                    try {
                        val cv = findContentValues(param.args[0])
                        val secret = cv?.getAsString("totpSecret")
                        TL_TOTP.set(
                            if (cv != null && secret != null && secret.isNotEmpty()) buildPayload(cv)
                            else null
                        )
                    } catch (t: Throwable) {
                        XposedLog.e(TAG, "totp-sync wrap-before err", t)
                    }
                }
                after { param ->
                    if (TL_FAKE.get() == true) return@after
                    try {
                        val payload = TL_TOTP.get()
                        TL_TOTP.remove()
                        if (payload == null) return@after
                        val envelope = buildEnvelope(payload) ?: return@after

                        val record = param.args[0]
                        val blob = getRecordBlob(record)
                        if (blob == null || blob.isEmpty()) return@after
                        val blobJson = JSONObject(blob)
                        blobJson.put(BLOB_TOTP_KEY, envelope)
                        setRecordCipher(record, blobJson.toString())
                        XposedLog.i(TAG, packageName, "totp-sync: upload 注入 blob.t 信封")
                    } catch (t: Throwable) {
                        TL_TOTP.remove()
                        XposedLog.e(TAG, "totp-sync wrap-after err", t)
                    }
                }
            }
            XposedLog.i(TAG, packageName, "totp-sync wrap hook: ${m.declaringClass.name}->${m.name}")
            hooked++
        }
        cryptoM?.let { m ->
            m.createAfterHook { param ->
                try {
                    if (TL_FAKE.get() == true) return@createAfterHook
                    if ((param.args[2] as? Int) != 2) return@createAfterHook
                    val r = param.result
                    if (r !is String) return@createAfterHook
                    val split = splitComposite(r) ?: return@createAfterHook
                    param.result = split[1]
                    TL_LEGACY.set(split[0])
                    XposedLog.i(TAG, packageName, "totp-sync: 旧版复合明文已剥离（兼容）")
                } catch (t: Throwable) {
                    XposedLog.e(TAG, "totp-sync legacy-strip err", t)
                }
            }
            XposedLog.i(TAG, packageName, "totp-sync legacy-strip hook: ${m.declaringClass.name}->${m.name}")
            hooked++
        }
        unwrapM?.let { m ->
            m.createHook {
                before { param ->
                    if (TL_FAKE.get() == true) return@before
                    try {
                        val blob = getRecordBlob(param.args[0])
                        if (blob == null || blob.isEmpty()) return@before
                        val blobJson = JSONObject(blob)
                        if (!blobJson.has(BLOB_TOTP_KEY)) return@before
                        val payload = openEnvelope(blobJson.optString(BLOB_TOTP_KEY))
                        if (payload != null) TL_TOTP.set(payload)
                    } catch (t: Throwable) {
                        XposedLog.e(TAG, "totp-sync unwrap-before err", t)
                    }
                }
                after { param ->
                    if (TL_FAKE.get() == true) return@after
                    try {
                        val payload = TL_TOTP.get() ?: TL_LEGACY.get()
                        TL_TOTP.remove()
                        TL_LEGACY.remove()
                        if (payload == null) return@after
                        val cv = findContentValues(param.args[0]) ?: return@after
                        val p = JSONObject(payload)
                        if (p.optInt(PAYLOAD_MARKER, 0) != 1) return@after
                        val s = p.optString("s", null)
                        if (s == null || s.isEmpty()) return@after
                        cv.put("totpSecret", s)
                        if (p.has("a")) cv.put("totpAlgorithm", p.optString("a"))
                        if (p.has("i")) cv.put("totpIssuer", p.optString("i"))
                        cv.put("totpDigits", p.optInt("d", 6))
                        XposedLog.i(TAG, packageName, "totp-sync: download 解出 TOTP")
                    } catch (t: Throwable) {
                        TL_TOTP.remove()
                        TL_LEGACY.remove()
                        XposedLog.e(TAG, "totp-sync unwrap-after err", t)
                    }
                }
            }
            XposedLog.i(TAG, packageName, "totp-sync unwrap hook: ${m.declaringClass.name}->${m.name}")
            hooked++
        }
        writeM?.let { m ->
            m.createAfterHook { param ->
                try {
                    val r = param.result
                    if (r !is ContentValues) return@createAfterHook
                    val src = findContentValues(param.args[0]) ?: return@createAfterHook
                    if (!src.containsKey("totpSecret")) return@createAfterHook

                    val s = src.getAsString("totpSecret")
                    if (s != null && s.isNotEmpty()) {
                        r.put("totpSecret", s)
                        r.put("totpAlgorithm", src.getAsString("totpAlgorithm"))
                        r.put("totpIssuer", src.getAsString("totpIssuer"))
                        val digits = src.getAsInteger("totpDigits")
                        r.put("totpDigits", digits ?: 6)
                    } else {
                        r.putNull("totpSecret")
                        r.putNull("totpAlgorithm")
                        r.putNull("totpIssuer")
                        r.put("totpDigits", 6)
                    }
                } catch (t: Throwable) {
                    XposedLog.e(TAG, "totp-sync db-write hook err", t)
                }
            }
            XposedLog.i(TAG, packageName, "totp-sync db-write hook: ${m.declaringClass.name}->${m.name}")
            hooked++
        }
        if (hooked < 4) {
            XposedLog.w(TAG, packageName, "totp-sync: hook 不全 ($hooked/4)，目标 APK 可能升级改了结构")
        }

        scheduleResyncOnce()
    }

    /** 锚点类里按形态找静态方法；want[i] 为 null 表示该位任意非原生参数 */
    private fun findStatic(
        anchors: Set<String>, want: Array<Class<*>?>, ret: Class<*>, cl: ClassLoader
    ): Method? {
        for (ref in anchors) {
            val hash = ref.lastIndexOf('#')
            if (hash <= 0) continue
            val cls = try {
                findClassIfExists(ref.substring(0, hash), cl)
            } catch (t: Throwable) {
                null
            } ?: continue
            for (m in cls.declaredMethods) {
                if (m.name != ref.substring(hash + 1)) continue
                val p = m.parameterTypes
                if (!Modifier.isStatic(m.modifiers) || m.returnType != ret) continue
                if (p.size != want.size) continue
                var ok = true
                for (i in p.indices) {
                    val w = want[i]
                    if (w != null) {
                        if (p[i] != w) {
                            ok = false
                            break
                        }
                    } else if (p[i].isPrimitive) {
                        ok = false
                        break
                    }
                }
                if (ok) {
                    m.isAccessible = true
                    return m
                }
            }
        }
        return null
    }

    // -------------------- 信封工具自定位（形态/探针，不依赖混淆名）--------------------

    /** 旧版直名兜底：类名 + 形态匹配本地加解密对 */
    private fun locateLocalCryptoByName(className: String, classLoader: ClassLoader) {
        try {
            val localCls = findClassIfExists(className, classLoader) ?: return
            for (m in localCls.declaredMethods) {
                val p = m.parameterTypes
                if (Modifier.isStatic(m.modifiers) && m.returnType == String::class.java
                    && p.size == 2 && p[0] == String::class.java && p[1] == Array<String>::class.java
                ) {
                    m.isAccessible = true
                    localEncM = m
                } else if (Modifier.isStatic(m.modifiers) && m.returnType == String::class.java
                    && p.size == 2 && p[0] == String::class.java && p[1] == String::class.java
                ) {
                    m.isAccessible = true
                    localDecM = m
                }
            }
        } catch (t: Throwable) {
            XposedLog.w(TAG, packageName, "totp-sync: 直名兜底 $className 未命中（混淆名已变？）")
        }
    }

    /** 形态枚举 + round-trip 探针（dec(enc(x)) == x）定位本地加解密对 */
    private fun locateLocalCryptoPair(apkPath: String, cl: ClassLoader) {
        val encHits = DexStringLocator.findMethodsWithShape(
            apkPath,
            "Ljava/lang/String;", "Ljava/lang/String;", "[Ljava/lang/String;"
        )
        val decHits = DexStringLocator.findMethodsWithShape(
            apkPath,
            "Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/String;"
        )
        val classes = classesOf(encHits)
        classes.retainAll(classesOf(decHits))
        for (cn in classes) {
            try {
                val cls = findClassIfExists(cn, cl) ?: continue
                var enc: Method? = null
                var dec: Method? = null
                for (m in cls.declaredMethods) {
                    val p = m.parameterTypes
                    if (!Modifier.isStatic(m.modifiers) || m.returnType != String::class.java) continue
                    if (p.size == 2 && p[0] == String::class.java && p[1] == Array<String>::class.java) enc = m
                    else if (p.size == 2 && p[0] == String::class.java && p[1] == String::class.java) dec = m
                }
                if (enc == null || dec == null || !probeLocalCrypto(enc, dec)) continue
                enc.isAccessible = true
                dec.isAccessible = true
                localEncM = enc
                localDecM = dec
                return
            } catch (t: Throwable) {
                continue
            }
        }
    }

    private fun probeLocalCrypto(enc: Method, dec: Method): Boolean {
        return try {
            val probe = "__mfptp_probe__" + System.nanoTime()
            val ivOut = arrayOfNulls<String>(1)
            val cipher = enc.invoke(null, probe, ivOut) as? String
            cipher != null && ivOut[0] != null
                && probe == dec.invoke(null, cipher, ivOut[0])
        } catch (t: Throwable) {
            false
        }
    }

    private fun classesOf(classHashMethod: Set<String>): MutableSet<String> {
        val out: MutableSet<String> = LinkedHashSet()
        for (ref in classHashMethod) {
            val hash = ref.lastIndexOf('#')
            if (hash > 0) out.add(ref.substring(0, hash))
        }
        return out
    }

    /** 哨兵探针找密码 setter：谁把哨兵写进 ContentValues["password"] 谁就是（不依赖方法名） */
    private fun findPasswordSetter(recCls: Class<*>?): Method? {
        if (recCls == null) return null
        for (m in recCls.declaredMethods) {
            val p = m.parameterTypes
            if (Modifier.isStatic(m.modifiers) || m.returnType != Void.TYPE
                || p.size != 1 || p[0] != String::class.java
            ) continue
            try {
                val rec = recCls.getDeclaredConstructor().newInstance()
                m.isAccessible = true
                m.invoke(rec, SETTER_SENTINEL)
                val cv = findContentValues(rec)
                if (cv != null && SETTER_SENTINEL == cv.getAsString("password")) {
                    return m
                }
            } catch (t: Throwable) {
                continue
            }
        }
        return null
    }

    /** 旧版直名兜底：按名找 setter */
    private fun findNamedSetter(recCls: Class<*>, name: String): Method? {
        for (m in recCls.declaredMethods) {
            val p = m.parameterTypes
            if (!Modifier.isStatic(m.modifiers) && m.returnType == Void.TYPE
                && p.size == 1 && p[0] == String::class.java && m.name == name
            ) {
                m.isAccessible = true
                return m
            }
        }
        return null
    }

    // -------------------- 合成信封工具 --------------------

    /** 新建合成 record（只用到 q 字段与本地加密封装，不触库） */
    private fun newFakeRecord(): Any? {
        return try {
            recordClass!!.getDeclaredConstructor().newInstance()
        } catch (t: Throwable) {
            XposedLog.e(TAG, "totp-sync newFakeRecord err", t)
            null
        }
    }

    /** record.s(x)：设置加密密码字段（字段名经 1.1.25/1.1.26 对照稳定） */
    private fun setRecordCipher(record: Any?, cipher: String) {
        if (record == null) return
        try {
            for (m in record.javaClass.declaredMethods) {
                val p = m.parameterTypes
                if (!Modifier.isStatic(m.modifiers) && m.returnType == Void.TYPE
                    && p.size == 1 && p[0] == String::class.java && m.name == "s"
                ) {
                    m.isAccessible = true
                    m.invoke(record, cipher)
                    return
                }
            }
        } catch (t: Throwable) {
            XposedLog.e(TAG, "totp-sync setRecordCipher err", t)
        }
    }

    /** 读 record 的 ContentValues 成员里的 "password"（blob 字符串） */
    private fun getRecordBlob(record: Any?): String? {
        val cv = findContentValues(record)
        return cv?.getAsString("password")
    }

    /**
     * 上传：totpJson → 第二信封 blob 字符串。
     * 复用官方 j()：合成 record 的 q = 本地加密(totpJson)，配假 Cursor 喂 stringIv，
     * j() 内部生成独立 record key 并封出完整信封。
     */
    private fun buildEnvelope(totpJson: String): String? {
        return try {
            val wrap = wrapM ?: return null
            val enc = localEncM ?: return null
            val ivOut = arrayOfNulls<String>(1)
            val cipher = enc.invoke(null, totpJson, ivOut) as? String ?: return null
            if (ivOut[0] == null) return null

            val fake = newFakeRecord() ?: return null
            setRecordCipher(fake, cipher)

            val cursor = MatrixCursor(arrayOf("stringIv"))
            cursor.addRow(arrayOf<Any?>(ivOut[0]))
            // j() 直接 getString 读当前行、不自行定位（真实调用方传入前已定位）；
            // 新建 MatrixCursor 游标停在首行之前，必须 moveToFirst，否则 CursorIndexOutOfBounds
            cursor.moveToFirst()

            TL_FAKE.set(true)
            try {
                wrap.invoke(null, fake, cursor)
            } finally {
                TL_FAKE.remove()
            }
            getRecordBlob(fake)   // j() 收尾把信封写进 q
        } catch (t: Throwable) {
            XposedLog.e(TAG, "totp-sync buildEnvelope err", t)
            null
        }
    }

    /**
     * 下载：第二信封 blob 字符串 → totpJson。
     * 复用官方 g()：合成 record 的 q = 信封，g() 解出明文并本地加密收进 q，
     * 再用 w2/e.a 还原出明文。
     */
    private fun openEnvelope(envelope: String): String? {
        return try {
            val unwrap = unwrapM ?: return null
            val dec = localDecM ?: return null
            val fake = newFakeRecord() ?: return null
            setRecordCipher(fake, envelope)

            TL_FAKE.set(true)
            try {
                unwrap.invoke(null, fake)
            } finally {
                TL_FAKE.remove()
            }
            val cv = findContentValues(fake)
            val cipher = getRecordBlob(fake)
            val iv = cv?.getAsString("stringIv")
            if (cipher == null || iv == null) {
                XposedLog.w(
                    TAG, packageName,
                    "totp-sync openEnvelope: g() 未产出 cipher/iv (cipher=${cipher != null}, iv=${iv != null})，信封格式可能不被 g() 认可"
                )
                return null
            }
            dec.invoke(null, cipher, iv) as? String
        } catch (t: Throwable) {
            XposedLog.e(TAG, "totp-sync openEnvelope err", t)
            null
        }
    }

    /** 从 record 的 ContentValues 成员取 totp 四列组 payload（列名稳定，不受混淆影响） */
    private fun buildPayload(cv: ContentValues): String? {
        return try {
            val p = JSONObject()
            p.put(PAYLOAD_MARKER, 1)
            p.put("s", cv.getAsString("totpSecret"))
            val alg = cv.getAsString("totpAlgorithm")
            if (alg != null && alg.isNotEmpty()) p.put("a", alg)
            val issuer = cv.getAsString("totpIssuer")
            if (issuer != null && issuer.isNotEmpty()) p.put("i", issuer)
            val digits = cv.getAsInteger("totpDigits")
            p.put("d", digits ?: 6)
            p.toString()
        } catch (t: Throwable) {
            XposedLog.e(TAG, "totp-sync buildPayload err", t)
            null
        }
    }

    /**
     * 旧版复合明文 = `<len>\n<totpJson><password>`；
     * 返回 {payload, password}，不是复合体返回 null。
     */
    private fun splitComposite(plain: String): Array<String>? {
        val nl = plain.indexOf('\n')
        if (nl <= 0 || nl > 9) return null
        val n = try {
            plain.substring(0, nl).toInt()
        } catch (e: NumberFormatException) {
            return null
        }
        val start = nl + 1
        if (n <= 0 || start + n > plain.length) return null
        val payload = plain.substring(start, start + n)
        try {
            val p = JSONObject(payload)
            if (p.optInt(PAYLOAD_MARKER, 0) != 1) return null
        } catch (t: Throwable) {
            return null
        }
        return arrayOf(payload, plain.substring(start + n))
    }

    /**
     * record 的基类持有一个 ContentValues 成员（构造/游标读入时同步积累
     * 整行的列值，setter 都往里 put）。按类型找，不依赖字段名。
     */
    private fun findContentValues(record: Any?): ContentValues? {
        if (record == null) return null
        var c: Class<*>? = record.javaClass
        while (c != null && c != Any::class.java) {
            for (f in c.declaredFields) {
                if (f.type == ContentValues::class.java) {
                    try {
                        f.isAccessible = true
                        val v = f.get(record)
                        if (v is ContentValues) return v
                    } catch (t: Throwable) {
                        continue
                    }
                }
            }
            c = c.superclass
        }
        return null
    }

    // -------------------- 一次性回灌：存量 TOTP 行标脏重传 --------------------

    /**
     * 装模块后跑一次：把已同步的存量 TOTP 行标 MODIFIED 强制重传，
     * 让 "t" 信封版 blob 覆盖云端旧值。SharedPreferences 一次性闸防循环。
     * v4：载体换 blob["t"] 独立信封（v3 复合明文已废弃，需重传一次洗云）。
     */
    private fun scheduleResyncOnce() {
        Thread(Runnable { resyncOnce() }, "mfptp-resync").start()
    }

    private fun resyncOnce() {
        try {
            val ctx = awaitContext()
            if (ctx == null) {
                XposedLog.w(TAG, packageName, "totp-sync resync: 拿不到 Context，跳过")
                return
            }
            val sp = ctx.getSharedPreferences("fuckmiuipasswords_totp", 0)
            if (sp.getBoolean("resync_v4_done", false)) return

            val cv = ContentValues()
            cv.put("localStatus", 1)   // 1 = MODIFIED
            val n = ctx.contentResolver.update(
                Uri.parse(PASSWORD_RECORD_URI), cv,
                "totpSecret IS NOT NULL AND localStatus = ?", arrayOf("-1")
            )
            sp.edit().putBoolean("resync_v4_done", true).apply()
            XposedLog.i(TAG, packageName, "totp-sync: resync v4 标脏 rows=$n")
        } catch (t: Throwable) {
            XposedLog.e(TAG, "totp-sync resync err", t)
        }
    }

    /** Application 建立前拿到的 currentApplication 是 null，给它一点耐心 */
    private fun awaitContext(): Context? {
        for (i in 0 until 20) {
            try {
                val app = Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null)
                if (app is Context) return app
            } catch (t: Throwable) {
                continue
            }
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                return null
            }
        }
        return null
    }
}
