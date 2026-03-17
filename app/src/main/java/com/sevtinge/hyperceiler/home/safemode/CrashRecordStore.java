package com.sevtinge.hyperceiler.home.safemode;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sevtinge.hyperceiler.common.log.AndroidLog;
import com.sevtinge.hyperceiler.common.utils.CrashIntentContract;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

public final class CrashRecordStore {

    public static final String EXTRA_RECORD_ID = "crash_record_id";

    public static final String SOURCE_APP = "app";
    public static final String SOURCE_HOOK = "hook";

    private static final String TAG = "CrashRecordStore";
    private static final String CRASH_DIR = "log/crash";

    private static final String HEADER = "HyperCeiler Crash Record";
    private static final String SECTION_META = "[Meta]";
    private static final String SECTION_MESSAGE = "[Message]";
    private static final String SECTION_STACK_TRACE = "[StackTrace]";

    private static final String KEY_SOURCE = "source";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_TYPE = "type";
    private static final String KEY_FILE = "file";
    private static final String KEY_CLASS = "class";
    private static final String KEY_METHOD = "method";
    private static final String KEY_LINE = "line";
    private static final String KEY_TIME = "time";
    private static final String KEY_STACK = "stack";
    private static final String KEY_PACKAGE_ALIAS = "package_alias";
    private static final String KEY_PACKAGE_NAME = "package_name";
    private static final String KEY_RECORD_ID = "record_id";

