package com.sevtinge.hyperceiler.libhook.base;
import java.util.*;import io.github.libxposed.api.XposedInterface;import io.github.lingqiqi5211.ezhooktool.xposed.java.*;
public abstract class BaseHook {
 public static final Map<String,Class<?>> classes=new HashMap<>();public static final List<Throwable>errors=new ArrayList<>();public static int writes,reads,failWrite,failRead;public static boolean failAfter,persistent,dropWrite,failInstall,nullInstall;public static String foreign;
 public abstract void init();public static Class<?>findClassIfExists(String n){return classes.get(n);}
 public static Object getStaticObjectField(Class<?>c,String n){if(++reads==failRead)throw new IllegalStateException("read");try{return c.getField(n).get(null);}catch(Exception e){throw new IllegalStateException(e);}}
 public static void setStaticObjectField(Class<?>c,String n,Object v){boolean fail=++writes==failWrite||(persistent&&writes>=failWrite);try{if(fail&&!failAfter)throw new IllegalStateException("write");if(!dropWrite)c.getField(n).set(null,foreign!=null&&fail?foreign:v);if(fail)throw new IllegalStateException("after write");}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
 public static XposedInterface.HookHandle findAndHookConstructor(Class<?>c,Object...a){if(failInstall)throw new IllegalStateException("install");if(nullInstall)return null;return Hooks.findAndHookConstructor(c,a);}
 public static XposedInterface.HookHandle findAndHookConstructor(String c,Object...a){return findAndHookConstructor(classes.get(c),a);}
 protected void debugCallbackError(String s,Throwable e){errors.add(e);}
}
