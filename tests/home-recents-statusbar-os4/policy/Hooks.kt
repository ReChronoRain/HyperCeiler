package io.github.lingqiqi5211.ezhooktool.xposed.dsl
import java.lang.reflect.Method
import io.github.libxposed.api.XposedInterface
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam

/** Only installation/original WMS code is simulated, callback forwarding uses the actual library. */
object Hooks {
 val hooks=mutableMapOf<Method,MutableList<XposedInterface.Hooker>>()
 var originals=0
 var primitiveResubmissions=0
 fun result(target:Any,name:String,args:Array<out Any?>,original:Any):Any? {
  val entries=hooks.entries.filter{(m,_)->m.name==name && m.parameterCount==args.size && m.declaringClass.isInstance(target)}
  if(entries.isEmpty())return original
  check(entries.size==1)
  val (method,callbacks)=entries.single()
  fun invokeAt(index:Int):Any? {
   if(index==callbacks.size){originals++;return original}
   return callbacks[index].intercept(object:XposedInterface.Chain {
    override fun getExecutable()=method
    override fun getThisObject()=target
    override fun getArgs():List<Any?> = args.toList()
    override fun getArg(index:Int)=args[index]
    override fun proceed()=invokeAt(index+1)
    override fun proceed(values:Array<out Any?>):Any? = replaced(target,values)
    override fun proceedWith(receiver:Any):Any? = replaced(receiver,args)
    override fun proceedWith(receiver:Any,values:Array<out Any?>):Any? = replaced(receiver,values)
    private fun replaced(receiver:Any?,values:Array<out Any?>):Any? {
     if(method.parameterTypes.any{it.isPrimitive}){
      primitiveResubmissions++
      throw IllegalArgumentException("primitive argument resubmitted as boxed value")
     }
     check(receiver===target && values.size==args.size && values.indices.all{values[it]===args[it]})
     return proceed()
    }
   })
  }
  return invokeAt(0)
 }
}
// Regression oracle for the previous production DSL path, no InvocationContext mock.
fun Method.createAfterHook(callback:(HookParam)->Unit){
 Hooks.hooks.getOrPut(this){mutableListOf()}.add(fixture.LegacyForwarding.after{callback(it)})
}
