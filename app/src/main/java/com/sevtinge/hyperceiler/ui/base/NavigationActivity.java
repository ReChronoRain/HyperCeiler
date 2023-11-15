package com.sevtinge.hyperceiler.ui.base;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.data.ModData;
import com.sevtinge.hyperceiler.data.adapter.ModSearchAdapter;
import com.sevtinge.hyperceiler.data.adapter.NavigationPagerAdapter;
import com.sevtinge.hyperceiler.ui.SubSettings;
import com.sevtinge.hyperceiler.ui.fragment.AboutFragment;
import com.sevtinge.hyperceiler.ui.fragment.MainFragment;
import com.sevtinge.hyperceiler.ui.fragment.ModuleSettingsFragment;
import com.sevtinge.hyperceiler.utils.SearchModeHelper;
import com.sevtinge.hyperceiler.utils.SettingLauncherHelper;

import java.util.ArrayList;

import moralnorm.preference.Preference;
import moralnorm.preference.PreferenceFragmentCompat;
import moralnorm.view.SearchActionMode;

public abstract class NavigationActivity extends BaseActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    String lastFilter;
    View mSearchView;
    TextView mSearchInputView;
    RecyclerView mSearchResultView;
    ModSearchAdapter mSearchAdapter;

    ViewPager mFragmentPage;
    RadioGroup mNavigationView;
    RadioButton mHomeNav, mSettingsNav, mAboutNav;
    ArrayList<Fragment> mFragmentList = new ArrayList<>();
    NavigationPagerAdapter mNavigationPagerAdapter;
    MainFragment mainFragment = new MainFragment();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        initSearchView();
        initNavigationView();
    }

    private void initSearchView() {
        mSearchView = findViewById(R.id.search_view);
        mSearchInputView = findViewById(android.R.id.input);
        mSearchResultView = findViewById(R.id.search_result_view);
        mSearchAdapter = new ModSearchAdapter();
        mSearchInputView.setHint(getResources().getString(R.string.search));
        mSearchResultView.setLayoutManager(new LinearLayoutManager(this));
        mSearchResultView.setAdapter(mSearchAdapter);

        mSearchView.setOnClickListener(v -> startSearchMode());
        mSearchAdapter.setOnItemClickListener((view, ad) -> onSearchItemClickListener(ad));
    }

    private void initNavigationView() {
        mFragmentPage = findViewById(R.id.frame_page);
        mNavigationView = findViewById(R.id.navigation);
        mHomeNav = findViewById(R.id.navigation_home);
        mSettingsNav = findViewById(R.id.navigation_settings);
        mAboutNav = findViewById(R.id.navigation_about);

        mFragmentList.add(mainFragment);
        mFragmentList.add(new ModuleSettingsFragment());
        mFragmentList.add(new AboutFragment());
        mNavigationPagerAdapter = new NavigationPagerAdapter(getSupportFragmentManager(), mFragmentList);
        mFragmentPage.setAdapter(mNavigationPagerAdapter);

        mNavigationView.setOnCheckedChangeListener((radioGroup, checkedId) -> {
            if (checkedId == R.id.navigation_home) {
                mFragmentPage.setCurrentItem(0);
            } else if (checkedId == R.id.navigation_settings) {
                mFragmentPage.setCurrentItem(1);
            } else if (checkedId == R.id.navigation_about) {
                mFragmentPage.setCurrentItem(2);
            }
        });

        mFragmentPage.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                changeSelect(position);
                mSearchView.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }


    private void changeSelect(int position) {
        switch (position) {
            case 0 -> {
                mHomeNav.setChecked(true);
                mFragmentPage.setCurrentItem(0);
                setTitle(R.string.app_name);
            }
            case 1 -> {
                mSettingsNav.setChecked(true);
                mFragmentPage.setCurrentItem(1);
                setTitle(R.string.settings);
            }
            case 2 -> {
                mAboutNav.setChecked(true);
                mFragmentPage.setCurrentItem(2);
                setTitle(R.string.about);
            }
        }
    }

    private void onSearchItemClickListener(ModData ad) {
        Bundle args = new Bundle();
        args.putString(":settings:fragment_args_key", ad.key);
        SettingLauncherHelper.onStartSettingsForArguments(
            this,
            SubSettings.class,
            ad.fragment,
            args,
            ad.catTitleResId
        );
    }

    private SearchActionMode startSearchMode() {
        return SearchModeHelper.startSearchMode(
            this,
            mSearchResultView,
            mFragmentPage,
            mSearchView,
            findViewById(android.R.id.list_container),
            mSearchResultListener
        );
    }

    TextWatcher mSearchResultListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            findMod(s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {
            findMod(s.toString());
        }
    };

    void findMod(String filter) {
        lastFilter = filter;
        mSearchResultView.setVisibility(filter.equals("") ? View.GONE : View.VISIBLE);
        ModSearchAdapter adapter = (ModSearchAdapter) mSearchResultView.getAdapter();
        if (adapter == null) return;
        adapter.getFilter().filter(filter);
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat preferenceFragmentCompat, @NonNull Preference preference) {
        mProxy.onStartSettingsForArguments(SubSettings.class, preference, false);
        return true;
    }
}
