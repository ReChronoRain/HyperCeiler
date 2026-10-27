package io.github.lingqiqi5211.ezhooktool.xposed.dsl
import java.lang.reflect.Method
class Param(val thisObject:Any,val args:Array<out Any?>,var result:Any?)
object Hooks {
 val hooks=mutableMapOf<Method,MutableList<(Param)->Unit>>()
 fun result(target:Any,name:String,args:Array<out Any?>,original:Any):Any? {
  val p=Param(target,args,original)
  for((m,callbacks) in hooks) if(m.name==name && m.parameterCount==args.size && m.declaringClass.isInstance(target))callbacks.forEach{it(p)}
  return p.result
 }
}
fun Method.createAfterHook(callback:(Param)->Unit){Hooks.hooks.getOrPut(this){mutableListOf()}.add(callback)}
