package com.sevtinge.hyperceiler.module.base.dexkit;

import org.luckypray.dexkit.DexKitBridge;

import java.lang.reflect.AnnotatedElement;
import java.util.List;

public interface IDexKitList {
    List<AnnotatedElement> dexkit(DexKitBridge bridge)
            throws ReflectiveOperationException;
}
