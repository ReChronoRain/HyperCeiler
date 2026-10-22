package android.os

class Looper {
    companion object { fun getMainLooper() = Looper() }
}
class Handler(@Suppress("UNUSED_PARAMETER") looper: Looper) {
    fun postDelayed(runnable: Runnable, delay: Long): Boolean {
        pending += runnable to delay
        return true
    }
    companion object {
        val pending = mutableListOf<Pair<Runnable, Long>>()
        fun drain() {
            val tasks = pending.toList()
            pending.clear()
            tasks.forEach { it.first.run() }
        }
    }
}
