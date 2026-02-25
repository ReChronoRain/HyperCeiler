package fan.provider;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import java.util.List;

public class SettingsLiveData<T> extends LiveData<T> {
    private final Context context;
    private final Uri uri;
    private final String key;
    private final T defValue;
    private final Fetcher<T> fetcher;

    public interface Fetcher<T> { T fetch(String k, T d); }

    private final ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override public void onChange(boolean self) { loadValue(); }
    };

    public SettingsLiveData(Context c, Uri u, String k, T d, Fetcher<T> f) {
        this.context = c.getApplicationContext();
        this.uri = u; this.key = k; this.defValue = d; this.fetcher = f;
    }

    private void loadValue() { postValue(fetcher.fetch(key, defValue)); }

    @Override
    protected void onActive() {
        context.getContentResolver().registerContentObserver(uri, false, observer);
        loadValue();
    }

    @Override
    protected void onInactive() {
        context.getContentResolver().unregisterContentObserver(observer);
    }

    public static SettingsLiveData<String> getString(Context c, Uri u, String k, String d) {
        return new SettingsLiveData<>(c, u, k, d, (key, def) -> {
            // 注意：这里读取不要用 Settings.Global.getString 以免造成递归监听，直接查 Resolver
            return queryRawValue(c, u, key, def);
        });
    }

    public static SettingsLiveData<Boolean> getBoolean(Context c, Uri u, String k, boolean d) {
        return new SettingsLiveData<>(c, u, k, d, (key, defValue) -> {
            // 从 Provider 获取原始字符串
            String raw = queryRawValue(c, u, key, null);
            // 转换为 boolean
            return raw != null ? Boolean.parseBoolean(raw) : defValue;
        });
    }

    public static <E> SettingsLiveData<List<E>> getList(Context c, Uri u, String k, List<E> d, Class<E> clazz) {
        return new SettingsLiveData<>(c, u, k, d, (key, def) -> {
            String raw = queryRawValue(c, u, key, null);
            List<E> list = Settings.ListConverter.fromString(raw, clazz);
            return (list.isEmpty() && raw == null) ? def : list;
        });
    }

    private static String queryRawValue(Context c, Uri u, String key, String def) {
        Cursor cursor = c.getContentResolver().query(u, new String[]{"value"}, "name=?", new String[]{key}, null);
        try {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        } finally { if (cursor != null) cursor.close(); }
        return def;
    }
}
