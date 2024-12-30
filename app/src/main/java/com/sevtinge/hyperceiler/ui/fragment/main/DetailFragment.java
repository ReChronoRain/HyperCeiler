package com.sevtinge.hyperceiler.ui.fragment.main;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceScreen;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.ui.fragment.dashboard.DashboardFragment;
import com.sevtinge.hyperceiler.utils.DialogHelper;
import com.sevtinge.hyperceiler.utils.log.AndroidLogUtils;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

import fan.appcompat.app.Fragment;

public class DetailFragment extends Fragment {

    String mFragmentName;
    String mFragmentTitle;

    int mFragmentinflatedXml;

    View mEmptyView;
    String mQuickRestartPackageName;

    MenuItem mQuickRestartMenu;

    List<String> mTitles = new ArrayList<>();

    private static final String TAG = "DetailFragment";
   final String APP_NS = "http://schemas.android.com/apk/res-auto";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeRes(R.style.NavigatorSecondaryContentTheme);
        if (hasActionBar()) getActionBar().setTitle("");
    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewInflated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewInflated(view, savedInstanceState);
        mEmptyView = view.findViewById(R.id.empty);
    }

    @Override
    public void onUpdateArguments(Bundle args) {
        super.onUpdateArguments(args);
        if (args != null) {
            mFragmentName = args.getString("FragmentName");
            mFragmentTitle = args.getString("FragmentTitle");
            mFragmentinflatedXml = args.getInt(":settings:fragment_resId");

            if (!TextUtils.isEmpty(mFragmentName)) {
                DashboardFragment fragment = (DashboardFragment) androidx.fragment.app.Fragment.instantiate(requireContext(), mFragmentName, args);
                FragmentManager manager = getChildFragmentManager();
                int count = manager.getBackStackEntryCount();
                getChildFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_content, fragment)
                        .addToBackStack(String.valueOf(count + 1))
                        .commit();
                if (!TextUtils.isEmpty(mFragmentTitle) && hasActionBar()) {
                    getActionBar().setTitle(mFragmentTitle);
                    mTitles.add(mFragmentTitle);
                }
                if (fragment.getPreferenceScreenResId() > 0) {
                    mQuickRestartPackageName = getQuickRestartPackageName(requireContext(), fragment.getPreferenceScreenResId());
                } else if (mFragmentinflatedXml > 0) {
                    mQuickRestartPackageName = getQuickRestartPackageName(requireContext(), mFragmentinflatedXml);
                }

                mQuickRestartMenu.setVisible(!TextUtils.isEmpty(mQuickRestartPackageName));

                mEmptyView.setVisibility(View.INVISIBLE);

            }
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navigation_immersion, menu);
        mQuickRestartMenu = menu.findItem(R.id.quick_restart);
        mQuickRestartMenu.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item == mQuickRestartMenu && !TextUtils.isEmpty(mQuickRestartPackageName)) {
            if (mQuickRestartPackageName.equals("system")) {
                DialogHelper.showRestartSystemDialog(getContext());
            } else {
                DialogHelper.showRestartDialog(getContext(), mQuickRestartPackageName);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private String getQuickRestartPackageName(Context context, @XmlRes int xmlResId) {
        Resources res = context.getResources();
        try (XmlResourceParser xml = res.getXml(xmlResId)) {
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xml.getName().equals(PreferenceScreen.class.getSimpleName())) {
                    return xml.getAttributeValue(APP_NS, "quick_restart");
                }
                eventType = xml.next();
            }
            return null;
        } catch (Throwable t) {
            AndroidLogUtils.logE(TAG, "Failed to access XML resource!", t);
            return null;
        }
    }

    @Override
    public boolean onKeyEvent(KeyEvent keyEvent) {
        if ((keyEvent.getKeyCode() == KeyEvent.KEYCODE_HOME || keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) && backStack()) {
            return true;
        }
        return super.onKeyEvent(keyEvent);
    }

    private boolean backStack() {
        FragmentManager manager = getChildFragmentManager();
        int count = manager.getBackStackEntryCount();
        if (count > 1) {
            manager.popBackStack(String.valueOf(count - 1), 0);
            getActionBar().setTitle(mTitles.get(count - 1));
            return true;
        }
        return false;
    }
}
