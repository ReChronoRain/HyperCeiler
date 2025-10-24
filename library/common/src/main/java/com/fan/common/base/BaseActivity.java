package com.fan.common.base;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fan.common.R;

import fan.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

    protected FrameLayout mContent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置基础布局
        setContentView(R.layout.activity_base);

        initBaseViews();

        // 子类布局注入
        int contentLayoutId = getContentLayoutId();
        if (contentLayoutId != 0) {
            getLayoutInflater().inflate(contentLayoutId, mContent, true);
        }
        onCreate();
    }

    /**
     * 初始化基础视图
     */
    private void initBaseViews() {
        mContent = findViewById(R.id.content);
    }

    /**
     * 获取内容布局ID
     */
    @LayoutRes
    protected abstract int getContentLayoutId();

    /**
     * 初始化 - 子类可重写
     */
    protected void onCreate() {
        // 子类实现具体初始化
    }

    /**
     * 获取菜单资源ID
     */
    @MenuRes
    protected int getMenuResId() {
        return 0; // 默认无菜单
    }

    /**
     * 处理菜单项点击
     */
    protected boolean onMenuItemClick(MenuItem item) {
        return false; // 子类处理返回true
    }


    // ==================== 菜单相关 ====================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int menuResId = getMenuResId();
        if (menuResId != 0) {
            getMenuInflater().inflate(menuResId, menu);
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // 先让子类处理
        if (onMenuItemClick(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
