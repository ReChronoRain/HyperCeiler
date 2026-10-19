package com.sevtinge.hyperceiler.libhook.base;
import java.lang.reflect.Method;import java.util.*;import io.github.libxposed.api.XposedInterface;import io.github.lingqiqi5211.ezhooktool.xposed.java.*;
/** Environment adapter: actual EzHook creates its Hooker; no callback or OEM algorithm reimplementation. */
public abstract class BaseHook {
 public static ClassLoader targetLoader; public static final List<Runnable>cleanups=new ArrayList<>();public static final List<String>errors=new ArrayList<>();public static Object[]seen;
 public abstract void init();public static Class<?>findClassIfExists(String n){try{return targetLoader.loadClass(n);}catch(ClassNotFoundException e){return null;}}
 protected void debugCallbackError(String where,Throwable error){errors.add(where+":"+error);}
 public static void registerHotReloadCleanup(Runnable r){cleanups.add(r);}
 public static XposedInterface.HookHandle hookMethod(Method method,IMethodHook cb){return Hooks.createHook(method,new IMethodHook(){public void before(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){cb.before(p);seen=p.getArgs().clone();}public void after(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){cb.after(p);}});}
}
