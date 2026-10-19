package kotlinx.coroutines.flow;
import kotlin.*;import kotlin.coroutines.*;
// Exact R8 field layout; upstream scheduling/combine is a supplied real kotlinx Flow, not reimplemented by this fixture.
public final class FlowKt__ZipKt$combine$$inlined$combineUnsafe$FlowKt__ZipKt$1 implements Flow<Object> {
 public Flow<?>[] $flows$inlined;public final int $r8$classId;public Function<?> $transform$inlined$1;
 public Flow<Object> delegate;public int calls;public FlowCollector<?>collector;public Continuation<?>continuation;public RuntimeException failure;public boolean rawReturn;public Object result;
 public FlowKt__ZipKt$combine$$inlined$combineUnsafe$FlowKt__ZipKt$1(int id){$r8$classId=id;}
 public Object collect(FlowCollector<? super Object> c,Continuation<? super Unit> k){calls++;collector=c;continuation=k;if(failure!=null)throw failure;if(rawReturn)return result;return delegate.collect(c,k);}
}
