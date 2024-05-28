package com.sevtinge.hyperceiler.module.base.dexkit;

import org.luckypray.dexkit.DexKitBridge;

import java.lang.reflect.AnnotatedElement;

public interface IDexKit {
    AnnotatedElement dexkit(DexKitBridge bridge)
            throws ReflectiveOperationException;
}
