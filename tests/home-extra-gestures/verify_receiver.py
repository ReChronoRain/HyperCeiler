"""Exercise production Receiver/apply against platform receiver/monitor/Looper stubs."""
from pathlib import Path
import sys,subprocess,json
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
J=R/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/gesture';D=W/'handover/gesture-extra-os4-20261017'/('receiver-'+R.name);D.mkdir(parents=True,exist_ok=True)
if not (J/'HomeExtraGesturesOS4.java').exists():print('EXTRA_RECEIVER=missing implementation; failed=1');sys.exit(1)
s=(J/'HomeExtraGesturesOS4.java').read_text()
def block(start):
 a=s.index(start);b=s.index('{',a);depth=1;i=b+1
 while depth:
  if s[i]=='{':depth+=1
  elif s[i]=='}':depth-=1
  i+=1
 return s[a:i]
code=r'''package com.sevtinge.hyperceiler.libhook.rules.home.gesture;
import java.lang.reflect.*;import java.util.*;
public class ReceiverTest {
 static int checks,failed;static void ok(boolean b,String n){checks++;if(!b){failed++;System.out.println("FAIL="+n);}}
 static class InputDevice {static int SOURCE_TOUCHSCREEN=4098;}
 public static class InputEvent {int display;public int getDisplayId(){return display;}}
 static class MotionEvent extends InputEvent {
  int action,count=1,device=7,source=4098,pointer=2;float x,y;long time,down;
  MotionEvent(int a,float x,float y,long t,long d){action=a;this.x=x;this.y=y;time=t;down=d;}
  boolean isFromSource(int s){return (source&s)==s;}int getActionMasked(){return action;}int getPointerCount(){return count;}int getDeviceId(){return device;}int getSource(){return source;}int getPointerId(int i){return pointer;}float getRawX(){return x;}float getRawY(){return y;}long getEventTime(){return time;}long getDownTime(){return down;}
 }
 static class InputMonitor {int pilfers;boolean fail;Object getInputChannel(){return this;}void pilferPointers(){pilfers++;if(fail)throw new IllegalStateException("pilfer failed");}}
 static class InputEventReceiver {int finishes;InputEvent last;boolean handled;InputEventReceiver(Object channel,Object looper){}public void onInputEvent(InputEvent i){}void finishInputEvent(InputEvent i,boolean h){finishes++;last=i;handled=h;}}
 static class FakeHandler {Map<Runnable,Long> callbacks=new HashMap<>();Object getLooper(){return this;}void removeCallbacks(Runnable r){callbacks.remove(r);}void postAtTime(Runnable r,long t){callbacks.put(r,t);}}
 static class Regions {HomeExtraGestureState.Geometry geometry;HomeExtraGestureState.Geometry current(){return geometry;}}
 boolean closed,available=true;long revision=1;int mask=30,device,source,display,pointer;
 HomeExtraGestureState touch=new HomeExtraGestureState(12,300,400);FakeHandler main=new FakeHandler();InputMonitor monitor=new InputMonitor();Regions regions=new Regions();HomeExtraGestureState.Geometry geometry;
 Method eventDisplay;List<Integer> events=new ArrayList<>();Runnable longPress=()->{};
 ReceiverTest()throws Exception{eventDisplay=InputEvent.class.getDeclaredMethod("getDisplayId");geometry=new HomeExtraGestureState.Geometry(1000,2000,0,1,1950,70,new HomeExtraGestureState.Region(400,1960,600,2000),new Object());regions.geometry=geometry;}
 void enqueue(int event){events.add(event);}static void log(String m,Exception e){}
 RECEIVER
 APPLY
 static void input(ReceiverTest a,Receiver r,MotionEvent e){int n=r.finishes;r.onInputEvent(e);ok(r.finishes==n+1&&r.last==e,"same owned event finished exactly once");}
 public static void main(String[]args)throws Exception {
  ReceiverTest a=new ReceiverTest();Receiver r=a.new Receiver(a.monitor);
  input(a,r,new MotionEvent(0,20,1980,100,100));ok(a.monitor.pilfers==0,"down passthrough");
  input(a,r,new MotionEvent(2,150,1850,200,100));ok(a.monitor.pilfers==1&&r.handled&&a.events.isEmpty(),"pilfer on intent only");
  input(a,r,new MotionEvent(1,150,1850,300,100));ok(a.events.equals(List.of(9)),"corner release after original cancel");
  a=new ReceiverTest();r=a.new Receiver(a.monitor);a.monitor.fail=true;input(a,r,new MotionEvent(0,20,1980,100,100));input(a,r,new MotionEvent(2,150,1850,200,100));input(a,r,new MotionEvent(1,150,1850,300,100));ok(a.events.isEmpty()&&!a.touch.active(),"failed pilfer never dispatches");
  a=new ReceiverTest();r=a.new Receiver(a.monitor);input(a,r,new MotionEvent(0,500,1980,100,100));ok(a.main.callbacks.get(a.longPress)==500,"real receiver schedules long timer");input(a,r,new MotionEvent(2,530,1900,200,100));ok(!a.main.callbacks.containsKey(a.longPress),"move removes timer");
  a=new ReceiverTest();r=a.new Receiver(a.monitor);input(a,r,new MotionEvent(0,500,1980,100,100));input(a,r,new MotionEvent(1,500,1980,160,100));input(a,r,new MotionEvent(0,500,1980,260,260));ok(a.monitor.pilfers==1,"second down claims original input");input(a,r,new MotionEvent(1,500,1980,320,260));ok(a.events.equals(List.of(12)),"double dispatched once");
  a=new ReceiverTest();r=a.new Receiver(a.monitor);input(a,r,new MotionEvent(0,500,1980,100,100));input(a,r,new MotionEvent(1,500,1980,160,100));var e=new MotionEvent(0,500,1980,260,260);e.device=8;input(a,r,e);ok(a.monitor.pilfers==0,"different device cannot join taps");
  a=new ReceiverTest();r=a.new Receiver(a.monitor);input(a,r,new MotionEvent(0,20,1980,100,100));e=new MotionEvent(2,150,1850,200,100);e.pointer=9;input(a,r,e);ok(a.events.isEmpty()&&a.monitor.pilfers==0&&!a.touch.active(),"pointer identity guard");
  a=new ReceiverTest();r=a.new Receiver(a.monitor);input(a,r,new MotionEvent(0,20,1980,100,100));input(a,r,new MotionEvent(2,150,1850,200,100));a.regions.geometry=null;input(a,r,new MotionEvent(1,150,1850,300,100));ok(a.events.isEmpty(),"owner disappeared at UP");
  a=new ReceiverTest();r=a.new Receiver(a.monitor);input(a,r,new MotionEvent(0,20,1980,100,100));input(a,r,new MotionEvent(1,150,1850,300,100));ok(a.events.isEmpty()&&a.monitor.pilfers==0,"UP-only crossing cannot cancel ended stock sequence");
  a=new ReceiverTest();r=a.new Receiver(a.monitor);a.closed=true;input(a,r,new MotionEvent(0,20,1980,100,100));ok(!a.touch.active(),"closed no intake");InputEvent key=new InputEvent();int n=r.finishes;r.onInputEvent(key);ok(r.finishes==n+1&&r.last==key,"non motion finishes too");
  System.out.println("EXTRA_RECEIVER_CHECKS="+checks+" FAILED="+failed+"; production adapter methods, framework ownership mocked");if(failed>0)System.exit(1);
 }
}
'''.replace(' RECEIVER',block('private final class Receiver extends InputEventReceiver')).replace(' APPLY',block('private boolean apply('))
(D/'ReceiverTest.java').write_text(code)
rows=[]
for cmd in [['javac','-encoding','UTF-8','-d',str(D),str(D/'ReceiverTest.java'),str(J/'HomeExtraGestureState.java')],['java','-cp',str(D),'com.sevtinge.hyperceiler.libhook.rules.home.gesture.ReceiverTest']]:
 p=subprocess.run(cmd,capture_output=True,text=True,encoding='utf8',errors='replace');print(p.stdout+p.stderr,end='');rows.append(dict(command=cmd,input_root=str(R),stdout=p.stdout,stderr=p.stderr,exit=p.returncode))
 if p.returncode:break
(D/'results.json').write_text(json.dumps(rows,indent=2));raise SystemExit(rows[-1]['exit'])
