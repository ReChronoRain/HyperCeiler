package com.sevtinge.hyperceiler.home.helper;

import androidx.preference.Preference;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.prefs.LayoutPreference;
import com.sevtinge.hyperceiler.utils.FrameworkStatusManager;
import com.sevtinge.hyperceiler.utils.ScopeManager;

public class FrameworkWarningHelpFragment extends SettingsPreferenceFragment {

    private Preference mHelpFrameworkWarning;
    private LayoutPreference mCompatibleCard;
    private LayoutPreference mServiceErrorCard;
    private LayoutPreference mPendingCard;
    private LayoutPreference mIncompatibleCard;
    private final ScopeManager.ServiceStateListener mServiceStateListener =
        service -> updateSummary();

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_help_framework_warning;
    }

    @Override
    public void initPrefs() {
        setTitle(com.sevtinge.hyperceiler.core.R.string.help);
        mCompatibleCard = findPreference("prefs_key_layout_framework_compatible");
        mServiceErrorCard = findPreference("prefs_key_layout_framework_service_error");
        mPendingCard = findPreference("prefs_key_layout_framework_pending");
        mIncompatibleCard = findPreference("prefs_key_layout_framework_incompatible");
        mHelpFrameworkWarning = findPreference("prefs_key_textview_help_framework_warning");
        updateSummary();
    }

    @Override
    public void onStart() {
        super.onStart();
        ScopeManager.addServiceStateListener(mServiceStateListener, true);
    }

    @Override
    public void onStop() {
        ScopeManager.removeServiceStateListener(mServiceStateListener);
        super.onStop();
    }

    private void updateSummary() {
        if (getContext() == null) {
            return;
        }

        FrameworkStatusManager.Status status = FrameworkStatusManager.getCurrentStatus();
        updateCards(status);

        if (mHelpFrameworkWarning != null) {
            mHelpFrameworkWarning.setSummary(FrameworkStatusManager.buildHelpSummary(requireContext()));
        }
    }

    private void updateCards(FrameworkStatusManager.Status status) {
        if (mCompatibleCard != null) {
            mCompatibleCard.setVisible(status.getReason() == FrameworkStatusManager.Reason.COMPATIBLE);
        }
        if (mIncompatibleCard != null) {
            mIncompatibleCard.setVisible(status.getReason() == FrameworkStatusManager.Reason.API_TOO_LOW);
        }
        if (mServiceErrorCard != null) {
            mServiceErrorCard.setVisible(status.getReason() == FrameworkStatusManager.Reason.SERVICE_ERROR);
        }
        if (mPendingCard != null) {
            mPendingCard.setVisible(status.getReason() == FrameworkStatusManager.Reason.PENDING);
        }
    }
}
