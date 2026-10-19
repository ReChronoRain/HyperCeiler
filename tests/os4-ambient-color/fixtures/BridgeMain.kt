import android.os.Looper
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import com.android.systemui.media.controls.shared.model.MediaData
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.AmbientNativeColorOS4 as N
import com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.AmbientColorResolverOS4 as R
import java.util.concurrent.*
fun main(args:Array<String>){
 Looper.getMainLooper();var checks=0
 fun ck(v:Boolean){check(v);checks++}
 val state=N::class.java.getDeclaredField("loadState").apply{isAccessible=true}
 ck(N.resolve(intArrayOf(12),1,1)==null&&state.getInt(null)==0)
 val pool=Executors.newSingleThreadExecutor()
 try{pool.submit(Callable{
  Thread.currentThread().interrupt();try{ck(N.resolve(intArrayOf(12),1,1)==null&&Thread.currentThread().isInterrupted&&state.getInt(null)==0)}finally{Thread.interrupted()}
  ck(N.resolve(IntArray(1),2,2)==null&&state.getInt(null)==0)
  val opt=R.create(R::class.java.classLoader,false,true)!!
  val d=MediaData(Icon(BitmapDrawable(Bitmap(4,4,15))),0,"x")
  ck(opt.resolve(Context(null),d,true)==R.Colors(19,22)&&state.getInt(null)==0)
  if(args.single()=="missing"){
   ck(N.resolve(intArrayOf(12),1,1)==null&&state.getInt(null)==-1)
   ck(N.resolve(intArrayOf(13),1,1)==null&&state.getInt(null)==-1)
   ck(opt.resolve(Context(null),d,true)==R.Colors(19,22))
  }else{
   ck(N.resolve(intArrayOf(0),1,1)==0&&state.getInt(null)==1)
   ck(N.resolve(intArrayOf(-1),1,1)==-1)
   ck(N.resolve(intArrayOf(0x11223344),1,1)==null)
   ck(N.resolve(intArrayOf(0x22334455),1,1)==null)
   ck(N.resolve(intArrayOf(0x13572468),1,1)==null)
   ck(N.resolve(intArrayOf(12345),1,1)==12345)
  }
 }).get(10,TimeUnit.SECONDS)}finally{pool.shutdownNow()}
 println("PASS bridge checks=$checks")
}
