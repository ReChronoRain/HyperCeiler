package io.github.lingqiqi5211.ezhooktool.xposed
import android.content.Context
object EzXposed {
    lateinit var appContext: Context
    fun initAppContext(context: Context?) { if (context != null) appContext = context }
}
