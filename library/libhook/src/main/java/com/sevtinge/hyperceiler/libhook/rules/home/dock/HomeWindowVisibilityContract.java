/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.dock;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** Platform visibility edges, not a traversal poll or a vendor field offset. */
public final class HomeWindowVisibilityContract {
    public record Contract(Method visible, List<Method> edges) { }
    private HomeWindowVisibilityContract() { }

    public static Contract resolve(Class<?> window) throws ReflectiveOperationException {
        Method visible=window.getDeclaredMethod("isVisible");
        if(visible.getReturnType()!=boolean.class||Modifier.isStatic(visible.getModifiers()))
            throw new NoSuchMethodException("Instance window visibility predicate required");
        List<Method> edges=new ArrayList<>();
        for(Method method:window.getDeclaredMethods()) {
            if(Modifier.isStatic(method.getModifiers()))continue;
            Class<?>[] parameters=method.getParameterTypes();
            boolean current=method.getReturnType()==void.class&&(
                method.getName().equals("onSurfaceShownChanged")&&parameters.length==1&&parameters[0]==boolean.class
                ||method.getName().equals("sendAppVisibilityToClients")&&parameters.length==0);
            if(current)edges.add(method);
        }
        if(edges.isEmpty())for(Method method:window.getDeclaredMethods()) {
            if(!method.getName().equals("onAppVisibilityChanged")||Modifier.isStatic(method.getModifiers()))continue;
            Class<?>[] parameters=method.getParameterTypes();
            if(parameters.length<1||parameters.length>2)continue;
            if(method.getReturnType()!=void.class&&method.getReturnType()!=boolean.class)continue;
            boolean booleans=true;
            for(Class<?> parameter:parameters)if(parameter!=boolean.class)booleans=false;
            if(booleans)edges.add(method);
        }
        if(edges.isEmpty())throw new NoSuchMethodException("No supported platform window visibility edge");
        visible.setAccessible(true);
        for(Method edge:edges)edge.setAccessible(true);
        return new Contract(visible,List.copyOf(edges));
    }
}
