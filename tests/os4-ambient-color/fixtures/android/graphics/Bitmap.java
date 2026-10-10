package android.graphics; public class Bitmap {
 public enum Config {ARGB_8888,HARDWARE}
 public final int w,h; public int color; public final Config config; public boolean recycled,copyNull,copyThrows,pixelsThrows; public int recycleCalls; public static int copies,scales;public static boolean lastFilter;
 public Bitmap(int w,int h,int color){this(w,h,color,Config.ARGB_8888);}public Bitmap(int w,int h,int color,Config c){this.w=w;this.h=h;this.color=color;config=c;}
 public int getWidth(){return w;}public int getHeight(){return h;}public Config getConfig(){return config;}public boolean isRecycled(){return recycled;}public void recycle(){recycleCalls++;recycled=true;}
 public Bitmap copy(Config c,boolean mutable){copies++;if(copyThrows)throw new IllegalStateException("copy");return copyNull?null:new Bitmap(w,h,color,c);}
 public static Bitmap createBitmap(int w,int h,Config c){return new Bitmap(w,h,0,c);}
 public static Bitmap createScaledBitmap(Bitmap a,int w,int h,boolean filter){scales++;lastFilter=filter;if(a.w==w&&a.h==h)return a;return new Bitmap(w,h,a.color);}
 public void getPixels(int[] a,int off,int stride,int x,int y,int width,int height){if(pixelsThrows||recycled||config==Config.HARDWARE)throw new IllegalStateException("pixels");if(width<1||height<1||(long)width*height>a.length)throw new ArrayIndexOutOfBoundsException();java.util.Arrays.fill(a,off,off+width*height,color);}
}
