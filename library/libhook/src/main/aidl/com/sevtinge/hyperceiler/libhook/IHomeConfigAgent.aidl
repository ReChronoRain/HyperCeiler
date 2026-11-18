package com.sevtinge.hyperceiler.libhook;
import android.os.ParcelFileDescriptor;

/** Narrow control interface; no paths, Shell commands or arbitrary Binder forwarding. */
interface IHomeConfigAgent {
    int protocolVersion();
    boolean initializeFramework(in ParcelFileDescriptor sharedMemory);
    boolean initializeFromCallerMap(long nonceBlockAddress);
    boolean isReady();
    void refresh();
    void stop();
}
