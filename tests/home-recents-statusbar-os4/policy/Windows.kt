package com.android.server.wm
import android.os.Handler
import android.view.inputmethod.ImeTracker
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.Hooks
interface InsetsTarget {fun getRequestedVisibleTypes():Int;fun isRequestedVisible(types:Int):Boolean}
class Session(@JvmField val mUid:Int,@JvmField val mPid:Int)
class WindowManagerService {
 @JvmField val mH=Handler()
 @JvmField val mGlobalLock=Any()
 var locked=false
 fun isKeyguardLocked()=locked
}
class DisplayContent {
 @JvmField var mCurrentFocus:WindowState?=null
 val policy=InsetsPolicy()
 fun getDisplayId()=0
 fun getInsetsPolicy()=policy
}
class WindowState(val dc:DisplayContent,@JvmField val mSession:Session):InsetsTarget {
 var types=15
 var shown=true
 fun getDisplayContent()=dc
 fun isVisible()=shown
 override fun getRequestedVisibleTypes()=Hooks.result(this,"getRequestedVisibleTypes",emptyArray(),types) as Int
 override fun isRequestedVisible(types:Int)=Hooks.result(this,"isRequestedVisible",arrayOf(types),(this.types and types)!=0) as Boolean
}
class InsetsPolicy {
 var appliedTypes=0
 var calls=0
 fun onRequestedVisibleTypesChanged(caller:InsetsTarget,changedTypes:Int,token:ImeTracker.Token?){
  check(changedTypes==1)
  appliedTypes=caller.getRequestedVisibleTypes();calls++
 }
}
