package com.sevtinge.hyperceiler.module.base;

public interface IXposedHook {

    void initZygote();

    void handleLoadPackage();
}
