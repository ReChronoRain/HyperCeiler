package com.sevtinge.hyperceiler.provision.state;

import android.view.KeyEvent;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sevtinge.hyperceiler.provision.R;
import com.sevtinge.hyperceiler.provision.activity.DefaultActivity;
import com.sevtinge.hyperceiler.provision.fragment.StartupFragment;
import com.sevtinge.hyperceiler.provision.utils.IKeyEvent;
import com.sevtinge.hyperceiler.provision.utils.IOnFocusListener;

public class StartupState extends State implements IKeyEvent, IOnFocusListener {

    private boolean mHasBooted;
    private Fragment.SavedState mSavedState;
    private StartupFragment mStartupFragment;

    public void setBooted(boolean booted) {
        mHasBooted = booted;
    }

    @Override
    public boolean isAvailable(boolean available) {
        return true;
    }

    @Override
    public void onEnter(boolean z, boolean z2) {
        FragmentManager fragmentManager = ((DefaultActivity) mContext).getSupportFragmentManager();
        mStartupFragment = new StartupFragment();
        if (fragmentManager.findFragmentByTag(StartupFragment.class.getSimpleName()) == null) {
            buildStartupFragment(mStartupFragment, fragmentManager);
        } else {
            mStartupFragment.setInitialSavedState(mSavedState);
            buildStartupFragment(mStartupFragment, fragmentManager);
        }
    }

    @Override
    public void onLeave() {
        FragmentManager supportFragmentManager = ((DefaultActivity) mContext).getSupportFragmentManager();
        Fragment fragmentByTag = supportFragmentManager.findFragmentByTag(StartupFragment.class.getSimpleName());
        if (fragmentByTag != null) {
            mSavedState = supportFragmentManager.saveFragmentInstanceState(supportFragmentManager.findFragmentByTag(StartupFragment.class.getSimpleName()));
            FragmentTransaction beginTransaction = supportFragmentManager.beginTransaction();
            beginTransaction.setCustomAnimations(0, R.anim.provision_slide_out_left_animator);
            beginTransaction.remove(fragmentByTag);
            beginTransaction.commitAllowingStateLoss();
        }
    }

    private void buildStartupFragment(Fragment fragment, FragmentManager fragmentManager) {
        FragmentTransaction beginTransaction = fragmentManager.beginTransaction();
        beginTransaction.replace(android.R.id.content, fragment, StartupFragment.class.getSimpleName());
        beginTransaction.commitAllowingStateLoss();
    }

    @Override
    public void keyDownDispatcher(int keyCode, KeyEvent event) {
        mStartupFragment.onKeyDownChild(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (mStartupFragment != null) {
            mStartupFragment.onWindowFocusChanged(hasFocus);
        }
    }
}
