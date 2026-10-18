"""Production pure lifecycle: only framework integration is excluded from this host test."""
from pathlib import Path
import sys, subprocess
W=Path(__file__).resolve().parents[2]
R=Path(sys.argv[1]).resolve() if len(sys.argv)>1 else W
J=R/'library/libhook/src/main/java/com/sevtinge/hyperceiler/libhook/rules/home/gesture'
p=J/'HomeQuickSwitchState.java'
if not p.exists():
 print('SYSTEMUI_LIFECYCLE=missing production state; FAILED=1');sys.exit(1)
out=W/'handover/gestures-os4-20261014'/('systemui-state-'+R.name);out.mkdir(parents=True,exist_ok=True)
(out/p.name).write_text(p.read_text(encoding='utf8'),encoding='utf8')
(out/'StateTest.java').write_text(r'''
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;
public class StateTest {
 static int checks,failed;
 static void ok(boolean x){checks++;if(!x){failed++;System.out.println("failed check="+checks);}}
 static HomeQuickSwitchState make(boolean right) {
  HomeQuickSwitchState s=new HomeQuickSwitchState(1000,5,2,0,4098,right?1198:2,1350,3.25f,1200);
  s.accepted=true;s.tracker=new Object();s.sample(2,1,right?750:450,1350,1160);s.sample(1,1,right?750:450,1350,1760);return s;
 }
 static HomeQuickSwitchState ready(){HomeQuickSwitchState s=make(false);ok(s.claim(5,true,1));s.expected=42;return s;}
 public static void main(String[]a){
  HomeQuickSwitchState s=make(false);ok(s.claim(5,true,1));ok(!s.claim(5,true,1));ok(!s.complete(1761,5,true));
  s.returned=true;ok(!s.complete(1761,5,true));s.finished=true;ok(!s.complete(1761,5,true));s.reset=true;ok(!s.complete(1761,5,true));
  s.expected=42;ok(s.complete(1761,5,true));ok(!s.complete(1762,5,true));ok(make(true).claim(5,true,1));
  int[][] orders={{0,1,2},{0,2,1},{1,0,2},{1,2,0},{2,0,1},{2,1,0}};
  for(int[] order:orders){s=ready();for(int i=0;i<3;i++){switch(order[i]){case 0:s.returned=true;break;case 1:s.reset=true;break;case 2:s.finished=true;}ok(s.complete(1800,5,true)==(i==2));}ok(!s.complete(1801,5,true));}
  for(int kind=0;kind<13;kind++){
   s=make(false);
   switch(kind){case 0:s.accepted=false;break;case 1:s.tracker=null;break;case 2:s.retired=true;break;case 3:s.valid=false;break;
    case 4:s.sample(3,1,450,1350,1761);break;case 5:s.sample(5,2,450,1350,1761);break;case 6:s.sample(1,1,450,1350,900);break;
    case 7:s.sample(1,1,Float.NaN,1350,1761);break;case 8:s.sample(1,1,450,1600,1761);break;case 9:s.sample(1,1,100,1350,1761);break;
    case 10:s.sample(1,1,-450,1350,1761);break;case 11:s.sample(1,1,450,1350,12000);break;case 12:s.sample(1,2,450,1350,1761);break;}
   ok(!s.claim(5,true,1));
  }
  ok(!make(false).claim(6,true,1));ok(!make(false).claim(5,false,1));ok(!make(false).claim(5,true,2));
  s=ready();s.returned=s.reset=s.finished=true;ok(!s.complete(1759,5,true));ok(!s.complete(2511,5,true));ok(!s.complete(1800,6,true));ok(!s.complete(1800,5,false));s.retired=true;ok(!s.complete(1800,5,true));
  s=new HomeQuickSwitchState(1000,5,2,0,4098,2,1350,3.25f,Float.POSITIVE_INFINITY);ok(!s.valid);
  s=new HomeQuickSwitchState(1000,5,2,0,4098,2,1350,0,1200);ok(!s.valid);
  System.out.println("SYSTEMUI_LIFECYCLE_CHECKS="+checks+" FAILED="+failed+"; production state, mocked platform lifecycle");if(failed>0)System.exit(1);
 }
}
''',encoding='utf8')
for cmd in [['javac','-d',str(out),str(out/p.name),str(out/'StateTest.java')],['java','-cp',str(out),'com.sevtinge.hyperceiler.libhook.rules.home.gesture.StateTest']]:
 x=subprocess.run(cmd,capture_output=True,text=True);print(x.stdout+x.stderr,end='')
 if x.returncode:sys.exit(x.returncode)
