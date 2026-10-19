package com.sevtinge.hyperceiler.libhook.base;
import java.lang.reflect.Method;import java.util.*;
import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.java.*;
public abstract class BaseHook {
 public static final Map<String,Class<?>> classes=new HashMap<>();
 public static final List<String> errors=new ArrayList<>();
 public static int installs,failAt;public static boolean throwInstall;
 public abstract void init();
 public static Class<?>findClassIfExists(String name){return classes.get(name);}
 protected final void debugCallbackError(String where,Throwable error){errors.add(where+":"+error.getClass().getName());}
 public static XposedInterface.HookHandle hookMethod(Method method,IMethodHook callback){
  if(++installs==failAt){if(throwInstall)throw new IllegalStateException("backend failure");return null;}
  return Hooks.createHook(method,callback);
 }
}
