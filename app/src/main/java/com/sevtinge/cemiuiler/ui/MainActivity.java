package com.sevtinge.cemiuiler.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.sevtinge.cemiuiler.R;
import com.sevtinge.cemiuiler.ui.fragment.MainFragment;
import com.sevtinge.cemiuiler.ui.main.base.BaseMainActivity;
import com.sevtinge.cemiuiler.utils.ALPermissionManager;
import com.sevtinge.cemiuiler.view.RestartAlertDialog;

public class MainActivity extends BaseMainActivity {
    private final MainFragment mMainFrag = new MainFragment();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setImmersionMenuEnabled(true);
        setFragment(mMainFrag);
        ALPermissionManager.RootCommand(getPackageCodePath());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.restart) {
            RestartAlertDialog dialog = new RestartAlertDialog(this);
            dialog.setTitle(item.getTitle());
            dialog.show();
        } else if (itemId == R.id.settings) {
            Intent intent = new Intent(this, ModuleSettingsActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.about) {
            Intent mAboutIntent = new Intent(this, AboutActivity.class);
            startActivity(mAboutIntent);
        }
        return super.onOptionsItemSelected(item);
    }
}
