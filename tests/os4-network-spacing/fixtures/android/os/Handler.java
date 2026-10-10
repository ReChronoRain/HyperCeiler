package android.os;
import java.util.HashMap;
import java.util.ArrayDeque;
public class Handler {
    public final HashMap<Integer, Long> pending = new HashMap<>();
    public final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
    public final Looper looper = new Looper();
    public int sends;
    public int removes;
    public boolean rejectPost;
    public boolean throwPost;
    public Runnable onHas;
    public Looper getLooper() { return looper; }
    public boolean post(Runnable body) {
        if(throwPost) throw new IllegalStateException("injected post failure");
        if(rejectPost) return false;
        tasks.add(body); return true;
    }
    public void drain() {
        int n=0;
        while(!tasks.isEmpty()) { if(++n>100) throw new AssertionError("unbounded task queue"); looper.run(tasks.remove()); }
    }
    public boolean hasMessages(int what) {
        boolean present = pending.containsKey(what);
        if(onHas!=null) { Runnable r=onHas; onHas=null; r.run(); }
        return present;
    }
    public void removeMessages(int what) { removes++; pending.remove(what); }
    public boolean sendEmptyMessageDelayed(int what, long delay) { sends++; pending.put(what, delay); return true; }
}
