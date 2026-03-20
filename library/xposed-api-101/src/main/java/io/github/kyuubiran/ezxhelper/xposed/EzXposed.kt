@file:Suppress("unused","PrivateApi", "DiscouragedPrivateApi")
package io.github.kyuubiran.ezxhelper.xposed

import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import io.github.kyuubiran.ezxhelper.core.EzXReflection
import io.github.kyuubiran.ezxhelper.xposed.EzXposed.addModuleAssetPath
import io.github.kyuubiran.ezxhelper.xposed.common.ModuleResources
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface

object EzXposed {
    internal lateinit var base: XposedInterface
        private set

    private var _appContext: Context? = null

    /**
     * Get application context.
     *
     * @throws NullPointerException if you get the appContext too early.
     */
    @JvmStatic
    val appContext: Context
        @Synchronized get() {
            if (_appContext == null) {
                _appContext = getCurrentApplicationContext()
                if (_appContext == null) {
                    throw NullPointerException("Cannot get application context, did application call Application.onCreate?")
                }
            }

            return _appContext!!
        }

    @JvmStatic
    lateinit var hookedPackageName: String
        private set

    @JvmStatic
    lateinit var modulePath: String
        private set

    @JvmStatic
    lateinit var moduleRes: Resources
        private set

    /**
     * Captures the current module interface when the module is loaded.
     *
     * @see XposedModule.onModuleLoaded
     * @see XposedModuleInterface.ModuleLoadedParam
     */
    @JvmStatic
    fun initOnModuleLoaded(base: XposedInterface, param: XposedModuleInterface.ModuleLoadedParam) {
        this.base = base
    }

    /**
     * You need to invoke this function at first in [XposedModule.onPackageLoaded].
     *
     * @see XposedModule.onPackageLoaded
     * @see XposedModuleInterface.PackageLoadedParam
     */
    @JvmStatic
    fun initOnPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        hookedPackageName = param.packageName
    }

    /**
     * Initialize reflection with the actual app class loader after the package is fully ready.
     *
     * @see XposedModule.onPackageReady
     * @see XposedModuleInterface.PackageReadyParam
     */
    @JvmStatic
    fun initOnPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        EzXReflection.init(param.classLoader)
        hookedPackageName = param.packageName
    }

    /**
     * You need to invoke this function at first in [XposedModule.onSystemServerStarting].
     *
     * @see XposedModule.onSystemServerStarting
     * @see XposedModuleInterface.SystemServerStartingParam
     */
    @JvmStatic
    fun initOnSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        EzXReflection.init(param.classLoader)
    }

    /**
     * Resolve the module APK path and prepare module-scoped Resources for immediate R access.
     * Call after initOnModuleLoaded so the base interface is already captured.
     */
    @JvmStatic
    fun initModuleResources() {
        this.modulePath = base.moduleApplicationInfo.sourceDir
        this.moduleRes = ModuleResources.create(modulePath)
    }

    /**
     * Initialize the application context.
     * Recommended invoke this after [Application.onCreate].
     *
     * @param context context
     * @param injectResources add module resources path to target [Context.resources]
     * @throws NullPointerException if context is null
     */
    @JvmStatic
    fun initAppContext(
        context: Context? = getCurrentApplicationContext(),
        injectResources: Boolean = false,
    ) {
        if (context == null) {
            throw NullPointerException("Cannot initialize application context, context is null.")
        }
        _appContext = context
        if (injectResources) addModuleAssetPath(_appContext!!)
    }

    private fun getCurrentApplicationContext(): Context? {
        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentApplicationMethod = activityThreadClass.getDeclaredMethod("currentApplication")
            currentApplicationMethod.invoke(null) as? Context
        } catch (e: Exception) {
            throw IllegalStateException("Failed to get application context", e)
        }
    }

    /**
     * Add module path to target Context.resources. Allow directly use module resources with R.xx.xxx.
     *
     * If you want to use this, please do:
     *
     * 1.Modify resources id(don't same as hooked application or other xposed module) in the build.gradle(.kts):
     *
     * Kotlin Gradle DSL:
     *
     *     androidResources.additionalParameters("--allow-reserved-package-id", "--package-id", "0x64")
     *
     * Groovy:
     *
     *     aaptOptions.additionalParameters '--allow-reserved-package-id', '--package-id', '0x64'
     *
     * `0x64` is the resource id, you can change it to any value you want.(recommended [0x30 to 0x6F])
     *
     * 2.Make sure EzXposed is initialized.
     *
     * 3.Invoked this function before use
     */
    @JvmStatic
    fun addModuleAssetPath(context: Context) {
        addModuleAssetPath(context.resources)
    }

    private val mAddAddAssertPath by lazy {
        AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java).also { it.isAccessible = true }
    }

    /**
     * @see [addModuleAssetPath]
     */
    @JvmStatic
    fun addModuleAssetPath(resources: Resources) {
        mAddAddAssertPath.invoke(resources.assets, modulePath)
    }
}
