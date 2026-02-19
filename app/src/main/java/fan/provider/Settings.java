package fan.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.BaseColumns;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class Settings {
    public static final String AUTHORITY = "fan.provider.settings";
    public static final String PERMISSION_READ = "fan.permission.READ_SETTINGS";
    public static final String PERMISSION_WRITE = "fan.permission.WRITE_SETTINGS";

    /**
     * 基础表结构定义
     */
    public static class NameValueTable implements BaseColumns {
        public static final String NAME = "name";
        public static final String VALUE = "value";

        protected static boolean putString(ContentResolver resolver, Uri uri, String name, String value) {
            try {
                ContentValues values = new ContentValues();
                values.put(NAME, name);
                values.put(VALUE, value);
                return resolver.insert(uri, values) != null;
            } catch (Exception e) {
                return false;
            }
        }

        public static Uri getUriFor(Uri contentUri, String name) {
            return Uri.withAppendedPath(contentUri, name);
        }
    }

    /**
     * 内部缓存逻辑：减少跨进程 IPC 调用，通过 Observer 实现自动失效同步
     */
    private static class NameValueCache {
        private final Uri mUri;
        private final HashMap<String, String> mValues = new HashMap<>();
        private ContentObserver mObserver;

        NameValueCache(Uri uri) {
            this.mUri = uri;
        }

        public String getString(ContentResolver cr, String name) {
            synchronized (this) {
                if (mValues.containsKey(name)) return mValues.get(name);

                if (mObserver == null) {
                    mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                        @Override
                        public void onChange(boolean selfChange) {
                            synchronized (NameValueCache.this) {
                                mValues.clear();
                            }
                        }
                    };
                    cr.registerContentObserver(mUri, true, mObserver);
                }

                String value = null;
                Cursor c = null;
                try {
                    c = cr.query(mUri, new String[]{"value"}, "name=?", new String[]{name}, null);
                    if (c != null && c.moveToFirst()) value = c.getString(0);
                } catch (Exception e) {
                    return null;
                } finally {
                    if (c != null) c.close();
                }

                mValues.put(name, value);
                return value;
            }
        }
    }

    /**
     * 列表转换工具类
     */
    public static class ListConverter {
        public static String toString(List<?> list) {
            return (list == null) ? null : new JSONArray(list).toString();
        }

        public static <T> List<T> fromString(String json, Class<T> clazz) {
            List<T> list = new ArrayList<>();
            if (TextUtils.isEmpty(json)) return list;
            try {
                JSONArray ja = new JSONArray(json);
                for (int i = 0; i < ja.length(); i++) {
                    Object item = ja.get(i);
                    if (clazz.isInstance(item)) {
                        list.add(clazz.cast(item));
                    } else if (clazz == Long.class && item instanceof Integer) {
                        list.add(clazz.cast(((Integer) item).longValue()));
                    }
                }
            } catch (Exception ignored) {
            }
            return list;
        }
    }

    /**
     * 公共业务逻辑基类
     */
    public static abstract class SettingsBase extends NameValueTable {
        protected abstract Uri getContentUri();

        protected abstract NameValueCache getCache();

        // --- String ---
        public String getString(ContentResolver cr, String name) {
            return getCache().getString(cr, name);
        }

        public boolean putString(ContentResolver cr, String name, String value) {
            return putString(cr, getContentUri(), name, value);
        }

        // --- Int ---
        public int getInt(ContentResolver cr, String name, int def) {
            String v = getString(cr, name);
            try {
                return v != null ? Integer.parseInt(v) : def;
            } catch (Exception e) {
                return def;
            }
        }

        public boolean putInt(ContentResolver cr, String name, int value) {
            return putString(cr, name, Integer.toString(value));
        }

        // --- Long ---
        public long getLong(ContentResolver cr, String name, long def) {
            String v = getString(cr, name);
            try {
                return v != null ? Long.parseLong(v) : def;
            } catch (Exception e) {
                return def;
            }
        }

        public boolean putLong(ContentResolver cr, String name, long value) {
            return putString(cr, name, Long.toString(value));
        }

        // --- Boolean ---
        public boolean getBoolean(ContentResolver cr, String name, boolean def) {
            String v = getString(cr, name);
            return v != null ? Boolean.parseBoolean(v) : def;
        }

        // 在 Settings.java 的 Global 内部类中添加
        public LiveData<Boolean> getBooleanLiveData(Context c, String name, boolean def) {
            return SettingsLiveData.getBoolean(c, getUriFor(name), name, def);
        }

        public boolean putBoolean(ContentResolver cr, String name, boolean value) {
            return putString(cr, name, Boolean.toString(value));
        }

        // --- Float ---
        public float getFloat(ContentResolver cr, String name, float def) {
            String v = getString(cr, name);
            try {
                return v != null ? Float.parseFloat(v) : def;
            } catch (Exception e) {
                return def;
            }
        }

        public boolean putFloat(ContentResolver cr, String name, float value) {
            return putString(cr, name, Float.toString(value));
        }

        // --- List Support ---
        public <E> boolean putList(ContentResolver cr, String name, List<E> list) {
            return putString(cr, name, ListConverter.toString(list));
        }

        public List<String> getStringList(ContentResolver cr, String name) {
            return ListConverter.fromString(getString(cr, name), String.class);
        }

        public List<Long> getLongList(ContentResolver cr, String name) {
            return ListConverter.fromString(getString(cr, name), Long.class);
        }

        // --- LiveData Support ---
        public LiveData<String> getStringLiveData(Context c, String name, String def) {
            return SettingsLiveData.getString(c, getUriFor(getContentUri(), name), name, def);
        }

        public <E> LiveData<List<E>> getListLiveData(Context c, String name, List<E> def, Class<E> clazz) {
            return SettingsLiveData.getList(c, getUriFor(getContentUri(), name), name, def, clazz);
        }

        public Uri getUriFor(String name) {
            return getUriFor(getContentUri(), name);
        }
    }

    // --- 三大作用域单例实现 ---

    public static final Global Global = new Global();

    public static final class Global extends SettingsBase {
        private static final Uri URI = Uri.parse("content://" + AUTHORITY + "/global");
        private static final NameValueCache CACHE = new NameValueCache(URI);

        @Override
        protected Uri getContentUri() {
            return URI;
        }

        @Override
        protected NameValueCache getCache() {
            return CACHE;
        }
    }

    public static final Secure Secure = new Secure();

    public static final class Secure extends SettingsBase {
        private static final Uri URI = Uri.parse("content://" + AUTHORITY + "/secure");
        private static final NameValueCache CACHE = new NameValueCache(URI);

        @Override
        protected Uri getContentUri() {
            return URI;
        }

        @Override
        protected NameValueCache getCache() {
            return CACHE;
        }
    }

    public static final System System = new System();

    public static final class System extends SettingsBase {
        private static final Uri URI = Uri.parse("content://" + AUTHORITY + "/system");
        private static final NameValueCache CACHE = new NameValueCache(URI);

        @Override
        protected Uri getContentUri() {
            return URI;
        }

        @Override
        protected NameValueCache getCache() {
            return CACHE;
        }
    }
}
