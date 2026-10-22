package android.content
class Context { val started = mutableListOf<Intent>(); fun startActivity(intent: Intent) { started += intent } }
class Intent(val action: String) {
    companion object {
        const val ACTION_MAIN = "android.intent.action.MAIN"
        const val FLAG_ACTIVITY_CLEAR_TASK = 0x8000
        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
    }
    var flags = 0; var targetPackage = ""; var targetClass = ""
    val extras = mutableMapOf<String, Any?>()
    fun addFlags(value: Int): Intent { flags = flags or value; return this }
    fun setClassName(pkg: String, clazz: String): Intent { targetPackage=pkg; targetClass=clazz; return this }
    fun putExtra(key: String, value: Any?): Intent { extras[key]=value; return this }
}
