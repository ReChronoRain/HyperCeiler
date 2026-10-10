import android.app.WallpaperColors
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.*
import android.os.Looper
import com.miui.maml.*
import com.android.systemui.media.controls.shared.model.MediaData
import com.android.systemui.monet.ColorScheme
import com.miui.utils.DrawableUtils
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.AmbientColorResolverOS4 as Resolver
import miuix.mipalette.MiPalette
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.AmbientNativeColorOS4 as Native
import java.util.concurrent.*

private var passed=0
private var failed=0
private val loader=Resolver::class.java.classLoader
private val worker=Executors.newSingleThreadExecutor()
private fun bitmap(c:Int=0xff123456.toInt(),w:Int=30,h:Int=20,config:Bitmap.Config=Bitmap.Config.ARGB_8888)=Bitmap(w,h,c,config)
private fun data(d:Drawable?,user:Int=10)=MediaData(d?.let(::Icon),user,"music.app")
private fun context()=Context(BitmapDrawable(bitmap(0xffabcdef.toInt())))
private fun resolver(n:Boolean=true,o:Boolean=true)=Resolver.create(loader,n,o)!!
private fun reset(){MamlDrawable.clones=0;MiPalette.reset();Bitmap.copies=0;Bitmap.scales=0;DrawableUtils.calls=0;DrawableUtils.returnNull=false;WallpaperColors.bitmapCalls=0;WallpaperColors.drawableCalls=0;WallpaperColors.returnNull=false;WallpaperColors.throwsPalette=false;ColorScheme.calls=0;ColorScheme.throwsCtor=false;ColorScheme.overrideShades=null;ColorScheme.nullAccent=false;ColorScheme.nullShades=false}
private fun test(name:String,ui:Boolean=false,block:()->Unit){try{reset();if(ui)block()else worker.submit(Callable{block()}).get(10,TimeUnit.SECONDS);passed++;println("PASS $name")}catch(t:Throwable){failed++;println("FAIL $name ${t.cause?:t}")}}
private class BlockLoader(private val names:Set<String>):ClassLoader(loader){val loaded=mutableListOf<String>();override fun loadClass(n:String,resolve:Boolean):Class<*>{loaded+=n;if(n in names)throw ClassNotFoundException(n);return super.loadClass(n,resolve)}}
fun main(){
 Looper.getMainLooper()
 test("standard_artwork_exact_typed_conversion"){val r=resolver();val b=bitmap();val d=BitmapDrawable(b);val c=context();val result=r.resolve(c,data(d),false);check(result==Resolver.Colors(b.color,b.color));check(DrawableUtils.lastWidth==0&&DrawableUtils.lastHeight==0&&c.fallbackCalls==0);check(MiPalette.bitmapCalls==0);check(MiPalette.buffers.single()!==MiPalette.global());check(MiPalette.global().all{it==0x12345678});check(!b.isRecycled&&b.recycleCalls==0);check(Bitmap.copies==1)}
 test("normal_nonbitmap_private_clone"){val d=Drawable(bitmap());val bounds=Rect(d.bounds);check(resolver().resolve(context(),data(d),false)!=null);check(d.bounds==bounds&&d.mutations==0)}
 test("optimized_exact_style_and_palette_indices"){val b=bitmap();val q=resolver().resolve(context(),data(BitmapDrawable(b)),true);check(q==Resolver.Colors(b.color+4,b.color+7));check(ColorScheme.calls==1&&ColorScheme.lastStyle==6&&ColorScheme.lastDark);check(MiPalette.reads==0&&DrawableUtils.calls==0)}
 test("optimized_nonbitmap_private_draw"){val d=Drawable(bitmap());val bounds=Rect(d.bounds);check(resolver().resolve(context(),data(d),true)!=null);check(WallpaperColors.drawableCalls==1&&d.bounds==bounds)}
 for(opt in listOf(false,true))test("fallback_user_identity_$opt"){val c=context();val md=MediaData(null,999,"dual.app");val q=resolver().resolve(c,md,opt);check(q!=null&&c.requestedUser==999&&c.requestedPackage=="dual.app"&&c.fallbackCalls==1)}
 test("artwork_load_exception_uses_original_fallback"){val c=context();val i=Icon(BitmapDrawable(bitmap()));i.throwsLoad=true;check(resolver().resolve(c,MediaData(i,17,"x"),false)!=null&&c.requestedUser==17)}
 test("fallback_exception_is_unavailable"){val c=context();c.throwsFallback=true;check(resolver().resolve(c,MediaData(null,0,"x"),false)==null)}
 test("both_sources_null_unavailable"){check(resolver().resolve(Context(null),MediaData(null,0,"x"),true)==null)}
 test("unconfigured_mode_no_dependency_or_resolution"){check(Resolver.create(loader,false,false)==null);val r=resolver(true,false);check(r.resolve(context(),data(BitmapDrawable(bitmap())),true)==null)}
 test("standard_without_monet"){val l=BlockLoader(setOf("com.android.systemui.monet.ColorScheme","com.android.systemui.monet.TonalPalette"));check(Resolver.create(l,true,false)!=null);check(l.loaded.none{it.startsWith("com.android.systemui.monet.")})}
 test("optimized_without_standard"){val l=BlockLoader(setOf("miuix.mipalette.MiPalette","com.miui.utils.DrawableUtils"));check(Resolver.create(l,false,true)!=null);check(l.loaded.none{it=="miuix.mipalette.MiPalette"||it=="com.miui.utils.DrawableUtils"})}
 for(n in listOf("com.android.systemui.media.controls.shared.model.MediaData","com.android.systemui.statusbar.notification.utils.NotificationUtil","miuix.mipalette.MiPalette","com.miui.utils.DrawableUtils","com.android.systemui.monet.ColorScheme","com.android.systemui.monet.TonalPalette"))test("missing_selected_${n.substringAfterLast('.')}"){check(Resolver.create(BlockLoader(setOf(n)),true,true)==null)}
 test("exact_media_type_only"){val c=context();check(resolver().resolve(c,Any(),false)==null&&c.fallbackCalls==0)}
 test("main_thread_rejected",true){check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),false)==null&&DrawableUtils.calls==0)}
 test("interrupted_before_work_rejected"){Thread.currentThread().interrupt();try{check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),false)==null&&Thread.currentThread().isInterrupted&&DrawableUtils.calls==0)}finally{Thread.interrupted()}}
 test("interrupted_after_sampling_no_color"){MiPalette.interrupt=true;try{check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),false)==null&&Thread.currentThread().isInterrupted)}finally{Thread.interrupted()}}
 test("conversion_exception_no_transparent"){val d=Drawable(bitmap());d.throwConversion=true;check(resolver().resolve(context(),data(d),false)==null)}
 test("conversion_null_no_transparent"){DrawableUtils.returnNull=true;check(resolver().resolve(context(),data(Drawable(bitmap())),false)==null)}
 test("real_transparent_result_distinguished_from_failure"){check(resolver().resolve(context(),data(BitmapDrawable(bitmap(0))),false)==Resolver.Colors(0,0))}
 for(opt in listOf(false,true))test("hardware_snapshot_$opt"){val b=bitmap(config=Bitmap.Config.HARDWARE);check(resolver().resolve(context(),data(BitmapDrawable(b)),opt)!=null&&Bitmap.copies==1&&b.config==Bitmap.Config.HARDWARE&&!b.isRecycled)}
 test("recycled_artwork_uses_fallback"){val b=bitmap();b.recycled=true;val c=context();check(resolver().resolve(c,data(BitmapDrawable(b)),false)!=null&&c.fallbackCalls==1&&b.recycleCalls==0)}
 test("unavailable_bitmap_copy_uses_fallback"){val b=bitmap();b.copyNull=true;val c=context();check(resolver().resolve(c,data(BitmapDrawable(b)),false)!=null&&c.fallbackCalls==1)}
 test("bitmap_copy_exception_uses_fallback"){val b=bitmap();b.copyThrows=true;val c=context();check(resolver().resolve(c,data(BitmapDrawable(b)),false)!=null&&c.fallbackCalls==1)}
 test("no_constant_state_uses_fallback_without_mutation"){val d=Drawable(bitmap());d.noState=true;val c=context();check(resolver().resolve(c,data(d),false)!=null&&c.fallbackCalls==1&&d.mutations==0)}
 test("constant_state_original_alias_rejected"){val d=Drawable(bitmap());d.aliasState=true;val c=Context(null);val bounds=Rect(d.bounds);check(resolver().resolve(c,data(d),false)==null&&d.bounds==bounds&&d.mutations==0)}
 test("mutate_original_alias_rejected"){val d=Drawable(bitmap());d.aliasMutate=true;check(resolver().resolve(Context(null),data(d),false)==null&&d.mutations==0)}
 test("constant_state_exception_uses_fallback"){val d=Drawable(bitmap());d.cloneFails=true;val c=context();check(resolver().resolve(c,data(d),true)!=null&&c.fallbackCalls==1)}
 for(a in listOf(0,-1,Int.MAX_VALUE,12543,12545))test("changed_max_area_$a"){MiPalette.area(a);check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),false)==null&&MiPalette.reads==0)}
 test("original_float_ceil_sampling_preserved"){val b=bitmap(w=1000,h=500);val q=resolver().resolve(context(),data(BitmapDrawable(b)),false);check(q==Resolver.Colors(b.color,b.color));check(Bitmap.scales==1&&!Bitmap.lastFilter);check(MiPalette.buffers.single().size==25088);check(MiPalette.buffers.single().count{it==b.color}==159*80)}
 test("input_int_product_overflow_rejected_before_sampler"){check(resolver().resolve(context(),data(BitmapDrawable(bitmap(w=Int.MAX_VALUE,h=2))),false)==null&&MiPalette.reads==0)}
 test("thin_scaled_output_exceeds_original_capacity_rejected_early"){check(resolver().resolve(context(),data(BitmapDrawable(bitmap(w=1,h=1_000_000))),false)==null&&MiPalette.reads==0)}
 test("sampled_dimensions_must_match_original_algorithm"){MiPalette.wrongSize=true;check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),false)==null)}
 test("sampler_null_is_unavailable"){MiPalette.returnNull=true;check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),false)==null)}
 test("sampler_exception_is_unavailable"){MiPalette.failPixels=true;check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),false)==null)}
 test("area_change_during_sampling_is_unavailable"){MiPalette.changeArea=true;check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),false)==null)}
 test("native_exception_is_unavailable"){check(resolver().resolve(context(),data(BitmapDrawable(bitmap(0x13572468))),false)==null)}
 test("scheme_constructor_exception_is_unavailable"){ColorScheme.throwsCtor=true;check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),true)==null)}
 test("wallpaper_exception_is_unavailable"){WallpaperColors.throwsPalette=true;check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),true)==null)}
 test("wallpaper_null_is_unavailable"){WallpaperColors.returnNull=true;check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),true)==null)}
 test("null_accent_unavailable"){ColorScheme.nullAccent=true;check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),true)==null)}
 test("null_shades_unavailable"){ColorScheme.nullShades=true;check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),true)==null)}
 for(count in listOf(0,8,12,14))test("strict_shades_count_$count"){ColorScheme.overrideShades=List(count){it};check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),true)==null)}
 test("do_not_filter_wrong_shade_types"){ColorScheme.overrideShades=List<Any?>(13){if(it==2)1L else it};check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),true)==null)}
 test("reject_null_shade_even_outside_selected_indices"){ColorScheme.overrideShades=List<Int?>(13){if(it==2)null else it};check(resolver().resolve(context(),data(BitmapDrawable(bitmap())),true)==null)}
 test("pair_does_not_share_palette_list"){val shades=(0..12).toMutableList();ColorScheme.overrideShades=shades;val q=resolver().resolve(context(),data(BitmapDrawable(bitmap())),true)!!;shades[4]=900;check(q==Resolver.Colors(4,7))}
 test("no_global_color_cache"){val r=resolver();val a=r.resolve(context(),data(BitmapDrawable(bitmap(11))),false);val b=r.resolve(context(),data(BitmapDrawable(bitmap(22))),false);check(a==Resolver.Colors(11,11)&&b==Resolver.Colors(22,22));check(MiPalette.buffers.toList().distinctBy{System.identityHashCode(it)}.size==2)}
 test("concurrent_task_pixels_are_private"){MiPalette.barrier=CyclicBarrier(2);val pool=Executors.newFixedThreadPool(2);try{val r1=resolver();val r2=resolver();val a=pool.submit(Callable{r1.resolve(context(),data(BitmapDrawable(bitmap(101))),false)});val b=pool.submit(Callable{r2.resolve(context(),data(BitmapDrawable(bitmap(202))),false)});check(a.get(5,TimeUnit.SECONDS)==Resolver.Colors(101,101)&&b.get(5,TimeUnit.SECONDS)==Resolver.Colors(202,202));check(MiPalette.buffers.size==2&&MiPalette.buffers.toList()[0]!==MiPalette.buffers.toList()[1]);check(MiPalette.global().all{it==0x12345678})}finally{pool.shutdownNow();MiPalette.barrier=null}}

 for(opt in listOf(false,true)){
  test("fancy_quiet_private_snapshot_$opt"){val b=bitmap(0xff102030.toInt());val d=FancyDrawable(BitmapDrawable(b));val c=context();val q=resolver().resolve(c,data(d),opt);check(q==Resolver.Colors(b.color+(if(opt)4 else 0),b.color+(if(opt)7 else 0)));check(MamlDrawable.clones==0&&c.fallbackCalls==0&&d.mutations==0&&b.recycleCalls==0)}
  test("animating_quiet_private_snapshot_$opt"){val d=AnimatingDrawable(BitmapDrawable(bitmap()));check(resolver().resolve(context(),data(d),opt)!=null&&MamlDrawable.clones==0)}
  test("maml_adaptive_top_quiet_snapshot_$opt"){val d=MamlAdaptiveIconDrawable(BitmapDrawable(bitmap(81)),FancyDrawable(null),FancyDrawable(null));val c=context();val q=resolver().resolve(c,data(d),opt);check(q==Resolver.Colors(81+(if(opt)4 else 0),81+(if(opt)7 else 0))&&c.fallbackCalls==0&&MamlDrawable.clones==0)}
  test("maml_adaptive_static_layers_$opt"){val d=MamlAdaptiveIconDrawable(null,AnimatingDrawable(BitmapDrawable(bitmap(71))),FancyDrawable(BitmapDrawable(bitmap(72))));val c=context();check(resolver().resolve(c,data(d),opt)!=null&&c.fallbackCalls==0&&MamlDrawable.clones==0)}
  test("standard_adaptive_private_layers_$opt"){val bg=BitmapDrawable(bitmap());val fg=Drawable(bitmap());val d=AdaptiveIconDrawable(bg,fg);check(resolver().resolve(context(),data(d),opt)!=null&&bg.mutations==0&&fg.mutations==0&&d.mutations==0)}
 }
 test("fancy_no_quiet_uses_same_app_fallback_without_clone"){val c=context();check(resolver().resolve(c,data(FancyDrawable(null),43),false)!=null&&c.requestedUser==43&&c.fallbackCalls==1&&MamlDrawable.clones==0)}
 test("unsafe_fancy_fallback_is_unavailable"){check(resolver().resolve(Context(FancyDrawable(null)),data(FancyDrawable(null)),false)==null&&MamlDrawable.clones==0&&DrawableUtils.calls==0)}
 test("unknown_maml_subclass_unavailable_before_clone"){check(resolver().resolve(Context(null),data(object:MamlDrawable(){}),false)==null&&MamlDrawable.clones==0)}
 test("quiet_self_cycle_unavailable"){val d=FancyDrawable(null);d.quiet=d;check(resolver().resolve(Context(null),data(d),false)==null&&MamlDrawable.clones==0)}
 test("quiet_two_object_cycle_bounded"){val a=FancyDrawable(null);val b=FancyDrawable(a);a.quiet=b;check(resolver().resolve(Context(null),data(a),false)==null&&MamlDrawable.clones==0)}
 test("adaptive_unknown_child_rejects_whole_before_clone"){val bg=object:Drawable(bitmap()){};check(resolver().resolve(Context(null),data(AdaptiveIconDrawable(bg,Drawable(bitmap()))),false)==null&&bg.mutations==0)}
 test("adaptive_missing_static_layer_unavailable"){val d=MamlAdaptiveIconDrawable(null,FancyDrawable(null),BitmapDrawable(bitmap()));check(resolver().resolve(Context(null),data(d),false)==null&&MamlDrawable.clones==0)}

 test("dimensionless_static_private_canvas_sample"){val d=Drawable(bitmap(91));d.noIntrinsic=true;val q=resolver().resolve(context(),data(d),false);check(q==Resolver.Colors(91,91)&&DrawableUtils.calls==0);check(MiPalette.buffers.single().count{it==91}==112*112&&d.mutations==0)}
 test("dimensionless_canvas_exception_unavailable"){val d=Drawable(bitmap());d.noIntrinsic=true;d.throwConversion=true;check(resolver().resolve(context(),data(d),false)==null&&MiPalette.reads==0)}
 test("native_unavailable_is_not_transparent_success"){check(resolver().resolve(context(),data(BitmapDrawable(bitmap(0x11223344))),false)==null)}
 test("native_invalid_success_word_rejected"){check(resolver().resolve(context(),data(BitmapDrawable(bitmap(0x22334455))),false)==null)}
 test("native_direct_main_thread_rejected",true){check(Native.resolve(intArrayOf(1),1,1)==null)}
 for(pair in listOf(0 to 1,-1 to 1,1 to 0,2 to 2,Int.MAX_VALUE to Int.MAX_VALUE))test("native_shape_guard_${pair.first}_${pair.second}"){check(Native.resolve(intArrayOf(1),pair.first,pair.second)==null)}
 worker.shutdownNow();println("RESULT passes=$passed failures=$failed");check(failed==0)
}
