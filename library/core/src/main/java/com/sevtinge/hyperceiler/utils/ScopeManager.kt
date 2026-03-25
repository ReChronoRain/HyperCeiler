package com.sevtinge.hyperceiler.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.sevtinge.hyperceiler.common.log.AndroidLog
import io.github.libxposed.service.XposedService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.concurrent.Volatile

object ScopeManager {

    private const val TAG = "ScopeManager"
    private const val SYSTEM_SCOPE_PACKAGE = "system"
    @Volatile
    private var sService: XposedService? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceStateListeners = CopyOnWriteArraySet<ServiceStateListener>()

    @JvmStatic
    fun setService(service: XposedService) {
        sService = service
        notifyServiceStateChanged(service)
    }

    @JvmStatic
    fun clearService() {
        sService = null
        notifyServiceStateChanged(null)
    }

    @JvmStatic
    fun getService(): XposedService? {
        return sService
    }

    fun requireService(): XposedService {
        val s: XposedService? = sService
        checkNotNull(s) { "XposedService not bound yet" }
        return s
    }

    interface ServiceStateListener {
        fun onServiceStateChanged(service: XposedService?)
    }

    @JvmStatic
    fun addServiceStateListener(
        listener: ServiceStateListener,
        notifyImmediately: Boolean = false
    ) {
        serviceStateListeners.add(listener)
        if (notifyImmediately) {
            dispatchServiceState(listener, sService)
        }
    }

    @JvmStatic
    fun removeServiceStateListener(listener: ServiceStateListener) {
        serviceStateListeners.remove(listener)
    }

    private fun notifyServiceStateChanged(service: XposedService?) {
        for (listener in serviceStateListeners) {
            dispatchServiceState(listener, service)
        }
    }

