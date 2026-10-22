@file:OptIn(kotlinx.coroutines.InternalCoroutinesApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
import com.sevtinge.hyperceiler.libhook.rules.systemui.statusbar.icon.v.WifiStandard
import com.sevtinge.hyperceiler.libhook.base.BaseHook
import com.sevtinge.hyperceiler.common.utils.PrefsBridge
import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper
import com.sevtinge.hyperceiler.libhook.utils.hookapi.StateFlowHelper
import com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.`WifiViewModelInject$special$$inlined$map$1` as Outer
import com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.`WifiViewModelInject$wifiStandardFlow$1` as Producer
import kotlinx.coroutines.flow.`FlowKt__ZipKt$combine$$inlined$combineUnsafe$FlowKt__ZipKt$1` as Combined
import fixture.HookRuntime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import java.util.concurrent.CancellationException
var passed=0;var failed=0
val owner="com.android.systemui.statusbar.pipeline.wifi.ui.viewmodel.WifiViewModelInject"
val collectMethod=Outer::class.java.getDeclaredMethod("collect",FlowCollector::class.java,Continuation::class.java)
fun reset(){HookRuntime.hooks.clear();HookRuntime.originalCalls=0;BaseHook.classes.clear();BaseHook.errors.clear();BaseHook.installs=0;BaseHook.failAt=0;BaseHook.failInstall=false;BaseHook.lookups=0;BaseHook.preSkipped=false;BaseHook.preResult=null;BaseHook.failDex=true;PrefsBridge.mode=0;DeviceHelper.System.version=4f;StateFlowHelper.calls=0}
fun test(name:String,body:()->Unit){reset();try{body();passed++;println("PASS $name")}catch(e:Throwable){failed++;println("FAIL $name: $e");e.printStackTrace(System.out)}}
fun start(mode:Int){PrefsBridge.mode=mode;check(!WifiStandard.useDexKit());check(WifiStandard.initDexKit());check(BaseHook.lookups==0);WifiStandard.init()}
class PlainContinuation:Continuation<Unit>{override val context=EmptyCoroutineContext;var result:Result<Unit>?=null;override fun resumeWith(result:Result<Unit>){this.result=result}}
class Sink:FlowCollector<Any>{val values=mutableListOf<Any>();override suspend fun emit(value:Any){values.add(value)}}
fun combine(producer:Producer=Producer(),flow:Flow<Any> = flowOf(4,5,6,7,0)):Combined = Combined(2).also{
 it.`$transform$inlined$1`=producer;it.`$flows$inlined`=Array<Flow<*>>(5){flowOf(Unit)};it.delegate=flow
}
fun outer(source:Any?,id:Int=3)=Outer(id).also{it.`$this_unsafeTransform$inlined`=source}
fun call(o:Outer,c:FlowCollector<Any>,k:Continuation<Unit>):Any?=HookRuntime.dispatch(collectMethod,o,c,k)
// Real coroutine machinery creates/resumes this continuation. No label is read or assigned by the test.
fun targetFlow(o:Outer):Flow<Any> = object:Flow<Any>{override suspend fun collect(collector:FlowCollector<Any>){suspendCoroutineUninterceptedOrReturn<Unit>{k->call(o,collector,k)}}}
fun values(o:Outer,slow:Boolean=false):List<Any> = runBlocking{val out=mutableListOf<Any>();targetFlow(o).collect{if(slow)delay(1);out.add(it)};out}
class WrongShape{fun collect(a:Any,b:Any):Any=Unit}
class LegacyCollector {var calls=0;fun emit(a:Any,b:Continuation<Unit>):Any{calls++;return Unit}}
fun main(){HookRuntime.setup()
 for(mode in listOf(-1,0,3,99))test("default_or_invalid_no_hooks_$mode"){start(mode);check(HookRuntime.hooks.isEmpty())}
 for(mode in 1..2)test("one_exact_hook_no_ctor_or_shared_emitter_$mode"){start(mode);check(HookRuntime.hooks.size==1);val m=HookRuntime.hooks.keys.single();check(m is java.lang.reflect.Method&&m.name==if(mode==1)"collect" else "invokeSuspend");check(StateFlowHelper.calls==0)}
 test("earlier_skip_preserves_result_no_collection"){BaseHook.preSkipped=true;val token=Any();BaseHook.preResult=token;start(1);val src=combine();val o=outer(src);check(call(o,Sink(),PlainContinuation())===token&&src.calls==0&&o.originalCalls==0)}
 test("null_source_keeps_original_failure"){start(1);val o=outer(null);var caught:Throwable?=null;try{call(o,Sink(),PlainContinuation())}catch(e:Throwable){caught=e};check(caught is NullPointerException&&o.originalCalls==1)}
 test("null_flow_array_declines"){start(1);val src=combine().apply{`$flows$inlined`=null};val o=outer(src);check(values(o)==listOf(0,0,6,7,0)&&o.originalCalls==1)}
 test("mode2_real_stateIn_shared_consumers"){start(2);val p=Producer().apply{standard=7};val o=outer(combine(p,flow{emit(p.call(Unit))}));runBlocking{val scope=CoroutineScope(coroutineContext+SupervisorJob());try{val shared=targetFlow(o).stateIn(scope,SharingStarted.Eagerly,99);val left=shared.combine(flowOf(false)){std,metered->(std as Int)>0||metered}.stateIn(scope,SharingStarted.Eagerly,true);withTimeout(1000){shared.first{it==0};left.first{!it}};check(shared.value==0&&!left.value&&p.calls==1)}finally{scope.cancel()}};check(o.originalCalls==1)}
 test("mode1_sync_no_caller_label_required"){start(1);val src=combine();val o=outer(src);val c=Sink();val k=PlainContinuation();check(call(o,c,k)===Unit);check(c.values==listOf(4,5,6,7,0));check(src.collector===c&&src.continuation===k&&src.calls==1&&o.originalCalls==0&&k.result==null)}
 test("mode1_real_suspending_collector_resumes_once"){start(1);val src=combine();val o=outer(src);check(values(o,true)==listOf(4,5,6,7,0));check(src.calls==1&&o.originalCalls==0)}
 test("mode1_real_suspending_upstream"){start(1);val src=combine(flow=flow{emit(4);delay(2);emit(5);yield();emit(6)});val o=outer(src);check(values(o,true)==listOf(4,5,6));check(src.calls==1&&o.originalCalls==0)}
 test("two_real_collectors_keep_independent_continuations"){start(1);val src=combine(flow=flow{emit(4);delay(1);emit(5)});val o=outer(src);runBlocking{val a=async{targetFlow(o).toList()};val b=async{targetFlow(o).toList()};check(a.await()==listOf(4,5)&&b.await()==listOf(4,5))};check(src.calls==2&&o.originalCalls==0)}
 test("same_suspension_marker"){start(1);val src=combine().apply{rawReturn=true;result=COROUTINE_SUSPENDED};val o=outer(src);check(call(o,Sink(),PlainContinuation())===COROUTINE_SUSPENDED);check(o.originalCalls==0&&src.calls==1)}
 for(value in listOf(null,Any(),17,Unit))test("sync_result_unit_$value"){start(1);val src=combine().apply{rawReturn=true;result=value};check(call(outer(src),Sink(),PlainContinuation())===Unit)}
 for(id in listOf(0,1,2))test("unrelated_outer_branch_$id"){start(1);val token=Any();val o=outer(null,id).apply{branchResult=token};check(call(o,Sink(),PlainContinuation())===token&&o.originalCalls==1)}
 for(id in listOf(-1,4,99))test("unknown_outer_id_$id"){start(1);val src=combine();val o=outer(src,id);check(values(o)==listOf(0,0,6,7,0));check(o.originalCalls==1)}
 for(id in listOf(-1,0,1,3))test("wrong_combine_id_$id"){start(1);val src=Combined(id).apply{`$transform$inlined$1`=Producer();`$flows$inlined`=Array<Flow<*>>(5){flowOf(Unit)};delegate=flowOf(5)};val o=outer(src);check(values(o)==listOf(0)&&o.originalCalls==1)}
 test("wrong_transform"){start(1);val src=combine().apply{`$transform$inlined$1`={Unit}};val o=outer(src);check(values(o)==listOf(0,0,6,7,0)&&o.originalCalls==1)}
 test("null_transform"){start(1);val src=combine().apply{`$transform$inlined$1`=null};val o=outer(src);check(values(o)==listOf(0,0,6,7,0)&&o.originalCalls==1)}
 for(size in listOf(0,4,6))test("wrong_flow_count_$size"){start(1);val src=combine().apply{`$flows$inlined`=Array<Flow<*>>(size){flowOf(Unit)}};val o=outer(src);check(values(o)==listOf(0,0,6,7,0)&&o.originalCalls==1)}
 test("null_flow_entry"){start(1);val src=combine();src.`$flows$inlined`[2]=null;val o=outer(src);check(values(o)==listOf(0,0,6,7,0)&&o.originalCalls==1)}
 test("other_upstream_flow"){start(1);val o=outer(flowOf(4,5,7));check(values(o)==listOf(0,0,7)&&o.originalCalls==1)}
 for(exception in listOf(IllegalStateException("upstream"),CancellationException("cancel")))test("upstream_exception_identity_${exception.javaClass.simpleName}"){start(1);val src=combine().apply{failure=exception};val o=outer(src);var caught:Throwable?=null;try{values(o)}catch(e:Throwable){caught=e};check(caught===exception&&src.calls==1&&o.originalCalls==0)}
 test("suspended_upstream_exception_identity"){start(1);val failure=IllegalArgumentException("after resume");val src=combine(flow=flow{emit(4);delay(1);throw failure});val o=outer(src);var caught:Throwable?=null;try{values(o,true)}catch(e:Throwable){caught=e};check(caught===failure&&src.calls==1&&o.originalCalls==0)}
 test("collector_exception_not_double_collection"){start(1);val failure=IllegalStateException("sink");val src=combine();val o=outer(src);var calls=0;var caught:Throwable?=null;try{runBlocking{targetFlow(o).collect{calls++;throw failure}}}catch(e:Throwable){caught=e};check(caught===failure&&calls==1&&src.calls==1&&o.originalCalls==0)}
 test("real_job_cancellation_runs_upstream_finally"){start(1);var finalized=0;val src=combine(flow=flow{try{emit(4);awaitCancellation()}finally{finalized++}});val o=outer(src);runBlocking{val entered=CompletableDeferred<Unit>();val j=launch{targetFlow(o).collect{entered.complete(Unit)}};entered.await();j.cancelAndJoin()};check(finalized==1&&src.calls==1&&o.originalCalls==0)}
 for(mode in 1..2)for(gate in 0..4)test("original_gate_${mode}_$gate"){start(mode);val p=Producer().apply{standard=5;when(gate){0->enabled=false;1->hide=true;2->metered=true;3->carrierMerged=true;4->active=false}};val src=combine(p,flow{emit(p.call(Unit))});val o=outer(src);check(values(o)==listOf(0)&&p.calls==1)}
 for(mode in 1..2)for(active in listOf(false,true))for(mtk in listOf(false,true))test("primary_slave_normalization_${mode}_${active}_$mtk"){start(mode);val p=Producer().apply{this.active=active;slave=!active;mediatek=mtk};val seen=mutableListOf<Int>();for(n in 0..8){p.standard=n;val want=if(mode==2)0 else when(n){4,5,6->n;7,8->if(active||mtk)7 else 6;else->0};val src=combine(p,flow{emit(p.call(Unit))});val got=values(outer(src));check(got==listOf(want));seen.add(got[0] as Int)};check(p.calls==9)}
 for(metered in listOf(false,true))test("mode2_common_standard_and_inoutleft_$metered"){start(2);val p=Producer().apply{standard=7};val std=p.call(Unit) as Int;val inoutLeft=(std>0||metered);val visible=std!=0;check(std==0&&!visible&&inoutLeft==metered&&p.calls==1)}
 for(value in listOf(null,Unit,COROUTINE_SUSPENDED,1L,2.0,"7"))test("mode2_non_integer_preserved_$value"){start(2);val p=Producer().apply{overrideSet=true;this.overrideValue=value};check(p.call(Unit)===value&&p.calls==1)}
 for(mode in 1..2)test("original_producer_failure_$mode"){start(mode);val failure=CancellationException("producer");val p=Producer().apply{this.failure=failure};var caught:Throwable?=null;try{p.call(Unit)}catch(e:Throwable){caught=e};check(caught===failure&&p.calls==1)}
 test("producer_label_failure_not_overridden"){start(2);val p=Producer().apply{label=1};var caught:Throwable?=null;try{p.call(Unit)}catch(e:Throwable){caught=e};check(caught is IllegalStateException&&p.calls==1)}
 for(mode in 1..2)test("mode_captured_not_changed_mid_collection_$mode"){start(mode);PrefsBridge.mode=3-mode;val p=Producer();val src=combine(p,flow{emit(p.call(Unit))});check(values(outer(src))==listOf(if(mode==1)5 else 0))}
 for(mode in 1..2)for(throwing in listOf(false,true))test("failed_registration_${mode}_$throwing"){BaseHook.failAt=1;BaseHook.failInstall=throwing;var caught:Throwable?=null;try{start(mode)}catch(e:Throwable){caught=e};check(caught is IllegalStateException&&HookRuntime.hooks.isEmpty())}
 for(name in listOf(owner+"\$wifiStandardFlow\$1",owner+"\$special\$\$inlined\$map\$1","kotlinx.coroutines.flow.FlowKt__ZipKt\$combine\$\$inlined\$combineUnsafe\$FlowKt__ZipKt\$1"))test("missing_class_$name"){BaseHook.classes[name]=null;var caught:Throwable?=null;try{start(1)}catch(e:Throwable){caught=e};check(caught is IllegalStateException&&HookRuntime.hooks.isEmpty())}
 test("wrong_method_shape_no_registration"){BaseHook.classes[owner+"\$wifiStandardFlow\$1"]=WrongShape::class.java;var caught:Throwable?=null;try{start(2)}catch(e:Throwable){caught=e};check(caught is NoSuchMethodException&&HookRuntime.hooks.isEmpty())}
 test("os3_retains_dexkit_and_legacy_registration"){DeviceHelper.System.version=3f;PrefsBridge.mode=1;BaseHook.failDex=false;BaseHook.selected=LegacyCollector::class.java.getDeclaredMethod("emit",Any::class.java,Continuation::class.java);check(WifiStandard.useDexKit());check(WifiStandard.initDexKit());check(BaseHook.lookups==1);WifiStandard.init();check(HookRuntime.hooks.keys.any{it==BaseHook.selected});check(HookRuntime.hooks.keys.any{it is java.lang.reflect.Constructor<*>});check(HookRuntime.hooks.keys.none{it==collectMethod})}
 println("RESULT passes=$passed failures=$failed");if(failed!=0)kotlin.system.exitProcess(1)
}
