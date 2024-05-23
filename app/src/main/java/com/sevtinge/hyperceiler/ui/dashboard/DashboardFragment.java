package com.sevtinge.hyperceiler.ui.dashboard;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.preference.PreferenceManager.OnActivityResultListener;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.sevtinge.hyperceiler.ui.settings.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.ui.settings.core.AbstractPreferenceController;
import com.sevtinge.hyperceiler.ui.settings.core.BasePreferenceController;
import com.sevtinge.hyperceiler.ui.settings.core.PreferenceControllerListHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fan.preference.Preference;
import fan.preference.PreferenceManager;
import fan.preference.PreferenceScreen;

/**
 * Base fragment for dashboard style UI containing a list of static and dynamic setting items.
 */
public abstract class DashboardFragment extends SettingsPreferenceFragment {

    public static final String CATEGORY = "category";
    private static final String TAG = "DashboardFragment";
    private static final long TIMEOUT_MILLIS = 50L;

    @VisibleForTesting
    final ArrayMap<String, List<DynamicDataObserver>> mDashboardTilePrefKeys = new ArrayMap<>();
    private final Map<Class, List<AbstractPreferenceController>> mPreferenceControllers = new ArrayMap<>();
    private final List<DynamicDataObserver> mRegisteredObservers = new ArrayList<>();
    private final List<AbstractPreferenceController> mControllers = new ArrayList<>();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Load preference controllers from code
        final List<AbstractPreferenceController> controllersFromCode =
                createPreferenceControllers(context);
        // Load preference controllers from xml definition
        final List<BasePreferenceController> controllersFromXml = PreferenceControllerListHelper
                .getPreferenceControllersFromXml(context, getPreferenceScreenResId());
        // Filter xml-based controllers in case a similar controller is created from code already.
        final List<BasePreferenceController> uniqueControllerFromXml =
                PreferenceControllerListHelper.filterControllers(
                        controllersFromXml, controllersFromCode);

        // Add unique controllers to list.
        if (controllersFromCode != null) {
            mControllers.addAll(controllersFromCode);
        }
        mControllers.addAll(uniqueControllerFromXml);

