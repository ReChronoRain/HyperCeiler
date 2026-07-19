@file:Suppress("unused", "PrivateApi", "DiscouragedPrivateApi")

package io.github.lingqiqi5211.ezhooktool.xposed

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import io.github.lingqiqi5211.ezhooktool.core.EzReflect
import io.github.lingqiqi5211.ezhooktool.xposed.common.ModuleResources
import java.lang.reflect.Executable

/** EzHookTool runtime adapter for libxposed API 101. */
@SuppressLint("PrivateApi", "DiscouragedPrivateApi", "StaticFieldLeak")
object EzXposed {
    private var appContextValue: Context? = null

    @JvmStatic
    lateinit var base: XposedInterface
        internal set

    @JvmStatic
    @Volatile
    var safeMode: Boolean = true

    @JvmStatic
    var packageName: String = ""
        private set

    @JvmStatic
    val hookedPackageName: String
        get() = packageName

    @JvmStatic
    var processName: String = ""
        private set

    @JvmStatic
    var isSystemServer: Boolean = false
        private set

    @JvmStatic
    lateinit var modulePath: String
        private set

    @JvmStatic
    lateinit var moduleRes: Resources
        private set

    @JvmStatic
    val classLoader: ClassLoader
        get() = EzReflect.classLoader

    @JvmStatic
    val safeClassLoader: ClassLoader
        get() = EzReflect.safeClassLoader

    @JvmStatic
    val appContext: Context
        @Synchronized get() {
            appContextValue?.let { return it }
            val current = getCurrentApplicationContext()
                ?: throw NullPointerException("Cannot get appContext now, is Application onCreate finished?")
            appContextValue = current
            return current
        }

    @JvmStatic
    val appContextOrNull: Context?
        @Synchronized get() {
            appContextValue?.let { return it }
            return getCurrentApplicationContext()?.also { appContextValue = it }
        }

    @JvmStatic
    @JvmOverloads
    fun initAppContext(
        context: Context? = getCurrentApplicationContext(),
        injectModuleAssetPath: Boolean = false,
        force: Boolean = false,
    ) {
        val resolved = context
            ?: throw NullPointerException("Cannot init appContext with null context.")
        synchronized(this) {
            if (force || appContextValue == null) {
                appContextValue = resolved.applicationContext ?: resolved
            }
        }
        if (injectModuleAssetPath) {
            addModuleAssetPath(resolved)
        }
    }

    @JvmStatic
    fun initOnModuleLoaded(base: XposedInterface, param: XposedModuleInterface.ModuleLoadedParam) {
        this.base = base
        processName = param.processName
        isSystemServer = param.isSystemServer
        modulePath = base.moduleApplicationInfo.sourceDir
        initModuleResources()
    }

    @JvmStatic
    fun initOnPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        packageName = param.packageName
    }

    @JvmStatic
    fun initOnPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        EzReflect.init(param.classLoader)
        packageName = param.packageName
        isSystemServer = false
    }

    @JvmStatic
    fun initOnSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        EzReflect.init(param.classLoader)
        packageName = "android"
        isSystemServer = true
    }

    @JvmStatic
    fun initModuleResources() {
        moduleRes = ModuleResources.create(modulePath)
    }

    @JvmStatic
    fun addModuleAssetPath(context: Context) {
        addModuleAssetPath(context.resources)
    }

    @JvmStatic
    fun addModuleAssetPath(resources: Resources) {
        addAssetPathMethod.invoke(resources.assets, modulePath)
    }

    @JvmStatic
    fun deoptimize(executable: Executable): Boolean =
        runCatching { base.deoptimize(executable) }.getOrDefault(false)

    private val addAssetPathMethod by lazy {
        AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java).apply {
            isAccessible = true
        }
    }

    private fun getCurrentApplicationContext(): Context? = runCatching {
        val activityThread = Class.forName("android.app.ActivityThread")
        val currentApplication = activityThread.getDeclaredMethod("currentApplication").apply {
            isAccessible = true
        }.invoke(null) as? Context
        currentApplication?.applicationContext ?: currentApplication
    }.getOrNull()
}
