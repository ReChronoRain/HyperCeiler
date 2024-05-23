package com.sevtinge.hyperceiler.ui.settings;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.XmlRes;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.CeilerTabActivity;
import com.sevtinge.hyperceiler.ui.settings.widget.HighlightablePreferenceGroupAdapter;
import com.sevtinge.hyperceiler.ui.settings.widget.LayoutPreference;
import com.sevtinge.hyperceiler.ui.settings.core.InstrumentedPreferenceFragment;
import com.sevtinge.hyperceiler.ui.settings.core.SubSettingLauncher;

import fan.appcompat.app.Fragment;
import fan.preference.Preference;
import fan.preference.PreferenceGroup;
import fan.preference.PreferenceScreen;

public class SettingsPreferenceFragment extends InstrumentedPreferenceFragment {

    private static final String TAG = "SettingsPreferenceFragment";

    private static final String SAVE_HIGHLIGHTED_KEY = "android:preference_highlighted";

    private static final int ORDER_FIRST = -1;

    protected DevicePolicyManager mDevicePolicyManager;
    // Cache the content resolver for async callbacks
    private ContentResolver mContentResolver;

    private RecyclerView.Adapter mCurrentRootAdapter;
    private boolean mIsDataSetObserverRegistered = false;
    private RecyclerView.AdapterDataObserver mDataSetObserver =
            new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    onDataSetChanged();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    onDataSetChanged();
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                    onDataSetChanged();
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    onDataSetChanged();
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    onDataSetChanged();
                }

                @Override
                public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                    onDataSetChanged();
                }
            };

    private LayoutPreference mHeader;
    private View mEmptyView;
    private LinearLayoutManager mLayoutManager;
    private ArrayMap<String, Preference> mPreferenceCache;

    private String mPreferenceKey;
    private boolean mPreferenceHighlighted = false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mPreferenceKey = getIntent().getStringExtra(SAVE_HIGHLIGHTED_KEY);
        mDevicePolicyManager = getContext().getSystemService(DevicePolicyManager.class);
        if (icicle != null) {
            mPreferenceHighlighted = icicle.getBoolean(SAVE_HIGHLIGHTED_KEY);
        }
        HighlightablePreferenceGroupAdapter.adjustInitialExpandedChildCount(this /* host */);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVE_HIGHLIGHTED_KEY, mPreferenceHighlighted);
    }

    @Override
    public void addPreferencesFromResource(@XmlRes int preferencesResId) {
        super.addPreferencesFromResource(preferencesResId);
        checkAvailablePrefs(getPreferenceScreen());
    }

    @VisibleForTesting
    void checkAvailablePrefs(PreferenceGroup preferenceGroup) {
        if (preferenceGroup == null) return;
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference pref = preferenceGroup.getPreference(i);
            if (pref instanceof SelfAvailablePreference
                    && !((SelfAvailablePreference) pref).isAvailable(getContext())) {
                pref.setVisible(false);
            } else if (pref instanceof PreferenceGroup) {
                checkAvailablePrefs((PreferenceGroup) pref);
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        highlightPreferenceIfNeeded();
    }

    @Override
    protected void onBindPreferences() {
        registerObserverIfNeeded();
    }

    @Override
    protected void onUnbindPreferences() {
        unregisterObserverIfNeeded();
    }

    public void registerObserverIfNeeded() {
        if (!mIsDataSetObserverRegistered) {
            if (mCurrentRootAdapter != null) {
                mCurrentRootAdapter.unregisterAdapterDataObserver(mDataSetObserver);
            }
            mCurrentRootAdapter = getListView().getAdapter();
            mCurrentRootAdapter.registerAdapterDataObserver(mDataSetObserver);
            mIsDataSetObserverRegistered = true;
            onDataSetChanged();
        }
    }

    public void unregisterObserverIfNeeded() {
        if (mIsDataSetObserverRegistered) {
            if (mCurrentRootAdapter != null) {
                mCurrentRootAdapter.unregisterAdapterDataObserver(mDataSetObserver);
                mCurrentRootAdapter = null;
            }
            mIsDataSetObserverRegistered = false;
        }
    }

    public void highlightPreferenceIfNeeded() {
        if (!isAdded() || mPreferenceHighlighted || TextUtils.isEmpty(mPreferenceKey)) {
            return;
        }
        Log.w(TAG, "highlightPreferenceIfNeeded: " + mPreferenceKey);
        requestHighlight(mPreferenceKey);
        mPreferenceHighlighted = true;
    }

    /**
     * Returns initial expanded child count.
     * <p/>
     * Only override this method if the initial expanded child must be determined at run time.
     */
    public int getInitialExpandedChildCount() {
        return 0;
    }

    protected void onDataSetChanged() {
        updateEmptyView();
    }

    public LayoutPreference getHeaderView() {
        return mHeader;
    }

    protected void setHeaderView(int resource) {
        mHeader = new LayoutPreference(getPrefContext(), resource);
        mHeader.setSelectable(false);
        addPreferenceToTop(mHeader);
    }

    protected void setHeaderView(View view) {
        mHeader = new LayoutPreference(getPrefContext(), view);
        mHeader.setSelectable(false);
        addPreferenceToTop(mHeader);
    }

    private void addPreferenceToTop(LayoutPreference preference) {
        preference.setOrder(ORDER_FIRST);
        if (getPreferenceScreen() != null) {
            getPreferenceScreen().addPreference(preference);
        }
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        super.setPreferenceScreen(preferenceScreen);
        if (preferenceScreen != null) {
            if (mHeader != null) {
                preferenceScreen.addPreference(mHeader);
            }
        }
    }

    @VisibleForTesting
    void updateEmptyView() {
        if (mEmptyView == null) return;
        if (getPreferenceScreen() != null) {
            final View listContainer = getActivity().findViewById(android.R.id.list_container);
            boolean show = (getPreferenceScreen().getPreferenceCount()
                    - (mHeader != null ? 1 : 0)) <= 0
                    || (listContainer != null && listContainer.getVisibility() != View.VISIBLE);
            mEmptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    public void setEmptyView(View v) {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
        mEmptyView = v;
        updateEmptyView();
    }

    public View getEmptyView() {
        return mEmptyView;
    }

    @Override
    public RecyclerView.LayoutManager onCreateLayoutManager() {
        mLayoutManager = new LinearLayoutManager(getContext());
        return mLayoutManager;
    }

    protected void cacheRemoveAllPrefs(PreferenceGroup group) {
        mPreferenceCache = new ArrayMap<>();
        final int N = group.getPreferenceCount();
        for (int i = 0; i < N; i++) {
            Preference p = group.getPreference(i);
            if (TextUtils.isEmpty(p.getKey())) {
                continue;
            }
            mPreferenceCache.put(p.getKey(), p);
        }
    }

    protected Preference getCachedPreference(String key) {
        return mPreferenceCache != null ? mPreferenceCache.remove(key) : null;
    }

    protected void removeCachedPrefs(PreferenceGroup group) {
        for (Preference p : mPreferenceCache.values()) {
            group.removePreference(p);
        }
        mPreferenceCache = null;
    }

    protected int getCachedCount() {
        return mPreferenceCache != null ? mPreferenceCache.size() : 0;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public boolean removePreference(String key) {
        return removePreference(getPreferenceScreen(), key);
    }

    @VisibleForTesting
    boolean removePreference(PreferenceGroup group, String key) {
        final int preferenceCount = group.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            final Preference preference = group.getPreference(i);
            final String curKey = preference.getKey();

            if (TextUtils.equals(curKey, key)) {
                return group.removePreference(preference);
            }

            if (preference instanceof PreferenceGroup) {
                if (removePreference((PreferenceGroup) preference, key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * The name is intentionally made different from Activity#finish(), so that
     * users won't misunderstand its meaning.
     */
    public final void finishFragment() {
        getActivity().onBackPressed();
    }

    // Some helpers for functions used by the settings fragments when they were activities

    /**
     * Returns the ContentResolver from the owning Activity.
     */
    protected ContentResolver getContentResolver() {
        Context context = getActivity();
        if (context != null) {
            mContentResolver = context.getContentResolver();
        }
        return mContentResolver;
    }

    /**
     * Returns the specified system service from the owning Activity.
     */
    protected Object getSystemService(final String name) {
        return getActivity().getSystemService(name);
    }

    /**
     * Returns the specified system service from the owning Activity.
     */
    protected <T> T getSystemService(final Class<T> serviceClass) {
        return getActivity().getSystemService(serviceClass);
    }

    /**
     * Returns the PackageManager from the owning Activity.
     */
    protected PackageManager getPackageManager() {
        return getActivity().getPackageManager();
    }


    public void finish() {
        Activity activity = getActivity();
        if (activity == null) return;
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            activity.finish();
        }
    }

    protected Intent getIntent() {
        if (getActivity() == null) {
            return null;
        }
        return getActivity().getIntent();
    }

    protected void setResult(int result, Intent intent) {
        if (getActivity() == null) {
            return;
        }
        getActivity().setResult(result, intent);
    }

    protected void setResult(int result) {
        if (getActivity() == null) {
            return;
        }
        getActivity().setResult(result);
    }

    protected boolean isFinishingOrDestroyed() {
        final Activity activity = getActivity();
        return activity == null || activity.isFinishing() || activity.isDestroyed();
    }

    protected void replaceEnterprisePreferenceScreenTitle(String overrideKey, int resource) {
        getActivity().setTitle(mDevicePolicyManager.getResources().getString(
                overrideKey, () -> getString(resource)));
    }

    public void replaceEnterpriseStringSummary(
            String preferenceKey, String overrideKey, int resource) {
        Preference preference = findPreference(preferenceKey);
        if (preference == null) {
            Log.d(TAG, "Could not find enterprise preference " + preferenceKey);
            return;
        }

        preference.setSummary(
                mDevicePolicyManager.getResources().getString(overrideKey,
                        () -> getString(resource)));
    }

    public void replaceEnterpriseStringTitle(
            String preferenceKey, String overrideKey, int resource) {
        Preference preference = findPreference(preferenceKey);
        if (preference == null) {
            Log.d(TAG, "Could not find enterprise preference " + preferenceKey);
            return;
        }

        preference.setTitle(
                mDevicePolicyManager.getResources().getString(overrideKey,
                        () -> getString(resource)));
    }

    public boolean startFragment(Fragment fragment, String str, int i, Bundle bundle) {
        return startFragment(fragment, str, R.string.app_name, i, bundle);
    }

    public boolean startFragment(Fragment fragment, String str, int i, Bundle bundle, int i2) {
        return startFragment(fragment, str, i2, i, bundle);
    }

    public boolean startFragment(Fragment fragment, String str, int i, int i2, Bundle bundle) {
        FragmentActivity activity = getActivity();
        if (activity instanceof CeilerTabActivity) {
            ((CeilerTabActivity) activity).startPreferencePanel(str, bundle, i, null, fragment, i2);
        } else {
            new SubSettingLauncher(getContext()).setDestination(str).setTitleRes(i).setArguments(bundle).setResultListener(fragment, i2).launch();
        }
        return true;
    }

    public boolean isCeilerTabActivity() {
        return getActivity() instanceof CeilerTabActivity;
    }
}
