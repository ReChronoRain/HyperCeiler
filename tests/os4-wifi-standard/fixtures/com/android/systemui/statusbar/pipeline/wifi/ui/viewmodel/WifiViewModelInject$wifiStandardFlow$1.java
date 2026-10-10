package com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel;
import kotlin.*;import kotlin.coroutines.*;import kotlin.jvm.functions.Function6;import java.lang.reflect.*;import fixture.HookRuntime;
// Host transcription of the complete ROM producer branch semantics. Android network/platform lookup is represented by input fields.
public final class WifiViewModelInject$wifiStandardFlow$1 implements Function6<Object,Object,Object,Object,Object,Object,Object> {
 public boolean enabled=true,hide,metered,carrierMerged,active=true,slave,mediatek;public int standard=5,label,calls;public RuntimeException failure;public Object overrideValue;public boolean overrideSet;
 public Object invoke(Object a,Object b,Object c,Object d,Object e,Object f){return call(Unit.INSTANCE);}
 public Object call(Object value){try{return HookRuntime.dispatch(getClass().getDeclaredMethod("invokeSuspend",Object.class),this,value);}catch(NoSuchMethodException e){throw new AssertionError(e);}}
 public Object invokeSuspend(Object result){calls++;if(label!=0)throw new IllegalStateException("resume before invoke");ResultKt.throwOnFailure(result);if(failure!=null)throw failure;if(overrideSet)return overrideValue;int n=0;if(enabled&&!hide&&!metered&&!carrierMerged){if(active||(slave&&!active)){n=switch(standard){case 4->4;case 5->5;case 6->6;case 7,8->active||mediatek?7:6;default->0;};}}return Integer.valueOf(n);}
}
