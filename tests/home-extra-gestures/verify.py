"""Production extra-gesture state, live-region resolver and wire mapping. No device operations."""
from pathlib import Path
import sys,subprocess,re,json
W=Path(__file__).resolve().parents[2];R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
J=R/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/gesture'
D=W/'handover/gesture-extra-os4-20261017'/('host-'+R.name);D.mkdir(parents=True,exist_ok=True)
if not (J/'HomeExtraGestureState.java').exists():print('EXTRA_GESTURES=missing implementation; failed=1');sys.exit(1)
files=[]
def write(n,s):
 p=D/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8');files.append(str(p))
stubs={
'android/graphics/Point.java':'package android.graphics;public class Point {public int x,y;}',
'android/graphics/Insets.java':'package android.graphics;public class Insets {public int left,right,bottom;public Insets(int l,int r,int b){left=l;right=r;bottom=b;}}',
'android/graphics/Rect.java':'''package android.graphics;public class Rect {public int left,top,right,bottom;public boolean isEmpty(){return left>=right||top>=bottom;}public int width(){return right-left;}public float exactCenterX(){return (left+right)/2f;}public void offset(int x,int y){left+=x;right+=x;top+=y;bottom+=y;}}''',
'android/content/Context.java':'package android.content;public class Context {public String getPackageName(){return "com.android.systemui";}}',
'android/util/DisplayMetrics.java':'package android.util;public class DisplayMetrics {public float density=1;}',
'android/content/res/Resources.java':'''package android.content.res;import java.util.*;public class Resources {
 public static class NotFoundException extends RuntimeException {}public int mode=2;public float width=400,assistantWidth=0;public boolean widthExists=true;public Map<Integer,String> names=new HashMap<>();
 public int getIdentifier(String n,String t,String p){return n.equals("config_navBarInteractionMode")?1:n.equals("gestures_assistant_width")&&assistantWidth>0?3:n.equals("navigation_home_handle_width")&&widthExists?2:0;}
 public int getInteger(int n){return mode;}public float getDimension(int n){return n==3?assistantWidth:width;}public String getResourceEntryName(int n){if(!names.containsKey(n))throw new NotFoundException();return names.get(n);}public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();}}''',
'android/view/Display.java':'package android.view;public class Display {public int id=0,rotation=0,w=1000,h=2000;public int getDisplayId(){return id;}public int getRotation(){return rotation;}public void getRealSize(android.graphics.Point p){p.x=w;p.y=h;}}',
'android/view/WindowManager.java':'package android.view;public class WindowManager {public static class LayoutParams {public static final int TYPE_NAVIGATION_BAR=2019;public int type=2019;}}',
'android/view/WindowInsets.java':'''package android.view;import android.graphics.Insets;public class WindowInsets {public int bottom=50,left=30,right=30;public Insets getInsets(int t){return new Insets(0,0,bottom);}public Insets getInsetsIgnoringVisibility(int t){return new Insets(left,right,bottom);}public static class Type {public static int mandatorySystemGestures(){return 1;}public static int systemGestures(){return 2;}public static int navigationBars(){return 3;}}}''',
'android/view/View.java':'''package android.view;import android.graphics.Rect;import android.content.*;import android.content.res.Resources;public class View {
 public static int NO_ID=-1;public int id=-1,l=0,t=0,r=1000,b=50,screenY=1950;public boolean attached=true,shown=true;public float alpha=1;public Display display=new Display();public Resources res=new Resources();public WindowManager.LayoutParams lp=new WindowManager.LayoutParams();public WindowInsets insets=new WindowInsets();public Object token=new Object();
 public boolean isAttachedToWindow(){return attached;}public boolean isShown(){return shown;}public float getAlpha(){return alpha;}public Display getDisplay(){return display;}public Object getLayoutParams(){return lp;}public Resources getResources(){return res;}public WindowInsets getRootWindowInsets(){return insets;}public boolean getGlobalVisibleRect(Rect out){out.left=l;out.top=t;out.right=r;out.bottom=b;return !out.isEmpty();}public void getLocationOnScreen(int[] a){a[0]=l;a[1]=t+screenY;}public void getLocationInWindow(int[]a){a[0]=l;a[1]=t;}public int getId(){return id;}public Context getContext(){return new Context();}public Object getWindowToken(){return token;}}
''',
'android/view/ViewGroup.java':'package android.view;import java.util.*;public class ViewGroup extends View {public List<View> children=new ArrayList<>();public int getChildCount(){return children.size();}public View getChildAt(int i){return children.get(i);}}',
'android/view/ViewConfiguration.java':'package android.view;public class ViewConfiguration {public static ViewConfiguration get(android.content.Context c){return new ViewConfiguration();}public int getScaledTouchSlop(){return 12;}}',
'android/view/WindowManagerGlobal.java':'package android.view;import java.util.*;public class WindowManagerGlobal {public static List<View> roots=new ArrayList<>();public static WindowManagerGlobal getInstance(){return new WindowManagerGlobal();}public List<View> getWindowViews(){return roots;}}'
}
for n,s in stubs.items():write(n,s)
endpoint=(J/'HomeQuickSwitchOS4.java').read_text();mapping=re.search(r'static int actionIndex\(int event\)\s*\{[^}]*\}',endpoint).group()
code=r'''package com.sevtinge.hyperceiler.libhook.rules.home.gesture;
import android.view.*;import java.util.*;
public class ExtraTest {
 static int checks,failed;static void ok(boolean b,String n){checks++;if(!b){failed++;System.out.println("FAIL="+n);}}
 MAPPING
 static Object owner=new Object();static HomeExtraGestureState.Geometry g=new HomeExtraGestureState.Geometry(1000,2000,0,1,1950,70,new HomeExtraGestureState.Region(400,1960,600,2000),owner);
 static HomeExtraGestureState state(){return new HomeExtraGestureState(12,300,400);}
 static HomeExtraGestureState.Result e(HomeExtraGestureState s,int a,float x,float y,long t,long down,int m){return s.sample(a,1,x,y,t,down,g,1,m);}
 static void corner(int m,int event,float x,float end){
  var s=state();ok(!e(s,0,x,1980,100,100,m).claim(),"stock DOWN");var r=e(s,2,end,1850,200,100,m);ok(r.claim()&&r.handled()&&r.event()==0,"reserve diagonal");r=e(s,1,end,1850,300,100,m);ok(!r.claim()&&r.event()==event,"release once");ok(e(s,1,end,1850,310,100,m).event()==0,"duplicate UP");
 }
 public static void main(String[] args)throws Exception {
  for(int i=-2;i<=15;i++){int want=i>=1&&i<=5?i-1:i>=8&&i<=12?i-3:-1;ok(actionIndex(i)==want,"wire "+i);}
  corner(2,9,20,150);corner(4,10,980,850);
  var s=state();e(s,0,20,1980,100,100,0);ok(!e(s,1,150,1850,200,100,0).handled(),"off stays stock");
  s=state();e(s,0,980,1980,100,100,2);ok(!e(s,1,850,1850,200,100,2).handled(),"sides independent");
  for(float[] p:new float[][]{{20,1850},{150,1980},{0,1850},{150,1990}}){s=state();e(s,0,20,1980,100,100,2);ok(!e(s,1,p[0],p[1],200,100,2).handled(),"non diagonal stock");}
  s=state();e(s,0,20,1980,100,100,2);ok(e(s,2,40,1960,200,100,2).claim(),"small intent reserved");ok(e(s,1,40,1960,250,100,2).event()==0,"short no action");
  s=state();e(s,0,20,1980,100,100,2);e(s,2,150,1850,200,100,2);ok(e(s,3,150,1850,210,100,2).event()==0&&!s.active(),"CANCEL");
  s=state();e(s,0,20,1980,100,100,2);ok(s.sample(2,2,150,1850,200,100,g,1,2).event()==0&&!s.active(),"multi reject");
  s=state();e(s,0,20,1980,100,100,2);ok(s.sample(1,1,150,1850,200,100,g,2,2).event()==0&&!s.active(),"generation");
  s=state();e(s,0,20,1980,100,100,2);var moved=new HomeExtraGestureState.Geometry(2000,1000,1,1,950,70,g.handle(),owner);ok(s.sample(1,1,150,850,200,100,moved,1,2).event()==0,"rotation invalidates");
  s=state();e(s,0,500,1980,100,100,8);ok(s.pendingLong()&&s.longDeadline()==500,"long armed");ok(s.longPress(499,g,1,8).event()==0,"not early");var r=s.longPress(500,g,1,8);ok(r.claim()&&r.event()==11&&r.handled(),"long owned");ok(s.longPress(510,g,1,8).event()==0,"long once");ok(e(s,1,500,1980,600,100,8).event()==0,"long UP no double");
  s=state();e(s,0,300,1980,100,100,8);ok(!s.pendingLong(),"not arbitrary bottom region");
  s=state();e(s,0,500,1980,100,100,8);e(s,2,520,1980,150,100,8);ok(s.longPress(500,g,1,8).event()==0,"movement rejects long");
  s=state();e(s,0,500,1980,100,100,8);ok(s.longPress(500,g,2,8).event()==0,"timer rev guard");s.reset();ok(s.longPress(500,g,1,8).event()==0,"cleanup timer guard");
  s=state();e(s,0,500,1980,100,100,16);ok(e(s,1,500,1980,160,100,16).event()==0,"first tap stock");r=e(s,0,501,1980,260,260,16);ok(r.claim()&&r.event()==0,"second DOWN cancels stock");ok(e(s,1,501,1980,320,260,16).event()==12,"double release");ok(e(s,1,501,1980,330,260,16).event()==0,"double once");
  for(int variant=0;variant<6;variant++){s=state();e(s,0,500,1980,100,100,24);e(s,1,500,1980,160,100,24);
   if(variant==0){r=e(s,0,500,1980,500,500,24);ok(!r.claim(),"slow stock");}
   if(variant==1){r=e(s,0,550,1980,260,260,24);ok(!r.claim(),"different location stock");}
   if(variant==2){s.reset();r=e(s,0,500,1980,260,260,24);ok(!r.claim(),"reset clears tap");}
   if(variant==3){r=s.sample(0,1,500,1980,260,260,g,2,24);ok(!r.claim(),"new config clears tap");}
   if(variant==4){e(s,0,500,1980,260,260,24);e(s,2,530,1900,280,260,24);ok(e(s,1,530,1900,320,260,24).event()==0,"double drag rejects");}
   if(variant==5){e(s,0,500,1980,260,260,24);ok(s.longPress(700,g,1,24).event()==0,"second tap not long too");}
  }
  s=state();ok(s.sample(0,1,Float.NaN,1980,100,100,g,1,31).event()==0,"NaN");ok(s.sample(0,1,500,1980,90,100,g,1,31).event()==0,"backwards time");
  var sh=new HomeExtraGestureState.Shake();ok(!sh.sample(0,0,9.8f,100),"sensor seed");ok(!sh.sample(6,6,9.8f,150),"first impulse");ok(sh.sample(-6,-6,9.8f,200),"shake");ok(!sh.sample(6,6,9.8f,250)&&!sh.sample(-6,-6,9.8f,300),"throttle");ok(!sh.sample(6,6,9.8f,350),"new pair");ok(!sh.sample(-6,-6,9.8f,400),"throttle second");sh.reset();ok(!sh.sample(0,0,9.8f,100)&&!sh.sample(1,1,9.8f,150),"stationary");ok(!sh.sample(Float.NaN,0,0,200),"invalid sensor");
  // Actual live-window resolver with platform objects supplied by host stubs.
  ViewGroup root=new ViewGroup();View h=new View();h.id=42;h.l=400;h.r=600;h.res=root.res;root.res.names.put(42,"home_handle");root.children.add(h);WindowManagerGlobal.roots.add(root);
  var resolver=new HomeGestureRegionsOS4();var actual=resolver.current();ok(actual!=null&&actual.navTop()==1950&&actual.handle().contains(500,1980),"window coordinates -> screen");ok(actual.equals(resolver.current()),"stable generation identity");root.res.assistantWidth=120;ok(resolver.current().corner()==120,"original assistant resource width");root.res.assistantWidth=160;ok(resolver.current().corner()==160,"assistant overlay changes without offset table");root.res.assistantWidth=0;
  h.shown=false;actual=resolver.current();ok(actual!=null&&actual.handle()==null,"hidden line: corners only");h.shown=true;
  root.res.mode=0;ok(resolver.current()==null,"three button excluded");root.res.mode=2;
  root.display.id=1;ok(resolver.current()==null,"other display excluded");root.display.id=0;
  root.attached=false;ok(resolver.current()==null,"detached excluded");root.attached=true;
  h.l=0;h.r=1000;actual=resolver.current();ok(actual.handle().left()==288&&actual.handle().right()==712,"wide box uses live dimension");root.res.width=200;actual=resolver.current();ok(actual.handle().left()==388,"overlay width dynamically updates");root.res.widthExists=false;ok(resolver.current().handle()==null,"missing dimension no guessed pill");root.res.widthExists=true;
  WindowManagerGlobal.roots.add(new ViewGroup());ok(resolver.current()==null,"ambiguous owners decline");WindowManagerGlobal.roots.remove(1);
  root.insets.bottom=0;ok(resolver.current()==null,"no bottom gesture inset");root.insets.bottom=50;
  root.lp.type=1;ok(resolver.current()==null,"not navigation window");root.lp.type=2019;
  root.display.w=2000;root.display.h=1000;root.display.rotation=1;root.r=2000;root.screenY=950;h.screenY=950;actual=resolver.current();ok(actual!=null&&actual.rotation()==1&&actual.height()==1000&&actual.navTop()==950,"landscape live metrics");
  System.out.println("EXTRA_GESTURE_CHECKS="+checks+" FAILED="+failed+"; production state/regions/mapping, Android objects mocked");if(failed>0)System.exit(1);
 }
}
'''.replace(' MAPPING',mapping)
write('ExtraTest.java',code)
files.extend(str(J/n) for n in ['HomeExtraGestureState.java','HomeGestureRegionsOS4.java'])
rows=[]
for cmd in [['javac','-encoding','UTF-8','-d',str(D)]+files,['java','-cp',str(D),'com.sevtinge.hyperceiler.libhook.rules.home.gesture.ExtraTest']]:
 p=subprocess.run(cmd,capture_output=True,text=True,encoding='utf8',errors='replace');print(p.stdout+p.stderr,end='');rows.append(dict(command=cmd,input_root=str(R),stdout=p.stdout,stderr=p.stderr,exit=p.returncode))
 if p.returncode:break
(D/'results.json').write_text(json.dumps(rows,indent=2));raise SystemExit(rows[-1]['exit'])
