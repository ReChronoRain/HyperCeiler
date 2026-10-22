package fixture;

import android.content.ContentProvider;
import android.os.Bundle;
import android.util.Log;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.libhook.base.BaseHook;
import com.sevtinge.hyperceiler.libhook.base.BaseLoad;
import com.sevtinge.hyperceiler.libhook.app.MediaEditor;
import com.sevtinge.hyperceiler.libhook.rules.mediaeditor.GalleryCreationAvailabilityOS4;
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
public class TestMain {
 static int passed;
 static int failed;
 static final String AVAILABLE="key_common_is_available";
 static final String[][] MAPPING={{"method_is_id_photo_available","gallery_enable_id_photo"},{"method_is_magic_matting_available","gallery_enable_magic_matting"},{"method_is_video_post_available","gallery_enable_video_post"},{"method_is_vlog_available","gallery_enable_video_editor"}};
 static void test(String name,Runnable body){try{reset();body.run();passed++;System.out.println("PASS "+name);}catch(Throwable e){failed++;System.out.println("FAIL "+name+" "+e);}}
 static void eq(Object x,Object y){if(!java.util.Objects.equals(x,y))throw new AssertionError(x+" != "+y);}
 // This assertion checks that provider failures preserve the exact original bundle/reference.
 @SuppressWarnings("PMD.CompareObjectsWithEquals")
 static void same(Object x,Object y){if(x!=y)throw new AssertionError("identity changed");}
 static void reset(){PrefsBridge.enabled.clear();PrefsBridge.fail=false;PrefsBridge.reads=0;Log.warnings=0;BaseHook.registered=null;BaseLoad.installed.clear();DeviceHelper.System.version=4f;new GalleryCreationAvailabilityOS4().init();}
 static Bundle result(Object value){Bundle b=new Bundle();b.values.put(AVAILABLE,value);b.values.put("unrelated","keep");return b;}
 static HookParam call(Object target,String method,Object original){HookParam p=new HookParam(target,original,method,"originalArg",new Bundle());BaseHook.registered.before(p);same(p.result,original);BaseHook.registered.after(p);return p;}
 static void enableAll(){for(String[] m:MAPPING)PrefsBridge.enabled.add(m[1]);}
 public static void main(String[] args){
  for(String[] m:MAPPING){
   test(m[0]+" enabled changes only availability in a copied bundle",()->{PrefsBridge.enabled.add(m[1]);Bundle original=result(false);Object extra=new Object();original.values.put("extra",extra);HookParam p=call(new ContentProvider(),m[0],original);Bundle b=(Bundle)p.result;eq(b.get(AVAILABLE),true);eq(original.get(AVAILABLE),false);eq(b.get("unrelated"),"keep");same(b.get("extra"),extra);eq(b.values.size(),original.values.size());eq(p.resultSets,1);});
   test(m[0]+" disabled preserves original false",()->{Bundle b=result(false);HookParam p=call(new ContentProvider(),m[0],b);same(p.result,b);eq(p.resultSets,0);});
   test(m[0]+" already true remains identical",()->{PrefsBridge.enabled.add(m[1]);Bundle b=result(true);same(call(new ContentProvider(),m[0],b).result,b);});
   test(m[0]+" other feature switch has no effect",()->{for(String[] other:MAPPING)if(!other[0].equals(m[0]))PrefsBridge.enabled.add(other[1]);Bundle b=result(false);same(call(new ContentProvider(),m[0],b).result,b);});
  }
  for(String caller:new String[]{"com.miui.mediaeditor","com.xiaomi.aicr","com.xiaomi.mihomemanager","com.miui.gallery.fake","android","",null})test("other caller unchanged "+caller,()->{enableAll();ContentProvider c=new ContentProvider();c.caller=caller;Bundle b=result(false);same(call(c,MAPPING[0][0],b).result,b);eq(PrefsBridge.reads,0);});
  for(String method:new String[]{"method_is_photo_movie_available","method_is_device_support_id_photo","method_is_device_support_capabilities","method_can_access_secret_album","method_is_document_available","method_is_id_photo_available_extra","",null})test("unmatched query unchanged "+method,()->{enableAll();Bundle b=result(false);same(call(new ContentProvider(),method,b).result,b);eq(PrefsBridge.reads,0);});
  test("null result retained",()->{enableAll();same(call(new ContentProvider(),MAPPING[0][0],null).result,null);});
  test("nonBundle result retained",()->{enableAll();Object x=new Object();same(call(new ContentProvider(),MAPPING[0][0],x).result,x);});
  test("missing availability not invented",()->{enableAll();Bundle b=new Bundle();same(call(new ContentProvider(),MAPPING[0][0],b).result,b);eq(b.values.size(),0);});
  for(Object value:new Object[]{"false",0,null})test("nonBoolean availability retained "+value,()->{enableAll();Bundle b=result(value);same(call(new ContentProvider(),MAPPING[0][0],b).result,b);});
  test("original exception stays untouched",()->{enableAll();Bundle b=result(false);HookParam p=new HookParam(new ContentProvider(),b,MAPPING[0][0],null,null);Throwable error=new SecurityException("denied by original provider");p.throwable=error;BaseHook.registered.after(p);same(p.result,b);same(p.throwable,error);eq(p.resultSets,0);eq(PrefsBridge.reads,0);});
  test("skipped original stays untouched",()->{enableAll();Bundle b=result(false);HookParam p=new HookParam(new ContentProvider(),b,MAPPING[0][0],null,null);p.skipped=true;BaseHook.registered.after(p);same(p.result,b);eq(p.resultSets,0);});
  test("caller lookup failure retains original",()->{enableAll();ContentProvider c=new ContentProvider();c.failCaller=true;Bundle b=result(false);same(call(c,MAPPING[0][0],b).result,b);eq(Log.warnings,1);});
  test("preference failure retains original then later request recovers",()->{enableAll();PrefsBridge.fail=true;Bundle b=result(false);same(call(new ContentProvider(),MAPPING[0][0],b).result,b);PrefsBridge.fail=false;eq(((Bundle)call(new ContentProvider(),MAPPING[0][0],b).result).get(AVAILABLE),true);eq(Log.warnings,1);});
  test("bad lazy parcel retains original",()->{enableAll();Bundle b=result(false);b.failGet=true;same(call(new ContentProvider(),MAPPING[0][0],b).result,b);eq(Log.warnings,1);});
  test("bundle copy failure retains original",()->{enableAll();Bundle b=result(false);b.failCopy=true;same(call(new ContentProvider(),MAPPING[0][0],b).result,b);eq(Log.warnings,1);});
  test("wrong receiver preserves result",()->{enableAll();Bundle b=result(false);same(call(new Object(),MAPPING[0][0],b).result,b);});
  test("OS4 installs while initial Gallery switches off and reads changes per request",()->{BaseHook.registered=null;new MediaEditor().onPackageLoaded();eq(BaseLoad.installed.toString(),"[GalleryCreationAvailabilityOS4]");Bundle b=result(false);same(call(new ContentProvider(),MAPPING[0][0],b).result,b);PrefsBridge.enabled.add(MAPPING[0][1]);eq(((Bundle)call(new ContentProvider(),MAPPING[0][0],b).result).get(AVAILABLE),true);PrefsBridge.enabled.clear();same(call(new ContentProvider(),MAPPING[0][0],b).result,b);});
  test("older OS does not install new provider hook",()->{BaseHook.registered=null;DeviceHelper.System.version=3f;new MediaEditor().onPackageLoaded();same(BaseHook.registered,null);eq(BaseLoad.installed.size(),0);});
  test("existing MediaEditor switch registration remains intact",()->{PrefsBridge.enabled.add("mediaeditor_unlock_aigc");new MediaEditor().onPackageLoaded();eq(BaseLoad.installed.toString(),"[GalleryCreationAvailabilityOS4, UnlockAigc]");});
  System.out.println("RESULT passes="+passed+" failures="+failed);if(failed>0)System.exit(1);
 }
}
