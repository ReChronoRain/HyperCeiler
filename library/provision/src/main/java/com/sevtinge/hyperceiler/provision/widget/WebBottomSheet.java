package com.sevtinge.hyperceiler.provision.widget;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.sevtinge.hyperceiler.provision.R;
import com.sevtinge.hyperceiler.provision.utils.OobeUtils;

import java.security.SecureRandom;

import fan.bottomsheet.BottomSheetBehavior;
import fan.bottomsheet.BottomSheetModal;

public class WebBottomSheet extends BottomSheetModal {


    public WebBottomSheet(FragmentActivity activity) {
        super(activity);
    }

    public final void init(String uri, FragmentActivity activity) {
        setDragHandleViewEnabled(true);

        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setForceFullHeight(true);
        behavior.setModeConfig(0);
        behavior.setDraggable(true);
        behavior.setSkipHalfExpanded(true);
        behavior.setSkipCollapsed(true);
        behavior.setFixedHeightRatioEnabled(false);

        setContentView(R.layout.fragment_bottom_sheet_web);

        View rootView = getRootView();

        String verificationCode = OobeUtils.verificationCode;

        TextView loadingView = rootView.findViewById(R.id.loading_view);
        TextView verificationCodeTip = rootView.findViewById(R.id.verification_code_tip);
        MarkdownView markdownView = rootView.findViewById(R.id.markdown);

        loadingView.setVisibility(View.VISIBLE);
        verificationCodeTip.setVisibility(View.GONE);

        verificationCodeTip.setText(
            activity.getString(
                R.string.provision_terms_of_use_verification_code_tip,
                verificationCode
            )
        );

        markdownView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                loadingView.post(() -> {
                    loadingView.setVisibility(View.GONE);
                    verificationCodeTip.setVisibility(View.VISIBLE);
                });
            }
        });

        markdownView.loadUrl(uri);
    }


}
