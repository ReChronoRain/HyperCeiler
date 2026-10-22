package android.util
object Log {
    val lines = mutableListOf<String>()
    fun i(tag: String, message: String): Int { lines += message; return 0 }
}
