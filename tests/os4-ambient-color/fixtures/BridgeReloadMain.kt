import android.os.Looper
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.AmbientNativeColorOS4 as N
import java.lang.reflect.Modifier
import java.util.concurrent.*

fun main(args: Array<String>) {
    Looper.getMainLooper()
    var assertions = 0
    fun ck(value: Boolean) { check(value); assertions++ }
    val state = N::class.java.getDeclaredField("loadState").apply { isAccessible = true }
    for (name in listOf("pauseForHotReload", "resumeAfterRejectedHotReload", "ensureLoaded")) {
        val method = N::class.java.getDeclaredMethod(name)
        ck(Modifier.isStatic(method.modifiers) && Modifier.isSynchronized(method.modifiers))
    }
    val executor = Executors.newSingleThreadExecutor()
    try {
        lateinit var pending: Future<Int?>
        val entered = CountDownLatch(1)
        synchronized(N::class.java) {
            pending = executor.submit(Callable {
                entered.countDown()
                N.resolve(intArrayOf(123), 1, 1)
            })
            ck(entered.await(2, TimeUnit.SECONDS))
            // Wait for the worker to block on the actual shared Class monitor.
            try { pending.get(100, TimeUnit.MILLISECONDS); error("load bypassed shared monitor") }
            catch (_: TimeoutException) { assertions++ }
            ck(state.getInt(null) == 0)
            ck(N.pauseForHotReload())
        }
        ck(pending.get(2, TimeUnit.SECONDS) == null && state.getInt(null) == 0)
        ck(executor.submit(Callable { N.resolve(intArrayOf(123), 1, 1) }).get() == null)
        N.resumeAfterRejectedHotReload()
        val first = executor.submit(Callable { N.resolve(intArrayOf(0x11223344), 1, 1) }).get()
        ck(first == null)
        if (args.single() == "loaded") {
            // JNI returns unavailable, but ART has already associated the DSO.
            ck(state.getInt(null) == 1)
            ck(!N.pauseForHotReload())
            ck(executor.submit(Callable { N.resolve(intArrayOf(123), 1, 1) }).get() == 123)
            N.resumeAfterRejectedHotReload()
            ck(!N.pauseForHotReload() && state.getInt(null) == 1)
        } else {
            ck(state.getInt(null) == -1)
            ck(N.pauseForHotReload())
            ck(executor.submit(Callable { N.resolve(intArrayOf(123), 1, 1) }).get() == null)
            N.resumeAfterRejectedHotReload()
            ck(state.getInt(null) == -1 && N.pauseForHotReload())
        }
    } finally { executor.shutdownNow() }
    println("PASS reload checks=$assertions")
}
