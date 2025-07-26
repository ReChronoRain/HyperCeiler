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

  * Copyright (C) 2023-2025 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.common.prefs;

import static com.sevtinge.hyperceiler.hook.utils.devicesdk.DeviceSDKKt.getLanguage;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TipsPreference extends Preference {

    private final Context mContext;

    public TipsPreference(@NonNull Context context) {
        this(context, null);
    }

    public TipsPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TipsPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context.getApplicationContext();
        setEnabled(false);
        updateTips();
    }

    public void updateTips() {
        String tip = getRandomTip(mContext);
        setSummary("Tip: " + tip);
    }

    private static final Object tipsCacheLock = new Object();
    private static List<String> cachedTips = null;
    private static String cachedLanguage = null;

    public static String getRandomTip(Context context) {
        String language = getLanguage();
        List<String> tipsList;

        synchronized (tipsCacheLock) {
            if (cachedTips != null && language.equals(cachedLanguage)) {
                tipsList = new ArrayList<>(cachedTips);
            } else {
                tipsList = loadTips(context, language);
                if (!tipsList.isEmpty()) {
                    cachedTips = new ArrayList<>(tipsList);
                    cachedLanguage = language;
                }
            }
        }

        if (tipsList.isEmpty()) {
            return "Get random tip is empty.";
        }

        Random random = new Random();
        String randomTip = "";
        int attempts = 0;
        while (randomTip.isEmpty() && !tipsList.isEmpty() && attempts < tipsList.size()) {
            int randomIndex = random.nextInt(tipsList.size());
            randomTip = tipsList.get(randomIndex);
            tipsList.remove(randomIndex);
            attempts++;
        }

        return !randomTip.isEmpty() ? randomTip : "Get random tip is empty.";
    }

    private static List<String> loadTips(Context context, String language) {
        AssetManager assetManager = context.getAssets();
        String fileName = "tips/tips-" + language;
        List<String> tipsList = new ArrayList<>();

        try (InputStream inputStream = openTipsFile(assetManager, fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().startsWith("//") && !line.trim().isEmpty()) {
                    tipsList.add(line);
                }
            }
        } catch (IOException e) {
            AndroidLogUtils.logE("MainActivityContextHelper", "getRandomTip() error: " + e.getMessage());
        }
        return tipsList;
    }

    private static InputStream openTipsFile(AssetManager assetManager, String fileName) throws IOException {
        try {
            return assetManager.open(fileName);
        } catch (IOException ex) {
            return assetManager.open("tips/tips");
        }
    }
}
