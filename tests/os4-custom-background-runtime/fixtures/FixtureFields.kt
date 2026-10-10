import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setAdditionalInstanceField
object FixtureFields{@JvmStatic fun set(o:Any,k:String,v:Any){o.setAdditionalInstanceField(k,v)}}
