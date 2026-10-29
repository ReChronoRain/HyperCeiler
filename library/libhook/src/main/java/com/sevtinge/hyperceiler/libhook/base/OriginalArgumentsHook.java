/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.base;

import io.github.libxposed.api.XposedInterface;
import io.github.lingqiqi5211.ezhooktool.xposed.common.AfterChainStage;
import io.github.lingqiqi5211.ezhooktool.xposed.common.BeforeChainStage;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookChain;
import io.github.lingqiqi5211.ezhooktool.xposed.java.IMethodHook;
import java.lang.reflect.Executable;
import java.util.List;
import kotlin.Unit;

/** Keep the framework's original invocation when a callback only observes arguments.
 * EzHookTool 1.3 always re-submits boxed arguments through proceedWith, even when
 * unchanged. Some API 102 implementations reject boxed primitive replacements.
 * Explicit replacements must still go through the framework and retain its checks.
 */
public final class OriginalArgumentsHook {
    private OriginalArgumentsHook() { }
    public static XposedInterface.Hooker of(IMethodHook callback) {
        HookChain callbacks = new HookChain(List.of(
            new BeforeChainStage(p -> { callback.before(p); return Unit.INSTANCE; }),
            new AfterChainStage(p -> { callback.after(p); return Unit.INSTANCE; })));
        return chain -> callbacks.invoke(new Forwarding(chain));
    }
    private static final class Forwarding implements XposedInterface.Chain {
        private final XposedInterface.Chain delegate;
        private final Object receiver;
        private final List<Object> arguments;
        Forwarding(XposedInterface.Chain chain) {
            delegate = chain; receiver = chain.getThisObject(); arguments = chain.getArgs();
        }
        public Executable getExecutable() { return delegate.getExecutable(); }
        public Object getThisObject() { return receiver; }
        public List<Object> getArgs() { return arguments; }
        public Object getArg(int at) { return arguments.get(at); }
        private boolean unchanged(Object[] values) {
            if (values.length != arguments.size()) return false;
            for (int i = 0; i < values.length; ++i) if (values[i] != arguments.get(i)) return false;
            return true;
        }
        public Object proceed() throws Throwable { return delegate.proceed(); }
        public Object proceed(Object[] values) throws Throwable {
            return unchanged(values) ? delegate.proceed() : delegate.proceed(values);
        }
        public Object proceedWith(Object target) throws Throwable {
            return target == receiver ? delegate.proceed() : delegate.proceedWith(target);
        }
        public Object proceedWith(Object target, Object[] values) throws Throwable {
            return target == receiver && unchanged(values) ? delegate.proceed() : delegate.proceedWith(target, values);
        }
    }
}
