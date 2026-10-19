package io.github.lingqiqi5211.ezhooktool.xposed.java;
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam;
public interface IMethodHook {
    default void before(HookParam p) { /* Hooks may observe only the after callback. */ }
    default void after(HookParam p) { /* Hooks may observe only the before callback. */ }
}
