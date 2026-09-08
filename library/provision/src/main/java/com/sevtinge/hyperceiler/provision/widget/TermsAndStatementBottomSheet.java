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
package com.sevtinge.hyperceiler.provision.widget;

import android.annotation.SuppressLint;
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

    static FragmentActivity mActivity;
    BottomSheetModal mBottomSheet;

    @SuppressLint("StaticFieldLeak")
    static ProgressBar mProgressBar;
    @SuppressLint("StaticFieldLeak")
    static TextView verificationCodeTip;
    static MarkdownView mMarkdownView;

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
        mProgressBar = rootView.findViewById(R.id.progress_bar);
        mMarkdownView = rootView.findViewById(R.id.markdown);
        verificationCodeTip = rootView.findViewById(R.id.verification_code_tip);
        initView();
    }

    public static void initView() {
        OobeUtils.refreshSecureSixDigit();

        mProgressBar.setVisibility(View.VISIBLE);
        mMarkdownView.setVisibility(View.INVISIBLE);
        verificationCodeTip.setVisibility(View.GONE);

        String verificationCode = OobeUtils.getSecureSixDigit();

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


    public static void loadMarkdown(String uri) {
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
