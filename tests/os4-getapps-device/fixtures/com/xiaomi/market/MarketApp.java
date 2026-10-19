package com.xiaomi.market;
import android.os.Build;
/** Consumer projection from the pinned complete ROM methods, not original DEX execution. */
public class MarketApp {
 public static int constructed;public static boolean basic,patch,throwConstructor;public static String[]seen,cached;
 public MarketApp(){constructed++;seen=new String[]{getDevice(),getModel(),getManufacturer()};if(cached==null)cached=seen.clone();if(throwConstructor)throw new IllegalArgumentException("original-constructor");}
 public static String getDevice(){return patch?"patched_device":basic?"":Build.DEVICE;}
 public static String getModel(){return patch?"patched_model":basic?"":Build.MODEL;}
 public static String getManufacturer(){return patch?"patched_maker":Build.MANUFACTURER;}
}