    private fun dispatchServiceState(listener: ServiceStateListener, service: XposedService?) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            listener.onServiceStateChanged(service)
            return
        }
        mainHandler.post { listener.onServiceStateChanged(service) }
    }

    interface ScopeCallback {
        fun onScopeOperationSuccess(message: String)
        fun onScopeOperationFail(message: String)
    }

    interface ScopeBatchCallback {
        fun onCompleted(success: Boolean, message: String)
    }

    @JvmStatic
    fun getScopeSync(): List<String>? {
        val service = getService()
        if (service == null) {
            AndroidLog.e(TAG, "getScopeSync: LSPosed service not available.")
            return null
        }
        return try {
            service.getScope()
        } catch (e: Exception) {
            AndroidLog.e(TAG, "getScopeSync failed", e)
            null
        }
    }

    suspend fun getScope(): List<String>? = withContext(Dispatchers.IO) {
        getScopeSync()
    }

    @JvmStatic
    fun normalizeScopePackageName(packageName: String?): String? {
        val normalized = packageName?.trim()?.lowercase()
        return if (normalized.isNullOrEmpty()) null else normalized
    }

    @JvmStatic
    fun normalizeScopePackages(packageNames: Collection<String>?): LinkedHashSet<String> {
        val normalized = LinkedHashSet<String>()
        packageNames?.forEach { packageName ->
            normalizeScopePackageName(packageName)?.let(normalized::add)
        }
        return normalized
    }

    @JvmStatic
    fun isSystemScopePackage(packageName: String?): Boolean {
        return normalizeScopePackageName(packageName) == SYSTEM_SCOPE_PACKAGE
    }

    @JvmStatic
    fun containsScopePackage(scopePackages: Collection<String>?, packageName: String?): Boolean {
        val normalizedPackage = normalizeScopePackageName(packageName) ?: return false
        return normalizeScopePackages(scopePackages).contains(normalizedPackage)
    }

    @JvmStatic
    fun peekNormalizedScopeSync(): LinkedHashSet<String>? {
        val service = getService() ?: return null
        return try {
            normalizeScopePackages(service.getScope())
        } catch (e: Exception) {
            AndroidLog.e(TAG, "peekNormalizedScopeSync failed", e)
            null
        }
    }

    @JvmStatic
    fun applyScopeDiffAsync(
        context: Context?,
        currentSelected: Collection<String>?,
        targetSelected: Collection<String>?,
        callback: ScopeBatchCallback
    ) {
        val current = normalizeScopePackages(currentSelected)
        val target = normalizeScopePackages(targetSelected)
        val toRemove = current.filterNot(target::contains)
        val toAdd = target.filterNot(current::contains)

        if (toRemove.isEmpty() && toAdd.isEmpty()) {
            dispatchBatchResult(callback, true, getBatchString(context, com.sevtinge.hyperceiler.core.R.string.scope_batch_no_changes))
            return
        }

        val service = getService()
        if (service == null) {
            dispatchBatchResult(callback, false, getBatchString(context, com.sevtinge.hyperceiler.core.R.string.scope_batch_service_unavailable))
            return
        }

        ThreadUtils.postOnBackgroundThread {
            try {
                if (toRemove.isNotEmpty()) {
                    service.removeScope(toRemove)
                }
            } catch (e: Exception) {
                AndroidLog.e(TAG, "applyScopeDiffAsync remove failed", e)
                dispatchBatchResult(
                    callback,
                    false,
                    e.message?.takeIf { it.isNotBlank() }
                        ?: getBatchString(context, com.sevtinge.hyperceiler.core.R.string.scope_batch_remove_failed)
                )
                return@postOnBackgroundThread
            }

            if (toAdd.isEmpty()) {
                dispatchBatchResult(callback, true, getBatchString(context, com.sevtinge.hyperceiler.core.R.string.scope_batch_updated))
                return@postOnBackgroundThread
            }

            mainHandler.post {
                val serviceCallback = object : XposedService.OnScopeEventListener {
                    override fun onScopeRequestApproved(approved: List<String>) {
                        val approvedSet = normalizeScopePackages(approved)
                        val missing = toAdd.filterNot(approvedSet::contains)
                        if (missing.isEmpty()) {
                            dispatchBatchResult(callback, true, getBatchString(context, com.sevtinge.hyperceiler.core.R.string.scope_batch_updated))
                        } else {
                            dispatchBatchResult(
                                callback,
                                false,
                                getBatchString(
                                    context,
                                    com.sevtinge.hyperceiler.core.R.string.scope_batch_request_not_approved,
                                    missing.joinToString(", ")
                                )
                            )
                        }
                    }

                    override fun onScopeRequestFailed(message: String) {
                        dispatchBatchResult(
                            callback,
                            false,
                            getBatchString(
                                context,
                                com.sevtinge.hyperceiler.core.R.string.scope_batch_update_failed,
                                message
                            )
                        )
                    }
                }

                try {
                    service.requestScope(toAdd, serviceCallback)
                } catch (e: Exception) {
                    AndroidLog.e(TAG, "applyScopeDiffAsync add failed", e)
                    dispatchBatchResult(
                        callback,
                        false,
                        e.message?.takeIf { it.isNotBlank() }
                            ?: getBatchString(context, com.sevtinge.hyperceiler.core.R.string.scope_batch_request_failed)
                    )
                }
            }
        }
    }

    @JvmStatic
    fun applyScopeDiffAsync(
        currentSelected: Collection<String>?,
        targetSelected: Collection<String>?,
        callback: ScopeBatchCallback
    ) {
        applyScopeDiffAsync(null, currentSelected, targetSelected, callback)
    }

    /**
     * 为指定应用请求作用域（启用模块）。
     * @param packageName 要启用的应用包名。
     * @param callback 用于接收操作结果的回调。
     */
    suspend fun addScope(packageName: String, callback: ScopeCallback) {
        withContext(Dispatchers.Main) {
            val service = getService()
            if (service == null) {
                callback.onScopeOperationFail("LSPosed service not available.")
                return@withContext
            }

            val serviceCallback = object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: List<String>) {
                    if (approved.contains(packageName)) {
                        callback.onScopeOperationSuccess("$packageName enabled successfully.")
                    } else {
                        callback.onScopeOperationFail("Scope request completed, but $packageName was not approved.")
                    }
                }

                override fun onScopeRequestFailed(message: String) {
                    callback.onScopeOperationFail("Failed to enable $packageName: $message")
                }
            }

            try {
                service.requestScope(listOf(packageName), serviceCallback)
            } catch (e: Exception) {
                AndroidLog.e(TAG, "addScope failed", e)
                callback.onScopeOperationFail(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 为指定应用移除作用域（禁用模块）。
     * @param packageName 要禁用的应用包名。
     * @return 成功则返回 null，失败则返回错误信息字符串。
     */
    suspend fun removeScope(packageName: String): String? = withContext(Dispatchers.IO) {
        val service = getService()
        if (service == null) {
            AndroidLog.e(TAG, "removeScope: LSPosed service not available.")
            return@withContext "LSPosed service not available."
        }
        try {
            service.removeScope(listOf(packageName))
            return@withContext null
        } catch (e: Exception) {
            AndroidLog.e(TAG, "removeScope failed", e)
            return@withContext e.message
        }
    }

    private fun dispatchBatchResult(
        callback: ScopeBatchCallback,
        success: Boolean,
        message: String
    ) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback.onCompleted(success, message)
            return
        }
        mainHandler.post { callback.onCompleted(success, message) }
    }

    private fun getBatchString(context: Context?, resId: Int, vararg args: Any): String {
        return if (context != null) {
            context.getString(resId, *args)
        } else {
            when (resId) {
                com.sevtinge.hyperceiler.core.R.string.scope_batch_no_changes -> "No scope changes."
                com.sevtinge.hyperceiler.core.R.string.scope_batch_service_unavailable -> "LSPosed service not available."
                com.sevtinge.hyperceiler.core.R.string.scope_batch_remove_failed -> "Failed to remove scope."
                com.sevtinge.hyperceiler.core.R.string.scope_batch_updated -> "Scope updated."
                com.sevtinge.hyperceiler.core.R.string.scope_batch_request_not_approved ->
                    "Some scope requests were not approved: ${args.getOrNull(0)?.toString().orEmpty()}"
                com.sevtinge.hyperceiler.core.R.string.scope_batch_update_failed ->
                    "Failed to update scope: ${args.getOrNull(0)?.toString().orEmpty()}"
                com.sevtinge.hyperceiler.core.R.string.scope_batch_request_failed -> "Failed to request scope."
                else -> ""
            }
        }
    }
}
