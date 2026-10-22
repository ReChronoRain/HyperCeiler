package com.sevtinge.hyperceiler.libhook.base;
import java.lang.reflect.Method;import java.util.*;
import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.java.*;
public abstract class BaseHook {
 public static final List<Runnable> cleanups=new ArrayList<>(); public static void registerHotReloadCleanup(Runnable r){cleanups.add(r);}
 public static final Map<String,Class<?>> classes=new HashMap<>();
 public static final List<String> errors=new ArrayList<>();
 public static int installs,failAt;public static boolean throwInstall,preSkipped,changeAfterArgs,skipAfterBefore;public static Object[] lastArgs;
 public abstract void init();
 public static Object invokeSuperMethod(String n,Object receiver,Object...args){throw new AssertionError("legacy super not under test");}
 public static Class<?>findClassIfExists(String name){return classes.get(name);}
 protected final void debugCallbackError(String where,Throwable error){errors.add(where+":"+error.getClass().getName());}
 public static XposedInterface.HookHandle hookMethod(Method method,IMethodHook callback){
  if(++installs==failAt){if(throwInstall)throw new IllegalStateException("backend failure");return null;}
  return Hooks.createHook(method,new IMethodHook(){public void before(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){if(preSkipped)p.setResult("already-skipped");callback.before(p);if(skipAfterBefore)p.setResult("late-skip");lastArgs=p.getArgs().clone();}public void after(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){if(changeAfterArgs)p.context$hook_xposed_102().setArgs(p.getArgs().clone());callback.after(p);}});
 }
}

