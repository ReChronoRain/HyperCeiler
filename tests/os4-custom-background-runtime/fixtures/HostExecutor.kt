package com.sevtinge.hyperceiler.libhook.utils.api
object HostExecutor {
 data class Job(val tag:Any,val background:()->Any?,val result:(Any)->Unit)
 @JvmField val jobs=ArrayList<Job>();@JvmField val ready=ArrayList<Pair<Job,Any>>()
 fun <T> execute(tag:Any,backgroundTask:()->T?,runOnMain:Boolean=true,onResult:((T)->Unit)?=null){jobs.add(Job(tag,backgroundTask as ()->Any?,{onResult?.invoke(it as T)}))}
 @JvmStatic fun background(index:Int=0){val j=jobs.removeAt(index);android.os.Looper.local.set(android.os.Looper.WORKER);try {j.background()?.let {ready.add(j to it)}} finally {android.os.Looper.local.set(android.os.Looper.MAIN)}}
 @JvmStatic fun result(index:Int=0){val(j,v)=ready.removeAt(index);j.result(v)}
 @JvmStatic fun drain(){while(jobs.isNotEmpty())background(0);while(ready.isNotEmpty())result(0)}
}
