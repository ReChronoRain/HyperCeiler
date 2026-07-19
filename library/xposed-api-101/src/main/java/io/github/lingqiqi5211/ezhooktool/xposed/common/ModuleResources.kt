@file:Suppress("DiscouragedPrivateApi", "PrivateApi", "DEPRECATION")

package io.github.lingqiqi5211.ezhooktool.xposed.common

import android.annotation.TargetApi
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.DisplayMetrics
import java.io.File
import java.io.IOException

internal object ModuleResources {
    /**
     * 根据模块 apk 路径创建独立的模块资源对象。
     *
     * Android R 及以上优先走 `ResourcesLoader`，
     * 更低版本回退到 `AssetManager.addAssetPath(...)`。
     */
    fun create(modulePath: String): Resources {
        require(modulePath.isNotBlank()) { "modulePath must not be blank" }

        val baseResources = Resources.getSystem()
        val metrics = DisplayMetrics().also { it.setTo(baseResources.displayMetrics) }
        val configuration = Configuration(baseResources.configuration)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ResourcesLoaderImpl.create(modulePath, metrics, configuration)
        } else {
            LegacyImpl.create(modulePath, metrics, configuration)
        }
    }

    private object LegacyImpl {
        private val assetManagerConstructor by lazy {
            AssetManager::class.java.getDeclaredConstructor().apply {
                isAccessible = true
            }
        }

        private val addAssetPathMethod by lazy {
            AssetManager::class.java.getDeclaredMethod("addAssetPath", String::class.java).apply {
                isAccessible = true
            }
        }

        fun create(
            modulePath: String,
            metrics: DisplayMetrics,
            configuration: Configuration,
        ): Resources {
            val assetManager = assetManagerConstructor.newInstance()
            val cookie = (addAssetPathMethod.invoke(assetManager, modulePath) as? Int) ?: 0
            require(cookie != 0) { "AssetManager.addAssetPath($modulePath) failed" }
            return Resources(assetManager, metrics, configuration)
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    private object ResourcesLoaderImpl {
        private val builderClass by lazy { Class.forName("android.content.res.AssetManager\$Builder") }

        private val builderConstructor by lazy {
            builderClass.getDeclaredConstructor().apply {
                isAccessible = true
            }
        }

        private val builderAddLoaderMethod by lazy {
            builderClass.getDeclaredMethod("addLoader", ResourcesLoader::class.java).apply {
                isAccessible = true
            }
        }

        private val builderBuildMethod by lazy {
            builderClass.getDeclaredMethod("build").apply {
                isAccessible = true
            }
        }

        fun create(
            modulePath: String,
            metrics: DisplayMetrics,
            configuration: Configuration,
        ): Resources {
            val loader = ResourcesLoader().apply {
                addProvider(createProvider(modulePath))
            }
            val builder = try {
                builderConstructor.newInstance()
            } catch (e: ReflectiveOperationException) {
                throw IllegalStateException("Cannot instantiate AssetManager.Builder", e)
            }

            return try {
                builderAddLoaderMethod.invoke(builder, loader)
                val assetManager = builderBuildMethod.invoke(builder) as? AssetManager
                    ?: error("AssetManager.Builder.build() returned null")
                Resources(assetManager, metrics, configuration)
            } catch (e: ReflectiveOperationException) {
                throw IllegalStateException("Failed to build AssetManager with ResourcesLoader", e)
            }
        }

        private fun createProvider(modulePath: String): ResourcesProvider {
            val moduleFile = File(modulePath)
            require(moduleFile.exists()) { "Module apk does not exist: $modulePath" }

            return try {
                ParcelFileDescriptor.open(moduleFile, ParcelFileDescriptor.MODE_READ_ONLY).use {
                    ResourcesProvider.loadFromApk(it)
                }
            } catch (e: IOException) {
                throw IllegalStateException("Failed to load module resources from $modulePath", e)
            }
        }
    }
}
