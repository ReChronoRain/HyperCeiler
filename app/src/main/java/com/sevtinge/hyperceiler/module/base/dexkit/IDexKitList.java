package com.sevtinge.hyperceiler.module.base.dexkit;

import org.luckypray.dexkit.DexKitBridge;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;

public interface IDexKitList {
    ArrayList<AnnotatedElement> dexkit(DexKitBridge bridge)
            throws ReflectiveOperationException;
}
