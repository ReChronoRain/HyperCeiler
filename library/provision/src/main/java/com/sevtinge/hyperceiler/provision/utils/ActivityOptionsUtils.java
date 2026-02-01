package com.sevtinge.hyperceiler.provision.utils;

import android.app.ActivityOptions;
import android.content.Context;
import android.os.Handler;

import java.lang.reflect.Method;

public class ActivityOptionsUtils {

    public static ActivityOptions makeCustomTaskAnimation(Context context, int enterResId, int exitResId) {
        try {
            // 1. 获取 ActivityOptions 类
            Class<?> activityOptionsClass = ActivityOptions.class;

            // 2. 获取隐藏接口的 Class 对象（即便传 null，定义方法签名也必须用到它们）
            Class<?> startListenerClass = Class.forName("android.app.ActivityOptions$OnAnimationStartedListener");
            Class<?> finishListenerClass = Class.forName("android.app.ActivityOptions$OnAnimationFinishedListener");

            // 3. 获取完整的 6 参数方法
            Method makeCustomTaskAnimationMethod = activityOptionsClass.getMethod(
                    "makeCustomTaskAnimation",
                    Context.class,   // 参数 1
                    int.class,       // 参数 2
                    int.class,       // 参数 3
                    Handler.class,   // 参数 4
                    startListenerClass, // 参数 5
                    finishListenerClass // 参数 6
            );

            // 4. 执行调用
            // 前三个传你的实际值，后面三个直接传 null 即可
            ActivityOptions options = (ActivityOptions) makeCustomTaskAnimationMethod.invoke(
                    null,           // 静态方法，此项为 null
                    context,        // context
                    enterResId,     // 入场动画资源 ID
                    exitResId,      // 退场动画资源 ID
                    null,           // handler 传 null
                    null,           // startedListener 传 null
                    null            // finishedListener 传 null
            );

            // 5. 使用获取到的 options 启动 Activity
            // context.startActivity(intent, options.toBundle());
            return options;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
}
