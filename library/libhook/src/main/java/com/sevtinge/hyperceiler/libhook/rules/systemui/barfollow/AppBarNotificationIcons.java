/*
 * Derived from MiuiBackGestureHook status-bar-follow implementation.
 * Source: https://github.com/wxxsfxyzm/MiuiBackGestureHook
 * Reference commit: 004bc059e5821775917b771d89aa1d8c18a1e669 (includes b6e582f and later fixes)
 * Copyright (C) 2026 MiuiBackGestureHook contributors.
 * Licensed under the Apache License, Version 2.0; modified for HyperCeiler.
 */
package com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow;

import android.content.Context;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.call;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.read;
import static com.sevtinge.hyperceiler.libhook.rules.systemui.barfollow.AppBarContentPreparation.write;

/** Owns real notification icon Views while retaining Xiaomi's default-display key lookup. */
final class AppBarNotificationIcons implements AutoCloseable {
    private final Object connected;
    private final Object binder;
    private final Object primaryBinder;
    private final Object primaryStore;
    private final Object primaryDelegate;
    private final Object primaryBlock;
    private final Object primaryModel;
    private final Object store;
    private final Object delegate;
    private final Object pipeline;
    private final Object iconManager;
    private final Collection<?> collectionListeners;
    private final Collection<Object> iconListeners;
    private final Object collectionListener;
    private final Object iconListener;
    private final Map<String, View> bundles = new HashMap<>();
    private final Map<String, Object> bundleOwners = new HashMap<>();
    private final Constructor<?> iconConstructor;
    private final Method bundleDescriptors;
    private final Method setBundleIcon;
    private final Context context;
    private final Class<?> collectionContract;
    private Object viewBinding;
    private Method disposeBinding;
    private boolean closing;
    private Object darkRepository;
    private Object darkInteractor;
    private Object darkState;
    private boolean closed;
    private final AppBarJobCompletion bindingJobs;

    static AppBarNotificationIcons create(ClassLoader loader, Object display, Object root,
                                          Object primaryRoot, Object connected)
            throws ReflectiveOperationException {
        requireMain();
        AppBarNotificationIcons icons = new AppBarNotificationIcons(loader, display, root, primaryRoot, connected);
        try {
            icons.install(loader);
            return icons;
        } catch (ReflectiveOperationException | RuntimeException | Error failure) {
            AppBarContentPreparation.cleanupCreation(icons, failure);
            throw failure;
        }
    }

    @SuppressWarnings("unchecked")
    private AppBarNotificationIcons(ClassLoader loader, Object display, Object root,
                                    Object primaryRoot, Object connected) throws ReflectiveOperationException {
        this.connected = connected;
        bindingJobs = new AppBarJobCompletion(loader);
        binder = read(root, "notificationIconsBinder");
        primaryBinder = read(primaryRoot, "notificationIconsBinder");
        primaryStore = read(primaryBinder, "defaultDisplayViewStore");
        primaryDelegate = read(primaryStore, "$$delegate_0");
        primaryBlock = read(primaryDelegate, "$block");
        primaryModel = read(primaryBinder, "viewModel");
        // This native method allocates both wrapper and key-lookup delegate fresh.
        store = call(display, "statusBarNotificationIconViewStore");
        delegate = read(store, "$$delegate_0");
        if (binder == primaryBinder || store == primaryStore || delegate == primaryDelegate)
            throw new IllegalStateException("Notification content owner is shared");
        pipeline = read(connected, "notifPipeline");
        if (read(delegate, "$this_iconViewStoreBy") != pipeline)
            throw new IllegalStateException("Notification lookup pipeline mismatch");
        iconManager = read(connected, "iconManager");
        context = (Context) read(read(iconManager, "iconBuilder"), "context");
        Class<?> iconClass = Class.forName("com.android.systemui.statusbar.StatusBarIconView", false, loader);
        Class<?> bundleClass = Class.forName("com.android.systemui.statusbar.notification.collection.BundleEntry", false, loader);
        Class<?> descriptor = Class.forName("com.android.internal.statusbar.StatusBarIcon", false, loader);
        iconConstructor = iconClass.getConstructor(Context.class, String.class, StatusBarNotification.class, boolean.class);
        bundleDescriptors = iconManager.getClass().getMethod("getIconDescriptors", Context.class, bundleClass);
        setBundleIcon = iconManager.getClass().getMethod("setIcon", descriptor, iconClass);
        collectionContract = Class.forName(
                "com.android.systemui.statusbar.notification.collection.notifcollection.NotifCollectionListener", false, loader);
        collectionListener = read(connected, "notifCollectionListener");
        iconListener = read(connected, "iconUpdateRequiredListener");
        collectionListeners = (Collection<?>) read(read(pipeline, "mNotifCollection"), "mNotifCollectionListeners");
        iconListeners = (Collection<Object>) read(iconManager, "onIconUpdateRequiredListeners");
    }

