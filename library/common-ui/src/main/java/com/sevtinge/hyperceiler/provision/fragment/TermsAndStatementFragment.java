package com.sevtinge.hyperceiler.provision.fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.provision.data.TermsAndStatementAdapter;
import com.sevtinge.hyperceiler.provision.utils.OobeUtils;

public class TermsAndStatementFragment extends BaseListFragment {

    private String mText;

    private View mNextView;
    private CheckBox mAgreeCheckBox;
    private TermsAndStatementAdapter mTermsAndStatementAdapter;


    @Override
    protected int getCustomLayoutId() {
        return R.layout.provision_terms_and_statement_layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTermsAndStatementAdapter = new TermsAndStatementAdapter(getActivity());
        getListView().setAdapter(mTermsAndStatementAdapter);
        getListView().setTextDirection(OobeUtils.isRTL() ? 4 : 3);

        TextView mTextView = view.findViewById(R.id.privacy);

        setWebText(mTextView, "https://gcore.jsdelivr.net/gh/ReChronoRain/website@main/Privacy.md");

        mAgreeCheckBox = view.findViewById(R.id.checkbox_agree);
        mAgreeCheckBox.setVisibility(View.VISIBLE);
        mAgreeCheckBox.setChecked(OobeUtils.getOperatorState(requireContext(), "cm_pick_status"));
        mAgreeCheckBox.setText(R.string.provision_agree_terms);

        if (getActivity() != null) {
            mNextView = OobeUtils.getNextView(getActivity());
            if (mNextView instanceof TextView) {
                ((TextView) mNextView).setText(R.string.provision_agree_and_next);
            }
            mNextView.setEnabled(mAgreeCheckBox.isChecked());
            mNextView.setAlpha(mAgreeCheckBox.isChecked() ? OobeUtils.NO_ALPHA : OobeUtils.HALF_ALPHA);
            mAgreeCheckBox.setOnCheckedChangeListener((v, isChecked) -> {
                mNextView.setEnabled(isChecked);
                mNextView.setAlpha(isChecked ? OobeUtils.NO_ALPHA : OobeUtils.HALF_ALPHA);
                OobeUtils.saveOperatorState(requireContext(), "cm_pick_status", isChecked);
            });
        }
    }

    public void setWebText(TextView tv, String httpUrl) {

    }

    public void goNext() {
        if (getActivity() != null) {
            getActivity().setResult(-1);
            getActivity().finish();
        }
    }
}
