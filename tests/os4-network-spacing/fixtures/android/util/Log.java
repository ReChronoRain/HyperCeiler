package android.util;
public class Log {
    public static int warnings;
    public static int w(String tag, String message, Throwable failure) { warnings++; return 0; }
}
