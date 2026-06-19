/*
  * This file is part of HyperCeiler.

  * HyperCeiler is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as
  * published by the Free Software Foundation, either version 3 of the
  * License.

  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.

  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <https://www.gnu.org/licenses/>.

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/

package com.sevtinge.hyperceiler.libhook.rules.home.gesture;

import static com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper.Miui.isPad;
import static com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils.findAndHookConstructor;

import android.content.Context;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;

import com.sevtinge.hyperceiler.libhook.appbase.systemframework.GlobalActionBridge;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.HomeBaseHookNew;
import com.sevtinge.hyperceiler.libhook.appbase.mihome.Version;
import com.sevtinge.hyperceiler.libhook.callback.IMethodHook;
import com.sevtinge.hyperceiler.libhook.utils.hookapi.tool.EzxHelpUtils;

import io.github.kyuubiran.ezxhelper.xposed.EzXposed;
import io.github.kyuubiran.ezxhelper.xposed.common.HookParam;

/**
 * Hook rule responsible for hijacking the default system assistant corner-swipe actions 
 * on the MIUI/HyperOS stock launcher (MiuiHome) and mapping them to custom user actions.
 */
public class CornerSlide extends HomeBaseHookNew {
    
    // Tracks swipe origin direction: 0 for Left Corner, 1 for Right Corner
    public int inDirection = 0;

    private Class<?> canTriggerAssistantActionParam3;
    Context mContext;
    Class<?> mGestureOperationHelperClass;

    /**
     * Target hook initialization for HyperOS 1.0+ / Android 14+ (OS3 branch)
     * MIUI/HyperOS versions higher than 600000000 use long values for specific parameter indices.
     */
    @SuppressWarnings("unused")
    @Version(isPad = false, min = 600000000)
    private void initOS3Hook() {
        canTriggerAssistantActionParam3 = long.class;
        initBaseCore();
    }

    /**
     * Default initialization wrapper fallback for legacy/older system versions.
     */
    @Override
    public void initBase() {
        canTriggerAssistantActionParam3 = int.class;
        initBaseCore();
    }

    /**
     * Orchestrates the initialization layout by finding dependencies and registering all necessary hooks.
     */
    public void initBaseCore() {
        // Force the device to always report that Google Assistant (or equivalent) gesture triggers are structurally supported.
        findAndHookMethod("com.android.systemui.shared.recents.system.AssistManager", "isSupportGoogleAssist", int.class, returnConstant(true));
        
        // Resolve references to internal target launcher framework classes
        Class<?> FsGestureAssistHelper = findClassIfExists("com.miui.home.recents.FsGestureAssistHelper");
        Class<?> gestureModeAssistantClass = findClassIfExists("com.miui.home.recents.GestureModeAssistant");
        mGestureOperationHelperClass = findClassIfExists("com.miui.home.recents.GestureOperationHelper");

        // Distribute hooks into separate modular routines to resolve high NPath complexity
        hookCanTriggerAssistantAction(FsGestureAssistHelper);
        hookHandleTouchEvent(FsGestureAssistHelper);
        hookOnStartGesture(gestureModeAssistantClass);
        hookNavStubViewConstructor();
        hookStartAssistant();
    }

