package com.sevtinge.hyperceiler.provision.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MarkdownView extends WebView {
    private boolean isTemplateLoaded = false;
    private String pendingMarkdown = null;
    private boolean isDarkPending = false;
    private String lastUrl;

    public MarkdownView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setBackgroundColor(Color.TRANSPARENT);
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebSettings settings = getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);

        addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void retry() {
                post(() -> {
                    if (lastUrl != null) loadMarkdownFromUrl(lastUrl);
                });
            }
        }, "Android");

        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                isTemplateLoaded = true;
                setDarkMode(isDarkPending);
                if (pendingMarkdown != null) {
                    setMarkdown(pendingMarkdown);
                    pendingMarkdown = null;
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http")) {
                    getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
                return false;
            }
        });
        loadUrl("file:///android_asset/markdown_template.html");
    }

    public void loadMarkdownFromUrl(String urlString) {
        this.lastUrl = urlString;
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                if (conn.getResponseCode() == 200) {
                    BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line).append("\n");
                    post(() -> setMarkdown(sb.toString()));
                } else post(this::showError);
            } catch (Exception e) {
                post(this::showError);
            }
        }).start();
    }

    public void setMarkdown(String md) {
        if (!isTemplateLoaded) {
            pendingMarkdown = md;
            return;
        }

        // 1. 先进行 Base64 编码（最稳妥的方法，避免所有特殊字符冲突）
        String base64Md = android.util.Base64.encodeToString(md.getBytes(), android.util.Base64.NO_WRAP);

        // 2. 传给 JS，在 JS 端解码后再解析
        post(() -> evaluateJavascript("javascript:decodeAndParse('" + base64Md + "')", null));
    }


    public void setDarkMode(boolean enabled) {
        isDarkPending = enabled;
        if (isTemplateLoaded) evaluateJavascript("javascript:setDarkMode(" + enabled + ")", null);
    }

    public void showError() {
        evaluateJavascript("javascript:showRetry()", null);
    }
}

