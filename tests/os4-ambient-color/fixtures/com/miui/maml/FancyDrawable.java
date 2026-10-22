package com.miui.maml;
import android.graphics.drawable.Drawable;
public class FancyDrawable extends MamlDrawable {
 public Drawable quiet;public FancyDrawable(Drawable q){quiet=q;}
 public Drawable getQuietDrawable(){return quiet;}
}
