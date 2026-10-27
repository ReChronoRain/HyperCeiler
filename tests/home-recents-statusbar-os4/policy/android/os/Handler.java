package android.os;
import java.util.*;
public class Handler {
 private final List<Runnable> queue=new ArrayList<>(),timers=new ArrayList<>();
 public boolean post(Runnable r){queue.add(r);return true;}
 public boolean postDelayed(Runnable r,long ms){timers.add(r);return true;}
 public void removeCallbacks(Runnable r){queue.removeIf(x->x==r);timers.removeIf(x->x==r);}
 public void drain(){int n=0;while(!queue.isEmpty()){if(n++>100)throw new AssertionError("loop");queue.remove(0).run();}}
 public void expire(){var list=new ArrayList<>(timers);timers.clear();list.forEach(Runnable::run);drain();}
}