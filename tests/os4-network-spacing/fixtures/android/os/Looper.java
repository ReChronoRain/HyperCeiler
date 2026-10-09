package android.os;
public class Looper {
    private static final ThreadLocal<Looper> CURRENT = new ThreadLocal<>();
    public static Looper myLooper() { return CURRENT.get(); }
    public void run(Runnable body) {
        Looper previous = CURRENT.get(); CURRENT.set(this);
        try { body.run(); } finally { if(previous==null) CURRENT.remove(); else CURRENT.set(previous); }
    }
}
