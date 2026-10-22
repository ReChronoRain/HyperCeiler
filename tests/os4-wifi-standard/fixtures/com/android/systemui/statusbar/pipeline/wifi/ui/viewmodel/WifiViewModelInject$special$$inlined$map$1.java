package com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel;
import kotlin.*;import kotlin.coroutines.*;import kotlin.coroutines.intrinsics.*;import kotlinx.coroutines.flow.*;
// R8 collection branch fixture. Nonstandard branches are distinguishable and retain their original callback/result.
public final class WifiViewModelInject$special$$inlined$map$1 implements Flow<Object> {
 public final int $r8$classId;public Object $this_unsafeTransform$inlined;public int originalCalls;public Object branchResult=Unit.INSTANCE;
 public WifiViewModelInject$special$$inlined$map$1(int id){$r8$classId=id;}
 public Object collect(FlowCollector<? super Object> c,Continuation<? super Unit> k){originalCalls++;if($r8$classId>=0&&$r8$classId<3)return branchResult;
  Flow<Object> upstream=(Flow<Object>)$this_unsafeTransform$inlined;
  Object v=upstream.collect(new FlowCollector<Object>(){public Object emit(Object n,Continuation<? super Unit> cont){int value=((Number)n).intValue();return c.emit(value>5?value:0,cont);}},k);
  return v==IntrinsicsKt.getCOROUTINE_SUSPENDED()?v:Unit.INSTANCE;
 }
}
