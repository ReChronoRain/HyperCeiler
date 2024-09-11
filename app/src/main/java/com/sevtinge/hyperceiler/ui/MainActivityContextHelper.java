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

  * Copyright (C) 2023-2024 HyperCeiler Contributions
*/
package com.sevtinge.hyperceiler.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;

import com.sevtinge.hyperceiler.R;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivityContextHelper {

    private Context context;

    public MainActivityContextHelper(Context context) {
        this.context = context;
    }

    @SuppressLint("HardwareIds")
    public String getAndroidId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public boolean verifyDexCRC() {
        String dexCrcStr = context.getResources().getString(R.string.crc_value);
        if(dexCrcStr.startsWith("Error")) return false;
        //long dexCrc = Long.parseLong("613BD799");

        //String orginalCrc = getString(R.string.str_code);
        ZipFile zf;
        try {
            zf = new ZipFile(context.getApplicationContext().getPackageCodePath());
            ZipEntry ze = zf.getEntry("classes.dex");
            long decCrc = ze.getCrc();
            String strCrc = Long.toHexString(decCrc);
            strCrc = strCrc.toUpperCase();
            //String MD5Crc = MD5Util.GetMD5Code(strCrc);
            //Log.e("checkcrc", MD5Crc);
            /*
            if (dexCrcStr.equals(strCrc)) {
                ActivityManagerUtil.getScreenManager().removeAllActivity();
                return true;
            } else return false;
             */
            return dexCrcStr.equals(strCrc);
        } catch (IOException e) {
            return false;
        }
    }

    public String getDexCRC() {
        String dexCrcStr = context.getResources().getString(R.string.crc_value);
        if(dexCrcStr.startsWith("Error")) return dexCrcStr;
        //long dexCrc = Long.parseLong("613BD799");

        //String orginalCrc = getString(R.string.str_code);
        ZipFile zf;
        try {
            zf = new ZipFile(context.getApplicationContext().getPackageCodePath());
            ZipEntry ze = zf.getEntry("classes.dex");
            long decCrc = ze.getCrc();
            String strCrc = Long.toHexString(decCrc);
            strCrc = strCrc.toUpperCase();
            //String MD5Crc = MD5Util.GetMD5Code(strCrc);
            //Log.e("checkcrc", MD5Crc);
            if (dexCrcStr.equals(strCrc)) {
                //ActivityManagerUtil.getScreenManager().removeAllActivity();
                return dexCrcStr + " = " + strCrc;
            } else return dexCrcStr + " != " + strCrc;
        } catch (IOException e) {
            return e.toString();
        }
    }
}
