package android.widget;
public class TextView {
 public float size=12,scaledDensity=1,density=1;public int calls,unit=-1;public String text="original";public int color=123,visibility=0;public boolean fail;
 public void setTextSize(int unit,float value){if(fail)throw new IllegalStateException("view failure");this.unit=unit;size=value*(unit==2?scaledDensity:unit==1?density:1);calls++;}
 public float getTextSize(){return size;}
}
