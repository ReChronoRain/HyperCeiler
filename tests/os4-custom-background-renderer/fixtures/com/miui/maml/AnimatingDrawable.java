package com.miui.maml;
import android.graphics.drawable.Drawable;
public class AnimatingDrawable extends MamlDrawable {
 public Drawable quiet;public AnimatingDrawable(Drawable q){quiet=q;}
 public Drawable getQuietDrawable(){return quiet;}
}
