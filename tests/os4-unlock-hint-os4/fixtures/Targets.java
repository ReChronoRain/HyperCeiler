import android.view.View;import android.content.res.ColorStateList;import android.graphics.drawable.Drawable;
public class Targets {
 public static class VC{public final View mView;public VC(View v){mView=v;}}
 public static class Bottom extends View{} public static class OtherBottom extends Bottom{}
 public static final class Indication{public CharSequence mMessage="Swipe up to continue";public ColorStateList mTextColor=new ColorStateList();public View.OnClickListener mOnClickListener;public Drawable mBackground;public Long mMinVisibilityMillis;public boolean mForceAccessibilityLiveRegionAssertive;}
 public static final class Rotate extends VC{public Object received;public int type,calls;public boolean immediate,fail;public Runnable nested;public Rotate(){super(new Bottom());}public final void updateIndication(int t,Indication i,boolean z){calls++;type=t;immediate=z;received=i;if(nested!=null)nested.run();if(fail)throw new IllegalArgumentException("original");}}
 public static final class WrongRotate extends VC{public WrongRotate(){super(new Bottom());}public final void updateIndication(int t,Indication i,boolean z){}}
 public static class Theme{public static String type="default";public static boolean fail;public static boolean isDefaultLockScreenTheme(){if(fail)throw new IllegalStateException("theme");return !"maml".equals(type);}}
}
