import com.sevtinge.hyperceiler.libhook.rules.systemui.navigation.BackGestureWidthState;
public class WidthTest {
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError(checks);}
 public static void main(String[] args){
  for(int base=1;base<=1024;base++)for(int pct=100;pct<=400;pct+=5){
   BackGestureWidthState s=new BackGestureWidthState(base,base+1);
   for(int i=0;i<4;i++)s.apply(pct);
   ok(s.appliedLeft==base*pct/100);ok(s.appliedRight==(base+1)*pct/100);
   s.apply(100);ok(s.appliedLeft==base&&s.appliedRight==base+1);
   s.reset(base+2,base+3);s.apply(pct);ok(s.appliedLeft==(base+2)*pct/100);
  }
  for(int p:new int[]{-1,0,99,401,Integer.MAX_VALUE})ok(BackGestureWidthState.scale(97,p)==97);
  ok(BackGestureWidthState.scale(97,200)>=136);ok(BackGestureWidthState.scale(97,400)>=273);
  System.out.println("SYSTEMUI_WIDTH="+checks+" checks; failed=0; no compounding/default restore/resource reset");
 }
}
