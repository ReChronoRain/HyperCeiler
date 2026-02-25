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

import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.provision.R;
import com.sevtinge.hyperceiler.provision.text.style.TermsTitleSpan;
import com.sevtinge.hyperceiler.provision.widget.SimpleTextWatcher;
import com.sevtinge.hyperceiler.provision.widget.TermsAndStatementBottomSheet;

import fan.provision.OobeUtils;

import fan.appcompat.app.AlertDialog;

public class TermsAndStatementFragment extends BaseFragment {


    private View mNextView;
    private TextView mPrivacyView;
    private CheckBox mAgreeCheckBox;

    @Override
    protected int getLayoutId() {
        return R.layout.provision_terms_and_statement_layout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPrivacyView = view.findViewById(R.id.privacy);

        String terms = getString(R.string.provision_terms_of_use_label_use_network_china);
        if (enhanceTermsTitle(terms) != null) {
            mPrivacyView.setText(enhanceTermsTitle(terms));
            mPrivacyView.setMovementMethod(fan.androidbase.widget.LinkMovementMethod.getInstance());
        }

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
                    Log.d("Verify", "Input: [" + inputStr + "] Target: [" + targetStr + "]");


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
        if (getActivity() != null) {
            getActivity().setResult(-1);
            getActivity().finish();
        }
    }

    public SpannableStringBuilder enhanceTermsTitle(String str) {

        String userAgreement = getString(R.string.provision_user_agreement);
        String privacyPolicy = getString(R.string.provision_privacy_policy);
        String spanned = Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY).toString();
        int lastIndexOf = spanned.lastIndexOf(userAgreement);
        int length = userAgreement.length() + lastIndexOf;
        if (lastIndexOf >= 0 && length <= spanned.length()) {
            int indexOf = spanned.indexOf(privacyPolicy);
            int length2 = privacyPolicy.length() + indexOf;
            if (indexOf >= 0 && length2 <= spanned.length()) {
                SpannableStringBuilder builder = new SpannableStringBuilder(spanned);
                int color = getResources().getColor(R.color.provision_button_text_high_color_light, requireContext().getTheme());
                builder.setSpan(new ForegroundColorSpan(color), lastIndexOf, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new ForegroundColorSpan(color), indexOf, length2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new TermsTitleSpan(requireActivity(), 2), lastIndexOf, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new TermsTitleSpan(requireActivity(), 1), indexOf, length2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return builder;
            }
        }
        return null;
    }
}
