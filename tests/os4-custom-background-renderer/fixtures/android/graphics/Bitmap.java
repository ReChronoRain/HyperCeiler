package android.graphics;
public class Bitmap {
 public enum Config{ARGB_8888,HARDWARE} public final int w,h;public int color;public final Config config;public boolean recycled,mutable,copyNull;public static java.util.List<Bitmap> created=new java.util.ArrayList<>();
 public Bitmap(int w,int h,int color){this(w,h,color,Config.ARGB_8888);}public Bitmap(int w,int h,int color,Config c){this.w=w;this.h=h;this.color=color;config=c;created.add(this);}
 public int getWidth(){return w;}public int getHeight(){return h;}public Config getConfig(){return config;}public boolean isRecycled(){return recycled;}public boolean isMutable(){return mutable;}public void recycle(){recycled=true;}
 public Bitmap copy(Config c,boolean m){if(copyNull)return null;Bitmap b=new Bitmap(w,h,color,c);b.mutable=m;return b;}public static Bitmap createBitmap(int w,int h,Config c){Bitmap b=new Bitmap(w,h,0,c);b.mutable=true;return b;}
 public static Bitmap createScaledBitmap(Bitmap b,int w,int h,boolean f){return b.w==w&&b.h==h?b:new Bitmap(w,h,b.color,b.config);}public int getPixel(int x,int y){if(recycled||config==Config.HARDWARE)throw new IllegalStateException();return color;}
 public static Bitmap wrapHardwareBuffer(android.hardware.HardwareBuffer b,Object c){Probe.hit("wrap");return Probe.nullAt.equals("wrap")?null:new Bitmap(16,16,123,Config.HARDWARE);}
}
