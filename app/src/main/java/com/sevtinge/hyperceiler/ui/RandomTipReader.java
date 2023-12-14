package com.sevtinge.hyperceiler.ui;

import static com.sevtinge.hyperceiler.utils.devicesdk.DeviceSDKKt.getLocale;
import static com.sevtinge.hyperceiler.utils.log.XposedLogUtils.*;

import android.content.res.AssetManager;
import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomTipReader {

    private Context context;

    public RandomTipReader(Context context) {
        this.context = context;
    }

    public String getRandomTip() {
        AssetManager assetManager = context.getAssets();
        String fileName = "tips/tips_" + getLocale();
        List<String> tipsList = new ArrayList<>();

        try {
            InputStream inputStream;
            try {
                inputStream = assetManager.open(fileName);
            } catch (Exception e) {
                inputStream = assetManager.open("tips/tips");
                logW("RandomTipReader", "Cannot found assets/" + fileName + " file, use default file.");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                tipsList.add(line);
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
            logE("RandomTipReader", "getRandomTip() error: " + e.getMessage());
            return "error";
        }
    }
}
