package com.sevtinge.provision.activity;

import androidx.fragment.app.Fragment;

import com.sevtinge.provision.R;
import com.sevtinge.provision.fragment.TermsAndStatementFragment;

public class TermsAndStatementActivity extends BaseActivity {

    @Override
    protected int getPreviewDrawable() {
        return R.drawable.provision_terms;
    }

    @Override
    protected Fragment getFragment() {
        return new TermsAndStatementFragment();
    }

    @Override
    protected String getFragmentTag() {
        return TermsAndStatementActivity.class.getSimpleName();
    }

    @Override
    protected CharSequence getListDescCharSequence() {
        return null;
    }

    @Override
    protected int getLogoDrawableId() {
        return R.drawable.provision_logo_terms;
    }

    @Override
    protected int getTitleStringId() {
        return R.string.provision_terms_and_statement_title;
    }

    @Override
    public void onNextAminStart() {
        if (mFragment instanceof TermsAndStatementFragment) {
            ((TermsAndStatementFragment) mFragment).goNext();
        }
    }

    @Override
    public void onBackAnimStart() {
        super.onBackAnimStart();
        setResult(0);
        finish();
    }
}