    private void install(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> function = Class.forName("kotlin.jvm.functions.Function1", false, loader);
        Object block = Proxy.newProxyInstance(loader, new Class<?>[]{function}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                switch (method.getName()) {
                    case "hashCode": return System.identityHashCode(proxy);
                    case "equals": return proxy == args[0];
                    case "toString": return "AppBarIconViews";
                    default: throw new UnsupportedOperationException(method.getName());
                }
            }
            if (!method.getName().equals("invoke") || args == null || args.length != 1)
                throw new IllegalStateException("Unexpected native icon lookup contract");
            return independentIcon(args[0]);
        });
        write(delegate, "$block", block);
        write(binder, "defaultDisplayViewStore", store);
        // Same registrations as native ConnectedDisplays...activate(), owned here
        // by the content lifetime. Never start a second activate coroutine as well.
        call(pipeline, "addCollectionListener", new Class<?>[]{collectionContract}, collectionListener);
        iconListeners.add(iconListener);
    }

    private Object independentIcon(Object pack) throws ReflectiveOperationException {
        requireMain();
        if (closing || closed || pack == null) return null;
        Object primary = read(pack, "mStatusBarIcon");
        if (primary == null) return null;
        Object bundle = read(primary, "mBundleEntry");
        if (bundle == null) {
            StatusBarNotification notification = (StatusBarNotification) call(primary, "getNotification");
            if (notification == null) return null;
            Object result = call(connected, "iconView", new Class<?>[]{String.class}, notification.getKey());
            if (result == primary) throw new IllegalStateException("App notification reused primary View");
            return result;
        }
        Map<?, ?> current = (Map<?, ?>) read(read(pipeline, "mShadeListBuilder"), "mIdToBundleEntry");
        // A bundle may be replaced under the same key. Never retain its old View
        // as the replacement's owner or keep removed bundles in the local cache.
        for (String key : new ArrayList<>(bundleOwners.keySet())) {
            if (current.get(key) != bundleOwners.get(key)) {
                bundleOwners.remove(key);
                bundles.remove(key);
            }
        }
        String key = entryKey(bundle);
        if (current.get(key) != bundle) return null;
        View result = bundles.get(key);
        if (result == null) {
            result = (View) iconConstructor.newInstance(context, key, null, false);
            write(result, "mBundleEntry", bundle);
            ((ImageView) result).setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            // Exact native BundleCoordinator icon creation, without replacing
            // BundleEntry.icons or creating a second notification row.
            Object descriptors = bundleDescriptors.invoke(null, context, bundle);
            setBundleIcon.invoke(null, call(descriptors, "component1"), result);
            bundles.put(key, result);
            bundleOwners.put(key, bundle);
        }
        return result;
    }

    void bindView(ClassLoader loader, View view) throws ReflectiveOperationException {
        requireMain();
        if (closing || closed || viewBinding != null || view.isAttachedToWindow())
            throw new IllegalStateException("Notification view lifecycle already active");
        Class<?> container = Class.forName("com.android.systemui.statusbar.phone.NotificationIconContainer", false, loader);
        if (!container.isInstance(view)) throw new IllegalStateException("Unknown App notification container");
        Class<?> continuation = Class.forName("kotlin.coroutines.Continuation", false, loader);
        Class<?> blockType = Class.forName("com.android.systemui.statusbar.notification.icon.ui.viewbinder."
                + "NotificationIconContainerStatusBarViewBinder$bindWhileAttached$1$1", false, loader);
        Constructor<?> constructor = blockType.getDeclaredConstructor(int.class, binder.getClass(), container, continuation);
        constructor.setAccessible(true);
        Object block = constructor.newInstance(0, binder, view, null);
        Class<?> function = Class.forName("kotlin.jvm.functions.Function3", false, loader);
        Method invoke = function.getMethod("invoke", Object.class, Object.class, Object.class);
        Object unit = Class.forName("kotlin.Unit", false, loader).getField("INSTANCE").get(null);
        Object ownedBlock = Proxy.newProxyInstance(loader, new Class<?>[]{function}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (method.getName().equals("equals")) return proxy == args[0];
                return "AppBarNotificationBinding";
            }
            if (closing || closed) return unit;
            bindingJobs.observeLifecycle(args[0]);
            try { return invoke.invoke(block, args); }
            catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
        });
        disposeBinding = Class.forName("kotlinx.coroutines.DisposableHandle", false, loader).getMethod("dispose");
        // Invoke the same block as native bindWhileAttached, but retain its
        // attachment handle. Display 0 selects our private defaultDisplayViewStore.
        viewBinding = Class.forName("com.android.systemui.lifecycle.RepeatWhenAttachedKt", false, loader)
                .getMethod("repeatWhenAttached$default", View.class,
                        Class.forName("kotlin.coroutines.CoroutineContext", false, loader),
                        Class.forName("kotlin.jvm.functions.Function3", false, loader), int.class)
                .invoke(null, view, null, ownedBlock, 3);
    }

    void prepareDarkModel(ClassLoader loader, Object component, Object display, Object dark, Object dump)
            throws ReflectiveOperationException {
        requireMain();
        if (darkRepository != null || closing || closed) throw new IllegalStateException("App dark model already owned");
        Object originalModel = read(binder, "viewModel");
        Object originalInteractor = read(originalModel, "darkIconInteractor");
        Object originalRepository = read(originalInteractor, "repository");
        Object displays = read(originalRepository, "displaySubComponentRepository");
        darkRepository = originalRepository.getClass().getConstructor(displays.getClass()).newInstance(displays);
        darkInteractor = originalInteractor.getClass().getConstructor(originalRepository.getClass()).newInstance(darkRepository);
        darkState = read(dark, "mDarkChangeFlow");
        Object keyguard = call(read(component, "keyguardInteractorProvider"), "get");
        Object observer = call(read(component, "notificationIconObserverProvider"), "get");
        Object shade = call(read(component, "shadeInteractorImplProvider"), "get");
        Object island = call(read(component, "dynamicIslandPluginControllerProvider"), "get");
        Object icons = call(display, "statusBarNotificationIconsInteractor");
        Constructor<?> constructor = originalModel.getClass().getConstructor(
                Class.forName("kotlin.coroutines.CoroutineContext", false, loader), originalInteractor.getClass(),
                Class.forName("com.android.systemui.dump.DumpManager", false, loader), icons.getClass(), keyguard.getClass(),
                observer.getClass(), Class.forName("com.android.systemui.shade.domain.interactor.ShadeInteractor", false, loader),
                island.getClass());
        Object model = constructor.newInstance(read(originalModel, "bgContext"), darkInteractor, dump,
                icons, keyguard, observer, shade, island);
        write(binder, "viewModel", model);
        // The fresh App binder may already own a different native model.
        // Verify Home against its captured identity, not the App model we replaced.
        if (read(primaryBinder, "viewModel") != primaryModel)
            throw new IllegalStateException("Primary notification model changed during private allocation");
    }

    Object darkInteractor() { return darkInteractor; }
    Object darkStateFor(Object repository, int displayId) {
        return repository == darkRepository && displayId == 0 ? darkState : null;
    }

    /** Exercise real current entries without attaching a window or modifying primary parents. */
    String verifyIndependentViews() throws ReflectiveOperationException {
        requireMain();
        Map<?, ?> notifications = (Map<?, ?>) read(read(pipeline, "mNotifCollection"), "mNotificationSet");
        Map<?, ?> bundleEntries = (Map<?, ?>) read(read(pipeline, "mShadeListBuilder"), "mIdToBundleEntry");
        int normal = 0;
        int grouped = 0;
        for (Object entry : new ArrayList<>(notifications.values())) {
            if (verifyEntry(entryKey(entry), read(entry, "mIcons"))) normal++;
        }
        for (Map.Entry<?, ?> entry : new ArrayList<>(bundleEntries.entrySet())) {
            if (verifyEntry((String) entry.getKey(), read(entry.getValue(), "icons"))) grouped++;
        }
        return "normal=" + normal + ", bundles=" + grouped;
    }

    private boolean verifyEntry(String key, Object pack) throws ReflectiveOperationException {
        if (pack == null) return false;
        View primary = (View) read(pack, "mStatusBarIcon");
        if (primary == null) return false;
        ViewParent parent = primary.getParent();
        View own = (View) call(store, "iconView", new Class<?>[]{String.class}, key);
        if (own == null || own == primary || own.getParent() != null)
            throw new IllegalStateException("No independent notification icon View");
        FrameLayout privateParent = new FrameLayout(context);
        privateParent.addView(own);
        try {
            if (call(store, "iconView", new Class<?>[]{String.class}, key) != own
                    || own.getParent() != privateParent || primary.getParent() != parent)
                throw new IllegalStateException("Icon lookup changed View ownership");
        } finally {
            privateParent.removeView(own);
        }
        return true;
    }

    @Override public void close() throws ReflectiveOperationException {
        requireMain();
        if (closed) return;
        closing = true;
        // Cancel native view population before releasing its private icon store.
        // Retain the handle when disposal fails so this owner can retry.
        if (viewBinding != null) {
            disposeBinding.invoke(viewBinding);
            viewBinding = null;
        }
        if (bindingJobs.pending())
            throw new IllegalStateException("Native notification binding is finishing cancellation");
        try {
            collectionListeners.remove(collectionListener);
        } finally {
            iconListeners.remove(iconListener);
            ((Map<?, ?>) read(connected, "cachedIcons")).clear();
            bundles.clear();
            bundleOwners.clear();
            // Keep the owned resolver installed: its closed gate returns null.
            // Restoring the native block would let a late lookup steal primary
            // Views from the global IconPack after this content is released.
        }
        if (read(primaryBinder, "defaultDisplayViewStore") != primaryStore
                || read(primaryStore, "$$delegate_0") != primaryDelegate
                || read(primaryDelegate, "$block") != primaryBlock
                || read(primaryBinder, "viewModel") != primaryModel
                || containsIdentity(collectionListeners, collectionListener)
                || containsIdentity(iconListeners, iconListener))
            throw new IllegalStateException("Primary icon store or native listeners changed after close");
        closed = true;
    }

    private static boolean containsIdentity(Collection<?> values, Object owned) {
        for (Object value : values) if (value == owned) return true;
        return false;
    }

    boolean awaitCompletion(Runnable callback) {
        if (!closing || !bindingJobs.pending()) return false;
        bindingJobs.whenDrained(callback);
        return true;
    }

    private static String entryKey(Object entry) throws ReflectiveOperationException {
        // Both native entry types inherit this public field from PipelineEntry.
        java.lang.reflect.Field key = entry.getClass().getField("key");
        if (!key.getDeclaringClass().getName().equals(
                "com.android.systemui.statusbar.notification.collection.PipelineEntry"))
            throw new IllegalStateException("Unexpected notification key owner");
        return (String) key.get(entry);
    }

    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("App notification icons require main Looper");
    }
}
