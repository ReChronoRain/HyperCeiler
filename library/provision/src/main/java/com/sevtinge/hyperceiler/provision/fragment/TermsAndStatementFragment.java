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
package com.sevtinge.hyperceiler.provision.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.PrefsBridge;
import com.sevtinge.hyperceiler.provision.R;
import com.sevtinge.hyperceiler.provision.activity.BaseActivity;
import com.sevtinge.hyperceiler.provision.text.style.TermsTitleSpan;
import com.sevtinge.hyperceiler.provision.utils.NoticeProvider;
import com.sevtinge.hyperceiler.provision.utils.ProvisionManager;
import com.sevtinge.hyperceiler.provision.widget.SimpleTextWatcher;

import java.util.List;

import fan.appcompat.app.AlertDialog;
import fan.provision.OobeUtils;

public class TermsAndStatementFragment extends BaseFragment {

    private static final String USER_AGREEMENT_LINK = "user_agreement";
    private static final String PRIVACY_POLICY_LINK = "privacy_policy";


    private View mNextView;
    private TextView mPrivacyView;
    private CheckBox mAgreeCheckBox;

    private AlertDialog mLoadingDialog;

    private volatile boolean mNoticeLoaded = false;
    private volatile boolean mWaitingAfterClick = false;

    private int protocolVersion = -1;
    private int privacyVersion = -1;

    @Override
    protected int getLayoutId() {
        return R.layout.provision_terms_and_statement_layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPrivacyView = view.findViewById(R.id.privacy);
        mPrivacyView.setText(enhanceTermsTitle());
        mPrivacyView.setMovementMethod(fan.androidbase.widget.LinkMovementMethod.getInstance());
        mPrivacyView.setLinksClickable(true);

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
            mAgreeCheckBox.setOnClickListener(v -> {
                if (mAgreeCheckBox.isChecked()) {
                    mAgreeCheckBox.setChecked(false);
                    showVerificationDialog(success -> {
                        if (success) {
                            handleNextClick();
                            mAgreeCheckBox.setChecked(true);
                        }
                    });
                }
            });
            mAgreeCheckBox.setOnCheckedChangeListener((v, isChecked) -> {
                mNextView.setEnabled(isChecked);
                mNextView.setAlpha(isChecked ? OobeUtils.NO_ALPHA : OobeUtils.HALF_ALPHA);
                OobeUtils.saveOperatorState(requireContext(), "cm_pick_status", isChecked);
            });
        }

        startSyncNotice();
    }

    private void startSyncNotice() {
        new Thread(() -> {
            try {
                NoticeProvider provider = ProvisionManager.getProvider();
                if (provider != null) {
                    final List<Integer>[] holder = new List[1];

                    Thread worker = new Thread(() -> {
                        holder[0] = provider.getNoticeResult(requireContext());
                    });

                    worker.start();
                    worker.join();

                    List<Integer> result = holder[0];

                    if (result != null && result.size() >= 2) {
                        protocolVersion = result.get(0);
                        privacyVersion = result.get(1);
                    }
                }

                mNoticeLoaded = true;

                new Handler(Looper.getMainLooper()).post(() -> {

                    if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                        mLoadingDialog.dismiss();
                    }

                    if (mWaitingAfterClick) {
                        mWaitingAfterClick = false;
                        writeNoticeToPrefs();
                    }
                });

            } catch (Exception e) {
                AndroidLog.d("TermsAndStatementFragment", e.toString());
            }
        }).start();
    }

    private void handleNextClick() {

        if (!mNoticeLoaded) {
            mWaitingAfterClick = true;
            showLoadingDialog();
            return;
        }

        writeNoticeToPrefs();
    }

    private void writeNoticeToPrefs() {
        PrefsBridge.putByApp("prefs_key_protocol_version", protocolVersion);
        PrefsBridge.putByApp("prefs_key_privacy_version", privacyVersion);
    }

    private void showLoadingDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) return;

        mLoadingDialog = new AlertDialog.Builder(requireActivity())
            .setMessage(R.string.provision_terms_of_use_processing)
            .setCancelable(false)
            .create();

        mLoadingDialog.show();
    }

    private void showVerificationDialog(VerificationCallback callback) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.edit_verification_code_dialog, null);
        EditText input = view.findViewById(R.id.title);

        AlertDialog dialog = new AlertDialog.Builder(requireActivity())
            .setTitle(R.string.provision_terms_of_use_verification_code_dialog_title)
            .setView(view)
            .setCancelable(false)
            .setPositiveButton(R.string.provision_terms_of_use_verification_code_dialog_continue, (d, w) -> callback.onResult(true))
            .setNegativeButton(android.R.string.cancel, (d, w) -> callback.onResult(false))
            .create();

        dialog.setOnShowListener(d -> {
            Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            okButton.setEnabled(false);

            input.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int st, int b, int c) {
                    // 核心：必须调用 toString()
                    String inputStr = (s == null) ? "" : s.toString();
                    String targetStr = OobeUtils.getSecureSixDigit();
                    // 调试打印（可选）：如果还是不亮，看一眼 Logcat
                    //Log.d("Verify", "Input: [" + inputStr + "] Target: [" + targetStr + "]");


                    okButton.setEnabled(inputStr.equals(targetStr));
                }
            });
        });

        dialog.show();
    }

    interface VerificationCallback {
        void onResult(boolean success);
    }

    private boolean verifyInput(AlertDialog dialog) {
        EditText editText = dialog.findViewById(R.id.title);
        if (editText == null) return false;

        String input = editText.getText().toString().trim();
        return "123456".equals(input);
    }


    public void setWebText(TextView tv, String httpUrl) {

    }

    public void goNext() {
        if (getActivity() instanceof BaseActivity activity) {
            activity.navigateForward();
        }
    }

    public SpannableStringBuilder enhanceTermsTitle() {
        CharSequence raw = getText(R.string.provision_terms_of_use_label_use_network_china);
        SpannableString builder = new SpannableString(raw);
        Annotation[] annotations = builder.getSpans(0, builder.length(), Annotation.class);
        int color = getResources().getColor(R.color.provision_button_text_high_color_light, requireContext().getTheme());
        for (Annotation annotation : annotations) {
            int start = builder.getSpanStart(annotation);
            int end = builder.getSpanEnd(annotation);
            builder.removeSpan(annotation);
            int hyperlinkType = getHyperlinkType(annotation.getValue());
            if (hyperlinkType == -1 || end <= start) {
                continue;
            }
            builder.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new TermsTitleSpan(requireActivity(), hyperlinkType), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return new SpannableStringBuilder(builder);
    }

    private int getHyperlinkType(String url) {
        if (USER_AGREEMENT_LINK.equals(url)) {
            return 2;
        }
        if (PRIVACY_POLICY_LINK.equals(url)) {
            return 1;
        }
        return -1;
    }
}
