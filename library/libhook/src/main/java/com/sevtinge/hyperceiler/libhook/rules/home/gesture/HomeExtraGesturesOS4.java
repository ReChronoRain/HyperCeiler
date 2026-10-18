/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import android.app.ActivityManager;
import android.app.Application;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.InputMonitor;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.provider.HomeGesturePrefsSnapshot;
import com.sevtinge.hyperceiler.libhook.utils.api.ContextUtils;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import java.lang.reflect.Method;

/** Default-display OS4 gestures through a private platform monitor, never a guessed vendor hook. */
public final class HomeExtraGesturesOS4 extends BaseHook {
    private static final String TAG_LOG="HyperCeiler.ExtraGesture";
    private static final Uri ROOT=Uri.parse("content://com.sevtinge.hyperceiler.provider.sharedprefs");
    private final Handler main=new Handler(Looper.getMainLooper());
    private final HomeExtraGestureState.Shake shake=new HomeExtraGestureState.Shake();
    private volatile boolean closed;
    private volatile int mask;
    private volatile long revision;
    private Context context;
    private Handler worker;
    private String fingerprint="";
    private HomeGestureRegionsOS4 regions;
    private HomeExtraGestureState touch;
    private InputMonitor monitor;
    private Receiver receiver;
    private HomeExtraGestureState.Geometry geometry;
    private SensorManager sensors;
    private Sensor accelerometer;
    private boolean sensorRegistered,available,homeForeground;
    private int device,source,display,pointer;
    private Method eventDisplay,monitorInput;

