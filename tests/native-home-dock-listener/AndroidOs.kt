package android.os
class Bundle { fun putStringArray(key: String, value: Array<String>) = Unit }
object SystemClock {
    fun sleep(millis: Long) = Thread.sleep(millis)
    fun uptimeMillis() = System.nanoTime() / 1_000_000
}
