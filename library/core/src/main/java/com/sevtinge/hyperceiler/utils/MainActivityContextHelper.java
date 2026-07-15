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

  * Copyright (C) 2023-2026 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import com.sevtinge.hyperceiler.core.R;

import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class MainActivityContextHelper {

    private static final String DEX_ENTRY_NAME = "classes.dex";

    private MainActivityContextHelper() {}

    @SuppressLint("HardwareIds")
    public static String getAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static boolean verifyDexCRC(Context context) {
        String expectedDexCrc = context.getString(R.string.crc_value);
        if (expectedDexCrc.startsWith("Error")) {
            return false;
        }

        try {
            return expectedDexCrc.equals(readDexCrc(context));
        } catch (IOException e) {
            return false;
        }
    }

    public static String getDexCRC(Context context) {
        String expectedDexCrc = context.getString(R.string.crc_value);
        if (expectedDexCrc.startsWith("Error")) {
            return expectedDexCrc;
        }

        try {
            String actualDexCrc = readDexCrc(context);
            String comparison = expectedDexCrc.equals(actualDexCrc) ? " = " : " != ";
            return expectedDexCrc + comparison + actualDexCrc;
        } catch (IOException e) {
            return e.toString();
        }
    }

    private static String readDexCrc(Context context) throws IOException {
        String packageCodePath = context.getApplicationContext().getPackageCodePath();
        try (ZipFile zipFile = new ZipFile(packageCodePath)) {
            ZipEntry dexEntry = zipFile.getEntry(DEX_ENTRY_NAME);
            if (dexEntry == null) {
                throw new IOException(DEX_ENTRY_NAME + " not found");
            }
            return Long.toHexString(dexEntry.getCrc()).toUpperCase(Locale.ROOT);
        }
    }
}
