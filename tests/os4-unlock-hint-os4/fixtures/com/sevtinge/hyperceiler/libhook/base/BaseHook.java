package com.sevtinge.hyperceiler.libhook.base;
import java.lang.reflect.Method;import java.util.*;import io.github.libxposed.api.XposedInterface;import io.github.lingqiqi5211.ezhooktool.xposed.java.*;
public abstract class BaseHook {
 public final String TAG=getClass().getSimpleName();public static final Map<String,Class<?>>classes=new HashMap<>();public static final List<String>lookups=new ArrayList<>(),errors=new ArrayList<>();public static final List<Runnable>cleanups=new ArrayList<>();
 public static boolean preSkipped,lateSkipped,failInstall,throwInstall,failCleanup;public static Object[]seen;public static Runnable duringCleanup;
 public abstract void init();public static Class<?>findClassIfExists(String n){lookups.add(n);return classes.get(n);}protected void debugCallbackError(String w,Throwable t){errors.add(w+":"+t);}
 public static void registerHotReloadCleanup(Runnable r){if(failCleanup)throw new IllegalStateException("cleanup registration");cleanups.add(r);if(duringCleanup!=null)duringCleanup.run();}
 public static XposedInterface.HookHandle hookMethod(Method m,IMethodHook cb){if(throwInstall)throw new IllegalStateException("install");if(failInstall)return null;return Hooks.createHook(m,new IMethodHook(){public void before(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){if(preSkipped)p.setResult("already skipped");cb.before(p);seen=p.getArgs().clone();if(lateSkipped)p.setResult("later skipped");}public void after(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){cb.after(p);}});}
}
