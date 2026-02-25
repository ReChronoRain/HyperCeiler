package com.sevtinge.hyperceiler.home.banner;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.sevtinge.hyperceiler.ui.page.HomePageFragment;

import java.lang.ref.WeakReference;

public class BannerCallback implements View.OnClickListener {

    private final WeakReference mRef;

    public BannerCallback(HomePageFragment fragment) {
        mRef = new WeakReference(fragment);
    }


    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        if (!(tag instanceof BannerBean)) return;

        BannerBean bean = (BannerBean) tag;
        Context context = v.getContext();

        // 1. 处理 URL 跳转 (比如打开网页)
        if (!TextUtils.isEmpty(bean.getUrl())) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(bean.getUrl()));
                context.startActivity(intent);
            } catch (Exception e) {
                Log.e("BannerCallback", "Failed to open URL: " + bean.getUrl());
            }
            return;
        }
        // 2. 处理特定的 Action (比如跳转特定的 Activity 或执行命令)
        String action = bean.getAction();
        if (!TextUtils.isEmpty(action)) {
            Intent intent = new Intent(action);
            // 如果有 Pkg，限制跳转的应用
            if (!TextUtils.isEmpty(bean.getPkg())) {
                intent.setPackage(bean.getPkg());
            }
            // 尝试处理 Extras (如果是 JSON 或简单字符串，这里需要对应的解析逻辑)
            if (!TextUtils.isEmpty(bean.getExtras())) {
                intent.putExtra("banner_extras", bean.getExtras());
            }

            try {
                context.startActivity(intent);
            } catch (Exception e) {
                // 如果是内部逻辑，也可以根据 action 字符串做 switch 判断
                handleCustomAction(context, action, bean);
            }
        }
    }


    private void handleCustomAction(Context context, String action, BannerBean bean) {
        // 在这里处理你自定义的特殊逻辑，比如 "Test"
        if ("Test".equals(action)) {
            // 执行你的测试逻辑
        }
    }

}
