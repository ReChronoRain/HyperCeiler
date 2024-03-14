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

 * Copyright (C) 2023-2024 HyperCeiler Contributions
 */
package com.sevtinge.hyperceiler.ui.base;

import static com.sevtinge.hyperceiler.utils.Helpers.isDarkMode;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.sevtinge.hyperceiler.ui.fragment.settings.ModuleSettingsFragment;
import com.sevtinge.hyperceiler.utils.DialogHelper;
import com.sevtinge.hyperceiler.utils.SettingLauncherHelper;
import com.sevtinge.hyperceiler.utils.blur.MiBlurUtils;
import com.sevtinge.hyperceiler.utils.search.SearchModeHelper;

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
        setRestartView(view -> DialogHelper.showRestartDialog(this));
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

        ViewCompat.setOnApplyWindowInsetsListener(mSearchResultView, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets inset = Insets.max(insets.getInsets(WindowInsetsCompat.Type.systemBars()),
                        insets.getInsets(WindowInsetsCompat.Type.displayCutout()));
                v.setPadding(0, 0, 0, inset.bottom);
                return insets;
            }
        });
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

        Context context = this;
        addPaddingForRadioButton(mHomeNav, context);
        addPaddingForRadioButton(mSettingsNav, context);
        addPaddingForRadioButton(mAboutNav, context);

        int i;
        if (isDarkMode(this)) i = 160;
        else i = 200;
        int a;
        if (isDarkMode(this)) a = 100;
        else a = 140;
        MiBlurUtils.setContainerPassBlur(mNavigationView, i);
        MiBlurUtils.setMiViewBlurMode(mNavigationView, 3);
        MiBlurUtils.clearMiBackgroundBlendColor(mNavigationView);
        MiBlurUtils.addMiBackgroundBlendColor(mNavigationView, Color.argb(a, 0, 0, 0), 103);

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

    private void addPaddingForRadioButton(View view, Context context) {
        ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                Insets inset = Insets.max(insets.getInsets(WindowInsetsCompat.Type.systemBars()),
                        insets.getInsets(WindowInsetsCompat.Type.displayCutout()));
                v.setPadding(0, dpToPx(10, context), 0, inset.bottom + dpToPx(18, context));
                return insets;
            }
        });
    }

    private static int dpToPx(int dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
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
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            findMod(s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {
            // findMod(s.toString());
        }
    };

    void findMod(String filter) {
        lastFilter = filter;
        mSearchResultView.setVisibility(filter.isEmpty() ? View.GONE : View.VISIBLE);
        ModSearchAdapter adapter = (ModSearchAdapter) mSearchResultView.getAdapter();
        if (adapter == null) return;
        adapter.getFilter(NavigationActivity.this).filter(filter);
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat preferenceFragmentCompat, @NonNull Preference preference) {
        mProxy.onStartSettingsForArguments(SubSettings.class, preference, false);
        return true;
    }
}
