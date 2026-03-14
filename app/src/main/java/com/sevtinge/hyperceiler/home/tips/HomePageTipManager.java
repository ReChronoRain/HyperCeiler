package com.sevtinge.hyperceiler.home.tips;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;

import com.sevtinge.hyperceiler.libhook.utils.api.DeviceHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomePageTipManager {
    private static final Random RANDOM = new Random();
    private static final Map<String, List<String>> TIPS_CACHE = new HashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface TipCallback {
        void onTipReady(String tip);
    }

    public static void getRandomTipAsync(Context context, TipCallback callback) {
        String lang = DeviceHelper.Hardware.getLanguage();
        EXECUTOR.execute(() -> {
            List<String> tips = getTipsFromAssets(context, lang);
            String selected = tips.isEmpty() ? "Enjoy using HyperCeiler!" : tips.get(RANDOM.nextInt(tips.size()));
            MAIN_HANDLER.post(() -> callback.onTipReady(selected));
        });
    }

    private static List<String> getTipsFromAssets(Context context, String lang) {
        synchronized (TIPS_CACHE) {
            if (TIPS_CACHE.containsKey(lang)) return TIPS_CACHE.get(lang);
            List<String> loaded = new ArrayList<>();
            AssetManager am = context.getAssets();
            String path = "tips/tips-" + lang;
            try (InputStream is = openFile(am, path);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("//")) loaded.add(line);
                }
                TIPS_CACHE.put(lang, List.copyOf(loaded));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return loaded;
        }
    }

    private static InputStream openFile(AssetManager am, String path) throws IOException {
        try {
            return am.open(path);
        } catch (IOException e) {
            return am.open("tips/tips");
        }
    }
}

