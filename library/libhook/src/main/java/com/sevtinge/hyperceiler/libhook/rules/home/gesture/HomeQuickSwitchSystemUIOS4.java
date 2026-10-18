/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import android.app.Application;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcel;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Coexists at the platform receiver and Shell cleanup boundary, not another module's hook entry. */
public final class HomeQuickSwitchSystemUIOS4 extends BaseHook {
    private static final String TAG_LOG="HyperCeiler.QuickSystemUI";
    private static final Uri ROOT=Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs");
    private final Object guard=new Object();
    private final Map<Object,HomeQuickSwitchState> receivers=new WeakHashMap<>();
    private final Map<Object,HomeQuickSwitchState> trackers=new IdentityHashMap<>();
    private final ThreadLocal<ArrayDeque<HomeQuickSwitchState>> dispatch=new ThreadLocal<>();
    private final ThreadLocal<ArrayDeque<HomeQuickSwitchState>> finishing=new ThreadLocal<>();
    private static final HomeQuickSwitchState EMPTY=new HomeQuickSwitchState(0,0,0,0,0,0,0,1,1);
    private Method finish, eventDisplay, navTask;
    private Field navigation;
    private Field[] trackerFields;
    private Class<?> trackerClass;
    private volatile boolean closed, enabled;
    private volatile long revision;
    private Context context;
    private Handler worker;

