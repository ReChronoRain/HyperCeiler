package com.sevtinge.hyperceiler.libhook.base;
import java.lang.reflect.*;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
public abstract class BaseHook {
 public static String failField, failMethod, registeredName, registeredClass;
 public static IMethodHook callback;
 protected final String TAG=getClass().getSimpleName();
 public abstract void init();
 public static String getPackageName(){return "com.android.settings";}
 public static Object getObjectField(Object obj,String name) {
  if(name.equals(failField))throw new IllegalStateException("test field read failure");
  try{return obj.getClass().getField(name).get(obj);}catch(Exception e){throw new RuntimeException(e);}
 }
 public static Object callMethod(Object obj,String name,Object...args) {
  if(name.equals(failMethod))throw new IllegalStateException("test reflective call failure");
  for(Method m:obj.getClass().getMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){
   try{return m.invoke(obj,args);}catch(Exception e){throw new RuntimeException(e);}
  }
  throw new IllegalArgumentException(name);
 }
 public static void findAndHookMethod(String cls,String name,Object...args) {
  if(cls.equals("android.os.SystemProperties"))return; // old rule's unrelated property hook
  try{
   Class<?>[] types=new Class<?>[args.length-1];for(int i=0;i<types.length;i++)types[i]=(Class<?>)args[i];
   Class.forName(cls).getDeclaredMethod(name,types);
  }catch(Exception e){throw new IllegalArgumentException(cls+"."+name,e);}
  registeredClass=cls;registeredName=name;callback=(IMethodHook)args[args.length-1];
 }
 public static void findAndHookConstructor(String cls,Object...args) {
  try{Class.forName(cls);}catch(Exception e){throw new IllegalArgumentException("missing ROM class "+cls,e);}
 }
}
