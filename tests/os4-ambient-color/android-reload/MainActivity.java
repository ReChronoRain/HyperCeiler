package local.hyperceiler.ambientreloadprobe;

import android.app.Activity;
import android.os.Bundle;
import dalvik.system.DexClassLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class MainActivity extends Activity {
    private final JSONArray checks = new JSONArray();
    private final JSONObject observations = new JSONObject();
    // Strong references intentionally retain both generations during the attempt.
    private final ClassLoader[] generations = new ClassLoader[2];
    private void check(String name, boolean result) throws Exception {
        checks.put(new JSONObject().put("name", name).put("passed", result));
    }
    public void onCreate(Bundle b) {
        super.onCreate(b);
        new Thread(this::runProbe, "same-native-path-reload").start();
    }
    private void runProbe() {
        try {
            File dex = new File(getFilesDir(), "helper.dex");
            if (dex.exists() && !dex.delete()) throw new IOException("old dex not removed");
            try (InputStream in = getAssets().open("helper.dex"); FileOutputStream out = new FileOutputStream(dex)) {
                if (!dex.setReadOnly()) throw new IOException("read-only dex required");
                byte[] buf = new byte[8192];
                for (int n; (n = in.read(buf)) >= 0;) out.write(buf, 0, n);
            }
            String nativeDir = getApplicationInfo().nativeLibraryDir;
            String path = nativeDir + "/libHyperCeilerAmbientOS4.so";
            String name = "com.sevtinge.hyperceiler.libhook.rules.systemui.controlcenter.media3.AmbientNativeColorOS4";
            for (int i = 0; i < 2; i++) generations[i] = new DexClassLoader(
                dex.getPath(), getCodeCacheDir().getPath(), nativeDir, getClassLoader().getParent());
            Class<?> first = Class.forName(name, true, generations[0]);
            Class<?> second = Class.forName(name, true, generations[1]);
            check("two distinct actual helper classes", first != second);
            check("first defining loader exact", first.getClassLoader() == generations[0]);
            check("second defining loader exact", second.getClassLoader() == generations[1]);
            Method a = first.getMethod("loadAndCall", String.class);
            Method c = second.getMethod("loadAndCall", String.class);
            check("first load and actual bridge invalid-input JNI returns zero", ((Long) a.invoke(null, path)) == 0L);
            check("same loader repeated native load succeeds", ((Long) a.invoke(null, path)) == 0L);
            Throwable failure = null;
            try { c.invoke(null, path); }
            catch (InvocationTargetException x) { failure = x.getCause(); }
            observations.put("second_loader_failure", failure == null ? JSONObject.NULL : failure.toString());
            observations.put("native_path", path);
            check("second live loader rejected by ART", failure instanceof UnsatisfiedLinkError);
            check("failure states another classloader already loaded library", failure != null
                && failure.getMessage().contains("already opened by ClassLoader"));
            check("first generation JNI remains callable after rejection", ((Long) a.invoke(null, path)) == 0L);
        } catch (Throwable t) {
            try { check("probe execution completed", false); observations.put("exception", t.toString()); }
            catch (Exception impossible) { throw new RuntimeException(impossible); }
        }
        try {
            boolean passed = true;
            for (int i = 0; i < checks.length(); i++) passed &= checks.getJSONObject(i).getBoolean("passed");
            JSONObject report = new JSONObject().put("passed", passed).put("checks", checks)
                .put("observations", observations).put("device_status", "NOT_TESTED")
                .put("scope", "API37 ART, two concurrently retained DexClassLoaders, identical helper DEX and exact same production bridge path. JNI invalid-input guard only; no OEM algorithm/private-instance initialization and no LSPosed lifecycle executed.");
            try (FileOutputStream out = new FileOutputStream(new File(getFilesDir(), "report.json"))) {
                out.write(report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (Exception e) { android.util.Log.e("AmbientReloadProbe", "report", e); }
    }
}
