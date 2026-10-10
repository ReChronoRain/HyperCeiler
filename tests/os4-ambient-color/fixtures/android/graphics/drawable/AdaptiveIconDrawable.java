package android.graphics.drawable;
public class AdaptiveIconDrawable extends Drawable {
 private final Drawable background,foreground;
 public AdaptiveIconDrawable(Drawable b,Drawable f){super(b!=null?b.bitmap:f!=null?f.bitmap:null);background=b;foreground=f;}
 public Drawable getBackground(){return background;}
 public Drawable getForeground(){return foreground;}
}
