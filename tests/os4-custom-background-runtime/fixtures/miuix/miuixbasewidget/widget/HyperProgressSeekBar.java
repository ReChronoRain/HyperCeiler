package miuix.miuixbasewidget.widget;
public class HyperProgressSeekBar extends android.widget.SeekBar {
 public final int mDeviceLevel;public final android.graphics.Paint mPaint=new android.graphics.Paint();
 public static Runnable insideDraw;public static boolean failDraw;public int draws;
 public int mForegroundPrimaryColor=106,mForegroundPrimaryDisableColor=107,mBackgroundPrimaryColor=108,mBackgroundPrimaryDisableColor=109;
 public HyperProgressSeekBar(android.content.Context c){this(c,0);}public HyperProgressSeekBar(android.content.Context c,int level){super(c);mDeviceLevel=level;}
 public void setForegroundPrimaryColor(int c){if(mDeviceLevel==0||mDeviceLevel==1)mForegroundPrimaryColor=c;}
 public void setForegroundPrimaryDisableColor(int c){if(mDeviceLevel==0||mDeviceLevel==1)mForegroundPrimaryDisableColor=c;}
 public void setBackgroundPrimaryColor(int c){if(mDeviceLevel==0||mDeviceLevel==1)mBackgroundPrimaryColor=c;}
 public void setBackgroundPrimaryDisableColor(int c){if(mDeviceLevel==0||mDeviceLevel==1)mBackgroundPrimaryDisableColor=c;}
 public final void drawHighEndMode(android.graphics.Canvas c){draws++;if(insideDraw!=null)insideDraw.run();c.drawRect(0,0,100,10,mPaint);if(failDraw)throw new IllegalArgumentException("original draw");}
}
