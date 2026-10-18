/* SPDX-License-Identifier: AGPL-3.0-or-later */
package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import android.content.res.Resources;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.view.WindowManager;
import java.lang.reflect.Method;
import java.util.ArrayDeque;

/** Resolves current platform windows, insets and semantic resource IDs; no vendor Java owner/field. */
final class HomeGestureRegionsOS4 {
    private final Method windows;
    private final int navigationType;
    HomeGestureRegionsOS4() throws Exception {
        Class<?> global=Class.forName("android.view.WindowManagerGlobal");
        windows=global.getDeclaredMethod("getWindowViews");windows.setAccessible(true);
        navigationType=WindowManager.LayoutParams.class.getDeclaredField("TYPE_NAVIGATION_BAR").getInt(null);
    }
    HomeExtraGestureState.Geometry current() {
        try {
            Class<?> global=windows.getDeclaringClass();
            Object instance=global.getDeclaredMethod("getInstance").invoke(null);
            Object result=windows.invoke(instance);
            if(!(result instanceof Iterable<?> roots))return null;
            HomeExtraGestureState.Geometry found=null;
            for(Object item:roots) {
                if(!(item instanceof View root)||!root.isAttachedToWindow()||!root.isShown()
                    ||root.getAlpha()<=0||root.getDisplay()==null||root.getDisplay().getDisplayId()!=0
                    ||!(root.getLayoutParams() instanceof WindowManager.LayoutParams lp)
                    ||lp.type!=navigationType)continue;
                HomeExtraGestureState.Geometry g=read(root);
                if(g==null)continue;
                if(found!=null&&!found.equals(g))return null; // transitioning/ambiguous owners
                found=g;
            }
            return found;
        }catch(Exception ex){return null;}
    }
    private static Rect visibleOnScreen(View view) {
        Rect r=new Rect();if(!view.getGlobalVisibleRect(r)||r.isEmpty())return null;
        int[] screen=new int[2],window=new int[2];
        view.getLocationOnScreen(screen);view.getLocationInWindow(window);
        r.offset(screen[0]-window[0],screen[1]-window[1]);return r;
    }
    private static HomeExtraGestureState.Geometry read(View root) {
        Resources res=root.getResources();
        int mode=res.getIdentifier("config_navBarInteractionMode","integer","android");
        if(mode==0||res.getInteger(mode)!=2)return null; // never steal three-button navigation
        WindowInsets wi=root.getRootWindowInsets();if(wi==null)return null;
        Insets mandatory=wi.getInsetsIgnoringVisibility(WindowInsets.Type.mandatorySystemGestures());
        Insets gestures=wi.getInsetsIgnoringVisibility(WindowInsets.Type.systemGestures());
        Insets bars=wi.getInsets(WindowInsets.Type.navigationBars());
        Point size=new Point();root.getDisplay().getRealSize(size);
        Rect visible=visibleOnScreen(root);if(visible==null||visible.bottom<size.y)return null;
        int bottom=Math.max(mandatory.bottom,bars.bottom);
        if(bottom<=0||bottom>size.y/4)return null;
        float navTop=Math.max(visible.top,size.y-bottom);
        int assistant=res.getIdentifier("gestures_assistant_width","dimen",root.getContext().getPackageName());
        float originalWidth=assistant==0?0:res.getDimension(assistant);
        float corner=Math.min(size.x/4f,originalWidth>0&&Float.isFinite(originalWidth)?originalWidth:
            Math.max(bottom,Math.max(gestures.left,gestures.right)));
        View handle=null;ArrayDeque<View> todo=new ArrayDeque<>();todo.add(root);int count=0;
        while(!todo.isEmpty()) {
            if(++count>512)return null;
            View view=todo.removeFirst();
            if(!view.isShown()||view.getAlpha()<=0)continue;
            if(view.getId()!=View.NO_ID)try {
                String name=res.getResourceEntryName(view.getId());
                if(name.equals("home_handle")||name.equals("navigation_handle")) {
                    if(handle!=null&&handle!=view)return null;handle=view;
                }
            }catch(Resources.NotFoundException ignored){}
            if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)todo.add(group.getChildAt(i));
        }
        HomeExtraGestureState.Region region=null;
        if(handle!=null) {
            Rect hit=visibleOnScreen(handle);
            if(hit!=null) {
                // Some firmware gives the handle a full-width layout box. Use its live
                // overlay dimension, not a hard-coded pixel/DP width or the whole bottom strip.
                float width=hit.width();
                if(width>size.x/2f) {
                    int id=res.getIdentifier("navigation_home_handle_width","dimen",root.getContext().getPackageName());
                    width=id==0?0:res.getDimension(id);
                }
                if(width>0&&width<=size.x/2f) {
                    float pad=ViewConfiguration.get(root.getContext()).getScaledTouchSlop();
                    region=new HomeExtraGestureState.Region(Math.max(hit.left,hit.exactCenterX()-width/2-pad),
                        Math.max(hit.top,navTop),Math.min(hit.right,hit.exactCenterX()+width/2+pad),Math.min(hit.bottom,size.y));
                    if(!region.valid())region=null;
                }
            }
        }
        var g=new HomeExtraGestureState.Geometry(size.x,size.y,root.getDisplay().getRotation(),
            res.getDisplayMetrics().density,navTop,corner,region,root.getWindowToken());
        return g.valid()?g:null;
    }
}
