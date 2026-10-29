package com.sevtinge.hyperceiler.libhook.rules.home.dock;
public class HomeLayoutNativeEndpointOS4 {
 public record RecentsBarPreference(boolean available,boolean enabled,long deadline){}
 public static RecentsBarPreference pref=new RecentsBarPreference(true,true,Long.MAX_VALUE);
 public static RecentsBarPreference recentsBarPreference(){return pref;}
}