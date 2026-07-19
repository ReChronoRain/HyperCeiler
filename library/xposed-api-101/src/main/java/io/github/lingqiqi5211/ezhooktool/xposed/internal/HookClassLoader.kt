package io.github.lingqiqi5211.ezhooktool.xposed.internal

import io.github.lingqiqi5211.ezhooktool.core.EzReflect

internal object HookClassLoader {
    fun currentOrDefault(explicit: ClassLoader? = null): ClassLoader =
        explicit ?: EzReflect.safeClassLoader
}
