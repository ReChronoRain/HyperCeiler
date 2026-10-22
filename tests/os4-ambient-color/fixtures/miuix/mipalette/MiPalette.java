package miuix.mipalette;
import android.graphics.Bitmap;import java.util.List;import java.util.concurrent.CyclicBarrier;import java.util.concurrent.ConcurrentLinkedQueue;
public class MiPalette {static{System.load(System.getProperty("fixture.native"));}static int MAX_AREA=0x3100;static int[] pixels=new int[MAX_AREA*2];public static int reads,bitmapCalls;public static boolean failPixels,wrongSize,returnNull,changeArea,interrupt;public static CyclicBarrier barrier;public static ConcurrentLinkedQueue<int[]> buffers=new ConcurrentLinkedQueue<>();
 public static void reset(){MAX_AREA=0x3100;java.util.Arrays.fill(pixels,0x12345678);reads=bitmapCalls=0;failPixels=wrongSize=returnNull=changeArea=interrupt=false;barrier=null;buffers.clear();}
 public static void area(int n){MAX_AREA=n;}public static int[] global(){return pixels;}
 // Transcription of the complete actual private getPixels branch used with rects=null.
 private static Bitmap getPixels(int[] target,Bitmap b,List<?> rects){reads++;buffers.add(target);if(rects!=null)throw new AssertionError("must preserve null rects");if(failPixels)throw new IllegalArgumentException("pixels");int w=b.getWidth(),h=b.getHeight();int area=w*h;Bitmap result=b;
 if(area>MAX_AREA){float scale=(float)Math.sqrt((double)((float)MAX_AREA/(float)area));int nw=(int)Math.ceil((double)((float)w*scale)),nh=(int)Math.ceil((double)((float)h*scale));result=Bitmap.createScaledBitmap(b,nw,nh,false);w=nw;h=nh;}result.getPixels(target,0,w,0,0,w,h);
 if(barrier!=null)try{barrier.await();}catch(Exception e){throw new RuntimeException(e);}if(changeArea)MAX_AREA++;if(interrupt)Thread.currentThread().interrupt();if(returnNull)return null;return wrongSize?new Bitmap(w+1,h,b.color):result;}
 public static int getMainColorHCT(Bitmap b){bitmapCalls++;Bitmap sample=getPixels(pixels,b,null);return getMainColorHCT(pixels,sample.w,sample.h);}
 public static native int getMainColorHCT(int[] input,int width,int height);
}
