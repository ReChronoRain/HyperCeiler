package com.sevtinge.cemiuiler.module.base;

public interface IXposedHook {

    void initZygote();

    void handleLoadPackage();
}