    @Override protected boolean useDexKit() { return true; }
    @Override protected boolean initDexKit() {
        // Shell is an active shared library on this build, not part of SystemUI's base APK.
        // Enumerate PackageManager's installed source set; no vendor jar/class/path is pinned.
        try {
            Context system=ContextUtils.getContextNoError(ContextUtils.FlAG_ONLY_ANDROID);
            if(system==null)throw new IllegalStateException("package context not ready");
            android.content.pm.ApplicationInfo app=system.getPackageManager().getApplicationInfo(
                "com.android.systemui",android.content.pm.PackageManager.GET_SHARED_LIBRARY_FILES);
            java.util.Set<String> sources=new java.util.LinkedHashSet<>();sources.add(app.sourceDir);
            if(app.splitSourceDirs!=null)java.util.Collections.addAll(sources,app.splitSourceDirs);
            if(app.sharedLibraryFiles!=null)java.util.Collections.addAll(sources,app.sharedLibraryFiles);
            ClassLoader loader=io.github.lingqiqi5211.ezhooktool.xposed.EzXposed.getSafeClassLoader();
            for(String source:sources)try(org.luckypray.dexkit.DexKitBridge bridge=org.luckypray.dexkit.DexKitBridge.create(source)) {
                for(var candidate:bridge.findMethod(FindMethod.create().matcher(MethodMatcher.create()
                    .addInvoke("Landroid/window/BackNavigationInfo;->onBackNavigationFinished(Z)V")
                    .paramTypes(boolean.class).returnType(void.class)))) {
                    Method method=candidate.getMethodInstance(loader);
                    if(finish!=null&&!finish.equals(method))throw new IllegalStateException("ambiguous original back cleanup");
                    finish=method;
                }
            }
            if(finish==null)throw new NoSuchMethodException("original back cleanup contract");
            return true;
        }catch(Exception ex){log("source-set resolution declined",ex);return false;}
    }
    private static Field onlyField(Class<?> owner,Class<?> type) throws Exception {
        Field found=null;
        for(Class<?> c=owner;c!=null;c=c.getSuperclass())for(Field f:c.getDeclaredFields()) {
            if(Modifier.isStatic(f.getModifiers())||f.getType()!=type)continue;
            if(found!=null)throw new IllegalStateException("ambiguous platform field "+type);
            f.setAccessible(true);found=f;
        }
        if(found==null)throw new NoSuchFieldException(type.getName());return found;
    }
    private static void push(ThreadLocal<ArrayDeque<HomeQuickSwitchState>> local,HomeQuickSwitchState s) {
        ArrayDeque<HomeQuickSwitchState> q=local.get();if(q==null){q=new ArrayDeque<>();local.set(q);}q.push(s==null?EMPTY:s);
    }
    private static HomeQuickSwitchState pop(ThreadLocal<ArrayDeque<HomeQuickSwitchState>> local) {
        ArrayDeque<HomeQuickSwitchState> q=local.get();return q==null||q.isEmpty()?EMPTY:q.pop();
    }
    private static boolean failed(HookParam p) { return p.getThrowable()!=null; }
    @Override public void init() {
        try {bind();}catch(Exception ex){closed=true;log("platform binding declined",ex);}
    }
    private void bind() throws Exception {
        if(!"com.android.systemui".equals(Application.getProcessName()))return;
        Class<?> receiver=Class.forName("android.view.InputEventReceiver");
        Class<?> monitor=Class.forName("android.view.InputMonitor");
        trackerClass=Class.forName("android.window.BackTouchTracker");
        Class<?> info=Class.forName("android.window.BackNavigationInfo");
        eventDisplay=InputEvent.class.getDeclaredMethod("getDisplayId");
        navTask=info.getDeclaredMethod("getFocusedTaskId");
        navigation=onlyField(finish.getDeclaringClass(),info);
        trackerFields=java.util.Arrays.stream(finish.getDeclaringClass().getDeclaredFields())
            .filter(f->!Modifier.isStatic(f.getModifiers())&&f.getType()==trackerClass).toArray(Field[]::new);
        if(trackerFields.length!=2)throw new IllegalStateException("expected two framework back trackers");
        for(Field f:trackerFields)f.setAccessible(true);
        Method update=trackerClass.getDeclaredMethod("update",float.class,float.class);
        Method reset=trackerClass.getDeclaredMethod("reset");
        Method input=receiver.getDeclaredMethod("dispatchInputEvent",int.class,InputEvent.class);
        hookMethod(input,new IMethodHook(){
            @Override public void before(HookParam p) {
                HomeQuickSwitchState state=null;
                try {if(!closed&&p.getArgs()[1] instanceof MotionEvent e)state=beforeInput(p.getThisObject(),e);}
                catch(Exception ex){log("input guard declined",ex);}
                push(dispatch,state);
            }
            @Override public void after(HookParam p) {
                HomeQuickSwitchState s=pop(dispatch);
                synchronized(guard){if(s!=EMPTY&&s.pending){if(failed(p))s.retired=true;else s.returned=true;complete(s);}}
                // The receiver owns/recycles the event. NEVER read or restore it here.
            }
        });
        hookMethod(monitor.getDeclaredMethod("pilferPointers"),new IMethodHook(){
            @Override public void after(HookParam p) {
                if(closed||failed(p))return;
                try {
                    // Android 17 moves the InputChannel into the receiver; the monitor's
                    // old channel wrapper is disposed. Bind actual ownership by the unique
                    // InputMonitor-typed member instead, never by a vendor field name.
                    synchronized(guard){for(var entry:receivers.entrySet()) {
                        HomeQuickSwitchState state=entry.getValue();
                        if(state.retired||!state.valid)continue;
                        Field owner;
                        try {owner=onlyField(entry.getKey().getClass(),monitor);}catch(NoSuchFieldException ex){continue;}
                        if(owner.get(entry.getKey())==p.getThisObject()){state.accepted=true;trace("pilfer accepted down="+state.down);}
                    }}
                }catch(Exception ex){log("pilfer guard declined",ex);}
            }
        });
        hookMethod(update,new IMethodHook(){
            @Override public void after(HookParam p) {
                if(closed||failed(p))return;
                synchronized(guard){
                    if(trackers.containsKey(p.getThisObject()))return;
                    HomeQuickSwitchState candidate=null;
                    long now=SystemClock.uptimeMillis();float x=(Float)p.getArgs()[0],y=(Float)p.getArgs()[1];
                    for(HomeQuickSwitchState s:receivers.values()) {
                        if(s.retired||!s.valid||!s.accepted||s.pending||now-s.latest<0||now-s.latest>250
                            ||Math.abs(s.currentX-x)>1||Math.abs(s.currentY-y)>1)continue;
                        if(candidate!=null&&candidate!=s)return;candidate=s;
                    }
                    if(candidate!=null&&candidate.tracker==null&&trackers.size()<16) {
                        candidate.tracker=p.getThisObject();trackers.put(p.getThisObject(),candidate);trace("tracker attached down="+candidate.down);
                    }
                }
            }
        });
        hookMethod(reset,new IMethodHook(){
            @Override public void after(HookParam p) {
                synchronized(guard){HomeQuickSwitchState s=trackers.get(p.getThisObject());
                    if(s==null)return;if(failed(p))s.retired=true;else s.reset=true;if(s.pending)trace("original tracker reset down="+s.down);complete(s);
                    if(!s.pending||s.retired||s.sent){trackers.remove(p.getThisObject());if(!s.pending){s.tracker=null;s.reset=false;}}}
            }
        });
        hookMethod(finish,new IMethodHook(){
            @Override public void before(HookParam p) {
                HomeQuickSwitchState found=null;
                try {if(!closed&&Boolean.FALSE.equals(p.getArgs()[0]))synchronized(guard){
                    Object nav=navigation.get(p.getThisObject());
                    if(nav!=null)for(Field f:trackerFields){HomeQuickSwitchState s=trackers.get(f.get(p.getThisObject()));
                        if(s==null||!s.pending||s.retired)continue;
                        if(found!=null&&found!=s){found=null;break;}
                        s.navigation=nav;s.expected=(Integer)navTask.invoke(nav);found=s;trace("original finish down="+s.down+" expected="+s.expected);
                    }
                }}catch(Exception ex){found=null;log("cleanup identity declined",ex);}
                push(finishing,found);
            }
            @Override public void after(HookParam p) {
                HomeQuickSwitchState s=pop(finishing);if(s==EMPTY)return;
                synchronized(guard){try {
                    if(failed(p)||navigation.get(p.getThisObject())!=null)s.retired=true;else s.finished=true;
                    complete(s);
                }catch(Exception ex){s.retired=true;log("cleanup guard declined",ex);}}
            }
        });
        hookMethod(android.app.Instrumentation.class.getDeclaredMethod("callApplicationOnCreate",Application.class),new IMethodHook(){
            @Override public void after(HookParam p){install((Context)p.getArgs()[0]);}
        });
        Context current=ContextUtils.getContextNoError(ContextUtils.FLAG_CURRENT_APP);if(current!=null)install(current);
        registerHotReloadCleanup(()->{closed=true;enabled=false;synchronized(guard){receivers.clear();trackers.clear();}});
        log("platform receiver / typed trackers / dynamic original cleanup bound "+finish,null);
    }
    private HomeQuickSwitchState beforeInput(Object receiver,MotionEvent e) throws Exception {
        if(!enabled||!e.isFromSource(InputDevice.SOURCE_TOUCHSCREEN))return null;
        synchronized(guard) {
            HomeQuickSwitchState s=receivers.get(receiver);int action=e.getActionMasked();
            if(action==0) {
                if(s==null&&receivers.size()>=64)return null;
                if(s!=null){s.retired=true;if(s.tracker!=null)trackers.remove(s.tracker);}
                var dm=context.getResources().getDisplayMetrics();
                s=new HomeQuickSwitchState(e.getDownTime(),revision,e.getDeviceId(),(Integer)eventDisplay.invoke(e),e.getSource(),
                    e.getRawX(),e.getRawY(),dm.density,dm.widthPixels);receivers.put(receiver,s);
            }
            if(s==null)return null;
            if(s.down!=e.getDownTime()||s.device!=e.getDeviceId()||s.display!=(Integer)eventDisplay.invoke(e)||s.source!=e.getSource())s.valid=false;
            s.sample(action,e.getPointerCount(),e.getRawX(),e.getRawY(),e.getEventTime());
            if(action==1&&s.accepted)trace("release valid="+s.valid+" tracker="+(s.tracker!=null)+" hold="+(s.latest-s.lastMove));
            if(s.claim(revision,enabled,action)) {
                // Preserve the original event sequence/token for finishInputEvent. Mutate this
                // receiver's owned event before dispatch, let its normal CANCEL path run once.
                try {e.setAction(MotionEvent.ACTION_CANCEL);}catch(Exception ex){s.retired=true;throw ex;}
                log("held release -> original CANCEL down="+s.down,null);
            }
            return s;
        }
    }
    private synchronized void install(Context app) {
        if(closed||worker!=null||!"com.android.systemui".equals(app.getPackageName()))return;
        context=app;HandlerThread thread=new HandlerThread("HC-QuickSwitch");thread.start();worker=new Handler(thread.getLooper());
        ContentObserver observer=new ContentObserver(worker){@Override public void onChange(boolean self){refresh();}};
        app.getContentResolver().registerContentObserver(ROOT,true,observer);worker.post(this::refresh);
        registerHotReloadCleanup(()->{app.getContentResolver().unregisterContentObserver(observer);thread.quitSafely();});
    }
    private void refresh() {
        boolean value=false;
        try(Cursor c=context.getContentResolver().query(Uri.withAppendedPath(ROOT,"home_gestures"),null,null,null,null)) {
            if(c!=null&&c.moveToFirst()&&c.getInt(0)==1){int i=c.getColumnIndex("home_navigation_quick_back");value=i>=0&&!c.isNull(i)&&c.getInt(i)==1;}
        }catch(Exception ex){log("snapshot declined",ex);}
        synchronized(guard){revision++;enabled=value;for(HomeQuickSwitchState s:receivers.values())s.retired=true;trackers.clear();receivers.clear();}
        log("config quick="+value+" revision="+revision,null);
    }
    private void complete(HomeQuickSwitchState s) {
        if(closed||worker==null||!s.complete(SystemClock.uptimeMillis(),revision,enabled))return;
        worker.post(()->send(s));
    }
    private void send(HomeQuickSwitchState s) {
        if(closed||!enabled||s.retired||s.revision!=revision||SystemClock.uptimeMillis()-s.released>750)return;
        Parcel data=Parcel.obtain(),reply=Parcel.obtain();
        try {
            IBinder window=(IBinder)Class.forName("android.os.ServiceManager").getDeclaredMethod("getService",String.class).invoke(null,"window");
            if(window==null)return;
            data.writeInterfaceToken("android.view.IWindowManager");data.writeInt(7);data.writeLong(s.released);data.writeInt(s.expected);
            boolean ok=window.transact(HomeQuickSwitchOS4.TRANSACTION,data,reply,0)
                &&reply.readInt()==HomeQuickSwitchOS4.ACK&&reply.readInt()==1;
            log("after original cleanup expected="+s.expected+" accepted="+ok,null);
        }catch(Exception ex){log("previous task dispatch failed",ex);}finally{data.recycle();reply.recycle();}
    }
    private static void trace(String msg) {
        if(android.os.SystemProperties.getBoolean("debug.hyperceiler.gesture_probe",false))log(msg,null);
    }
    private static void log(String msg,Exception ex) {
        if(ex!=null)android.util.Log.w(TAG_LOG,msg,ex);else android.util.Log.i(TAG_LOG,msg);
    }
}
