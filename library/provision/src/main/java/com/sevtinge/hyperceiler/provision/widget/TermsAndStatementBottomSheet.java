package com.sevtinge.hyperceiler.provision.widget;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.sevtinge.hyperceiler.provision.R;

import fan.provision.OobeUtils;

import fan.bottomsheet.BottomSheetBehavior;
import fan.bottomsheet.BottomSheetModal;

public class TermsAndStatementBottomSheet {

    FragmentActivity mActivity;
    BottomSheetModal mBottomSheet;

    ProgressBar mProgressBar;
    MarkdownView mMarkdownView;

    public TermsAndStatementBottomSheet(FragmentActivity activity) {
        mActivity = activity;
        mBottomSheet = new BottomSheetModal(activity);
        mBottomSheet.setDragHandleViewEnabled(true);
        BottomSheetBehavior<FrameLayout> behavior = mBottomSheet.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setForceFullHeight(true);
        behavior.setModeConfig(0);
        behavior.setDraggable(true);
        behavior.setSkipHalfExpanded(true);
        behavior.setSkipCollapsed(true);
        behavior.setFixedHeightRatioEnabled(false);

        mBottomSheet.setContentView(R.layout.fragment_bottom_sheet_web);
        View rootView = mBottomSheet.getRootView();
        initView(rootView);
    }

    public void initView(View view) {
        OobeUtils.refreshSecureSixDigit();
        mProgressBar = view.findViewById(R.id.progress_bar);
        mMarkdownView = view.findViewById(R.id.markdown);

        mProgressBar.setVisibility(View.VISIBLE);
        mMarkdownView.setVisibility(View.INVISIBLE);

        String verificationCode = OobeUtils.getSecureSixDigit();

        TextView verificationCodeTip = view.findViewById(R.id.verification_code_tip);
        mMarkdownView.setOnMarkdownLoadListener(success -> {
            if (success) {
                mProgressBar.setVisibility(View.INVISIBLE);
                mMarkdownView.setVisibility(View.VISIBLE);
                verificationCodeTip.setText(
                    mActivity.getString(
                        R.string.provision_terms_of_use_verification_code_tip,
                        verificationCode
                    )
                );
                verificationCodeTip.setVisibility(View.VISIBLE);
            } else {
                verificationCodeTip.setVisibility(View.GONE);
            }
        });
    }


    public void loadMarkdown(String uri) {
        if (mMarkdownView != null) {
            mMarkdownView.loadMarkdownFromUrl(uri);
        }
    }

    public void show() {
        if (mBottomSheet != null) {
            mBottomSheet.show();
        }
    }

    private void dismiss() {
        if (mBottomSheet != null) {
            mBottomSheet.dismiss();
        }
    }
}