    private static final SimpleDateFormat DISPLAY_TIME_FORMAT =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.getDefault());

    private CrashRecordStore() {}

    @Nullable
    public static CrashRecord persistAppCrash(Context context, Throwable throwable) {
        StackTraceElement element = getTopStackTraceElement(throwable);
        CrashRecord record = new CrashRecord(
            buildRecordId(SOURCE_APP),
            SOURCE_APP,
            throwable.getMessage(),
            throwable.getClass().getName(),
            element != null ? element.getFileName() : null,
            element != null ? element.getClassName() : null,
            element != null ? element.getMethodName() : null,
            element != null ? element.getLineNumber() : -1,
            System.currentTimeMillis(),
            Log.getStackTraceString(throwable),
            null,
            null
        );
        return write(context, record);
    }

    @Nullable
    public static CrashRecord persistHookCrash(Context context, Intent intent) {
        String alias = intent.getStringExtra(CrashIntentContract.KEY_PKG_ALIAS);
        CrashRecord record = new CrashRecord(
            buildRecordId(SOURCE_HOOK),
            SOURCE_HOOK,
            intent.getStringExtra(CrashIntentContract.KEY_LONG_MSG),
            intent.getStringExtra(CrashIntentContract.KEY_THROW_CLASS),
            intent.getStringExtra(CrashIntentContract.KEY_THROW_FILE),
            intent.getStringExtra(CrashIntentContract.KEY_THROW_CLASS),
            intent.getStringExtra(CrashIntentContract.KEY_THROW_METHOD),
            intent.getIntExtra(CrashIntentContract.KEY_THROW_LINE, -1),
            System.currentTimeMillis(),
            intent.getStringExtra(CrashIntentContract.KEY_STACK_TRACE),
            alias,
            alias
        );
        return write(context, record);
    }

    @Nullable
    public static CrashRecord read(Context context, @Nullable String recordId) {
        if (recordId == null || recordId.isEmpty()) {
            return null;
        }

        File file = new File(getCrashDir(context), recordId);
        if (!file.isFile()) {
            return null;
        }

        try {
            String content = readFile(file);
            CrashRecord readableRecord = parseReadableRecord(recordId, content);
            if (readableRecord != null) {
                return readableRecord;
            }
            return parseLegacyPropertiesRecord(recordId, content);
        } catch (IOException e) {
            AndroidLog.e(TAG, "Failed to read crash record: " + recordId, e);
            return null;
        }
    }

    public static void fillIntent(Intent intent, @NonNull CrashRecord record) {
        intent.putExtra(EXTRA_RECORD_ID, record.recordId);
        intent.putExtra("crash_message", record.message);
        intent.putExtra("crash_type", record.type);
        intent.putExtra("crash_file", record.fileName);
        intent.putExtra("crash_class", record.className);
        intent.putExtra("crash_method", record.methodName);
        intent.putExtra("crash_line", record.lineNumber);
        intent.putExtra("crash_time", record.timeMillis);
        intent.putExtra("crash_stack", record.stackTrace);

        intent.putExtra(CrashIntentContract.KEY_LONG_MSG, record.message);
        intent.putExtra(CrashIntentContract.KEY_STACK_TRACE, record.stackTrace);
        intent.putExtra(CrashIntentContract.KEY_THROW_CLASS, record.className);
        intent.putExtra(CrashIntentContract.KEY_THROW_FILE, record.fileName);
        intent.putExtra(CrashIntentContract.KEY_THROW_LINE, record.lineNumber);
        intent.putExtra(CrashIntentContract.KEY_THROW_METHOD, record.methodName);
        intent.putExtra(CrashIntentContract.KEY_PKG_ALIAS, record.packageAlias);
    }

    @NonNull
    public static File getCrashDir(Context context) {
        return new File(context.getApplicationContext().getFilesDir(), CRASH_DIR);
    }

    public static void clearAll(Context context) {
        File crashDir = getCrashDir(context);
        if (!crashDir.exists()) {
            return;
        }
        deleteDirectory(crashDir);
    }

    @Nullable
    private static CrashRecord write(Context context, CrashRecord record) {
        File dir = getCrashDir(context);
        if (!dir.exists() && !dir.mkdirs()) {
            AndroidLog.e(TAG, "Failed to create crash dir: " + dir.getAbsolutePath());
            return null;
        }

        File file = new File(dir, record.recordId);
        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(HEADER);
            writer.newLine();
            writer.write("Version: 2");
            writer.newLine();
            writer.newLine();

            writer.write(SECTION_META);
            writer.newLine();
            writeMetaLine(writer, KEY_RECORD_ID, record.recordId);
            writeMetaLine(writer, KEY_SOURCE, record.source);
            writeMetaLine(writer, "time_readable", DISPLAY_TIME_FORMAT.format(new Date(record.timeMillis)));
            writeMetaLine(writer, KEY_TIME, String.valueOf(record.timeMillis));
            writeMetaLine(writer, KEY_TYPE, record.type);
            writeMetaLine(writer, KEY_FILE, record.fileName);
            writeMetaLine(writer, KEY_CLASS, record.className);
            writeMetaLine(writer, KEY_METHOD, record.methodName);
            writeMetaLine(writer, KEY_LINE, String.valueOf(record.lineNumber));
            writeMetaLine(writer, KEY_PACKAGE_ALIAS, record.packageAlias);
            writeMetaLine(writer, KEY_PACKAGE_NAME, record.packageName);
            writer.newLine();

            writer.write(SECTION_MESSAGE);
            writer.newLine();
            writeBlock(writer, record.message);
            writer.newLine();

            writer.write(SECTION_STACK_TRACE);
            writer.newLine();
            writeBlock(writer, record.stackTrace);

            return record;
        } catch (IOException e) {
            AndroidLog.e(TAG, "Failed to write crash record: " + file.getAbsolutePath(), e);
            return null;
        }
    }

    @Nullable
    private static CrashRecord parseReadableRecord(@NonNull String recordId, @NonNull String content) {
        if (!content.startsWith(HEADER)) {
            return null;
        }

        String source = null;
        String type = null;
        String file = null;
        String className = null;
        String method = null;
        String packageAlias = null;
        String packageName = null;
        long time = System.currentTimeMillis();
        int lineNumber = -1;

        StringBuilder message = new StringBuilder();
        StringBuilder stackTrace = new StringBuilder();
        String section = "";

        String[] lines = content.split("\\r?\\n", -1);
        for (String line : lines) {
            if (HEADER.equals(line) || line.startsWith("Version:")) {
                continue;
            }
            if (SECTION_META.equals(line)) {
                section = SECTION_META;
                continue;
            }
            if (SECTION_MESSAGE.equals(line)) {
                section = SECTION_MESSAGE;
                continue;
            }
            if (SECTION_STACK_TRACE.equals(line)) {
                section = SECTION_STACK_TRACE;
                continue;
            }

            if (SECTION_META.equals(section)) {
                int separator = line.indexOf(':');
                if (separator <= 0) {
                    continue;
                }
                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                switch (key) {
                    case KEY_SOURCE -> source = emptyToNull(value);
                    case KEY_TYPE -> type = emptyToNull(value);
                    case KEY_FILE -> file = emptyToNull(value);
                    case KEY_CLASS -> className = emptyToNull(value);
                    case KEY_METHOD -> method = emptyToNull(value);
                    case KEY_LINE -> lineNumber = parseInt(value, -1);
                    case KEY_TIME -> time = parseLong(value, System.currentTimeMillis());
                    case KEY_PACKAGE_ALIAS -> packageAlias = emptyToNull(value);
                    case KEY_PACKAGE_NAME -> packageName = emptyToNull(value);
                    default -> {
                    }
                }
                continue;
            }

            if (SECTION_MESSAGE.equals(section)) {
                appendLine(message, line);
                continue;
            }

            if (SECTION_STACK_TRACE.equals(section)) {
                appendLine(stackTrace, line);
            }
        }

        return new CrashRecord(
            recordId,
            source,
            emptyToNull(trimTrailingNewline(message.toString())),
            type,
            file,
            className,
            method,
            lineNumber,
            time,
            emptyToNull(trimTrailingNewline(stackTrace.toString())),
            packageAlias,
            packageName
        );
    }

    @Nullable
    private static CrashRecord parseLegacyPropertiesRecord(@NonNull String recordId, @NonNull String content) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(content));
            return new CrashRecord(
                recordId,
                properties.getProperty(KEY_SOURCE),
                properties.getProperty(KEY_MESSAGE),
                properties.getProperty(KEY_TYPE),
                properties.getProperty(KEY_FILE),
                properties.getProperty(KEY_CLASS),
                properties.getProperty(KEY_METHOD),
                parseInt(properties.getProperty(KEY_LINE), -1),
                parseLong(properties.getProperty(KEY_TIME), System.currentTimeMillis()),
                properties.getProperty(KEY_STACK),
                properties.getProperty(KEY_PACKAGE_ALIAS),
                properties.getProperty(KEY_PACKAGE_NAME)
            );
        } catch (IOException e) {
            AndroidLog.e(TAG, "Failed to parse legacy crash record: " + recordId, e);
            return null;
        }
    }

    @Nullable
    private static StackTraceElement getTopStackTraceElement(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return null;
        }
        return stackTrace[0];
    }

    @NonNull
    private static String buildRecordId(String source) {
        long now = System.currentTimeMillis();
        return String.format(Locale.US, "%s_%d_%d.log", source, now, System.nanoTime());
    }

    private static void writeMetaLine(BufferedWriter writer, String key, @Nullable String value) throws IOException {
        writer.write(key);
        writer.write(": ");
        writer.write(value == null ? "" : value);
        writer.newLine();
    }

    private static void writeBlock(BufferedWriter writer, @Nullable String content) throws IOException {
        if (content == null || content.isEmpty()) {
            return;
        }
        writer.write(content);
        if (!content.endsWith("\n")) {
            writer.newLine();
        }
    }

    @NonNull
    private static String readFile(File file) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    private static void appendLine(StringBuilder builder, String line) {
        builder.append(line).append('\n');
    }

    @NonNull
    private static String trimTrailingNewline(@NonNull String value) {
        int end = value.length();
        while (end > 0) {
            char c = value.charAt(end - 1);
            if (c != '\n' && c != '\r') {
                break;
            }
            end--;
        }
        return value.substring(0, end);
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
    }

    private static int parseInt(@Nullable String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static long parseLong(@Nullable String value, long fallback) {
        try {
            return value == null ? fallback : Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void deleteDirectory(@NonNull File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else if (!file.delete()) {
                    AndroidLog.w(TAG, "Failed to delete crash file: " + file.getAbsolutePath());
                }
            }
        }
        if (!directory.delete()) {
            AndroidLog.w(TAG, "Failed to delete crash directory: " + directory.getAbsolutePath());
        }
    }

    public static final class CrashRecord {
        @NonNull public final String recordId;
        @Nullable public final String source;
        @Nullable public final String message;
        @Nullable public final String type;
        @Nullable public final String fileName;
        @Nullable public final String className;
        @Nullable public final String methodName;
        public final int lineNumber;
        public final long timeMillis;
        @Nullable public final String stackTrace;
        @Nullable public final String packageAlias;
        @Nullable public final String packageName;

        private CrashRecord(
            @NonNull String recordId,
            @Nullable String source,
            @Nullable String message,
            @Nullable String type,
            @Nullable String fileName,
            @Nullable String className,
            @Nullable String methodName,
            int lineNumber,
            long timeMillis,
            @Nullable String stackTrace,
            @Nullable String packageAlias,
            @Nullable String packageName
        ) {
            this.recordId = recordId;
            this.source = source;
            this.message = message;
            this.type = type;
            this.fileName = fileName;
            this.className = className;
            this.methodName = methodName;
            this.lineNumber = lineNumber;
            this.timeMillis = timeMillis;
            this.stackTrace = stackTrace;
            this.packageAlias = packageAlias;
            this.packageName = packageName;
        }
    }
}
