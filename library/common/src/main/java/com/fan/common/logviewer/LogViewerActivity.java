package com.fan.common.logviewer;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;


import androidx.annotation.Nullable;

import com.fan.common.R;
import com.fan.common.base.BaseActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogViewerActivity extends BaseActivity {

    private AppLogger mAppLogger;
    private ModuleLogger mNetworkLogger;
    private ModuleLogger mDatabaseLogger;

    private CustomLogFragment mCustomFragment;

    private static final int sExportRequestCode = 1001;

    @Override
    protected int getContentLayoutId() {
        return 0;
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        initViews();
        setupLoggers();
        generateSampleLogs();
        // 添加测试日志
        addTestLogs();
    }

    private void initViews() {
        mCustomFragment = new CustomLogFragment();
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.content, mCustomFragment)
            .commit();
    }

    private void setupLoggers() {
        // 从Application获取或直接使用静态方法
        mAppLogger = AppLogger.getInstance();
        mNetworkLogger = new ModuleLogger("Network");
        mDatabaseLogger = new ModuleLogger("Database");
    }

    private void generateSampleLogs() {
        // 生成一些示例日志
        mAppLogger.info("Application started");
        mNetworkLogger.debug("Network request initiated");
        mDatabaseLogger.info("Database connection established");
        mNetworkLogger.warn("Slow network response");
        mDatabaseLogger.error("Database query failed");

        refreshFragments();
    }

    private void refreshFragments() {
        if (mCustomFragment != null) mCustomFragment.refreshLogs();
    }

    private void exportLogs() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, generateDefaultFileName());
        startActivityForResult(intent, sExportRequestCode);
    }

    private static String generateDefaultFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return "app_" + sdf.format(new Date()) + ".log";
    }

    private void addTestLog() {
        mAppLogger.debug("Test log entry at " + System.currentTimeMillis());
        refreshFragments();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == sExportRequestCode && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                // 这里需要处理文件写入权限，简化处理
                LogManager.getInstance().exportLogs("exported_logs.txt", true);
            }
        }
    }

    @Override
    protected int getMenuResId() {
        return R.menu.menu_log_actions;
    }

    @Override
    protected boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_clear_custom) {
            LogManager.getInstance().clearCustomLogs();
            refreshFragments();
            return true;
        } else if (itemId == R.id.menu_clear_system) {
            LogManager.getInstance().clearSystemLogs();
            refreshFragments();
            return true;
        } else if (itemId == R.id.menu_export) {
            exportLogs();
            return true;
        } else if (itemId == R.id.menu_refresh) {
            refreshFragments();
            return true;
        } else if (itemId == R.id.menu_add_test_log) {
            addTestLog();
            return true;
        }

        return super.onMenuItemClick(item);
    }


    private void addTestLogs() {
        LogManager logManager = LogManager.getInstance(this);
        AppLogger appLogger = AppLogger.getInstance();
        ModuleLogger networkLogger = new ModuleLogger("Network");
        ModuleLogger databaseLogger = new ModuleLogger("Database");

        // 添加各种类型的测试日志
        appLogger.verbose("This is a verbose message with detailed information");
        appLogger.debug("Debug information for development");
        appLogger.info("Application initialized successfully");
        appLogger.warn("Warning: Low memory detected");
        appLogger.error("Error: Network connection failed");

        networkLogger.info("Network request started");
        networkLogger.debug("Sending request to: https://api.example.com");
        networkLogger.error("Network timeout after 30 seconds");

        databaseLogger.info("Database connection established");
        databaseLogger.debug("Executing query: SELECT * FROM users");
        databaseLogger.warn("Slow query detected: took 2.5 seconds");
    }
}
