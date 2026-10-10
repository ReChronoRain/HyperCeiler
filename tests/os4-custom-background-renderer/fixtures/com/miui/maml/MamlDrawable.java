package com.miui.maml;
public class MamlDrawable extends android.graphics.drawable.Drawable {
 public static int clones;
 @Override public ConstantState getConstantState(){clones++;return super.getConstantState();}
}
