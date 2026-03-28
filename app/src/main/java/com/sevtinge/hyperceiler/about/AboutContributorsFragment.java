package com.sevtinge.hyperceiler.about;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.sevtinge.hyperceiler.R;
import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.dashboard.SettingsPreferenceFragment;
import com.sevtinge.hyperceiler.utils.GithubUserContentGetter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

public class AboutContributorsFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceClickListener {

    PreferenceCategory mContributors;
    PreferenceCategory mProcessing;
    PreferenceCategory mLoadFailed;

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_about_contributor;
    }

    private PreferenceCategory mContributorsCategory;

    @Override
    public void initPrefs() {
        mContributorsCategory = findPreference("prefs_key_about_contributors");
        mContributors = findPreference("prefs_key_about_contributors");
        mProcessing = findPreference("prefs_key_about_contributors_progressing");
        mLoadFailed = findPreference("prefs_key_about_contributors_load_failed");

        if (mContributorsCategory == null) return;

        new Thread(() -> {
            ArrayList<String> contributors = getContributors(
                "rechronorain", "hyperceiler",
                "rechronorain", "cemiuiler"
            );

            if (contributors.isEmpty()) {
                mProcessing.setVisible(false);
                mLoadFailed.setVisible(true);
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    mContributorsCategory.removeAll();
                    for (String item : contributors) {
                        String[] parts = item.split(",", 2);
                        String login = parts[0].replace("@", "");
                        String nickname = parts.length > 1 ? parts[1] : login;

                        if (login.equalsIgnoreCase("Sevtinge")) {
                            continue;
                        }

                        Preference p = new Preference(getContext());
                        p.setTitle(nickname);
                        p.setSummary("@" + login);
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/" + login));
                        p.setIntent(intent);

                        mContributorsCategory.addPreference(p);
                    }

                    mProcessing.setVisible(false);
                    mContributors.setVisible(true);
                });
            }
        }).start();
    }

    public static ArrayList<String> getContributors(String owner1, String repo1, String owner2, String repo2) {

        ArrayList<String> list1 = new ArrayList<>();
        ArrayList<String> list2 = new ArrayList<>();

        CountDownLatch latch = new CountDownLatch(2);

        new Thread(() -> {
            try {
                ArrayList<String> temp = GithubUserContentGetter.getContributors(owner1, repo1);
                synchronized (list1) {
                    list1.addAll(temp);
                }
            } finally {
                latch.countDown();
            }
        }).start();

        new Thread(() -> {
            try {
                ArrayList<String> temp = GithubUserContentGetter.getContributors(owner2, repo2);
                synchronized (list2) {
                    list2.addAll(temp);
                }
            } finally {
                latch.countDown();
            }
        }).start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            AndroidLog.w("AboutContributorsFragment", "Get contributors failed by" + e);
        }

        HashSet<String> seenLogins = new HashSet<>();
        ArrayList<String> finalList = new ArrayList<>();

        for (String item : list1) {
            String login = item.split(",", 2)[0];
            if (seenLogins.add(login)) {
                finalList.add(item);
            }
        }

        for (String item : list2) {
            String login = item.split(",", 2)[0];
            if (seenLogins.add(login)) {
                finalList.add(item);
            }
        }

        return finalList;
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        return false;
    }
}
