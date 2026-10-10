package com.sevtinge.hyperceiler.libhook.base;
import java.lang.reflect.Method;
import java.util.*;
import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.java.*;
public abstract class BaseHook {
 public static final Map<String,Class<?>> classes=new HashMap<>();
 public static boolean nullHandle,throwInstall;
 public abstract void init();
 public static Class<?> findClassIfExists(String name){return classes.get(name);}
 public static XposedInterface.HookHandle hookMethod(Method method,IReplaceHook callback){
  if(throwInstall)throw new IllegalStateException("backend failure");
  if(nullHandle)return null;
  // The production BaseHook overload delegates to this exact dependency method.
  return Hooks.createHook(method,callback);
 }
}
