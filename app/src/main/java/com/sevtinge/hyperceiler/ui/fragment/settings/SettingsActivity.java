package com.sevtinge.hyperceiler.ui.fragment.settings;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.settings.core.SubSettingLauncher;
import com.sevtinge.hyperceiler.ui.fragment.settings.core.gateway.SettingsGateway;
import com.sevtinge.hyperceiler.ui.fragment.settings.utils.SettingsFeatures;
import com.sevtinge.hyperceiler.ui.fragment.settings.utils.Utils;
import com.sevtinge.hyperceiler.ui.fragment.settings.core.OnActivityResultListener;
import com.sevtinge.hyperceiler.ui.fragment.settings.core.SettingsBaseActivity;

import java.util.List;

import fan.appcompat.app.ActionBar;
import fan.preference.Preference;
import fan.preference.PreferenceManager;
import fan.preference.core.PreferenceFragmentCompat;

public class SettingsActivity extends SettingsBaseActivity
        implements PreferenceManager.OnPreferenceTreeClickListener,
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        FragmentManager.OnBackStackChangedListener {

    private static final String LOG_TAG = "SettingsActivity";

    // Constants for state save/restore
    private static final String SAVE_KEY_CATEGORIES = ":settings:categories";

    /**
     * When starting this activity, the invoking Intent can contain this extra
     * string to specify which fragment should be initially displayed.
     * <p/>Starting from Key Lime Pie, when this argument is passed in, the activity
     * will call isValidFragment() to confirm that the fragment class name is valid for this
     * activity.
     */
    public static final String EXTRA_SHOW_FRAGMENT = ":settings:show_fragment";

    /**
     * When starting this activity and using {@link #EXTRA_SHOW_FRAGMENT},
     * this extra can also be specified to supply a Bundle of arguments to pass
     * to that fragment when it is instantiated during the initial creation
     * of the activity.
     */
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args";

    /**
     * Fragment "key" argument passed thru {@link #EXTRA_SHOW_FRAGMENT_ARGUMENTS}
     */
    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";

    /**
     * When starting this activity and using {@link #EXTRA_SHOW_FRAGMENT},
     * those extra can also be specify to supply the title or title res id to be shown for
     * that fragment.
     */
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":settings:show_fragment_title";
    /**
     * The package name used to resolve the title resource id.
     */
    public static final String EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME =
            ":settings:show_fragment_title_res_package_name";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE_RESID =
            ":settings:show_fragment_title_resid";

    public static final String EXTRA_SHOW_FRAGMENT_AS_SUBSETTING =
            ":settings:show_fragment_as_subsetting";

    public static final String EXTRA_IS_SECOND_LAYER_PAGE = ":settings:is_second_layer_page";

    /**
     * Personal or Work profile tab of {@link ProfileSelectFragment}
     * <p>0: Personal tab.
     * <p>1: Work profile tab.
     */
    public static final String EXTRA_SHOW_FRAGMENT_TAB =
            ":settings:show_fragment_tab";

    public static final String META_DATA_KEY_FRAGMENT_CLASS =
            "com.android.settings.FRAGMENT_CLASS";

    public static final String META_DATA_KEY_HIGHLIGHT_MENU_KEY =
            "com.android.settings.HIGHLIGHT_MENU_KEY";

    private static final String EXTRA_UI_OPTIONS = "settings:ui_options";

    private String mFragmentClass;
    private String mHighlightMenuKey;

    private CharSequence mInitialTitle;
    private int mInitialTitleResId;

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        new SubSettingLauncher(this)
                .setDestination(pref.getFragment())
                .setArguments(pref.getExtras())
                .setSourceMetricsCategory(0)
                .setTitleRes(-1)
                .launch();
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(@NonNull Preference preference) {
        return false;
    }

    protected int getOwnerTheme() {
        return R.style.Theme_Settings_Main;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedState) {
        final Intent intent = getIntent();
        super.onCreate(savedState);
        Log.d(LOG_TAG, "Starting onCreate");
        createUiFromIntent(savedState, intent);
    }

    protected void createUiFromIntent(Bundle savedState, Intent intent) {
        if (intent.hasExtra(EXTRA_UI_OPTIONS)) {
            getWindow().setUiOptions(intent.getIntExtra(EXTRA_UI_OPTIONS, 0));
        }

        // Getting Intent properties can only be done after the super.onCreate(...)
        final String initialFragmentName = getInitialFragmentName(intent);

        // If this is a sub settings, then apply the SubSettings Theme for the ActionBar content
        // insets.
        // If this is in setup flow, don't apply theme. Because light theme needs to be applied
        // in SettingsBaseActivity#onCreate().
        if (isSubSettings(intent)) {
            setTheme(getOwnerTheme());
        }

        setContentView(R.layout.settings_main_prefs);

        getSupportFragmentManager().addOnBackStackChangedListener(this);

        if (savedState != null) {
            // We are restarting from a previous saved state; used that to initialize, instead
            // of starting fresh.
            setTitleFromIntent(intent);
        } else {
            launchSettingFragment(initialFragmentName, intent);
        }

        if (SettingsFeatures.isSplitTablet(this)) {
            ActionBar actionBar = getAppCompatActionBar();
            if (actionBar != null) {
                actionBar.setExpandState(ActionBar.STATE_COLLAPSE);
                actionBar.setResizable(false);
            }
        }
    }

    private boolean isSubSettings(Intent intent) {
        return this instanceof SubSettings ||
                intent.getBooleanExtra(EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, false);
    }

    /** Returns the initial fragment name that the activity will launch. */
    @VisibleForTesting
    public String getInitialFragmentName(Intent intent) {
        return intent.getStringExtra(EXTRA_SHOW_FRAGMENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment instanceof OnActivityResultListener) {
                    fragment.onActivityResult(requestCode, resultCode, data);
                }
            }
        }
    }

    @VisibleForTesting
    void launchSettingFragment(String initialFragmentName, Intent intent) {
        if (initialFragmentName != null) {
            setTitleFromIntent(intent);
            Bundle initialArguments = intent.getBundleExtra(":android:show_fragment_args");
            if (initialArguments == null) {
                initialArguments = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
            }
            switchToFragment(initialFragmentName, initialArguments, true,
                    mInitialTitleResId, mInitialTitle);
        } else {
            // Show search icon as up affordance if we are displaying the main Dashboard
            mInitialTitleResId = R.string.app_name;
            startActivity(new Intent("android.settings.SETTINGS"));
            finish();
        }
    }

    private void setTitleFromIntent(Intent intent) {
        Log.d(LOG_TAG, "Starting to set activity title");
        final int initialTitleResId = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, -1);
        if (initialTitleResId > 0) {
            mInitialTitle = null;
            mInitialTitleResId = initialTitleResId;

            final String initialTitleResPackageName = intent.getStringExtra(
                    EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME);
            if (initialTitleResPackageName != null) {
                try {
                    Context authContext = createPackageContext(initialTitleResPackageName,
                            0 /* flags */);
                    mInitialTitle = authContext.getResources().getText(mInitialTitleResId);
                    setTitle(mInitialTitle);
                    mInitialTitleResId = -1;
                    return;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w(LOG_TAG, "Could not find package" + initialTitleResPackageName);
                } catch (Resources.NotFoundException resourceNotFound) {
                    Log.w(LOG_TAG,
                            "Could not find title resource in " + initialTitleResPackageName);
                }
            } else {
                setTitle(mInitialTitleResId);
            }
        } else {
            mInitialTitleResId = -1;
            final String initialTitle = intent.getStringExtra(EXTRA_SHOW_FRAGMENT_TITLE);
            mInitialTitle = (initialTitle != null) ? initialTitle : getTitle();
            setTitle(mInitialTitle);
        }
        Log.d(LOG_TAG, "Done setting title");
    }

    @Override
    public void onBackStackChanged() {
        setTitleFromBackStack();
    }

    private void setTitleFromBackStack() {
        final int count = getSupportFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            if (mInitialTitleResId > 0) {
                setTitle(mInitialTitleResId);
            } else {
                setTitle(mInitialTitle);
            }
            return;
        }

        FragmentManager.BackStackEntry bse = getSupportFragmentManager().
                getBackStackEntryAt(count - 1);
        setTitleFromBackStackEntry(bse);
    }

    private void setTitleFromBackStackEntry(FragmentManager.BackStackEntry bse) {
        final CharSequence title;
        final int titleRes = bse.getBreadCrumbTitleRes();
        if (titleRes > 0) {
            title = getText(titleRes);
        } else {
            title = bse.getBreadCrumbTitle();
        }
        if (title != null) {
            setTitle(title);
        }
    }

    protected boolean isValidFragment(String fragmentName) {
        // Almost all fragments are wrapped in this,
        // except for a few that have their own activities.
        for (int i = 0; i < SettingsGateway.ENTRY_FRAGMENTS.length; i++) {
            if (SettingsGateway.ENTRY_FRAGMENTS[i].equals(fragmentName)) return true;
        }
        return false;
    }

    /**
     * Switch to a specific Fragment with taking care of validation, Title and BackStack
     */
    private void switchToFragment(String fragmentName, Bundle args, boolean validate,
                                  int titleResId, CharSequence title) {
        Log.d(LOG_TAG, "Switching to fragment " + fragmentName);
        if (validate && !isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: "
                    + fragmentName);
        }
        Fragment f = Utils.getTargetFragment(this, fragmentName, args);
        if (f == null) {
            return;
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, f);
        if (titleResId > 0) {
            transaction.setBreadCrumbTitle(titleResId);
        } else if (title != null) {
            transaction.setBreadCrumbTitle(title);
        }
        transaction.commitAllowingStateLoss();
        getSupportFragmentManager().executePendingTransactions();
        Log.d(LOG_TAG, "Executed frag manager pendingTransactions");
    }
}