    /**
     * Hooks the underlying verification calculation that decides if a user's swipe coordinates 
     * qualify as a corner assistant trigger.
     */
    private void hookCanTriggerAssistantAction(Class<?> FsGestureAssistHelper) {
        // Tablets (Pads) match parameter signatures against explicit integer indexes, phones use long types
        Class<?> paramType = canTriggerAssistantActionParam3; //isPad() ? int.class : long.class;
        
        findAndHookMethod(FsGestureAssistHelper, "canTriggerAssistantAction", float.class, float.class, paramType, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                // Ensure assistant triggers haven't been forcefully disabled by system settings
                boolean isDisabled = (boolean) EzxHelpUtils.callStaticMethod(FsGestureAssistHelper, "isAssistantGestureDisabled", param.getArgs()[2]);
                if (!isDisabled) {
                    int mAssistantWidth = (int) EzxHelpUtils.getObjectField(param.getThisObject(), "mAssistantWidth");
                    float f = (float) param.getArgs()[0];  // Current X coordinate of gesture input
                    float f2 = (float) param.getArgs()[1]; // Screen edge width maximum bounding limit
                    
                    // Force system to return true if coordinate matches within the left or right corner boundary zones
                    if (f < mAssistantWidth || f > f2 - mAssistantWidth) {
                        param.setResult(true);
                        return;
                    }
                }
                param.setResult(false);
            }
        });
    }

    /**
     * Hooks raw touch events to immediately intercept when a user places their finger down on the screen.
     */
    private void hookHandleTouchEvent(Class<?> FsGestureAssistHelper) {
        hookAllMethods(FsGestureAssistHelper, "handleTouchEvent", new IMethodHook() {
            @Override
            public void after(HookParam param) {
                MotionEvent motionEvent = (MotionEvent) param.getArgs()[0];
                // Record orientation data early on structural ACTION_DOWN flags
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    updateDirection(param.getThisObject(), motionEvent);
                }
            }
        });
    }

    /**
     * Monitors when the gesture tracking system fires its specialized initiation routines.
     */
    private void hookOnStartGesture(Class<?> gestureModeAssistantClass) {
        if (gestureModeAssistantClass != null) {
            hookAllMethods(gestureModeAssistantClass, "onStartGesture", new IMethodHook() {
                @Override
                public void after(HookParam param) {
                    // Update current tracked state based on cached baseline raw coordinate entries
                    updateDirectionFromDownPoint(
                            param.getThisObject(),
                            EzxHelpUtils.getFloatField(param.getThisObject(), "mDownX"),
                            EzxHelpUtils.getFloatField(param.getThisObject(), "mDownY")
                    );
                }
            });
        }
    }

    /**
     * Captures and retains instances of the navigation frame context when initialized by the launcher UI thread.
     */
    private void hookNavStubViewConstructor() {
        findAndHookConstructor("com.miui.home.recents.NavStubView", Context.class, new IMethodHook() {
            @Override
            public void after(HookParam param) {
                mContext = (Context) param.getArgs()[0];
            }
        });
    }

    /**
     * Intercepts the final intent dispatch bound for the system Assistant service, 
     * replacing it with custom mapped user actions config settings.
     */
    private void hookStartAssistant() {
        findAndHookMethod("com.miui.home.recents.SystemUiProxyWrapper", "startAssistant", Bundle.class, new IMethodHook() {
            @Override
            public void before(HookParam param) {
                Bundle bundle = (Bundle) param.getArgs()[0];
                // Intent bundle safety check: Verify the event source matches expected assistant corner metadata indices
                if (bundle.getInt("triggered_by", 0) != 83 || bundle.getInt("invocation_type", 0) != 1) {
                    return;
                }
                
                // Map internal integer flag to a target direction config handle string
                String direction = inDirection == 1 ? "right" : "left";
                
                // Fire custom action registered within HyperCeiler config hooks. 
                // If intercepted cleanly, block execution to stop stock Google Assistant from popping up.
                if (GlobalActionBridge.handleAction(
                        EzXposed.getAppContext(),
                        "home_navigation_assist_" + direction + "_slide"
                )) {
                    param.setResult(null); // Cancel the rest of the original execution pipeline
                }
            }
        });
    }

    /**
     * Direction translation wrapper extracting raw floats directly from a MotionEvent object instance.
     */
    private void updateDirection(Object helper, MotionEvent motionEvent) {
        updateDirectionFromDownPoint(helper, motionEvent.getRawX(), motionEvent.getRawY());
    }

    /**
     * Sets internal state values depending on whether input fell closer to the left side or the right side boundaries.
     */
    private void updateDirectionFromDownPoint(Object helper, float downX, float downY) {
        // Fall back to specialized tablet geometric regions if conditions apply
        if (isPad() && updateDirectionWithGestureRegions(downX, downY)) {
            return;
        }
        
        // Baseline calculation logic loop intended for phone form factors
        int assistantWidth = EzxHelpUtils.getIntField(helper, "mAssistantWidth");
        inDirection = downX < assistantWidth ? 0 : 1;
    }

    /**
     * Custom bounding box calculation loop specific to tablet sizing metrics, utilizing RectF mappings from MIUI.
     */
    private boolean updateDirectionWithGestureRegions(float downX, float downY) {
        if (mGestureOperationHelperClass == null) {
            return false;
        }
        try {
            // Pull static predefined bounding boxes for structural screen regions out of launcher classes
            RectF leftRegion = (RectF) EzxHelpUtils.getStaticObjectField(mGestureOperationHelperClass, "REGION_BOTTOM_LEFT_CORNER");
            RectF rightRegion = (RectF) EzxHelpUtils.getStaticObjectField(mGestureOperationHelperClass, "REGION_BOTTOM_RIGHT_CORNER");
            
            if (leftRegion != null && leftRegion.contains(downX, downY)) {
                inDirection = 0;
                return true;
            }
            if (rightRegion != null && rightRegion.contains(downX, downY)) {
                inDirection = 1;
                return true;
            }
        } catch (Throwable ignored) {
            // Absorb lookup crashes smoothly to prevent launcher system crashes (SystemUI/Home stability)
        }
        return false;
    }
}
