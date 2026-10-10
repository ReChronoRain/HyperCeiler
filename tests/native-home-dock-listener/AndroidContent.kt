package android.content

interface SharedPreferences {
    fun interface OnSharedPreferenceChangeListener {
        fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String?)
    }
    fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener)
}

class Context {
    val packageManager = PackageManager()
    val contentResolver = ContentResolver()
}
class PackageManager {
    fun getApplicationInfo(packageName: String, flags: Int) = ApplicationInfo()
}
class ApplicationInfo { val nativeLibraryDir: String? = null }
class ContentResolver {
    fun call(uri: android.net.Uri, method: String, argument: String?, extras: android.os.Bundle) = Unit
}
