package com.miui.maml;
import android.graphics.drawable.*;
public class MamlAdaptiveIconDrawable extends AdaptiveIconDrawable {
 public Drawable quiet;public MamlAdaptiveIconDrawable(Drawable q,Drawable b,Drawable f){super(b,f);quiet=q;}
 public Drawable getTopQuietDrawable(){return quiet;}
 @Override public ConstantState getConstantState(){MamlDrawable.clones++;return super.getConstantState();}
}
