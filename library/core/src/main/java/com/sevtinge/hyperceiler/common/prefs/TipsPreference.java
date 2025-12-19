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
import android.os.Handler;
import android.os.Looper;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        updateTips();
    }

    @Override
    public void onClick() {
        super.onClick();
        updateTips();
    }

    public void updateTips() {
        getRandomTipAsync(mContext, getLanguage(), tip -> {
            setSummary("Tip: " + tip);
        });
    }


    private static final Random RANDOM = new Random();
    private static final Object TIPS_CACHE_LOCK = new Object();
    private static final Map<String, List<String>> TIPS_CACHE = new HashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static void getRandomTipAsync(Context context, String language, TipCallback callback) {
        List<String> tips = getTips(context, language);
        if (tips.isEmpty()) {
            callback.onTipReady("Get random tip is empty.");
            return;
        }

        EXECUTOR.execute(() -> {
            String tip = tips.get(RANDOM.nextInt(tips.size()));
            MAIN_HANDLER.post(() -> callback.onTipReady(tip));
        });
    }

    public static List<String> getTips(Context context, String language) {
        List<String> cached = TIPS_CACHE.get(language);
        if (cached != null) {
            return cached;
        }

        List<String> loaded = loadTips(context, language);
        if (loaded == null || loaded.isEmpty()) {
            return Collections.emptyList();
        }

        synchronized (TIPS_CACHE_LOCK) {
            List<String> again = TIPS_CACHE.get(language);
            if (again != null) {
                return again;
            }

            List<String> immutable = List.copyOf(loaded);
            TIPS_CACHE.put(language, immutable);
            return immutable;
        }
    }

    private static List<String> loadTips(Context context, String language) {
        AssetManager assetManager = context.getAssets();
        String fileName = "tips/tips-" + language;
        List<String> tipsList = new ArrayList<>();

        try (InputStream inputStream = openTipsFile(assetManager, fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("//")) {
                    tipsList.add(line);
                }
            }
        } catch (IOException e) {
            AndroidLogUtils.logE(
                "MainActivityContextHelper",
                "loadTips error: " + e.getMessage()
            );
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

    public interface TipCallback {
        void onTipReady(String tip);
    }
}
