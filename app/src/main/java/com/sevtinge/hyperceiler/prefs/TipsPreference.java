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
package com.sevtinge.hyperceiler.prefs;

import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getLanguage;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.logE;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.sevtinge.hyperceiler.ui.main.utils.MainActivityContextHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TipsPreference extends Preference {

    Context mContext;
    MainActivityContextHelper mainActivityContextHelper;

    public TipsPreference(@NonNull Context context) {
        this(context, null);
    }

    public TipsPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TipsPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        setEnabled(false);
        updateTips();
    }

    public void updateTips() {
        String tip = getRandomTip(mContext);
        setSummary("Tip: " + tip);
    }

    public static String getRandomTip(Context context) {
        AssetManager assetManager = context.getAssets();
        String fileName = "tips/tips-" + getLanguage();
        List<String> tipsList = new ArrayList<>();

        try {
            InputStream inputStream;
            try {
                inputStream = assetManager.open(fileName);
            } catch (IOException ex) {
                inputStream = assetManager.open("tips/tips");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().startsWith("//")) {
                    tipsList.add(line);
                }
            }

            reader.close();
            inputStream.close();

            Random random = new Random();
            String randomTip = "";
            while (randomTip.isEmpty() && !tipsList.isEmpty()) {
                int randomIndex = random.nextInt(tipsList.size());
                randomTip = tipsList.get(randomIndex);
                tipsList.remove(randomIndex);
            }

            if (!randomTip.isEmpty()) {
                return randomTip;
            } else {
                return "Get random tip is empty.";
            }
        } catch (IOException e) {
            logE("MainActivityContextHelper", "getRandomTip() error: " + e.getMessage());
            return "error";
        }
    }
}
