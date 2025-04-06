package com.sevtinge.hyperceiler.provision.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sevtinge.hyperceiler.ui.R;
import com.sevtinge.hyperceiler.provision.utils.OobeUtils;
import com.sevtinge.hyperceiler.provision.utils.PageIntercepHelper;

public abstract class BaseActivity extends ProvisionBaseActivity {

    protected Fragment mFragment;

    private boolean mCheckNewJump = true;
    private boolean mIsDisableBack = false;

    private final View.OnClickListener mBackListener = v -> onBackPressed();

    protected abstract Fragment getFragment();
    protected abstract String getFragmentTag();

    protected abstract int getLogoDrawableId();
    protected abstract int getPreviewDrawable();
    protected abstract int getTitleStringId();
    protected abstract CharSequence getListDescCharSequence();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (OobeUtils.isProvisioned(this)) {
            setResult(-1);
            finish();
        } else {
            mIsDisableBack = getIntent().getBooleanExtra("extra_disable_back", false);
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager.isDestroyed()) {
                setResult(-1);
                finish();
            } else {
                mFragment = fragmentManager.findFragmentByTag(getFragmentTag());
                if (mFragment == null) {
                    FragmentTransaction beginTransaction = fragmentManager.beginTransaction();
                    mFragment = getFragment();
                    beginTransaction.replace(R.id.provision_container, mFragment, getFragmentTag());
                    beginTransaction.commit();
                }
                if (getTitleStringId() > 0) {
                    setTitle(getTitleStringId());
                } else {
                    setTitle(getTitleStringText());
                }

                if (getPreviewDrawable() > 0) {
                    setPreviewDrawable(getPreviewDrawable());
                }
            }
        }
    }

    protected String getTitleStringText() {
        return "";
    }

    protected CharSequence getDescriptionContent() {
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mTitle != null) {
            mTitle.setTextDirection(OobeUtils.isRTL() ? 4 : 3);
            int titleStringId = getTitleStringId();
            if (titleStringId > 0) {
                mTitle.setText(titleStringId);
            }
        }
        if (mNewBackBtn != null && !mIsDisableBack && !hasPreview()) {
            mNewBackBtn.setOnClickListener(mBackListener);
        }
        if (mFragment != null) {
            View description = mFragment.getView().findViewById(R.id.list_description);
            if (description != null && (description instanceof TextView)) {
                CharSequence listDescCharSequence = getListDescCharSequence();
                if (listDescCharSequence != null) {
                    description.setTextDirection(OobeUtils.isRTL() ? 4 : 3);
                    TextView textView = (TextView) description;
                    textView.setText(listDescCharSequence);
                    description.setVisibility(View.VISIBLE);
                } else {
                    description.setVisibility(View.GONE);
                }
            }
            View view = mFragment.getView().findViewById(R.id.description);
            if (view != null && view instanceof TextView) {
                CharSequence descriptionContent = getDescriptionContent();
                if (!TextUtils.isEmpty(descriptionContent)) {
                    view.setTextDirection(OobeUtils.isRTL() ? 4 : 3);
                    ((TextView) view).setText(descriptionContent);
                    view.setVisibility(View.VISIBLE);
                } else {
                    view.setVisibility(View.GONE);
                }
            }
        }
    }

    protected void additionalProcess() {
        setResult(0);
    }

    @Override
    public void onBackPressed() {
        if (!mIsDisableBack) {
            try {
                additionalProcess();
                if (!getSupportFragmentManager().isDestroyed()) {
                    super.onBackPressed();
                }
                if (mFragment != null) {
                    mFragment.getClass();
                }
            } catch (Exception e) {
                Log.e("BaseActivity", "ex: " + e.getMessage());
            }
        }
    }

    protected void setPreviewDrawable(int id) {
        if (mImageView != null) {
            setPreviewView(ContextCompat.getDrawable(this, id));
        }
    }

    public void setCheck(boolean jump) {
        mCheckNewJump = jump;
    }


    @Override
    public void finish() {
        if (PageIntercepHelper.getInstance().isAdapterNewJump(this) && mCheckNewJump) {
            PageIntercepHelper.getInstance().sendFinish(this);
            setResult(PageIntercepHelper.getInstance().getPlaceHolderCode(this));
        } else {
            super.finish();
        }
    }

}
