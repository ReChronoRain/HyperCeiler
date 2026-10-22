package fixture;
import java.lang.reflect.*;import java.util.*;
import io.github.libxposed.api.XposedInterface;import io.github.lingqiqi5211.ezhooktool.xposed.EzXposed;import io.github.lingqiqi5211.ezhooktool.core.EzReflect;
public final class HookRuntime {
 public static final Map<Executable,XposedInterface.Hooker>hooks=new LinkedHashMap<>();public static int originalCalls;
 static Object proxy(Class<?>c,InvocationHandler h){return Proxy.newProxyInstance(HookRuntime.class.getClassLoader(),new Class<?>[]{c},h);}
 public static void setup(){EzReflect.init(HookRuntime.class.getClassLoader());XposedInterface api=(XposedInterface)proxy(XposedInterface.class,(p,m,a)->{
  if(m.getName().equals("hook")){Executable target=(Executable)a[0];return proxy(XposedInterface.HookBuilder.class,(b,bm,ba)->{
   if(bm.getName().equals("intercept")){if(hooks.containsKey(target))throw new AssertionError("duplicate registration");hooks.put(target,(XposedInterface.Hooker)ba[0]);return proxy(XposedInterface.HookHandle.class,(h,hm,ha)->{if(hm.getName().equals("unhook"))hooks.remove(target);if(hm.getName().equals("getExecutable"))return target;return null;});}return b;});}
  if(m.getReturnType()==int.class)return 102;if(m.getReturnType()==long.class)return 0L;if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==String.class)return "fixture API102";return null;});EzXposed.setBase$hook_xposed_102(api);EzXposed.setHotReloadEnabled(false);
 }
 public static Object dispatch(Method m,Object receiver,Object...args){try{var c=new Chain(m,receiver,args);return hooks.containsKey(m)?hooks.get(m).intercept(c):c.proceed();}catch(Throwable e){return HookRuntime.<RuntimeException,Object>raise(e);}}
 static final class Chain implements XposedInterface.Chain {
  final Method m;final Object t;final Object[]a;Chain(Method m,Object t,Object[]a){this.m=m;this.t=t;this.a=a;}
  public Executable getExecutable(){return m;}public Object getThisObject(){return t;}public List<Object>getArgs(){return Arrays.asList(a);}public Object getArg(int i){return a[i];}
  public Object proceed()throws Throwable{return proceedWith(t,a);}public Object proceed(Object[]v)throws Throwable{return proceedWith(t,v);}public Object proceedWith(Object v)throws Throwable{return proceedWith(v,a);}public Object proceedWith(Object v,Object[]args)throws Throwable{originalCalls++;try{return m.invoke(v,args);}catch(InvocationTargetException e){throw e.getCause();}}
 }
 @SuppressWarnings("unchecked") public static <E extends Throwable,T>T raise(Throwable e)throws E{throw (E)e;}
}
