package com.sevtinge.hyperceiler.module.hook.calendar;

import android.content.Context;
import android.view.ViewGroup;

import com.sevtinge.hyperceiler.module.base.BaseHook;
import com.sevtinge.hyperceiler.module.base.dexkit.DexKit;
import com.sevtinge.hyperceiler.module.base.dexkit.IDexKit;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.base.BaseData;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import fan.recyclerview.widget.RecyclerView;

public class RemoveFortuneCard extends BaseHook {
    @Override
    public void init() throws NoSuchMethodException {
        Method method = DexKit.findMember("CalendarCard", new IDexKit() {
            @Override
            public BaseData dexkit(DexKitBridge bridge) throws ReflectiveOperationException {
                MethodData methodData = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .declaredClass(ClassMatcher.create()
                            .usingStrings("Cal:D:CardController"))
                        .paramTypes(ViewGroup.class, int.class, Context.class)
                    )).singleOrNull();
                return methodData;
            }
        });
        logD(TAG, lpparam.packageName, "method is "+method);
        hookMethod(method, new MethodHook() {
            @Override
            protected void before(XC_MethodHook.MethodHookParam param) throws Throwable {
                int id = (int)param.args[1];
                if(id == 42) {
                    param.setResult(null);
                }
            }
        });
    }
}