        for (AbstractPreferenceController controller : mControllers) {
            addPreferenceController(controller);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Set ComparisonCallback so we get better animation when list changes.
        getPreferenceManager().setPreferenceComparisonCallback(
                new PreferenceManager.SimplePreferenceComparisonCallback());
        if (icicle != null) {
            // Upon rotation configuration change we need to update preference states before any
            // editing dialog is recreated (that would happen before onResume is called).
            updatePreferenceStates();
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        refreshAllPreferences(getLogTag());
        mControllers.stream()
                .map(controller -> (Preference) findPreference(controller.getPreferenceKey()))
                .filter(Objects::nonNull)
                .forEach(preference -> {
                    // Give all controllers a chance to handle click.
                    //preference.getExtras().putInt(CATEGORY, getMetricsCategory());
                });
        initPrefs();
    }

    public void initPrefs() {}

    @Override
    public void onResume() {
        super.onResume();
        updatePreferenceStates();
    }

    @Override
    protected void onBindPreferences() {
        super.onBindPreferences();
        refreshAllPreferences(getLogTag());
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final Collection<List<AbstractPreferenceController>> controllers =
                mPreferenceControllers.values();
        for (List<AbstractPreferenceController> controllerList : controllers) {
            for (AbstractPreferenceController controller : controllerList) {
                if (controller.handlePreferenceTreeClick(preference)) {
                    // log here since calling super.onPreferenceTreeClick will be skipped
                    //writePreferenceClickMetric(preference);
                    return true;
                }
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected abstract int getPreferenceScreenResId();


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        for (List<AbstractPreferenceController> controllerList : mPreferenceControllers.values()) {
            for (AbstractPreferenceController controller : controllerList) {
                if (controller instanceof OnActivityResultListener) {
                    ((OnActivityResultListener) controller).onActivityResult(
                            requestCode, resultCode, data);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected boolean shouldForceRoundedIcon() {
        return false;
    }

    protected <T extends AbstractPreferenceController> T use(Class<T> clazz) {
        List<AbstractPreferenceController> controllerList = mPreferenceControllers.get(clazz);
        if (controllerList != null) {
            if (controllerList.size() > 1) {
                Log.w(TAG, "Multiple controllers of Class " + clazz.getSimpleName()
                        + " found, returning first one.");
            }
            return (T) controllerList.get(0);
        }

        return null;
    }

    /** Returns all controllers of type T. */
    protected <T extends AbstractPreferenceController> List<T> useAll(Class<T> clazz) {
        return (List<T>) mPreferenceControllers.getOrDefault(clazz, Collections.emptyList());
    }

    protected void addPreferenceController(AbstractPreferenceController controller) {
        if (mPreferenceControllers.get(controller.getClass()) == null) {
            mPreferenceControllers.put(controller.getClass(), new ArrayList<>());
        }
        mPreferenceControllers.get(controller.getClass()).add(controller);
    }

    /**
     * Get the tag string for logging.
     */
    protected String getLogTag() {
        return getClass().getSimpleName();
    }

    /**
     * Get a list of {@link AbstractPreferenceController} for this fragment.
     */
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    /**
     * Displays resource based tiles.
     */
    private void displayResourceTiles() {
        final int resId = getPreferenceScreenResId();
        if (resId <= 0) {
            return;
        }
        addPreferencesFromResource(resId);
        final PreferenceScreen screen = getPreferenceScreen();
        //screen.setOnExpandButtonClickListener(this);
        displayResourceTilesToScreen(screen);
    }

    /**
     * Perform {@link AbstractPreferenceController#displayPreference(PreferenceScreen)}
     * on all {@link AbstractPreferenceController}s.
     */
    protected void displayResourceTilesToScreen(PreferenceScreen screen) {
        mPreferenceControllers.values().stream().flatMap(Collection::stream).forEach(
                controller -> controller.displayPreference(screen));
    }

    /**
     * Get current PreferenceController(s)
     */
    protected Collection<List<AbstractPreferenceController>> getPreferenceControllers() {
        return mPreferenceControllers.values();
    }

    /**
     * Update state of each preference managed by PreferenceController.
     */
    protected void updatePreferenceStates() {
        final PreferenceScreen screen = getPreferenceScreen();
        Collection<List<AbstractPreferenceController>> controllerLists =
                mPreferenceControllers.values();
        for (List<AbstractPreferenceController> controllerList : controllerLists) {
            for (AbstractPreferenceController controller : controllerList) {
                if (!controller.isAvailable()) {
                    continue;
                }

                final String key = controller.getPreferenceKey();
                if (TextUtils.isEmpty(key)) {
                    Log.d(TAG, String.format("Preference key is %s in Controller %s",
                            key, controller.getClass().getSimpleName()));
                    continue;
                }

                final Preference preference = screen.findPreference(key);
                if (preference == null) {
                    Log.d(TAG, String.format("Cannot find preference with key %s in Controller %s",
                            key, controller.getClass().getSimpleName()));
                    continue;
                }
                controller.updateState(preference);
            }
        }
    }

    /**
     * Refresh all preference items, including both static prefs from xml, and dynamic items from
     * DashboardCategory.
     */
    private void refreshAllPreferences(final String tag) {
        final PreferenceScreen screen = getPreferenceScreen();
        // First remove old preferences.
        if (screen != null) {
            // Intentionally do not cache PreferenceScreen because it will be recreated later.
            screen.removeAll();
        }

        // Add resource based tiles.
        displayResourceTiles();

        //refreshDashboardTiles(tag);

        final Activity activity = getActivity();
        if (activity != null) {
            Log.d(tag, "All preferences added, reporting fully drawn");
            activity.reportFullyDrawn();
        }

        updatePreferenceVisibility(mPreferenceControllers);
    }

    /**
     * Force update all the preferences in this fragment.
     */
    public void forceUpdatePreferences() {
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen == null || mPreferenceControllers == null) {
            return;
        }
        for (List<AbstractPreferenceController> controllerList : mPreferenceControllers.values()) {
            for (AbstractPreferenceController controller : controllerList) {
                final String key = controller.getPreferenceKey();
                final Preference preference = findPreference(key);
                if (preference == null) {
                    continue;
                }
                final boolean available = controller.isAvailable();
                if (available) {
                    controller.updateState(preference);
                }
                preference.setVisible(available);
            }
        }
    }

    @VisibleForTesting
    void updatePreferenceVisibility(
            Map<Class, List<AbstractPreferenceController>> preferenceControllers) {
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen == null || preferenceControllers == null) {
            return;
        }

        final boolean visible = true;
        for (List<AbstractPreferenceController> controllerList :
                preferenceControllers.values()) {
            for (AbstractPreferenceController controller : controllerList) {
                final String key = controller.getPreferenceKey();
                final Preference preference = findPreference(key);
                if (preference == null) {
                    continue;
                }
                preference.setVisible(visible && controller.isAvailable());
            }
        }
    }

    @VisibleForTesting
    void registerDynamicDataObservers(List<DynamicDataObserver> observers) {
        if (observers == null || observers.isEmpty()) {
            return;
        }
        final ContentResolver resolver = getContentResolver();
        observers.forEach(observer -> registerDynamicDataObserver(resolver, observer));
    }

    private void registerDynamicDataObserver(ContentResolver resolver,
                                             DynamicDataObserver observer) {
        Log.d(TAG, "register observer: @" + Integer.toHexString(observer.hashCode())
                + ", uri: " + observer.getUri());
        resolver.registerContentObserver(observer.getUri(), false, observer);
        mRegisteredObservers.add(observer);
    }

    private void unregisterDynamicDataObservers(List<DynamicDataObserver> observers) {
        if (observers == null || observers.isEmpty()) {
            return;
        }
        final ContentResolver resolver = getContentResolver();
        observers.forEach(observer -> {
            Log.d(TAG, "unregister observer: @" + Integer.toHexString(observer.hashCode())
                    + ", uri: " + observer.getUri());
            mRegisteredObservers.remove(observer);
            resolver.unregisterContentObserver(observer);
        });
    }

    private void awaitObserverLatch(CountDownLatch latch) {
        try {
            latch.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // Do nothing
        }
    }
}