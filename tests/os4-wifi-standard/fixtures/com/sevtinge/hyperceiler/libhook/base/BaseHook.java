package com.sevtinge.hyperceiler.libhook.base;
import java.lang.reflect.*;import java.util.*;import io.github.libxposed.api.XposedInterface;import io.github.lingqiqi5211.ezhooktool.xposed.java.*;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.dexkit.IDexKit;
public abstract class BaseHook {
 public static final Map<String,Class<?>> classes=new HashMap<>();public static final List<String>errors=new ArrayList<>();public static int lookups,installs,failAt;public static boolean failDex,failInstall;public static Method selected;public static boolean preSkipped;public static Object preResult;
 protected static final String TAG="WifiStandard";public final Param lpparam=new Param();public static final class Param{public String packageName="com.android.systemui";}
 public abstract void init();public boolean useDexKit(){return false;}public boolean initDexKit(){return true;}
 protected final <T>T optionalMember(String key,IDexKit finder){lookups++;if(failDex)throw new IllegalStateException("DexKit is unavailable");return (T)selected;}
 public static Class<?>findClassIfExists(String n){try{return classes.containsKey(n)?classes.get(n):Class.forName(n);}catch(ClassNotFoundException e){return null;}}
 protected final void debugCallbackError(String where,Throwable e){errors.add(where+":"+e.getClass().getName());}
 public static XposedInterface.HookHandle hookMethod(Method m,IMethodHook callback){if(++installs==failAt){if(failInstall)throw new IllegalStateException("install failed");return null;}return Hooks.createHook(m,new IMethodHook(){public void before(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){if(preSkipped)p.setResult(preResult);callback.before(p);}public void after(io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam p){callback.after(p);}});}
}
