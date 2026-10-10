package com.sevtinge.hyperceiler.libhook.rules.weather;
import android.content.ContentResolver;
import android.os.*;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.common.*;
import java.util.*;
import java.lang.reflect.*;
import java.nio.*;
public class EzHookCallbackTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static boolean onTransact(int code,Parcel data,Parcel reply,int flags){return false;}
 static class Chain implements XposedInterface.Chain {
  final Object[] args;int originals;Chain(Object...args){this.args=args;}
  public Executable getExecutable(){try{return EzHookCallbackTest.class.getMethod("onTransact",int.class,Parcel.class,Parcel.class,int.class);}catch(Exception e){throw new AssertionError(e);}}
  public Object getThisObject(){return null;}public List<Object> getArgs(){return Arrays.asList(args);}public Object getArg(int i){return args[i];}
  public Object proceed(){originals++;return false;}public Object proceed(Object[]a){return proceed();}public Object proceedWith(Object t){return proceed();}public Object proceedWith(Object t,Object[]a){return proceed();}
 }
 public static void main(String[]args){
  SystemClock.source=()->10_000_000_000L;Binder.setCallingIdentityForTest(10100,42);ContentResolver.values=Map.of("prefs_key_allow_hook",true,"prefs_key_weather_device_level","0");var e=new WeatherNativeEndpointOS4((u,p)->u==10100&&p==42);check(e.refreshFromProvider());new WeatherNativeBridgeOS4(e).init();
  int count=0;
  for(int code:new int[]{0x00484353,0x00484359,0x0048434c,1234}){var chain=new Chain(code,new Parcel("android.view.IWindowManager"),new Parcel("android.view.IWindowManager"),0);var context=new InvocationContext(chain);BaseHook.callback.before(new HookParam(context));check(!context.getSkipped());context.proceedOriginal();check(chain.originals==1);count++;}
  for(String descriptor:new String[]{"android.view.IWindowManager","wrong"}){Parcel reply=new Parcel("");var chain=new Chain(WeatherNativeEndpointOS4.TRANSACTION_CODE,new Parcel(descriptor),reply,0);var context=new InvocationContext(chain);BaseHook.callback.before(new HookParam(context));check(context.getSkipped());check(Boolean.TRUE.equals(context.getResult()));context.proceedOriginal();check(chain.originals==0);check(reply.dataSize()==48);check(ByteBuffer.wrap(reply.marshall()).order(ByteOrder.LITTLE_ENDIAN).getInt(4)==(descriptor.equals("wrong")?0:1));count++;}
  e.retire();Parcel reply=new Parcel("");var chain=new Chain(WeatherNativeEndpointOS4.TRANSACTION_CODE,new Parcel("android.view.IWindowManager"),reply,0);var context=new InvocationContext(chain);BaseHook.callback.before(new HookParam(context));context.proceedOriginal();check(chain.originals==0);check(ByteBuffer.wrap(reply.marshall()).order(ByteOrder.LITTLE_ENDIAN).getInt(4)==0);count++;
  System.out.println("RESULT passes="+count+" failures=0; real EzHook1.3 HookParam/InvocationContext and API102 Chain; Android adapters, no framework install");
 }
}