    @Override public void init() {
        if(!"com.android.systemui".equals(Application.getProcessName()))return;
        try {
            hookMethod(Instrumentation.class.getDeclaredMethod("callApplicationOnCreate",Application.class),new IMethodHook(){
                @Override public void after(HookParam p){install((Context)p.getArgs()[0]);}
            });
            Context app=ContextUtils.getContextNoError(ContextUtils.FLAG_CURRENT_APP);if(app!=null)install(app);
            registerHotReloadCleanup(()->{closed=true;main.post(this::dispose);});
        }catch(Exception ex){log("bootstrap declined",ex);}
    }
    private synchronized void install(Context app) {
        if(closed||worker!=null||!"com.android.systemui".equals(app.getPackageName()))return;
        context=app;
        HandlerThread thread=new HandlerThread("HC-ExtraGesture");thread.start();worker=new Handler(thread.getLooper());
        ContentObserver observer=new ContentObserver(worker){@Override public void onChange(boolean self){refresh();}};
        app.getContentResolver().registerContentObserver(ROOT,true,observer);
        registerHotReloadCleanup(()->{app.getContentResolver().unregisterContentObserver(observer);thread.quitSafely();});
        main.post(()->{
            if(closed)return;
            try {
                sensors=app.getSystemService(SensorManager.class);
                accelerometer=sensors==null?null:sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                ViewConfiguration vc=ViewConfiguration.get(app);
                // Reserve a stationary long press before the stock timeout; original monitor
                // owners receive CANCEL, while the first ordinary swipe remains untouched.
                int timeout=Math.max(ViewConfiguration.getTapTimeout()+1,ViewConfiguration.getLongPressTimeout()*4/5);
                touch=new HomeExtraGestureState(vc.getScaledTouchSlop(),ViewConfiguration.getDoubleTapTimeout(),timeout);
                try {
                    regions=new HomeGestureRegionsOS4();
                    eventDisplay=InputEvent.class.getDeclaredMethod("getDisplayId");
                    monitorInput=InputManager.class.getDeclaredMethod("monitorGestureInput",String.class,int.class);
                    monitorInput.setAccessible(true);
                }catch(Exception ex){log("navigation contract declined; sensor remains independent",ex);}
                tick.run();
            }catch(Exception ex){log("platform contract declined",ex);}
        });
        worker.post(this::refresh);worker.post(pollHome);
    }
    private void refresh() {
        int next=0;StringBuilder stamp=new StringBuilder();
        try(Cursor c=context.getContentResolver().query(Uri.withAppendedPath(ROOT,"home_gestures"),null,null,null,null)) {
            if(c==null||!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("gesture snapshot unavailable");
            for(int i=0;i<c.getColumnCount();i++)stamp.append(c.getColumnName(i)).append('=').append(c.isNull(i)?"<null>":c.getString(i)).append('\n');
            int master=c.getColumnIndex("home_gesture_enable");
            if(master>=0&&!c.isNull(master)&&c.getInt(master)==1)for(int i=5;i<HomeGesturePrefsSnapshot.ACTIONS.length;i++) {
                int col=c.getColumnIndex(HomeGesturePrefsSnapshot.ACTIONS[i]+"_action");
                int action=col<0||c.isNull(col)?0:c.getInt(col);
                if(action>0&&action<=100)next|=1<<(i-5);
            }
        }catch(Exception ex){stamp.setLength(0);log("configuration declined",ex);}
        int value=next;String identity=stamp.toString();
        main.post(()->{if(closed||fingerprint.equals(identity))return;fingerprint=identity;mask=value;revision++;
            if(touch!=null)touch.reset();shake.reset();main.removeCallbacks(longPress);main.removeCallbacks(tick);if(touch!=null)tick.run();
            log("config mask="+mask+" revision="+revision,null);});
    }
    private final Runnable pollHome=new Runnable(){@Override public void run(){
        if(closed)return;
        boolean foreground=false;
        try {
            if((mask&1)!=0) {
            var home=context.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.MATCH_DEFAULT_ONLY);
            var tasks=context.getSystemService(ActivityManager.class).getRunningTasks(1);
            if(home!=null&&home.activityInfo!=null&&!tasks.isEmpty()) {
                var top=tasks.get(0).topActivity;
                foreground=top!=null&&top.getPackageName().equals(home.activityInfo.packageName)
                    &&top.getClassName().equals(home.activityInfo.targetActivity!=null?home.activityInfo.targetActivity:home.activityInfo.name);
            }
            }
        }catch(Exception ignored){}
        boolean current=foreground;
        main.post(()->{if(closed)return;homeForeground=current;reconcile();});
        worker.postDelayed(this,500);
    }};
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(closed)return;
        try {
            available=mask!=0&&context.getSystemService(PowerManager.class).isInteractive()
                &&!context.getSystemService(KeyguardManager.class).isKeyguardLocked();
            geometry=!available||(mask&30)==0||regions==null?null:regions.current();
            if(!available||geometry==null){touch.reset();main.removeCallbacks(longPress);}
            reconcile();
        }catch(Exception ex){available=false;geometry=null;disposeInput();log("navigation owner declined",ex);}
        main.postDelayed(this,mask==0?1000:200);
    }};
    private void reconcile() {
        if(context==null||touch==null||closed)return;
        boolean input=available&&geometry!=null&&eventDisplay!=null&&monitorInput!=null&&(mask&30)!=0;
        if(input&&monitor==null)try {
            Object created=monitorInput.invoke(context.getSystemService(InputManager.class),"HyperCeiler-OS4-navigation",0);
            if(!(created instanceof InputMonitor value))throw new IllegalStateException("unexpected platform monitor");
            monitor=value;
            try{receiver=new Receiver(value);}catch(Exception ex){value.dispose();monitor=null;throw ex;}
            log("platform input monitor attached",null);
        }catch(Exception ex){log("input monitor declined",ex);}
        else if(!input&&monitor!=null)disposeInput();
        boolean sensor=available&&homeForeground&&(mask&1)!=0&&accelerometer!=null;
        if(sensor&&!sensorRegistered){shake.reset();sensorRegistered=sensors.registerListener(listener,accelerometer,SensorManager.SENSOR_DELAY_GAME,main);}
        else if(!sensor&&sensorRegistered){sensors.unregisterListener(listener);sensorRegistered=false;shake.reset();}
    }
    private final SensorEventListener listener=new SensorEventListener(){
        @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
        @Override public void onSensorChanged(SensorEvent e){
            if(!closed&&available&&homeForeground&&(mask&1)!=0&&e.values.length>=3
                &&shake.sample(e.values[0],e.values[1],e.values[2],e.timestamp/1000000))enqueue(8);
        }
    };
    private final Runnable longPress=()->{
        if(closed||!available||monitor==null||geometry==null)return;
        // Re-resolve the real owner/bounds at timer expiry, not just at DOWN.
        apply(touch.longPress(SystemClock.uptimeMillis(),regions.current(),revision,mask));
    };
    private final class Receiver extends InputEventReceiver {
        Receiver(InputMonitor owner){super(owner.getInputChannel(),main.getLooper());}
        @Override public void onInputEvent(InputEvent input) {
            boolean handled=false;
            try {
                if(!closed&&available&&input instanceof MotionEvent e&&e.isFromSource(InputDevice.SOURCE_TOUCHSCREEN)) {
                    int id=(Integer)eventDisplay.invoke(e);
                    if(e.getActionMasked()==0){
                        if(device!=e.getDeviceId()||source!=e.getSource()||display!=id)touch.reset();
                        device=e.getDeviceId();source=e.getSource();display=id;pointer=e.getPointerId(0);
                        geometry=regions.current();
                    }else if(e.getActionMasked()==1)geometry=regions.current();
                    if(id!=0||id!=display||device!=e.getDeviceId()||source!=e.getSource()||pointer!=e.getPointerId(0)){touch.reset();main.removeCallbacks(longPress);}
                    else {
                        var decision=touch.sample(e.getActionMasked(),e.getPointerCount(),e.getRawX(),e.getRawY(),
                            e.getEventTime(),e.getDownTime(),geometry,revision,mask);
                        handled=apply(decision);
                        main.removeCallbacks(longPress);
                        if(touch.pendingLong())main.postAtTime(longPress,touch.longDeadline());
                    }
                }
            }catch(Exception ex){touch.reset();main.removeCallbacks(longPress);log("input declined",ex);}
            finally {finishInputEvent(input,handled);} // same owned sequence; exactly one finish, never clone/recycle it
        }
    }
    private boolean apply(HomeExtraGestureState.Result decision) {
        if(decision.claim())try {
            if(monitor==null)throw new IllegalStateException("no input owner");
            monitor.pilferPointers();
        }catch(Exception ex){touch.reset();main.removeCallbacks(longPress);log("ownership declined",ex);return false;}
        if(decision.event()!=0)enqueue(decision.event());
        return decision.handled();
    }
    private void enqueue(int event) {
        final long generation=revision,time=SystemClock.uptimeMillis();
        worker.post(()->{
            if(closed||generation!=revision||(mask&(1<<(event-8)))==0||SystemClock.uptimeMillis()-time>750)return;
            Parcel data=Parcel.obtain(),reply=Parcel.obtain();
            try {
                IBinder window=(IBinder)Class.forName("android.os.ServiceManager").getDeclaredMethod("getService",String.class).invoke(null,"window");
                if(window==null)return;
                data.writeInterfaceToken("android.view.IWindowManager");data.writeInt(event);data.writeLong(time);
                boolean ok=window.transact(HomeQuickSwitchOS4.TRANSACTION,data,reply,0)
                    &&reply.readInt()==HomeQuickSwitchOS4.ACK&&reply.readInt()==1;
                log("action event="+event+" accepted="+ok,null);
            }catch(Exception ex){log("action declined",ex);}finally{data.recycle();reply.recycle();}
        });
    }
    private void disposeInput() {
        main.removeCallbacks(longPress);if(touch!=null)touch.reset();
        if(receiver!=null){receiver.dispose();receiver=null;}
        if(monitor!=null){monitor.dispose();monitor=null;}
    }
    private void dispose() {
        main.removeCallbacksAndMessages(null);disposeInput();
        if(sensorRegistered&&sensors!=null)sensors.unregisterListener(listener);sensorRegistered=false;shake.reset();
    }
    private static void log(String message,Exception ex){if(ex==null)android.util.Log.i(TAG_LOG,message);else android.util.Log.w(TAG_LOG,message,ex);}
}
