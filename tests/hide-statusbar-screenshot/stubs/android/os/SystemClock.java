package android.os;

public class SystemClock {
    public static long uptimeMillis() {
        return System.nanoTime() / 1_000_000L;
    }
}
