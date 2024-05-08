package com.sevtinge.hyperceiler.ui.fragment.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RestartTag {

    boolean isRestartSystem() default false;

    int appLabel() default 0;

    String pkg() default "";
}
