package com.sevtinge.hyperceiler.module.base.dexkit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.result.BaseDataList;

public interface IDexKitData {
    BaseDataList<?> dexkit(DexKitBridge bridge);
}
