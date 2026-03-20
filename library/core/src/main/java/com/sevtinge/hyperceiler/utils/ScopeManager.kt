package com.sevtinge.hyperceiler.utils

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

    suspend fun getScope(): List<String>? = withContext(Dispatchers.IO) {
        val service = getService()
        if (service == null) {
            AndroidLog.e(TAG, "getScope: LSPosed service not available.")
            return@withContext null
        }
        return@withContext try {
            service.getScope()
        } catch (e: Exception) {
            AndroidLog.e(TAG, "getScope failed", e)
            null
        }
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
}
