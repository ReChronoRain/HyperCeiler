package com.hchen.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface HookBase {
    String pkg();

    boolean isPad();

    int tarAndroid();

    boolean skip() default false;
}
