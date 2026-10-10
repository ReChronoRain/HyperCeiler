import android.content.Context
import android.graphics.*
import android.graphics.drawable.*
import android.os.Looper
import com.android.systemui.media.controls.shared.model.MediaData
import com.android.systemui.monet.ColorScheme
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.CustomBackgroundRendererOS4
import com.sevtinge.hyperceiler.libhook.utils.hookapi.systemui.controlcenter.media.MediaViewColorConfig
import kotlin.concurrent.thread

private fun pixels(f:CustomBackgroundRendererOS4.Frame):Bitmap{val x=f.javaClass.getDeclaredField("background");x.isAccessible=true;return x.get(f) as Bitmap}
private var pass=0
private var fail=0
private fun check(name:String,block:()->Boolean){try{if(!block())error("assertion");pass++;println("PASS $name")}catch(t:Throwable){fail++;println("FAIL $name $t")}}
fun main(){
 Looper.getMainLooper()
 val loader=MediaData::class.java.classLoader
 val renderer=CustomBackgroundRendererOS4.create(loader,setOf(1,2,3,4))!!
 val image=Bitmap(64,48,0xffeeeeee.toInt())
 val context=Context(BitmapDrawable(Bitmap(20,30,123)))
 val data=MediaData(Icon(BitmapDrawable(image)),10,"work.pkg")
 val options=CustomBackgroundRendererOS4.Options(false,false,0)
 check("main refuses work"){renderer.render(context,data,3,options,30,20)==null}
 thread {
  fun render(style:Int=3,o:CustomBackgroundRendererOS4.Options=options,d:Any=data,w:Int=30,h:Int=20)=renderer.render(context,d,style,o,w,h)
  check("actual class preflight"){renderer.mediaDataClass===MediaData::class.java}
  check("no default/unknown activation"){CustomBackgroundRendererOS4.create(loader,emptySet())==null&&CustomBackgroundRendererOS4.create(loader,setOf(0,1))==null&&CustomBackgroundRendererOS4.create(loader,setOf(5))==null}
  check("unselected mode"){CustomBackgroundRendererOS4.create(loader,setOf(3))!!.render(context,data,4,options,30,20)==null}
  check("exact data type"){render(d=Any())==null}
  check("invalid settings/dimensions"){render(o=options.copy(blurPercent=-1))==null&&render(o=options.copy(blurPercent=21))==null&&render(w=-1)==null&&render(h=Int.MAX_VALUE)==null}
  check("zero size private fallback"){val f=render(w=0,h=0);f!=null&&f.width==64&&f.height==48}
  check("cross-user application fallback"){val f=render(d=MediaData(null,77,"other.pkg"));f!=null&&context.requestedUser==77&&context.requestedPackage=="other.pkg"}
  check("fallback failure is null"){context.throwsFallback=true;val f=render(d=MediaData(null,9,"missing"));context.throwsFallback=false;f==null}
  check("all palette indices 1"){val f=render(1);f?.colors==MediaViewColorConfig(302,302,308,308)}
  check("all palette indices 2"){val f=render(2);f?.colors==MediaViewColorConfig(101,203,409,309)}
  check("all palette indices 3"){val f=render(3);f?.colors==MediaViewColorConfig(101,203,409,309)}
  check("inverse per request"){render(4)?.colors==MediaViewColorConfig(302,302,308,308)&&render(4,options.copy(inverse=true))?.colors==MediaViewColorConfig(308,308,303,303)}
  check("content6 dark constructor"){render();ColorScheme.lastStyle==6&&ColorScheme.lastDark}
  check("all 13 required"){ColorScheme.count=12;val f=render();ColorScheme.count=13;f==null}
  check("all exact Integer required"){ColorScheme.bad=true;val f=render();ColorScheme.bad=false;f==null}
  check("wallpaper unavailable not transparent"){android.app.WallpaperColors.returnNull=true;val f=render();android.app.WallpaperColors.returnNull=false;f==null}
  check("borrowed image live immutable exports"){val f=render()!!;val a=pixels(f);!image.isRecycled&&a!==image&&!a.isMutable&&!a.isRecycled}
  check("DI independent drawable instances options"){val f=render(o=options.copy(colorAnimation=true))!!;val a=f.newPresentation(false);val b=f.newPresentation(true);a!==b&&a.initial!==b.initial&&!a.resize&&b.resize&&a.animation&&b.animation}
  check("independent animation request"){!render(o=options.copy(colorAnimation=false))!!.newPresentation(false).animation&&render(o=options.copy(colorAnimation=true))!!.newPresentation(false).animation}
  check("zero blur no GPU acquisition"){Probe.reset();val f=render(2);f!=null&&"reader" !in Probe.hits}
  check("nonzero blur uses GPU"){Probe.reset();val f=render(2,options.copy(blurPercent=10));f!=null&&"reader" in Probe.hits&&"reader.close" in Probe.hits}
  check("unknown GPU result declined"){Probe.reset();HardwareRenderer.status=2;val f=render(2,options.copy(blurPercent=10));HardwareRenderer.status=0;f==null&&"reader.close" in Probe.hits&&"destroy" in Probe.hits}
  check("hardware source bounded private copy"){val h=Bitmap(4096,2048,3,Bitmap.Config.HARDWARE);val f=render(d=MediaData(Icon(BitmapDrawable(h)),0,"x"))!!;val b=pixels(f);b.width==1024&&b.height==1024&&b.config==Bitmap.Config.ARGB_8888&&!h.isRecycled}
  check("dimensionless static private render"){val c=ColorDrawable(123);val f=render(d=MediaData(Icon(c),0,"x"),w=0,h=0);f!=null&&f.width==112&&f.height==112&&c.mutations==0}
  check("interruption declines"){Thread.currentThread().interrupt();val f=render();Thread.interrupted();f==null}
  val nullPoints=listOf("acquire","buffer","wrap")
  for(point in nullPoints)check("null GPU $point cleanup"){Probe.reset();Probe.nullAt=point;val f=render(2,options.copy(blurPercent=10));f==null&&"reader.close" in Probe.hits&&"destroy" in Probe.hits&&(point=="acquire"||"image.close" in Probe.hits)&&(point!="wrap"||"buffer.close" in Probe.hits)}
  val throws=listOf("node","renderer","surface","position","effect","begin","draw","end","request","sync","acquire","buffer","wrap")
  for(point in throws)check("throw GPU $point releases acquired objects"){Probe.reset();Probe.failAt=point;val f=render(2,options.copy(blurPercent=10));f==null&&(point=="draw"||"reader.close" in Probe.hits)&&(!Probe.hits.contains("surface")||"destroy" in Probe.hits)}
  check("cleanup exception does not skip following resources"){Probe.reset();Probe.failAt="buffer.close";val f=render(2,options.copy(blurPercent=10));f!=null&&listOf("image.close","destroy","discard","reader.close").all{it in Probe.hits}}
  Probe.reset()
  check("complete failure task bitmaps retired"){val start=Bitmap.created.size;android.app.WallpaperColors.returnNull=true;render();android.app.WallpaperColors.returnNull=false;Bitmap.created.drop(start).all{it.isRecycled}}
  check("complete task recycles no borrowed input"){!image.isRecycled}
 }.join()
 println("RESULT passes=$pass failures=$fail");if(fail>0)throw AssertionError("failures")
}
