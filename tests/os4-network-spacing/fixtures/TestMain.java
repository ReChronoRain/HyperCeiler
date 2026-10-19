package fixture;

import android.os.Message;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import com.android.systemui.statusbar.policy.NetworkSpeedController;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.network.NetworkSpeedSpacing;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
public class TestMain {
    static int passes;
    static int failures;
    static void test(String name, Runnable body) {
        try { body.run(); passes++; System.out.println("PASS "+name); }
        catch(Throwable t) { failures++; System.out.println("FAIL "+name+" "+t); }
    }
    static void equal(Object got,Object want) { if (!java.util.Objects.equals(got,want)) throw new AssertionError("got="+got+" expected="+want); }
    static void init(boolean legacy) {
        SystemClock.now=0; Log.warnings=0; PrefsBridge.value=15; PrefsBridge.fail=false; PrefsBridge.onRead=null; BaseHook.failedField=null;
        BaseHook.legacy=legacy; BaseHook.registered=null; new NetworkSpeedSpacing().init();
        equal(BaseHook.hookedName,legacy ? "postUpdateNetworkSpeedDelay" : "handleMessage");
    }
    static HookParam begin(NetworkSpeedController.CombinedHandler h,int what) {
        h.pending.remove(what); // Dispatch removes the current message before handleMessage.
        HookParam p=new HookParam(h,new Message(what));
        h.looper.run(()->BaseHook.registered.before(p)); return p;
    }
    static void original(HookParam p) {
        var h=(NetworkSpeedController.CombinedHandler)p.getThisObject();
        h.looper.run(()->h.handleMessage((Message)p.getArgs()[0]));
    }
    static void finish(HookParam p) {
        var h=(NetworkSpeedController.CombinedHandler)p.getThisObject();
        // Real EzHook allocates a different wrapper over the SAME context args array.
        HookParam after=new HookParam(h,p.getArgs()); after.failed=p.failed; after.skipped=p.skipped;
        h.looper.run(()->BaseHook.registered.after(after));
    }
    static void invoke(NetworkSpeedController.CombinedHandler h,int what) {
        HookParam p=begin(h,what); original(p); finish(p);
    }
    static void periodic(NetworkSpeedController c) { invoke(c.mBgHandler,200001); }
    static void visibility(NetworkSpeedController c,boolean visible,int what) {
        c.mHandler.looper.run(()->{ c.mNetworkSpeedState.visible=visible; invoke(c.mHandler,what); });
    }
    static void delay(NetworkSpeedController c,long want) { equal(c.mBgHandler.pending.get(200001),want); }
    static void empty(NetworkSpeedController c) { equal(c.mBgHandler.pending.size(),0); }
    static void join(Thread thread) {
        try { thread.join(2000); } catch(InterruptedException e) { throw new AssertionError(e); }
        if(thread.isAlive()) throw new AssertionError("BG blocked while reading preferences");
    }
    static void await(java.util.concurrent.CountDownLatch latch) {
        try { if(!latch.await(2,java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("latch timeout"); }
        catch(InterruptedException e) { throw new AssertionError(e); }
    }
    private static void testSchedulingBasics() {
        test("OS4 fallback selects real Handler method",()->init(false));
        for(int value:new int[]{10,15,40,100}) test("visible periodic delay "+value,()->{
            init(false); PrefsBridge.value=value; var c=new NetworkSpeedController(); periodic(c); c.mHandler.drain();
            delay(c,value*100L); equal(c.mBgHandler.pending.size(),1);
        });
        test("hidden status bar does not resurrect refresh",()->{
            init(false); var c=new NetworkSpeedController(); c.mIsStatusBarHidden=true;
            periodic(c); c.mHandler.drain(); empty(c); equal(c.mBgHandler.sends,0);
        });
        test("system cancellation before after-hook is respected",()->{
            init(false); var c=new NetworkSpeedController(); var p=begin(c.mBgHandler,200001); original(p);
            c.mBgHandler.removeMessages(200001); int sends=c.mBgHandler.sends;
            finish(p); c.mHandler.drain(); empty(c); equal(c.mBgHandler.sends,sends);
        });
        test("merged UI Handler never rewrites background queue for 200001",()->{
            init(false); var c=new NetworkSpeedController(); c.mBgHandler.sendEmptyMessageDelayed(200001,2300L);
            invoke(c.mHandler,200001); c.mHandler.drain(); delay(c,2300L); equal(c.mBgHandler.sends,1);
        });
        test("unrelated background message preserves queue",()->{
            init(false); var c=new NetworkSpeedController(); c.mBgHandler.sendEmptyMessageDelayed(200001,2700L);
            invoke(c.mBgHandler,100004); c.mHandler.drain(); delay(c,2700L);
        });
        test("different controller queues and generations stay separate",()->{
            init(false); var first=new NetworkSpeedController(); var second=new NetworkSpeedController();
            first.mBgHandler.sendEmptyMessageDelayed(200001,2700L); periodic(second);
            visibility(first,false,100002); second.mHandler.drain(); empty(first); delay(second,1500L);
        });
        for(int value:new int[]{Integer.MIN_VALUE,-1,0,9,101,Integer.MAX_VALUE}) test("invalid stored interval falls back "+value,()->{
            init(false); PrefsBridge.value=value; var c=new NetworkSpeedController(); periodic(c); c.mHandler.drain(); delay(c,4000L);
        });
        for(long value:new long[]{4000L,0L,123L}) test("legacy argument "+value,()->{
            init(true); HookParam p=new HookParam(new BaseHook.LegacyController(),value); BaseHook.registered.before(p);
            equal(p.getArgs()[0],value==4000L ? 1500L : value);
        });
        test("legacy invalid setting cannot create fast loop",()->{
            init(true); PrefsBridge.value=0; HookParam p=new HookParam(new BaseHook.LegacyController(),4000L);
            BaseHook.registered.before(p); equal(p.getArgs()[0],4000L);
        });
    }

    private static void testCancellationAndRestart() {
        test("BG completion leaves original timer until UI runnable",()->{
            init(false); var c=new NetworkSpeedController(); periodic(c); delay(c,4000L); equal(c.mHandler.tasks.size(),1);
        });
        test("unverified separate UI and BG Handler classes retain system scheduling",()->{
            init(false); var c=new NetworkSpeedController(true); periodic(c); c.mHandler.drain(); delay(c,4000L);
            equal(c.mHandler.tasks.size(),0);
        });
        test("UI cancellation after completed BG stays canceled",()->{
            init(false); var c=new NetworkSpeedController(); periodic(c); visibility(c,false,100002); c.mHandler.drain(); empty(c);
        });
        test("UI cancellation racing original BG enqueue is cleaned up",()->{
            init(false); var c=new NetworkSpeedController(); var p=begin(c.mBgHandler,200001);
            visibility(c,false,100002); original(p); finish(p); c.mHandler.drain(); empty(c);
        });
        test("cancel requested inside hasMessages is serialized with UI retime",()->{
            init(false); var c=new NetworkSpeedController(); c.mBgHandler.onHas=()->{
                Runnable cancel=()->visibility(c,false,100002);
                if(Looper.myLooper()==c.mHandler.getLooper()) c.mHandler.post(cancel);
                else c.mHandler.looper.run(cancel);
            };
            periodic(c); c.mHandler.drain(); empty(c);
        });
        for(int what:new int[]{100001,100002,100003,100005}) test("UI control generation preserves immediate restart "+what,()->{
            init(false); var c=new NetworkSpeedController(); periodic(c); visibility(c,false,what); visibility(c,true,what);
            c.mHandler.drain(); delay(c,0L);
        });
        test("restart after BG original but before BG after is not delayed",()->{
            init(false); var c=new NetworkSpeedController(); var p=begin(c.mBgHandler,200001); original(p);
            visibility(c,false,100002); visibility(c,true,100002); finish(p); c.mHandler.drain(); delay(c,0L);
        });
        test("original ROM overwriting a concurrent restart retains ROM delay",()->{
            init(false); var c=new NetworkSpeedController(); var p=begin(c.mBgHandler,200001);
            visibility(c,false,100002); visibility(c,true,100002); delay(c,0L);
            original(p); delay(c,4000L); finish(p); c.mHandler.drain(); delay(c,4000L);
        });
        test("BG begun inside UI control is invalidated by UI after",()->{
            init(false); var c=new NetworkSpeedController(); var ui=begin(c.mHandler,100002);
            periodic(c); original(ui); finish(ui); c.mHandler.drain(); delay(c,0L);
        });
        test("new immediate restart cycle may acquire custom periodic delay",()->{
            init(false); var c=new NetworkSpeedController(); periodic(c); visibility(c,false,100002); visibility(c,true,100002);
            periodic(c); c.mHandler.drain(); delay(c,1500L);
        });
        test("sample UI message does not invalidate periodic delay",()->{
            init(false); var c=new NetworkSpeedController(); periodic(c); invoke(c.mHandler,100004); c.mHandler.drain(); delay(c,1500L);
        });
    }

    private static void testQueueRaces() {
        test("UI backlog coalesces and uses latest completed cycle",()->{
            init(false); var c=new NetworkSpeedController(); periodic(c); SystemClock.now=4000; periodic(c);
            equal(c.mHandler.tasks.size(),1); SystemClock.now=4500; c.mHandler.drain(); delay(c,1000L);
        });
        test("UI waiting time is subtracted from desired interval",()->{
            init(false); var c=new NetworkSpeedController(); periodic(c); SystemClock.now=2000; c.mHandler.drain(); delay(c,0L);
        });
        test("old UI task yields to running BG cycle and completion retries",()->{
            init(false); var c=new NetworkSpeedController(); periodic(c); var p=begin(c.mBgHandler,200001);
            c.mHandler.drain(); empty(c); original(p); finish(p); equal(c.mHandler.tasks.size(),1); c.mHandler.drain(); delay(c,1500L);
        });
    }

    private static void testFailureRecovery() {
        for(boolean throwing:new boolean[]{false,true}) test("failed UI post retains original and later retries "+throwing,()->{
            init(false); var c=new NetworkSpeedController(); c.mHandler.rejectPost=!throwing; c.mHandler.throwPost=throwing;
            periodic(c); delay(c,4000L); equal(c.mHandler.tasks.size(),0);
            c.mHandler.rejectPost=false; c.mHandler.throwPost=false; periodic(c); c.mHandler.drain(); delay(c,1500L);
        });
        test("preference exception leaves system timer and does not wedge posting",()->{
            init(false); var c=new NetworkSpeedController(); periodic(c); PrefsBridge.fail=true; c.mHandler.drain(); delay(c,4000L);
            equal(Log.warnings,1); PrefsBridge.fail=false; periodic(c); c.mHandler.drain(); delay(c,1500L);
        });
        test("visibility field exception leaves original timer",()->{
            init(false); var c=new NetworkSpeedController(); periodic(c); BaseHook.failedField="visible"; c.mHandler.drain(); delay(c,4000L);
            BaseHook.failedField=null; periodic(c); c.mHandler.drain(); delay(c,1500L);
        });
        for(boolean skipped:new boolean[]{false,true}) test("failed or skipped original releases running count "+skipped,()->{
            init(false); var c=new NetworkSpeedController(); var p=begin(c.mBgHandler,200001); p.failed=!skipped; p.skipped=skipped;
            finish(p); equal(c.mHandler.tasks.size(),0); empty(c); periodic(c); c.mHandler.drain(); delay(c,1500L);
        });
        test("unpaired after does not retime pending message",()->{
            init(false); var c=new NetworkSpeedController(); c.mBgHandler.sendEmptyMessageDelayed(200001,2900L);
            BaseHook.registered.after(new HookParam(c.mBgHandler,new Message(200001))); c.mHandler.drain(); delay(c,2900L);
        });
        test("nested skipped-before after does not consume caller frame",()->{
            init(false); var c=new NetworkSpeedController(); var outer=begin(c.mBgHandler,200001);
            BaseHook.registered.after(new HookParam(c.mBgHandler,new Message(200001)));
            original(outer); finish(outer); c.mHandler.drain(); delay(c,1500L);
        });
        test("runnable on wrong Looper leaves original queue untouched",()->{
            init(false); var c=new NetworkSpeedController(); periodic(c); equal(c.mHandler.tasks.size(),1);
            c.mHandler.tasks.remove().run(); delay(c,4000L);
        });
        test("reflection failure before original does not break original dispatch",()->{
            init(false); var c=new NetworkSpeedController(); BaseHook.failedField="mHandler"; periodic(c); delay(c,4000L);
            BaseHook.failedField=null; periodic(c); c.mHandler.drain(); delay(c,1500L);
        });
    }

    private static void testPhysicalThreadRaces() {
        test("new BG completes on another thread during unlocked preference read",()->{
            init(false); var c=new NetworkSpeedController(); periodic(c);
            PrefsBridge.onRead=()->{
                SystemClock.now=500; Thread next=new Thread(()->periodic(c)); next.start(); join(next); SystemClock.now=800;
            };
            c.mHandler.drain(); delay(c,1200L); equal(c.mBgHandler.sends,3); equal(Log.warnings,0);
        });
        test("two physical threads cancel while original BG callback is in flight",()->{
            init(false); var c=new NetworkSpeedController();
            var begun=new java.util.concurrent.CountDownLatch(1); var canceled=new java.util.concurrent.CountDownLatch(1);
            var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
            Thread bg=new Thread(()->{
                try { var p=begin(c.mBgHandler,200001); begun.countDown(); await(canceled); original(p); finish(p); }
                catch(Throwable t) { failure.set(t); }
            });
            bg.start(); await(begun); visibility(c,false,100002); canceled.countDown(); join(bg);
            if(failure.get()!=null) throw new AssertionError(failure.get());
            c.mHandler.drain(); empty(c);
        });
        test("two physical threads preserve restart before old BG after",()->{
            init(false); var c=new NetworkSpeedController();
            var originalDone=new java.util.concurrent.CountDownLatch(1); var restarted=new java.util.concurrent.CountDownLatch(1);
            var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
            Thread bg=new Thread(()->{
                try { var p=begin(c.mBgHandler,200001); original(p); originalDone.countDown(); await(restarted); finish(p); }
                catch(Throwable t) { failure.set(t); }
            });
            bg.start(); await(originalDone); visibility(c,false,100002); visibility(c,true,100002); restarted.countDown(); join(bg);
            if(failure.get()!=null) throw new AssertionError(failure.get());
            c.mHandler.drain(); delay(c,0L);
        });
    }

    public static void main(String[] args) {
        testSchedulingBasics();
        testCancellationAndRestart();
        testQueueRaces();
        testFailureRecovery();
        testPhysicalThreadRaces();
        System.out.println("RESULT passes="+passes+" failures="+failures);
        if(failures!=0) System.exit(1);
    }
}
