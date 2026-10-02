package com.sevtinge.hyperceiler.libhook.rules.home.dock;
import java.util.concurrent.*; import java.util.concurrent.atomic.AtomicInteger;
public final class DockRevealStyleRefreshGateTest {
 static void check(boolean v,String n){if(!v)throw new AssertionError(n);}
 static DockRevealStyleRefreshGate gate(){return new DockRevealStyleRefreshGate(30000);}
 public static void main(String[] args)throws Exception {
  DockRevealStyleRefreshGate g=gate();check(g.request(0,false),"boot zero uptime reads immediately");g.begin(0);check(!g.finish(),"no pending write");
  check(!g.request(100,false),"ordinary traversal throttled");check(!g.request(29999,false),"fallback limit maintained");check(g.request(30000,false),"fallback expires");g.begin(30000);g.finish();
  check(g.request(30001,true),"new launcher/style event bypasses fallback");
  for(int i=0;i<1000;i++)check(!g.request(30001,true),"queued burst coalesces");g.begin(30002);check(!g.finish(),"pre-read writes covered by query");
  check(g.request(30003,true),"first concurrent write queues");g.begin(30003);
  ExecutorService pool=Executors.newFixedThreadPool(4);AtomicInteger accepted=new AtomicInteger();for(int i=0;i<1000;i++)pool.execute(()->{if(g.request(30004,true))accepted.incrementAndGet();});pool.shutdown();check(pool.awaitTermination(10,TimeUnit.SECONDS),"concurrent callbacks complete");check(accepted.get()==0,"inflight events do not flood worker");
  check(g.finish(),"write during read survives");check(g.request(30005,false),"follow-up bypasses limit");g.begin(30005);check(!g.finish(),"follow-up settles");
  check(!g.request(30006,false),"no repeated polling after settle");
  DockRevealStyleRefreshGate failed=gate();check(failed.request(1,true),"queue failure case");failed.begin(1);failed.finish();check(failed.request(2,true),"query failure releases latch");failed.finish();check(failed.request(3,true),"rejected post releases latch");
  System.out.println("Dock style refresh gate: boot, throttle, restart, burst, 1000 concurrent events, follow-up and failure recovery passed");
 }
}
