package android.os;
/** Clock transport only; a test may advance suspend-inclusive time without sleeping. */
public final class SystemClock {
    public static java.util.function.LongSupplier source = System::nanoTime;
    public static long elapsedRealtimeNanos() { return source.getAsLong(); }
}
